"""What a node's name says: its Minecraft version and its mod loader.

A node is `<version>` on Fabric and `<version>-<loader>` on any other loader, as settings.gradle.kts
names it. The Gradle side asks `loaderOf` and `minecraftOf` in build-logic; every Python script asks
this module, so a new loader is one line in each.

    python tools/node_names.py matrix    # the CI matrix, one {"version", "loader"} per node, as JSON
"""

from __future__ import annotations

import json
import re
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
PROPERTIES = ROOT / "stonecutter.properties.toml"

FABRIC = "fabric"
# Every loader, in the order a listing shows them. Fabric's nodes carry no suffix.
LOADERS = (FABRIC, "neoforge", "forge")

_LOADER_TABLE = re.compile(r'^\s*\[(' + "|".join(LOADERS) + r')\."([^"]+)"\]\s*$')


def suffix(loader: str) -> str:
    """What follows the Minecraft version in the name of a node of `loader`: nothing on Fabric."""
    return "" if loader == FABRIC else f"-{loader}"


def loader_of(node: str) -> str:
    """The mod loader a node builds for: `neoforge` for `26.2.x-neoforge`, `fabric` for `26.2.x`."""
    for loader in LOADERS:
        if loader != FABRIC and node.endswith(suffix(loader)):
            return loader
    return FABRIC


def minecraft_of(node: str) -> str:
    """The node's name without its loader: `26.2.x` for `26.2.x-neoforge`. It names the shared table."""
    return node.removesuffix(suffix(loader_of(node)))


def node_name(minecraft: str, loader: str) -> str:
    """The node building `minecraft` for `loader`, the inverse of `minecraft_of` and `loader_of`."""
    return minecraft + suffix(loader)


def loader_tables(path: Path = PROPERTIES) -> list[str]:
    """Every node with a loader table in stonecutter.properties.toml, in file order.

    `[fabric."26.2.x"]` is the node `26.2.x` and `[neoforge."26.2.x"]` the node `26.2.x-neoforge`; a
    shared `["26.2.x"]` table is not a node. The build reads a node's dependency versions from its
    loader table, so every node has one.
    """
    nodes = []
    for line in path.read_text(encoding="utf-8").splitlines():
        match = _LOADER_TABLE.match(line)
        if match:
            nodes.append(node_name(match.group(2), match.group(1)))
    return nodes


def main(argv: list[str]) -> int:
    if argv != ["matrix"]:
        print(__doc__, file=sys.stderr)
        return 2
    nodes = loader_tables()
    if not nodes:
        print(f"No loader tables found in {PROPERTIES.name}.", file=sys.stderr)
        return 1
    print(json.dumps([{"version": node, "loader": loader_of(node)} for node in nodes]))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
