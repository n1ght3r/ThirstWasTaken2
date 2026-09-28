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
| `docs/integrations/` | One page per optional mod that changes what this one does, under the sidebar's Integrations. Farmer's Delight and its addons (Brewin' and Chewin', Cultural Delights, Fruits Delight, Ocean's Delight, Expanded Delight, Rustic Delight) share `docs/integrations/farmers-delight/`, whose `index.md` is Farmer's Delight's own page. AppleSkin and Jade only show what is already there, so they get no page. |
| `docs/developers/` | For mod and data pack authors: the data pack format and the Java API. |
| `.vitepress/config.mts` | Nav, sidebar and the GitHub, Modrinth and CurseForge icons. |
| `.vitepress/theme/` | Default theme plus `custom.css` for the brand colour. |
| `public/` | `logo.png` for the navbar and favicon, and `screenshots/`, by subject: `hud/`, `water/`, `recipes/`, `config/`, and `integrations/<mod>/` for each optional mod. A new image goes in the folder of what it shows. The home hero is a slideshow of three screenshots, in `.vitepress/theme/HeroSlideshow.vue`. |

`docs/` contains all player and server documentation. Pages describing gameplay belong in
`docs/features/`, pages about another mod in `docs/integrations/` (an addon of Farmer's Delight in its
folder), and pages listing config keys directly in `docs/`. A new integration page also goes in the
sidebar's Integrations group in `.vitepress/config.mts`, and a row in `MODRINTH.md` and
`CURSEFORGE.md`. There, Mod Compatibility has three parts: the main table, a screenshot per mod; the
"Also works with" table, the mod's banner and versions beside one sentence, for a smaller
integration (Cold Sweat, Serene Seasons); and the Delight Ecosystem (collapsed:
a `<details>` on Modrinth, a `<div class="spoiler">` on CurseForge, which strips `<details>`),
which holds Farmer's Delight and every addon of it, in the same two kinds of table. A banner is linked
from the mod's own page or an image host, never committed, and scaled to about 74 px high. Versions
read "1.21.1 NeoForge", and a mod is named beside a loader only when that loader uses a different one
(a Refabricated port, Create Fly).

An integration page that opens with the mod's banner has no `# Mod name` heading, since the banner
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
