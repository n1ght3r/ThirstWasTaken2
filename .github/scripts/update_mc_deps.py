"""Bumps the Minecraft-bound dependency versions in stonecutter.properties.toml.

Dependabot cannot do this. The versions live in a Stonecutter properties file it does not read, and
even if it did, it would offer every version node the newest Fabric API, the one built for the newest
Minecraft. This asks Modrinth instead, once per node, for the newest upload of each mod that is built
for that node's loader and lists the Minecraft version that node compiles against, and writes it back in
place, comments and layout untouched.

    python .github/scripts/update_mc_deps.py            # rewrite the file
    python .github/scripts/update_mc_deps.py --dry-run  # only report
    python .github/scripts/update_mc_deps.py --summary build/deps-summary.md
    python .github/scripts/update_mc_deps.py --check    # only report where the docs disagree

Rules:
- A node compiles against the Minecraft version settings.gradle.kts gives it (`26.1.x` -> `26.1.2`),
  so that is the version a candidate has to list. A newer Minecraft patch is a manual bump.
- A node takes the uploads of the loader its name ends in (`26.2.x-neoforge`, NeoForge; `1.20.1-forge`,
  Forge); a node with no loader in its name takes Fabric ones. `tools/node_names.py` reads the name.
  Each node's values are read and rewritten in its loader table, `[fabric."26.2.x"]`,
  `[neoforge."26.2.x"]` or `[forge."1.20.1"]`, or else in the shared `["26.2.x"]` table.
- NeoForge itself comes from maven.neoforged.net: the newest build for the same Minecraft version as the
  pinned one, releases only unless the pinned build is a beta. Like Fabric Loader, it raises the
  minimum players need, since neoforge.mods.toml writes it as the lower bound.
- Forge comes from maven.minecraftforge.net's promotions: the build it promotes as latest for the node's
  Minecraft version. It does not raise what players need: mods.toml asks for Forge 47 or later, as
  Forge's own template does, since 1.20.1 packs stay on older 47 builds.
- MixinExtras, which only the Forge nodes nest in their jar, comes from Maven Central, releases only.
- Only release uploads are taken, unless the pinned version is itself a beta or alpha: a node on a
  pre-release dependency stays on that channel until a release catches up.
- A candidate has to be published after the pinned version. Nothing is ever downgraded.
- Fabric Loader is one global value and comes from Fabric's meta API, stable builds only. Raising it
  raises the minimum loader players need, since fabric.mod.json writes it as `>=`.
- Loom is left alone: a Loom bump tends to need a Gradle bump alongside it.
- README.md, docs/docs/installation.md and docs/dev/VERSION-DIFFERENCES.md print the same versions for
  people to read, so a bump rewrites them too, and only there: CHANGELOG.md says what a past release was
  built against and has to keep saying it. All three print Fabric API and NeoForge; the first two print
  Fabric Loader; only the installation page prints the optional mods, in one table per loader.
  An integration's own page under docs/dev/integration may print its pins too, ids included, as the
  Kaleidoscope Cookery one does; a dependency lists every such page in `mirrors` (Fabric nodes) and
  `neoforge_mirrors` (NeoForge and Forge nodes), and a bump rewrites the number and, when pinned by id,
  the id. Pages that say what a version was written or tested against (`Written on ... from ...`,
  manual test logs) are records like CHANGELOG.md and stay out of those lists.
- The pull request lists the other tracked files that still name an old version, for a person to
  judge, except those in `RECORDS`, whose every mention is a record and is meant to stay.
- `--check` goes the other way: it reports a version the properties file pins that those pages do
  not name, which is what a bump made by hand leaves behind. It reads the properties file and those
  pages and nothing else, so it needs no network and gates a pull request in well under a second. What lets it work
  offline is that an id-pinned version carries its number in a comment above it, so `--check` also
  fails when one of those comments is missing, on every node rather than only the Fabric ones.

Standard library only, so CI needs nothing but a Python interpreter.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import time
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]

# tools/node_names.py: what a node's name says, shared with the release and benchmark scripts.
sys.path.insert(0, str(ROOT / "tools"))
from node_names import LOADERS, loader_of, minecraft_of  # noqa: E402
PROPERTIES = ROOT / "stonecutter.properties.toml"
SETTINGS = ROOT / "settings.gradle.kts"
README = ROOT / "README.md"
INSTALLATION = ROOT / "docs" / "docs" / "installation.md"
VERSION_DIFFERENCES = ROOT / "docs" / "dev" / "VERSION-DIFFERENCES.md"
# The only files besides the properties file this script ever writes. An allowlist rather than a search,
# because most other mentions must not move: CHANGELOG.md records what a past release was built against
# and has to keep saying so, and docs/dev names versions inside prose no rewrite can follow.
DOC_MIRRORS = (README, INSTALLATION, VERSION_DIFFERENCES)
KALEIDOSCOPE_DOC = ROOT / "docs" / "dev" / "integration" / "cooking" / "KALEIDOSCOPE-COOKERY-INTEGRATION.md"
# Every page some dependency mirrors, in the order they are rewritten and reported.
ALL_MIRRORS = DOC_MIRRORS + (KALEIDOSCOPE_DOC,)
# Files that name a version only as a record of what something was built, written or tested against,
# so they keep the old number on purpose and the pull request does not ask anyone to update them. A
# file goes here only when every version it names is such a record; one that also states a current
# pin belongs in a dependency's mirrors, or should say it without the number.
RECORDS = (
    ROOT / "CHANGELOG.md",
    ROOT / ".github" / "scripts" / "update_mc_deps.py",
    ROOT / "docs" / "dev" / "MANUAL-TESTING.md",
    ROOT / "docs" / "dev" / "integration" / "storage" / "SOPHISTICATED-INTEGRATION.md",
    ROOT / "src" / "main" / "createforge" / "AGENTS.md",
    ROOT / "src" / "main" / "sereneseasons" / "AGENTS.md",
)
# The pages that print Fabric Loader. VERSION-DIFFERENCES.md lists each node's loader API only.
LOADER_MIRRORS = (README, INSTALLATION)
# Modrinth puts the loader on some version numbers, after a `+` or a `-`. The docs leave it off.
LOADER_SUFFIXES = ("+fabric", "+neoforge", "+forge", "-fabric", "-neoforge", "-forge")

MODRINTH = "https://api.modrinth.com/v2"
FABRIC_META = "https://meta.fabricmc.net/v2/versions/loader"
NEOFORGE_METADATA = "https://maven.neoforged.net/releases/net/neoforged/neoforge/maven-metadata.xml"
# The same list from the repository's own API, a different path on the same host: asked when the
# metadata file keeps failing, which has happened while the API still answered.
NEOFORGE_API = "https://maven.neoforged.net/api/maven/versions/releases/net/neoforged/neoforge"
NEOFORGE_ATTEMPTS = 3
FORGE_PROMOTIONS = "https://files.minecraftforge.net/net/minecraftforge/forge/promotions_slim.json"
MIXINEXTRAS_METADATA = "https://repo1.maven.org/maven2/io/github/llamalad7/mixinextras-forge/maven-metadata.xml"
# Modrinth asks every client for a User-Agent that identifies the project.
USER_AGENT = "n1ght3r/ThirstWasTaken2 dependency updater (github.com/n1ght3r/ThirstWasTaken2)"


@dataclass(frozen=True)
class ModrinthDep:
    key: str
    """The key after `deps.` in the properties file."""
    project: str
    """Modrinth project slug."""
    by_id: bool = False
    """Pinned by Modrinth version id instead of version number."""
    mirrors: tuple[Path, ...] = (INSTALLATION,)
    """The doc mirrors that print this dependency's version on the Fabric nodes."""
    neoforge_mirrors: tuple[Path, ...] = (INSTALLATION,)
    """The doc mirrors that print its version on the NeoForge and Forge nodes."""
    neoforge_project: str | None = None
    """Modrinth project slug on the NeoForge and Forge nodes, when their build is a different project."""
    frozen: tuple[str, ...] = ()
    """Nodes whose pin is left alone, because the upstream build for that version will not change again."""

    def project_for(self, node: str) -> str:
        return self.neoforge_project if self.neoforge_project and loader_of(node) != "fabric" else self.project

    def mirrors_for(self, node: str) -> tuple[Path, ...]:
        return self.neoforge_mirrors if loader_of(node) != "fabric" else self.mirrors


# What a dependency passes when no page prints it on any loader.
NO_PAGE = {"mirrors": (), "neoforge_mirrors": ()}


# Every per-node dependency the build resolves from Modrinth or from a Maven that publishes the same
# version numbers (Fabric API). Add a line here when build.gradle.kts gains a `deps.*` property.
MODRINTH_DEPS = [
    # Every page prints Fabric API. The optional mods are on the installation page only, in its table for
    # each loader, which the defaults of `mirrors` and `neoforge_mirrors` point at; a dependency that page
    # does not print passes `NO_PAGE` for both.
    ModrinthDep("fabric_api", "fabric-api", mirrors=DOC_MIRRORS),
    ModrinthDep("modmenu", "modmenu"),
    # AppleSkin shares one version number between its Fabric and NeoForge uploads.
    ModrinthDep("appleskin", "appleskin", by_id=True),
    ModrinthDep("cloth_config", "cloth-config"),
    ModrinthDep("jade", "jade"),
    # Refabricated is the Fabric port; the NeoForge nodes use vectorwing's original.
    ModrinthDep("farmersdelight", "farmers-delight-refabricated", neoforge_project="farmers-delight"),
    ModrinthDep("create_fly", "create-fly"),
    # Create's version numbers are not spelled alike from one upload to the next, so it is pinned by id.
    ModrinthDep("create", "create", by_id=True),
    # Pinned by id like Create. The installation page prints Core under Sophisticated Backpacks, and
    # neither Backpacks nor Storage, which are only on the runClient classpath.
    ModrinthDep("sophisticated_core", "sophisticated-core", by_id=True),
    ModrinthDep("sophisticated_backpacks", "sophisticated-backpacks", by_id=True, **NO_PAGE),
    ModrinthDep("sophisticated_storage", "sophisticated-storage", by_id=True, **NO_PAGE),
    # Supplementaries and the Moonlight Lib it needs share one version number between their Fabric and
    # NeoForge uploads, like AppleSkin, so both are pinned by id. Only Supplementaries is printed.
    ModrinthDep("supplementaries", "supplementaries", by_id=True),
    ModrinthDep("moonlight", "moonlight", by_id=True, **NO_PAGE),
    # Refabricated is the Fabric port and the official mod is the NeoForge build, under one mod id. Its
    # Fabric uploads of different Minecraft versions share one version number, so it is pinned by id.
    # 1.21.11 is frozen upstream at 1.3.0.9. The installation page prints every build, and so does the
    # integration page's table, with its id.
    ModrinthDep("kaleidoscope_cookery", "kaleidoscope-cookery-refabricated", by_id=True,
                mirrors=(INSTALLATION, KALEIDOSCOPE_DOC), neoforge_mirrors=(INSTALLATION, KALEIDOSCOPE_DOC),
                neoforge_project="kaleidoscope-cookery", frozen=("1.21.11",)),
    # Kaleidoscope Cookery's required library on the Fabric 1.21.x nodes, runClient only.
    ModrinthDep("forge_config_api_port", "forge-config-api-port", by_id=True, **NO_PAGE),
    # Brewin' and Chewin' shares one version number between its Fabric and NeoForge uploads, so it is
    # pinned by id. Both 1.21.1 nodes only; its Greenhouse Config is nested in its jar.
    ModrinthDep("brewin_and_chewin", "brewin-and-chewin", by_id=True),
    # Cold Sweat, pinned by id like the others.
    ModrinthDep("cold_sweat", "cold-sweat", by_id=True),
    # Cultural Delights and the Cook's Collection it requires, NeoForge 1.21.1 only, pinned by id. Its
    # uploads for different Minecraft versions put the version in the number in no fixed place.
    ModrinthDep("cultural_delights", "cultural-delights", by_id=True),
    ModrinthDep("cooks_collection", "cooks-collection", by_id=True, **NO_PAGE),
    # Serene Seasons, every node, and the GlitchCore it requires, runClient only. Both share version
    # numbers across loaders and across Minecraft versions (Serene's 26.2 and 26.3 uploads are both
    # 26.1.2.0.x), so both are pinned by id.
    ModrinthDep("serene_seasons", "serene-seasons", by_id=True),
    ModrinthDep("glitchcore", "glitchcore", by_id=True, **NO_PAGE),
    # Fruits Delight, 1.21.1 NeoForge and 1.20.1 Forge, runClient only, pinned by id like the others.
    ModrinthDep("fruits_delight", "fruits-delight", by_id=True),
    # Expanded Delight, NeoForge 1.21.1 only and runClient only, pinned by id like the others.
    ModrinthDep("expanded_delight", "expanded-delight", by_id=True),
    # Miner's Delight, 1.21.1 NeoForge and 1.20.1 Forge, pinned by id like the others, and the Lodestone
    # it requires on 1.21.1, runClient only.
    ModrinthDep("miners_delight", "miners-delight", by_id=True),
    ModrinthDep("lodestone", "lodestonelib", by_id=True, **NO_PAGE),
    # Hearth and Harvest, 1.21.1 NeoForge and 1.20.1 Forge, pinned by id like the others.
    ModrinthDep("hearth_and_harvest", "hearth-and-harvest", by_id=True),
    # Extra Delight, 1.21.1 NeoForge only, pinned by id like the others.
    ModrinthDep("extra_delight", "extradelight", by_id=True),
    # Let's Do: Farm & Charm and Candlelight, its addon, HerbalBrews, Beachparty and Vinery, both 1.21.1 nodes,
    # pinned by id: their Fabric and NeoForge uploads share a version number. Architectury API, which
    # all of them require, runClient only.
    ModrinthDep("farm_and_charm", "lets-do-farm-charm", by_id=True),
    ModrinthDep("candlelight", "lets-do-candlelight-farmcharm-compat", by_id=True),
    ModrinthDep("herbalbrews", "lets-do-herbalbrews", by_id=True),
    ModrinthDep("beachparty", "lets-do-beachparty", by_id=True),
    ModrinthDep("vinery", "lets-do-vinery", by_id=True),
    # Trinkets and Curios, which Beachparty needs on Fabric and NeoForge, runClient only.
    ModrinthDep("trinkets", "trinkets", by_id=True, **NO_PAGE),
    ModrinthDep("curios", "curios", by_id=True, **NO_PAGE),
    ModrinthDep("architectury", "architectury-api", by_id=True, **NO_PAGE),
    # Spelunkery, both 1.21.1 nodes, pinned by id like the Let's Do mods. It requires Moonlight, above.
    ModrinthDep("spelunkery", "spelunkery", by_id=True),
    # No Man's Land, NeoForge 1.21.1 only, pinned by id like the others. The Biolith and Mixed Litter it
    # requires are nested in its jar.
    ModrinthDep("nomansland", "no-mans-land", by_id=True),
    # Not here: `croptopia` and `epherolib`, which are on CurseForge alone and pinned by CurseForge file
    # id, resolved through CurseMaven. Nothing here can read CurseForge, so they are bumped by hand.
]


@dataclass
class Change:
    node: str | None
    """Version node, or None for a global value."""
    key: str
    old: str
    new: str
    old_label: str
    """What a person reads as the old version; differs from `old` for id-pinned dependencies."""
    new_label: str
    url: str
    mirrors: tuple[Path, ...] = ()
    """The doc mirrors to rewrite; empty when no page names this version."""
    by_id: bool = False
    """`old` and `new` are Modrinth version ids, which a mirror may print beside the number."""


def get_json(url: str):
    request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
    with urllib.request.urlopen(request, timeout=30) as response:
        return json.load(response)


def node_minecraft_versions() -> dict[str, str]:
    """Maps each Stonecutter node to the Minecraft version it compiles against, from settings.gradle.kts."""
    text = SETTINGS.read_text(encoding="utf-8")
    nodes: dict[str, str] = {}
    # versions("1.21.1", "1.21.11"): the node name is the Minecraft version.
    for group in re.findall(r'\bversions\(([^)]*)\)', text):
        for name in re.findall(r'"([^"]+)"', group):
            nodes[name] = name
    # version("26.1.x", "26.1.2"): a named node and the version behind it.
    for name, minecraft in re.findall(r'\bversion\(\s*"([^"]+)"\s*,\s*"([^"]+)"\s*\)', text):
        nodes[name] = minecraft
    if not nodes:
        sys.exit(f"No Stonecutter versions found in {SETTINGS.name}.")
    return nodes


class Properties:
    """The properties file as lines, so a rewrite keeps every comment and blank line.

    The file is layered by loader, the way stonecutter.gradle.kts tags it: `["26.2.x"]` holds what both
    loaders of that Minecraft version share, `[fabric."26.2.x"]` and `[neoforge."26.2.x"]` what only one
    of them has. The node `26.2.x-neoforge` reads its loader table and then the shared one.
    """

    def __init__(self, path: Path):
        self.path = path
        # newline="" keeps CRLF on a Windows checkout, so the rewrite only touches the changed values.
        with path.open(encoding="utf-8", newline="") as file:
            self.lines = file.read().splitlines(keepends=True)

    def _pattern(self, key: str) -> re.Pattern[str]:
        return re.compile(r'^(\s*' + re.escape(key) + r'\s*=\s*")([^"]*)(".*)$', re.DOTALL)

    def _find_in(self, section: tuple[str | None, str] | None, key: str) -> tuple[int, str] | None:
        """Line index and value of `key` in one table, `(loader, version)`, or the top level for None."""
        current: tuple[str | None, str] | None = None
        pattern = self._pattern(key)
        for index, line in enumerate(self.lines):
            header = re.match(r'^\s*\[(?:(' + "|".join(LOADERS) + r')\.)?"([^"]+)"\]\s*$', line)
            if header:
                current = (header.group(1), header.group(2))
                continue
            if current == section:
                match = pattern.match(line)
                if match:
                    return index, match.group(2)
        return None

    def find(self, node: str | None, key: str) -> tuple[int, str] | None:
        """Line index and value of `key` for `node`, from its loader table or else the shared table of its
        version, or in the top level when node is None."""
        if node is None:
            return self._find_in(None, key)
        version = minecraft_of(node)
        return self._find_in((loader_of(node), version), key) or self._find_in((None, version), key)

    def has_node(self, node: str) -> bool:
        """Whether `node` has a loader table, which is what makes it a node to the build and to CI."""
        header = f'[{loader_of(node)}."{minecraft_of(node)}"]'
        return any(line.strip() == header for line in self.lines)

    def set(self, index: int, key: str, value: str) -> None:
        match = self._pattern(key).match(self.lines[index])
        assert match, self.lines[index]
        self.lines[index] = match.group(1) + value + match.group(3)

    def version_comment_above(self, index: int) -> str | None:
        """The version a comment above an id-pinned value names, `3.0.10+mc26.2` for
        `# 3.0.10+mc26.2, the Fabric upload.`, or None when there is no such comment.

        A Modrinth version id says nothing to a person, so every id-pinned value carries its number in a
        comment above it, and `replace_in_comment_above` keeps that comment in step with the id. Only a
        comment whose first word starts with a digit counts, so prose above a value is not mistaken for
        one. A leading `v` is allowed, since Forge Config API Port spells its versions `v21.1.6-1.21.1-Fabric`,
        and a leading `mc<version>-`, since Create's 1.20.1 Forge upload is `mc1.20.1-6.0.8`.
        """
        above = index - 1
        if above < 0:
            return None
        match = re.match(r"^\s*#\s*((?:v|mc\d[\d.]*-)?\d[^\s,]*)", self.lines[above])
        return match.group(1) if match else None

    def replace_in_comment_above(self, index: int, old: str, new: str) -> None:
        """Keeps a comment such as `# 3.0.6+mc1.21, the Fabric upload.` in step with the id below it."""
        above = index - 1
        if above >= 0 and self.lines[above].lstrip().startswith("#") and old in self.lines[above]:
            self.lines[above] = self.lines[above].replace(old, new)

    def save(self) -> None:
        self.path.write_text("".join(self.lines), encoding="utf-8", newline="")


def modrinth_version(project: str, id_or_number: str) -> dict | None:
    try:
        return get_json(f"{MODRINTH}/project/{project}/version/{urllib.parse.quote(id_or_number, safe='')}")
    except urllib.error.HTTPError as error:
        if error.code == 404:
            return None
        raise


def modrinth_candidates(project: str, minecraft: str, loader: str) -> list[dict]:
    query = urllib.parse.urlencode({
        "loaders": json.dumps([loader]),
        "game_versions": json.dumps([minecraft]),
        "include_changelog": "false",
    })
    versions = get_json(f"{MODRINTH}/project/{project}/version?{query}")
    return sorted(versions, key=lambda v: v["date_published"], reverse=True)


def check_modrinth(props: Properties, node: str, minecraft: str, dep: ModrinthDep, changes: list[Change],
                   warnings: list[str]) -> None:
    found = props.find(node, f"deps.{dep.key}")
    if found is None or node in dep.frozen:
        return
    index, pinned = found

    project = dep.project_for(node)
    current = modrinth_version(project, pinned)
    if current is None:
        warnings.append(f"`{node}` `deps.{dep.key}` = `{pinned}` was not found on Modrinth ({project}); skipped.")
        return

    channels = {"release", current["version_type"]}
    newest = next((v for v in modrinth_candidates(project, minecraft, loader_of(node)) if v["version_type"] in channels), None)
    if newest is None or newest["date_published"] <= current["date_published"]:
        return
    value = newest["id"] if dep.by_id else newest["version_number"]
    # Jade sometimes re-uploads a build under the same number, which resolves to the same artifact.
    if value == pinned:
        return

    props.set(index, f"deps.{dep.key}", value)
    if dep.by_id:
        props.replace_in_comment_above(index, current["version_number"], newest["version_number"])
    changes.append(Change(
        node=node,
        key=dep.key,
        old=pinned,
        new=value,
        old_label=current["version_number"],
        new_label=newest["version_number"],
        url=f"https://modrinth.com/mod/{project}/version/{newest['id']}",
        mirrors=dep.mirrors_for(node),
        by_id=dep.by_id,
    ))


def numeric(version: str) -> tuple[int, ...]:
    return tuple(int(part) for part in re.findall(r"\d+", version))


def check_loader(props: Properties, changes: list[Change]) -> None:
    found = props.find(None, "deps.fabric_loader")
    if found is None:
        return
    index, pinned = found
    stable = [v["version"] for v in get_json(FABRIC_META) if v.get("stable")]
    if not stable:
        return
    newest = max(stable, key=numeric)
    if numeric(newest) <= numeric(pinned):
        return
    props.set(index, "deps.fabric_loader", newest)
    changes.append(Change(None, "fabric_loader", pinned, newest, pinned, newest,
                          "https://github.com/FabricMC/fabric-loader/releases", LOADER_MIRRORS))


class Unreachable(Exception):
    """A source that kept failing. The run skips what needed it instead of failing whole."""


_neoforge_versions: list[str] | Unreachable | None = None


def fetch_neoforge_versions() -> list[str]:
    # maven.neoforged.net now and then answers 404 or times out on a file that is there, so a failure is
    # retried, then the API asked, before giving up. Unlike Modrinth, a 404 here never means "none".
    sources = [
        (NEOFORGE_METADATA, lambda body: re.findall(r"<version>([^<]+)</version>", body)),
        (NEOFORGE_API, lambda body: json.loads(body)["versions"]),
    ]
    errors = []
    for url, parse in sources:
        request = urllib.request.Request(url, headers={"User-Agent": USER_AGENT})
        for attempt in range(NEOFORGE_ATTEMPTS):
            try:
                with urllib.request.urlopen(request, timeout=30) as response:
                    versions = parse(response.read().decode("utf-8"))
                if versions:
                    return versions
                error: object = "no versions listed"
            except (urllib.error.URLError, TimeoutError, ValueError, KeyError) as caught:
                error = caught
            print(f"{url}: {error}, attempt {attempt + 1} of {NEOFORGE_ATTEMPTS}", file=sys.stderr)
            if attempt < NEOFORGE_ATTEMPTS - 1:
                time.sleep(10 * (attempt + 1))
        errors.append(f"{url}: {error}")
    raise Unreachable("; ".join(errors))


def neoforge_versions() -> list[str]:
    """Fetched once per run, every NeoForge node reads the same list. A failure is remembered too, so
    the second node does not wait through the retries again."""
    global _neoforge_versions
    if _neoforge_versions is None:
        try:
            _neoforge_versions = fetch_neoforge_versions()
        except Unreachable as error:
            _neoforge_versions = error
    if isinstance(_neoforge_versions, Unreachable):
        raise _neoforge_versions
    return _neoforge_versions


def check_neoforge(props: Properties, node: str, changes: list[Change]) -> None:
    """NeoForge's version starts with the Minecraft version it is built for and ends with the build
    number, `26.2.0.88` for 26.2 and `21.1.251` for 1.21.1, so only builds sharing everything but the
    pinned version's last part are candidates. Betas are skipped unless the pinned build is one."""
    found = props.find(node, "deps.neoforge")
    if found is None:
        return
    index, pinned = found
    prefix = ".".join(pinned.split("-")[0].split(".")[:-1]) + "."
    allow_beta = "-" in pinned
    candidates = [v for v in neoforge_versions() if v.startswith(prefix) and (allow_beta or "-" not in v)]
    if not candidates:
        return
    newest = max(candidates, key=numeric)
    if numeric(newest) <= numeric(pinned):
        return
    props.set(index, "deps.neoforge", newest)
    changes.append(Change(node, "neoforge", pinned, newest, pinned, newest,
                          "https://projects.neoforged.net/neoforged/neoforge", DOC_MIRRORS))


def check_forge(props: Properties, node: str, minecraft: str, changes: list[Change]) -> None:
    """Forge promotes one build per Minecraft version as latest, `47.4.23` for 1.20.1; that is the one."""
    found = props.find(node, "deps.forge")
    if found is None:
        return
    index, pinned = found
    try:
        promoted = get_json(FORGE_PROMOTIONS)["promos"].get(f"{minecraft}-latest")
    except (urllib.error.URLError, TimeoutError, ValueError, KeyError) as error:
        raise Unreachable(f"{FORGE_PROMOTIONS}: {error}") from error
    if promoted is None or numeric(promoted) <= numeric(pinned):
        return
    props.set(index, "deps.forge", promoted)
    changes.append(Change(node, "forge", pinned, promoted, pinned, promoted,
                          "https://files.minecraftforge.net/net/minecraftforge/forge/", DOC_MIRRORS))


def check_mixinextras(props: Properties, node: str, changes: list[Change]) -> None:
    """The newest MixinExtras release, for the nodes that nest it. No page prints it."""
    found = props.find(node, "deps.mixinextras")
    if found is None:
        return
    index, pinned = found
    request = urllib.request.Request(MIXINEXTRAS_METADATA, headers={"User-Agent": USER_AGENT})
    try:
        with urllib.request.urlopen(request, timeout=30) as response:
            release = re.search(r"<release>([^<]+)</release>", response.read().decode("utf-8"))
    except (urllib.error.URLError, TimeoutError) as error:
        raise Unreachable(f"{MIXINEXTRAS_METADATA}: {error}") from error
    if release is None or "-" in release.group(1) or numeric(release.group(1)) <= numeric(pinned):
        return
    props.set(index, "deps.mixinextras", release.group(1))
    changes.append(Change(node, "mixinextras", pinned, release.group(1), pinned, release.group(1),
                          "https://github.com/LlamaLad7/MixinExtras/releases"))


def forms(version: str) -> list[str]:
    """A version as the docs may print it: the way Modrinth numbers it, and, for the numbers that carry
    a loader suffix, without it, since that is the form the pages use. The same for a Minecraft prefix,
    `mc1.20.1-6.0.8` printed as `6.0.8`, and a Minecraft suffix, `6.0.10+mc1.21.1` printed as `6.0.10`."""
    prefixed = re.match(r"^mc\d[\d.]*-(\d.*)$", version)
    if prefixed:
        return [version, prefixed.group(1)]
    suffixed = re.match(r"^(\d.*)\+mc\d[\d.]*$", version)
    if suffixed:
        return [version, suffixed.group(1)]
    for suffix in LOADER_SUFFIXES:
        if version.endswith(suffix):
            return [version, version[: -len(suffix)]]
    return [version]


def version_pattern(*versions: str) -> re.Pattern[str]:
    """Matches any of `versions`, but only where a whole version stands: the characters a version is made
    of are what bound it, so `26.2.11` is not found inside `26.2.110`, `1.26.2.11` or `26.2.11+fabric`.
    A period after it only continues the version when more of one follows, so `26.2.11.` ending a
    sentence still names `26.2.11`. Longest first, so the alternation prefers the fuller number where two
    of them start alike."""
    longest = sorted(versions, key=len, reverse=True)
    return re.compile(r"(?<![\w.+-])(" + "|".join(re.escape(v) for v in longest) + r")(?![\w+-]|\.\w)")


def read(path: Path) -> str:
    # newline="" keeps CRLF on a Windows checkout, the way Properties reads the file it rewrites.
    with path.open(encoding="utf-8", newline="") as file:
        return file.read()


def update_docs(changes: list[Change], dry_run: bool) -> dict[Path, list[str]]:
    """Rewrites the versions the doc mirrors print so they keep saying what the build uses.

    Returns the swaps made, per file, for the pull request body.
    """
    updated: dict[Path, list[str]] = {}
    for path in ALL_MIRRORS:
        swaps: dict[str, str] = {}
        for change in changes:
            if path not in change.mirrors:
                continue
            for old, new in zip(forms(change.old_label), forms(change.new_label)):
                if old != new:
                    swaps[old] = new
            # A page that prints the id beside the number, as the integration pages do, keeps it in step.
            if change.by_id:
                swaps[change.old] = change.new
        if not swaps:
            continue

        made: list[str] = []

        def replace(match: re.Match[str]) -> str:
            old = match.group(1)
            if f"{old} -> {swaps[old]}" not in made:
                made.append(f"{old} -> {swaps[old]}")
            return swaps[old]

        # One pass over the whole file, so a version this writes cannot be picked up again by another
        # swap that happens to be looking for exactly the number just written.
        text = original = read(path)
        text = version_pattern(*swaps).sub(replace, text)
        if text != original:
            if not dry_run:
                path.write_text(text, encoding="utf-8", newline="")
            updated[path.relative_to(ROOT)] = made
    return updated


def check_docs(props: Properties) -> list[str]:
    """Every version the doc mirrors are meant to print, checked against what the properties file pins.

    This is the half a bump made by hand forgets: the build moves and the pages people read do not. It
    reads the properties file and those pages and nothing else, so it needs no network and is cheap
    enough to gate a pull request.
    """
    pages = {path: read(path) for path in ALL_MIRRORS}

    def names(path: Path, version: str) -> bool:
        return any(version_pattern(form).search(pages[path]) for form in forms(version))

    def missing(dep_name: str, label: str, mirrors: tuple[Path, ...], node: str | None) -> list[str]:
        where = f"`{node}` pins" if node else "the build pins"
        return [f"{path.relative_to(ROOT).as_posix()} does not name {dep_name} {label}, which {where}."
                for path in mirrors if not names(path, label)]

    problems: list[str] = []
    loader = props.find(None, "deps.fabric_loader")
    if loader:
        problems += missing("Fabric Loader", loader[1], LOADER_MIRRORS, None)

    for node in node_minecraft_versions():
        # A node without a loader table is not a node; main warns about that separately.
        if not props.has_node(node):
            continue
        neoforge = props.find(node, "deps.neoforge")
        if neoforge:
            problems += missing("NeoForge", neoforge[1], DOC_MIRRORS, node)
        forge = props.find(node, "deps.forge")
        if forge:
            problems += missing("Forge", forge[1], DOC_MIRRORS, node)
        for dep in MODRINTH_DEPS:
            found = props.find(node, f"deps.{dep.key}")
            if found is None:
                continue
            index, pinned = found
            label = pinned
            if dep.by_id:
                # An id says nothing to a person, so the comment above it is where the number lives.
                # Checked on every node, NeoForge included, or the comment is only as reliable as
                # whoever last remembered to write one.
                label = props.version_comment_above(index)
                if label is None:
                    problems.append(f"`{node}` `deps.{dep.key}` = `{pinned}` is a Modrinth version id "
                                    f"with no version above it. Put the number in a comment, as "
                                    f"`# 3.0.10+mc26.2, the Fabric upload.` does.")
                    continue
            problems += missing(dep.key, label, dep.mirrors_for(node), node)
    return problems


def files_mentioning(value: str) -> list[str]:
    """Tracked files other than the properties file and `RECORDS` that still name an old version, for
    the PR body."""
    records = [f":!{path.relative_to(ROOT).as_posix()}" for path in RECORDS]
    result = subprocess.run(["git", "grep", "-l", "-w", "-F", value, "--", ".", f":!{PROPERTIES.name}",
                             ":!**/package-lock.json", *records],
                            cwd=ROOT, capture_output=True, text=True)
    return [line for line in result.stdout.splitlines() if line]


def summary(changes: list[Change], warnings: list[str], updated_docs: dict[Path, list[str]]) -> str:
    out = ["Updates the Minecraft-bound dependencies in `stonecutter.properties.toml`. Each candidate is an upload "
           "on Modrinth for its node's loader that lists the Minecraft version the node compiles against.", ""]
    if changes:
        out += ["| Node | Dependency | From | To |", "| --- | --- | --- | --- |"]
        for change in changes:
            out.append(f"| {change.node or 'all'} | `{change.key}` | `{change.old_label}` | "
                       f"[`{change.new_label}`]({change.url}) |")
        out.append("")

    notes = []
    if any(change.key == "fabric_loader" for change in changes):
        notes.append("Fabric Loader is written into `fabric.mod.json` as `>=`, so this raises the minimum loader "
                     "players need.")
    if any(change.key == "neoforge" for change in changes):
        notes.append("NeoForge is written into `neoforge.mods.toml` as the lower bound, so this raises the minimum "
                     "NeoForge players need.")
    if updated_docs:
        notes.append("The versions these pages print were rewritten to match:")
        notes += [f"  - `{path.as_posix()}`: {', '.join(f'`{swap}`' for swap in swaps)}"
                  for path, swaps in sorted(updated_docs.items())]
    stale = {}
    for change in changes:
        for path in files_mentioning(change.old_label):
            stale.setdefault(path, set()).add(change.old_label)
    if stale:
        notes.append("These files still mention an old version and are not rewritten for you. Update them if "
                     "it is a documented minimum:")
        notes += [f"  - `{path}`: {', '.join(f'`{v}`' for v in sorted(values))}" for path, values in sorted(stale.items())]
    notes += warnings
    if notes:
        out += ["### Notes", ""] + [note if note.startswith("  ") else f"- {note}" for note in notes] + [""]
    return "\n".join(out)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--dry-run", action="store_true", help="report what would change without writing")
    parser.add_argument("--check", action="store_true",
                        help="only report versions the docs do not name, and exit non-zero if there are any")
    parser.add_argument("--summary", type=Path, help="write a Markdown summary here, for a pull request body")
    args = parser.parse_args()

    props = Properties(PROPERTIES)

    if args.check:
        problems = check_docs(props)
        for problem in problems:
            print(problem, file=sys.stderr)
        if not problems:
            print("The docs name every pinned version.")
        return 1 if problems else 0

    changes: list[Change] = []
    warnings: list[str] = []

    check_loader(props, changes)
    for node, minecraft in node_minecraft_versions().items():
        if not props.has_node(node):
            warnings.append(f"`{node}` is in settings.gradle.kts but has no loader table in {PROPERTIES.name}; skipped.")
            continue
        for dep in MODRINTH_DEPS:
            check_modrinth(props, node, minecraft, dep, changes, warnings)
        if loader_of(node) == "neoforge":
            try:
                check_neoforge(props, node, changes)
            except Unreachable as error:
                # A NeoForge outage should not hold back the Modrinth bumps found above; the next
                # daily run checks NeoForge again.
                warnings.append(f"`{node}` `deps.neoforge` was not checked, maven.neoforged.net did not answer ({error}).")
        if loader_of(node) == "forge":
            try:
                check_forge(props, node, minecraft, changes)
            except Unreachable as error:
                warnings.append(f"`{node}` `deps.forge` was not checked, files.minecraftforge.net did not answer ({error}).")
            try:
                check_mixinextras(props, node, changes)
            except Unreachable as error:
                warnings.append(f"`{node}` `deps.mixinextras` was not checked, Maven Central did not answer ({error}).")

    for change in changes:
        print(f"{change.node or 'all'}: {change.key} {change.old_label} -> {change.new_label}")
    for warning in warnings:
        print(f"warning: {warning}", file=sys.stderr)
        # Shown on the run's summary page, so a skipped check is seen even when no pull request is opened.
        if os.environ.get("GITHUB_ACTIONS") == "true":
            print(f"::warning::{warning}")
    if not changes:
        print("Everything is up to date.")

    updated_docs: dict[Path, list[str]] = {}
    if changes:
        if not args.dry_run:
            props.save()
        # After the properties file, so a summary written below greps the tree as it now stands.
        updated_docs = update_docs(changes, args.dry_run)
        for path, swaps in updated_docs.items():
            print(f"{path.as_posix()}: {', '.join(swaps)}")

    if args.summary:
        args.summary.parent.mkdir(parents=True, exist_ok=True)
        args.summary.write_text(summary(changes, warnings, updated_docs), encoding="utf-8")
    return 0


if __name__ == "__main__":
    sys.exit(main())
