# docs/

VitePress site for players and server owners, in English only. The in-game text is still translated;
the site is not, and no other language gets a copy of it.

```bash
npm install
npm run docs:dev      # local preview
npm run docs:build    # must pass before you call a docs change done
```

## Layout

| Path | Holds |
|---|---|
| `index.md` | The hero page. Feature cards link into the documentation. |
| `docs/` | The manual: overview, features, installation, commands, configuration, FAQ. |
| `docs/features/` | What the mod does on its own and why, in prose. No config key listings. |
| `docs/integrations/` | One page per optional mod that changes what this one does, under the sidebar's Integrations. Farmer's Delight and its addons (Brewin' and Chewin', Cultural Delights, Fruits Delight, Ocean's Delight, Expanded Delight, Rustic Delight, Extra Delight) share `docs/integrations/farmers-delight/`, whose `index.md` is Farmer's Delight's own page. Let's Do: Farm & Charm and its addon Candlelight share `docs/integrations/farm-and-charm/` the same way. In the sidebar and the home page's grid, HerbalBrews, Beachparty and Vinery sit in Farm & Charm's group too (`LETS_DO` in `.vitepress/mods.ts`), as the store pages' "Let's Do mods" family does, while their pages stay directly in `docs/integrations/`. AppleSkin and Jade only show what is already there, so they get no page. |
| `docs/developers/` | For mod and data pack authors: the data pack format and the Java API. |
| `.vitepress/config.mts` | Nav, sidebar and the GitHub, Modrinth and CurseForge icons. |
| `.vitepress/mods.ts` | Every other mod the site links to, with its icon: the sidebar's Integrations group and the home page's "Works with" grid both read it. A new integration is added here once. |
| `.vitepress/theme/` | Default theme plus `custom.css` for the brand colour. |
| `public/` | `logo.png` for the navbar and favicon, and `screenshots/`, by subject: `hud/`, `water/`, `recipes/`, `config/`, and `integrations/<mod>/` for each optional mod. A new image goes in the folder of what it shows. The home hero is `.vitepress/theme/HomeHero.vue` in place of VitePress's own, its words under `thirstHero` in `index.md`; the slideshow of screenshots beside it is `HeroSlideshow.vue`. The home page stays plain: Lucide icons and the brand blue, no item textures of this mod. Other mods' icons stay. |

`docs/` contains all player and server documentation. Pages describing gameplay belong in
`docs/features/`, pages about another mod in `docs/integrations/` (an addon of Farmer's Delight in its
folder), and pages listing config keys directly in `docs/`. A new integration page also goes in the
sidebar's Integrations group in `.vitepress/config.mts`. It gets a row in `MODRINTH.md` and
`CURSEFORGE.md` only when it is a Farmer's Delight addon, a Let's Do mod or a Kaleidoscope Cookery addon, or when the maintainer
asks; any other new integration stays off the store pages, which point to the site's full list. There, Mod Compatibility opens with one collapsed section per family of mods, with no
heading of its own (Farmer's Delight and its addons, then Let's Do: Farm & Charm with Candlelight, HerbalBrews, Beachparty and Vinery, then Kaleidoscope Cookery and its addon Kaleidoscope Flora), then
the main table, a screenshot per mod beside its icon and name, then the "Also works with" table, the
mod's banner and versions beside one sentence, for a smaller integration (Cold Sweat, Serene Seasons,
Spelunkery). That table stays short: the rest are left to "...and more. [View full list]", which links
to the site's `docs/installation#compatible-mods` (the store pages strip in-page anchors). A family is a `<details>` on Modrinth, whose summary is the main mod's icon and
"<b>Name and its addons</b>, click to expand" (the Let's Do family's is just "<b>Let's Do mods</b>") (Farmer's Delight's is its banner at 204 px wide,
unlinked so a click still opens it, then "<b>and its addons</b>, click to expand"), and on CurseForge,
which strips `<details>` and ignores `align` and `vertical-align` on an image, the main mod's icon and
"<b>Name and its addons</b>" in a `<p>` above a `<div class="spoiler">`, Farmer's Delight included,
since text beside a banner there sits at its top edge. Each holds its main mod and its addons in the same two kinds of table. In the
Farmer's Delight family, the rows with a screenshot (Farmer's Delight, Brewin' and Chewin') open with
the mod's linked banner in place of its icon and name. The table of which integration works on which
loader and version is not collapsed: it sits under `### Mod Compatibility by Loader`, below the
Version Support table. Notes on one version of one addon go on that addon's page on the site, not
here. CurseForge renders
no Markdown inside an HTML block, so text inside a spoiler there is HTML (`<p>`, `<ul><li>`, `<a>`):
a `- ` list comes out as one run-on paragraph. A banner is linked
from the mod's own page or an image host, never committed, and scaled to about 74 px high. A link to
another mod's page (its Modrinth or CurseForge page, or our page about it) has the mod's icon before
it, from the mod's own page: on the site `![](url){.mod-icon}`, on the store pages an `<img>` 20 px
square with `align="absmiddle"`. A link that is a banner gets none. Icons are linked, never downloaded
or edited into the repo, even one with a solid background (Sophisticated Backpacks): they are the
mod authors' work. An icon drawn inside a wide transparent margin (Farmer's Delight and its
addons) goes through wsrv.nl with `trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png`, which
crops the margin and pads it square again; those show at 24 px on the store pages and a little
larger on the site (`.mod-icon-lg`). Every sidebar entry has an icon, set in `.vitepress/config.mts`: an
integration's page the mod's icon, every other page a Lucide icon. Versions
read "1.21.1 NeoForge", and a mod is named beside a loader only when that loader uses a different one
(a Refabricated port, Create Fly).

An integration page that opens with the mod's banner (marked `{.mod-banner}`, or `class="mod-banner"` on an
`<img>`, which centres it) has no `# Mod name` heading, since the banner
already shows the name; it sets `title:` in frontmatter instead, for the browser tab. A page without a
banner keeps its `#` heading.

## Style

Written for a server owner who has never seen the code. That means:

- Plain language. Say what a setting does to the game, not which class reads it.
- No Java identifiers, no file paths inside `src/`, no mention of Mojang mappings. `docs/developers/`
  is the exception: its readers write mods, so it names the API's classes and methods, but still never
  the mod's internal ones.
- Short sentences. A default and a one-line reason beats a paragraph.
- Config keys as `### keyName` with the default in the first line, so the on-page outline becomes a
  usable index.
- **No em dashes or en dashes.** Use a comma, a period, or "to" for a range. Check with a ripgrep for
  `[—–]` before finishing.

Say the same thing in exactly one place and link to it. Numbers a player cares about (item thirst
values, sickness chances, purity by location) live on the `docs/features/` page that explains them, and
`docs/configuration.md` links there instead of repeating the tables.

In-game wording comes from `src/main/resources/assets/thirstwastaken2/lang/en_us.json`, so a page
names a grade, an item or a setting exactly as the game shows it.

## Keeping it true

The pages describe real behaviour, so a change to the mod means a docs edit in the same pass. The
ones most likely to go stale:

| Changed | Update |
|---|---|
| A field in `ThirstConfig` | `docs/configuration.md` |
| A `/thirst` subcommand | `docs/commands.md` |
| Exhaustion, climate or damage in `ThirstManager` | `docs/features/thirst-and-quenched.md` |
| Thirst values, bowls, loot | `docs/features/drinking.md` |
| Anything in `WaterPurity` or a purify recipe | `docs/features/water-purity.md` |
| Farmer's Delight values, Cooking Pot recipes or Nourishment | `docs/integrations/farmers-delight/index.md` |
| The Drinking Upgrade or another Sophisticated upgrade | `docs/integrations/sophisticated-backpacks.md` |
| Supported Minecraft, Loader or Fabric API version | `docs/installation.md` |
| `com.thirstwastaken2.api`, the data pack format in `DataPackDrinks` | `docs/developers/java-api.md`, `docs/developers/data-packs.md` |
