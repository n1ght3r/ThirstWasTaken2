"""Runs `runBenchmark` over several nodes, several times over, and keeps every report.

    python tools/benchmark/bench.py --repeats 3
    python tools/benchmark/bench.py --nodes 26.3.x,26.3.x-neoforge --repeats 5 --profile quick

One run of one node answers "what did this cost just now". The benchmark's own notes put run-to-run
noise at 25-30% on an interaction's mean and 9-20% on the allocation figures, so one run cannot
answer "is this different from before". This runs the same thing k times and keeps every report for
`aggregate.py` to reduce to a median and a spread.

Three things it does on purpose:

- **Compiles everything first.** Gradle compiling a node between two timed runs hands the next run a
  different machine from the one the last run measured.
- **Repeats in the outer loop**, nodes in the inner one. Every node gets a run early, one in the
  middle and one late, so a machine that warms up, throttles or gets busier over the hour biases all
  of them the same way instead of only the ones at the end.
- **One Gradle invocation per run.** `org.gradle.parallel=true` would otherwise start two dedicated
  servers at once, and the second one dies on port 25565 -- or worse, does not, and they measure each
  other.

Reports land in `<out>/<node>/<profile>-<run>.json`, copied out of `run/<node>/benchmark/latest.json`
while it is still that run's. The exit code is 1 when any run did not finish with `status: ok`.
"""

import argparse
import datetime
import json
import os
import pathlib
import shutil
import subprocess
import sys
import time

ROOT = pathlib.Path(__file__).resolve().parents[2]

# tools/node_names.py: what a node's name says, shared with the release and dependency scripts.
sys.path.insert(0, str(ROOT / "tools"))
from node_names import LOADERS, loader_of, loader_tables  # noqa: E402

# Every node, by the loader tables in stonecutter.properties.toml, the way CI finds them. Fabric first.
NODES = sorted(loader_tables(), key=lambda node: LOADERS.index(loader_of(node)))


def gradlew():
    """The wrapper for this platform, as a path Python can execute."""
    return str(ROOT / ("gradlew.bat" if os.name == "nt" else "gradlew"))


def gradle(arguments, quiet):
    """Runs Gradle in the project root. Returns its exit code; output goes to this process's."""
    command = [gradlew(), *arguments, "--console=plain"]
    if quiet:
        return subprocess.run(command, cwd=ROOT, stdout=subprocess.DEVNULL,
                              stderr=subprocess.DEVNULL).returncode
    return subprocess.run(command, cwd=ROOT).returncode


def nodes_from(argument):
    if argument in (None, "all"):
        return list(NODES)
    if argument in LOADERS:
        return [node for node in NODES if loader_of(node) == argument]
    return [node.strip() for node in argument.split(",") if node.strip()]


def report_of(node):
    return ROOT / "run" / node / "benchmark" / "latest.json"


def status_of(path):
    try:
        with path.open(encoding="utf-8") as file:
            return json.load(file).get("status", "no status")
    except (OSError, ValueError) as error:
        return "unreadable report: " + str(error)


def main():
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--nodes", default="all",
                        help="all, a loader (" + ", ".join(LOADERS) + "), or a comma separated list of node names")
    parser.add_argument("--repeats", type=int, default=3, help="runs per node (default 3)")
    parser.add_argument("--profile", default="standard",
                        help="what to pass to -Pbenchmark (default standard)")
    parser.add_argument("--out", default=None,
                        help="where to keep the reports (default run/benchmark-sets/<timestamp>)")
    parser.add_argument("--skip-build", action="store_true",
                        help="do not compile first; only for a tree that was just built")
    parser.add_argument("--quiet-gradle", action="store_true",
                        help="hide Gradle's own output; this script still prints one line per run")
    options = parser.parse_args()

    nodes = nodes_from(options.nodes)
    stamp = datetime.datetime.now().strftime("%Y%m%d-%H%M%S")
    out = pathlib.Path(options.out) if options.out else ROOT / "run" / "benchmark-sets" / stamp
    out.mkdir(parents=True, exist_ok=True)

    print(f"{len(nodes)} nodes x {options.repeats} runs, profile {options.profile}, into {out}")

    if not options.skip_build:
        print("compiling every node first, so no build runs between two timed runs")
        build = [f":{node}:devClasses" for node in nodes]
        if gradle(build, options.quiet_gradle) != 0:
            print("the build failed; nothing was measured", file=sys.stderr)
            return 2

    failures = []
    for run in range(1, options.repeats + 1):
        for node in nodes:
            started = time.monotonic()
            code = gradle([f":{node}:runBenchmark", f"-Pbenchmark={options.profile}"], options.quiet_gradle)
            report = report_of(node)
            # The Gradle task exits 0 even when the benchmark itself failed, so the report decides.
            status = status_of(report) if report.is_file() else "no report written"
            kept = ""
            if report.is_file():
                destination = out / node / f"{options.profile}-{run}.json"
                destination.parent.mkdir(parents=True, exist_ok=True)
                shutil.copyfile(report, destination)
                kept = f", kept as {destination.relative_to(out)}"
            elapsed = time.monotonic() - started
            print(f"run {run}/{options.repeats} {node}: status {status} in {elapsed:.0f} s{kept}")
            if code != 0 or status != "ok":
                failures.append(f"{node} run {run}: gradle exit {code}, status {status}")

    print(f"\nreports in {out}")
    print(f"next: python tools/benchmark/aggregate.py {out}")
    if failures:
        print("\nruns that did not finish cleanly:", file=sys.stderr)
        for failure in failures:
            print("  " + failure, file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
