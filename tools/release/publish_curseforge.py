"""Uploads one release of ThirstWasTaken2 to CurseForge.

The CurseForge counterpart of `publish.py`, with the same flags and the same run: the nodes, the mods
each lists, the changelog and the jars all come from there, so a file is described the same way on both
sites. Only talking to CurseForge lives here.

    python tools/release/publish_curseforge.py --dry-run --no-build   # print every upload, send nothing
    python tools/release/publish_curseforge.py --no-build             # upload the jars publish.py built
    python tools/release/publish_curseforge.py --only 26.2.x          # one node only

Without `--no-build` it builds every node first, as `publish.py` does.

Files are uploaded from the oldest Minecraft version to the newest. CurseForge treats the last upload
as the project's main file, so the newest Minecraft version must come last.

The upload API only adds files to a project that already exists: the project itself was created by hand
at https://authors.curseforge.com, with `docs/CURSEFORGE.md` as its description. That API has no way to
change the description either, so after the uploads the script only says where to paste the file;
`--description-only` says the same without uploading anything. Its numeric id goes in
`.env` as `CURSEFORGE_PROJECT_ID`, beside `CURSEFORGE_TOKEN`, an upload token from
https://legacy.curseforge.com/account/api-tokens; both can come from the environment instead.

Re-running skips a file already on CurseForge, by file name. The upload API cannot list a project's
files, so the public site's file list is read instead; a file still waiting for CurseForge's review can
be missing from it, so right after an upload finish with `--only` rather than a plain re-run.

The mods a file lists are set when it is uploaded and cannot be changed through the API afterwards: its
update-file endpoint answers 500 whenever `relations` is sent. Fixing them on a file already up is done
by hand under the file's Related Projects on the site.

Standard library only, like the rest of the Python in this repository.
"""

from __future__ import annotations

import sys
import urllib.error
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
from publish import ENV_FILE, ROOT, Node, env_value, fail, http, multipart, release  # noqa: E402

DESCRIPTION = ROOT / "docs" / "CURSEFORGE.md"

CURSEFORGE = "https://minecraft.curseforge.com/api"
# The site's own API, which the file list on the project page is drawn from. Undocumented, but public
# and the only way to see which files a project already has.
CURSEFORGE_SITE = "https://www.curseforge.com/api/v1"
# The upload API does not return the project, so the name on the page is written here.
TITLE = "Thirst Was Taken 2"
LOADERS = {"fabric": "Fabric", "neoforge": "NeoForge", "forge": "Forge"}


class CurseForge:
    name = "CurseForge"

    def __init__(self):
        self.token = env_value("CURSEFORGE_TOKEN")
        if not self.token:
            fail(f"no CURSEFORGE_TOKEN in the environment or in {ENV_FILE.name}")
        self.project_id = env_value("CURSEFORGE_PROJECT_ID")
        if not self.project_id:
            fail(f"no CURSEFORGE_PROJECT_ID in the environment or in {ENV_FILE.name}")
        self.version_ids = self.game_version_ids()
        self.published = self.published_files()

    def request(self, path: str, body: bytes | None = None, content_type: str | None = None):
        headers = {"X-Api-Token": self.token}
        if content_type:
            headers["Content-Type"] = content_type
        return http(f"{CURSEFORGE}{path}", headers, "POST" if body else "GET", body)

    def game_version_ids(self) -> dict[str, int]:
        """Every name an upload tags a file with, to CurseForge's id for it.

        Minecraft releases are read only from the `minecraft-*` version types: the same name also appears
        under Bukkit's and the addons' types, and those ids would tag the file as something else.
        """
        types = {t["id"]: t["slug"] for t in self.request("/game/version-types")}
        ids: dict[str, int] = {}
        for version in self.request("/game/versions"):
            slug = types.get(version["gameVersionTypeID"], "")
            if slug.startswith("minecraft-") or slug in ("modloader", "java", "environment"):
                ids.setdefault(version["name"], version["id"])
        return ids

    def published_files(self) -> set[str]:
        """The file names already on the project, from the public site's file list, a page at a time."""
        # `pageIndex` counts pages, not files. The site ignores an unknown parameter such as `index` and
        # answers with the first page every time, so the loop also stops once it has seen `totalCount`.
        names: set[str] = set()
        page_index, size = 0, 50
        while True:
            url = f"{CURSEFORGE_SITE}/mods/{self.project_id}/files?pageIndex={page_index}&pageSize={size}"
            try:
                answer = http(url, {})
                page, total = answer["data"], answer["pagination"]["totalCount"]
            except (urllib.error.URLError, KeyError, ValueError) as error:
                fail(f"could not read the files already on CurseForge ({error}); "
                     "pass --only to choose the nodes to upload by hand")
            names.update(file["fileName"] for file in page)
            if len(page) < size or (page_index + 1) * size >= total:
                return names
            page_index += 1

    def is_published(self, node: Node, number: str) -> bool:
        return node.jar.name in self.published

    def tags(self, node: Node) -> list[str]:
        # 26.1 and later run on Java 25, 1.21.x on Java 21, as the build's toolchains do.
        java = "Java 21" if node.minecraft.startswith("1.") else "Java 25"
        names = [*node.game_versions, LOADERS[node.loader], java, "Client", "Server"]
        unknown = [name for name in names if name not in self.version_ids]
        if unknown:
            fail(f"CurseForge has no game version called {', '.join(unknown)}")
        return names

    def describe(self, node: Node) -> list[str]:
        lines = [f"tags      {', '.join(self.tags(node))}"]
        by_hand = [dep.modrinth_slug for dep in node.dependencies if dep.curseforge_slug is None]
        if by_hand:
            lines.append(f"by hand   {', '.join(by_hand)} (add under Related Projects; the API refuses it)")
        return lines

    def upload(self, node: Node, number: str, changelog: str) -> str:
        metadata = {
            "changelog": changelog,
            "changelogType": "markdown",
            "displayName": f"{TITLE} {number}",
            "gameVersions": [self.version_ids[name] for name in self.tags(node)],
            "releaseType": "release",
        }
        if node.dependencies:
            metadata["relations"] = {"projects": [
                {"slug": dep.curseforge_slug,
                 "type": "requiredDependency" if dep.required else "optionalDependency"}
                for dep in node.dependencies if dep.curseforge_slug is not None]}
        body, content_type = multipart("metadata", metadata, node.jar)
        created = self.request(f"/projects/{self.project_id}/upload-file", body, content_type)
        return f"file id {created['id']}"

    def sync_description(self, dry_run: bool) -> None:
        """The upload API cannot touch the description, so this only points at where it is edited."""
        print(f"Description: CurseForge has no API for it. If {DESCRIPTION.relative_to(ROOT).as_posix()} changed,")
        print(f"             paste it (Markdown) at https://authors.curseforge.com/#/projects/{self.project_id}/description")

    def update_dependencies(self, node: Node, number: str) -> None:
        # update-file answers 500 whenever `relations` is sent; see the module docstring.
        fail("CurseForge cannot change the mods a published file lists through its API; "
             "edit Related Projects on the file's page instead")

    def done_url(self) -> str:
        return f"https://authors.curseforge.com/#/projects/{self.project_id}/files"

    @staticmethod
    def order_nodes(nodes: list[Node]) -> list[Node]:
        """Upload oldest Minecraft first so the newest version becomes CurseForge's main file."""
        return sorted(nodes, key=lambda node: (
            tuple(int(part) for part in node.minecraft.split(".")),
            node.loader != "fabric",
        ))


if __name__ == "__main__":
    release(CurseForge, "Upload a ThirstWasTaken2 release to CurseForge.")
