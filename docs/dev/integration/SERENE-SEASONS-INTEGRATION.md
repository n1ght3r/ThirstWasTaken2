# Serene Seasons integration plan

What ThirstWasTaken2 should do with [Serene Seasons](https://modrinth.com/mod/serene-seasons) (mod id
`sereneseasons`, package `sereneseasons`), the seasons mod by the Glitchfiend team. This file sets the
order of work, what each step needs and how each one is checked. Once the work is built, how it works
goes in `src/main/sereneseasons/AGENTS.md`.

Written on 2026-09-26 from:

- the repository [Glitchfiend/SereneSeasons](https://github.com/Glitchfiend/SereneSeasons), branches
  `1.21.1` at `cb33de7` (2026-09-05), `1.21.11`, `26.1.2`, `26.2` and `26.3` at `3c0fe4b`
  (2026-09-21);
- the Modrinth project `serene-seasons` (`e0bNACJD`), 7 M downloads, Fabric, Forge and NeoForge;
- Cold Sweat `2.4.3.1` sources, for what it already does with seasons.

Every step is done (2026-09-26), each with the recommended option (3c, 4a, 5a); how the code works is
in [src/main/sereneseasons/AGENTS.md](../../../src/main/sereneseasons/AGENTS.md). Where the work parted
from the plan below, the step says so.

## Which build for which node

Serene Seasons ships **every node we build**, on both loaders, and every build requires
[GlitchCore](https://modrinth.com/mod/glitchcore) (`s3dmwKy5`). Modrinth lists that dependency only on
the 1.21.x uploads, but the 26.x jars' manifests require it too (`glitchcore >=26.3.0.0.3` on 26.3), so
every table pins one. Fabric also requires Fabric API, which we already have.

| Node | Serene Seasons | Modrinth id | Requires on `runClient` |
|---|---|---|---|
| `1.21.1` | `10.1.0.9` | `q34wStJM` | GlitchCore `2.1.0.2` (`sux8kYHe`) |
| `1.21.1-neoforge` | `10.1.0.9` | `pHEgQQUE` | GlitchCore `2.1.0.2` (`S2TfWrZR`) |
| `1.21.11` | `21.11.0.4` | `6GpaVF7N` | GlitchCore `21.11.0.4` (`CO7NeLTt`) |
| `1.21.11-neoforge` | `21.11.0.4` | `eDxStG5h` | GlitchCore `21.11.0.4` (`6dbbrOrO`) |
| `26.1.x` | `26.1.2.0.7` | `kxB8vG5W` | GlitchCore `26.1.2.0.2` (`WNtSATXw`) |
| `26.1.x-neoforge` | `26.1.2.0.7` | `atclxQCD` | GlitchCore `26.1.2.0.2` (`mYUbCfgT`) |
| `26.2.x` | `26.1.2.0.6` | `q5mzi8wy` | GlitchCore `26.2.0.0.0` (`SDUCBYRU`) |
| `26.2.x-neoforge` | `26.1.2.0.6` | `MZd2wAtJ` | GlitchCore `26.2.0.0.0` (`POAebwFo`) |
| `26.3.x` | `26.1.2.0.7` | `V9PxJPuw` | GlitchCore `26.3.0.0.3` (`aaUghyGp`) |
| `26.3.x-neoforge` | `26.1.2.0.7` | `QnvW5HPm` | GlitchCore `26.3.0.0.3` (`5FMzBcg1`) |

The `26.1.x` node covers 26.1 to 26.1.2; the Serene build is for 26.1.2 only (and `26.1.1.0.1` for
26.1.1). Pin the 26.1.2 one; a player on 26.1 simply has no Serene to meet.

It is the first integration on **every node and both loaders**, so the row has
`loaders = setOf(FABRIC, NEOFORGE)` and `checkLoaderSeam` forbids naming either loader's API anywhere in
`src/main/sereneseasons`. Nothing below needs a loader API.

## The API, and what differs between versions

Everything we need is in `sereneseasons.api.season`, identical on every branch except where noted:

| Call | What it gives | Versions |
|---|---|---|
| `SeasonHelper.getSeasonState(Level)` → `ISeasonState` | works on client and server | all |
| `ISeasonState.getSubSeason()` / `getSeason()` / `getTropicalSeason()` | `EARLY_SPRING` … `LATE_WINTER`; `SPRING` … `WINTER`; `EARLY_DRY` … `LATE_WET` | all |
| `ISeasonState.getSeasonCycleTicks()`, `getSubSeasonDuration()`, `getDay()` | position in the year, for blending between sub-seasons | all |
| `ISeasonState.getSubSeasonProgress()` / `getCycleProgress()` | the same, ready made | 26.2+ only |
| `SeasonHelper.usesTropicalSeasons(Holder<Biome>)` | whether a biome runs the wet/dry cycle (`#sereneseasons:tropical_biomes`) | all |
| `SeasonHelper.hasSeasons(Level)` | the dimension whitelist | **26.2+ only**; before it, `sereneseasons.init.ModConfig.seasons.isDimensionWhitelisted(ResourceKey<Level>)`, an internal class |
| `SeasonChangedEvent.Standard` / `Tropical` | fired on a sub-season change | all, but a **GlitchCore** event on 1.21.x: posting and listening go through GlitchCore's bus |

Two differences, both for `com.thirstwastaken2.sereneseasons.platform` (`checkVersionSeam` allows a
`//?` nowhere else): the dimension check, and blending progress (compute it ourselves from cycle ticks
on every version rather than fork for `getSubSeasonProgress`). Avoid `SeasonChangedEvent` entirely:
the drain reads the season each time it recomputes, so no listener is needed and GlitchCore is never
named.

`sereneseasons.season.SeasonHooks` (the seasonal biome temperature, precipitation) is **not** API: it
is public but lives outside `api`, and reads the internal `ModConfig`. Do not call it.

## What Serene Seasons changes in the world, and what that means for us

Serene Seasons does not touch `Biome.getBaseTemperature`. It mixins `Biome.shouldSnow`,
`Biome.shouldFreeze`, `Level.isRainingAt`, `ServerLevel.tickPrecipitation` and
`BlockStateBase.randomTick` (crops), and keeps its own seasonal temperature for those calls only:

| Serene Seasons | Default | What it does to us today |
|---|---|---|
| Biome temperature offset (`biome_temp_adjustment`) | winter `-0.8`, early spring and late autumn `-0.25`, **summer `0`** | nothing: `ThirstManager.climateModifier` reads `getBaseTemperature`, so the drain is the same all year. Step 3 |
| Only biomes with base temperature ≤ 0.8, not `#sereneseasons:tropical_biomes` or `blacklisted_biomes` | — | deserts, badlands, jungles never cool; the scorching override (`waterEvaporates`) is unchanged |
| Winter: rain becomes snow, water freezes | `generate_snow_ice = true` | `tickPrecipitation` hands `SNOW` to `handlePrecipitation`, so cauldrons get powder snow and the **Hanging Pot stops filling** (it takes `RAIN` only). Surface water ices over. Step 2 checks, step 4 decides |
| Tropical dry season: no rain in `MID_DRY`, always rain in `MID_WET` | — | rain collection stops in jungles/savannas mid dry season; our humidity term still says "wet" from `biome.hasPrecipitation()`. Step 3 |
| Seasonal crops (`#sereneseasons:*_crops`) | — | we register no crop or plant block, so nothing |
| Calendar item, Season Sensor block | — | shows the season; nothing for us to add to the HUD |

### Cold Sweat already feeds seasons in

Cold Sweat has `SereneSeasonsTempModifier`: with both mods, `Temperature.get(player, Trait.WORLD)` adds
Cold Sweat's own per-season temperatures (its `SPRING_TEMPS` … `WINTER_TEMPS`). On `1.21.1-neoforge`
with Cold Sweat and `coldSweatClimate` on, **our drain already follows the seasons**. Step 3 must not
count the season a second time there.

## Status

| # | Item | Kind | Nodes | Status |
|---|---|---|---|---|
| 1 | Build dependency and gate | build | all ten | Done |
| 2 | What actually happens today: rain, snow, ice, drain, Cold Sweat | investigation | `26.3.x`, `1.21.1-neoforge` | Done: rain and ice from the source, the drain in game |
| 3 | Seasons drive the thirst drain | feature | all | Done, 3c |
| 4 | Winter and the Hanging Pot / cauldrons | decision | all | Decided, 4a: nothing |
| 5 | Seasonal water quality | decision | all | Decided, 5a: nothing |
| 6 | Changelog and player docs | docs | — | Done, but the Modrinth and CurseForge rows wait for a screenshot |
| 7 | Nothing crashes without the mod: `checkOptionalSeam`, `-PwithoutOptional`, `boot.jsonl` | test | all | Done |

## 1. Build dependency and gate

- A row in [the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt):
  `dir = "sereneseasons"`, `depsKey = "deps.serene_seasons"`, `loaders = setOf(Loader.FABRIC,
  Loader.NEOFORGE)`, mixin config `thirstwastaken2.sereneseasons.mixins.json` (empty unless step 4b
  needs one; if nothing needs a mixin, check whether the row can go without, rather than ship an empty
  config), `neoForgeDependencies = listOf("sereneseasons")`. On Fabric, an `main` entrypoint for the
  init hand-over, since no loader API may be named.
- `deps.serene_seasons` in each loader table of `stonecutter.properties.toml` (ids above, with the
  version in the comment, like the other pins), and `deps.glitchcore` in the four 1.21.x loader tables
  for `runClientMod` only. `serene-seasons` and `glitchcore` in `MODRINTH_DEPS` of
  `.github/scripts/update_mc_deps.py`. Watch the updater: Serene's 26.2 and 26.3 uploads reuse the
  version number `26.1.2.0.x`, so it must pick by game version, not version number.
- `compileOnly` and `runClientMod` lines in both loader scripts (GlitchCore on 1.21.x only).
- `SereneSeasonsPresence`: `Loader.isModLoaded("sereneseasons")` plus a marker-class probe on
  `sereneseasons.api.season.SeasonHelper`. The entry class names no Serene type, lambdas included, and
  hands over through a static call.
- `-PwithoutOptional` gets a `serene-seasons` name in `build-logic`.

**Check:** `:26.3.x:build`, `:1.21.1:build`, `:1.21.1-neoforge:build` (the three API shapes: new,
old, old with Cold Sweat), `checkOptionalSeam`, `checkLoaderSeam`, `checkVersionSeam`, `runServer`
prints the init line with and without the mod, `runGametest`.

**Built**, parting from the plan in four places:

- **No mixins, so no mixin config.** `Integration.mixinConfig` became optional, and the row names none.
- **The entrypoint.** A directory both loaders compile can carry neither `@Mod` nor `ModInitializer`,
  and the both-loader integrations before this one were mixins only. Core `platform/` gained
  `@IntegrationEntrypoint`: a `Runnable` Fabric runs through the `thirstwastaken2:integration`
  entrypoint, NeoForge by finding the annotation in the mod's scan data, both straight after
  `ThirstWasTaken2.initialize`. `checkOptionalSeam` treats the annotation as a root.
- **Compile classpath.** Before 26.2 the dimension check reads `ModConfig.seasons`, whose class
  extends GlitchCore's `Config`, which extends Night Config's; javac needs all three. GlitchCore is
  `compileOnly` on every node, and on Fabric so is the Night Config nested in Serene's jar (NeoForge
  ships Night Config itself).
- **runClient** never loads Serene Seasons, as with Cold Sweat: its lines in both loader scripts are
  commented out, to uncomment only while working on the integration. It recolours grass and snows on
  plains, which gets into every other test and screenshot. `-PwithoutOptional=serene-seasons` stays a
  known name.

## 2. Investigation: what actually happens today

A throwaway agent script with Serene Seasons and no code of ours, `/season set <sub_season>` to move
through the year. On `26.3.x` (Fabric) and `1.21.1-neoforge` (with and without Cold Sweat):

| Case | To find out |
|---|---|
| Drain modifier in plains in mid summer, mid winter | equal (expected: yes, base temperature only) |
| Same with Cold Sweat, `coldSweatClimate` on | differs by Cold Sweat's season temperatures (expected: yes) |
| Empty cauldron and Hanging Pot in plains, raining, mid winter | powder snow in the cauldron, the pot stays empty (expected) |
| Same in a jungle in `MID_DRY` and `MID_WET` | no rain, then rain every tick |
| A powder snow cauldron carried into spring | stays powder snow (vanilla has no melting) |
| A water source in plains through winter | freezes; drinking by hand, bottle and canteen from ice fails |
| Jade on frozen and unfrozen water | our line shows only on water, not ice |
| `WaterPurity.rainwaterPurity()` on a filled cauldron | unchanged |

Recorded 2026-09-26. The rain, snow and ice rows were settled from Serene Seasons' source rather than
in game, since nothing of ours takes part: its `ServerLevel.tickPrecipitation` redirect decides `RAIN`,
`SNOW` or nothing before `handlePrecipitation` is called, which is the only way in for the Hanging Pot
and the cauldron mixins, and freezing goes through its `Biome.shouldFreeze` redirect. The drain rows
were measured in game, under step 3. The Cold Sweat row was not run in game (Cold Sweat is left out
of runClient); the code skips the season factor whenever a measured temperature is in use.

## 3. Seasons drive the thirst drain

The expected player story: you drink more in summer, less in winter. Serene Seasons' own offsets only
cool (summer is `0`), so reading them would only make winter easier. Options:

- (a) **Serene's own temperature.** Recompute `getBiomeTemperatureInSeason` from the API: the
  sub-season's offset needs the internal `ModConfig.seasons.getSeasonProperties`, and the summer
  offset is `0`. Honest to the world (snow and drain agree) but summer feels like spring, and it names
  an internal class on every version.
- (b) **Our own offset per sub-season**, added to the biome's base temperature under the same rules
  Serene uses (base ≤ 0.8, not tropical, not `#sereneseasons:blacklisted_biomes`, dimension has
  seasons). Blended across the sub-season so there is no step at a boundary. Defaults, in biome units:

  | Early / mid / late | Spring | Summer | Autumn | Winter |
  |---|---|---|---|---|
  | offset | `-0.25` / `0` / `0` | `+0.1` / `+0.3` / `+0.2` | `0` / `0` / `-0.25` | `-0.5` / `-0.8` / `-0.5` |

  With the current curve, plains (0.8) in mid summer: `(1.3 × 0.5) / 1.4` ≈ 0.46 before the config
  multiplier and softening, against 0.71 in spring. **Careful:** above 1.0 the curve halves, so a warmer
  plains drains *less*. The curve was built for biome temperatures; step 3 must either keep the summer
  offset under the fold (plains + 0.2 = 1.0, so offsets ≤ 0) or add the offset **after** the curve as
  a multiplier. Settle this with numbers from `climateModifier` before writing defaults.
- (c) **A drain multiplier per season**, applied after the climate modifier: summer ×1.15, winter
  ×0.9, spring/autumn ×1.0, blended. Independent of the temperature curve's fold, easy to read in the
  config, one number per season.

  Tropical biomes: dry season uses the dry humidity (`1.1`), wet season the wet one (`1.4`), in both
  (b) and (c), replacing `biome.hasPrecipitation()`.

**Recommended (c)** unless step 2 or the curve check makes (b) clean, because it cannot be inverted by
the curve and is the easiest for a modpack maker to reason about.

**Built as (c).** The curve check settled it: plains at +0.3 goes from 0.71 to 0.46 before softening,
so (b) would have needed every summer offset at or below 0. The factor defaults to 1.0, 1.15, 1.0, 0.9
and applies where Serene Seasons' own temperature shifts; deserts, badlands, jungles and savannas never
take it. The tropical humidity follows Serene's rain exactly: dry at `MID_DRY`, wet at `MID_WET`.
Measured by [tools/agent/integrations/serene-seasons.jsonl](../../../tools/agent/integrations/serene-seasons.jsonl)
on `26.3.x` and `1.21.1-neoforge`, the same on both, at the default `thirstDepletionModifier` 1.2:

| Where, when | Modifier | Without the mod (computed) |
|---|---|---|
| Plains, first tick of mid spring | 0.913 | 0.929 |
| Plains, first tick of mid summer | 1.045 | 0.929 |
| Plains, first tick of mid winter | 0.851 | 0.929 |
| Snowy plains, mid summer | 0.659 | 0.586 |
| Jungle, late summer (middle of the dry season) | 0.814 | 0.746 |
| Jungle, late winter (middle of the wet season) | 0.746 | 0.746 |
| Desert, late summer (dry season) | 1.2 | 1.2 |
| Desert, late winter (wet season) | 0.971 | 1.2 |

`/season set` puts the calendar at a sub-season's first tick, so mid summer reads 1.125 of the factor
(five sixths of the way from spring's middle to summer's), not 1.15.

The settings are a Seasons tab on the Thirst page, beside a Drain tab holding what was there. The tab
is left off without Serene Seasons, and a page left with one tab shows no strip, so the page looks as
it did.

Wiring, without core naming Serene Seasons:

- A second internal seam in `ThirstManager` next to `setClimateTemperature`: a
  `ToDoubleFunction<Player>` season multiplier (and a humidity override) that defaults to none and
  `SereneSeasonsPresence` sets at init. Not `com.thirstwastaken2.api`, no `API_VERSION` bump.
- **With Cold Sweat active** (its source set and `coldSweatClimate` on and it returned a finite
  temperature), skip the seasonal term: Cold Sweat already adds seasons. One line in
  `climateModifier`, with a comment at the divergence.
- Cost: `getSeasonState` is a map lookup per level, and the modifier is cached for 20 ticks
  (`MODIFIER_REFRESH_TICKS`). `runBenchmark` on `26.3.x` before and after.
- Config: `sereneSeasonsClimate` (default true) and, for (c), four multipliers `seasonDrainSpring` …
  `seasonDrainWinter` (clamped 0.25 – 4). Field, `sanitize()`, widgets in the Thirst page's climate tab
  of `ConfigCategory` (by topic, next to `coldSweatClimate`), lang in all nine files. They show on every
  node and do nothing without the mod; the tooltip says so.

**Check:** a gametest cannot load Serene Seasons, so an agent script
`tools/agent/integrations/serene-seasons.jsonl` reads the modifier in plains in each season, in a
jungle in dry and wet, in a desert (unchanged), in the Nether (unchanged), with the toggle off, and on
`1.21.1-neoforge` with Cold Sweat (no double count).

## 4. Winter and the Hanging Pot / cauldrons

In winter a player's rain collection stops: the Hanging Pot ignores snow, and a cauldron gets powder
snow that never turns back into water. That is vanilla behaviour in a cold biome, now reaching plains
for a quarter of the year.

- (a) **Nothing**, and say so in the docs: winter means melting snow or finding unfrozen water.
  Players of Serene Seasons expect it.
- (b) The Hanging Pot over a lit fire takes snow as well, as melted rainwater (a serving per snowfall
  tick, at the rain chance). Core change (`handlePrecipitation` accepts `SNOW` when the pot is lit), not
  Serene-specific, so it also changes snowy biomes without the mod.
- (c) (b), plus a powder snow cauldron over a fire melts into rainwater-grade water.

**Recommended (a)** for this integration; (b)/(c) are a core feature to weigh on their own (roadmap),
because they change snowy biomes for every player, with or without seasons. **Decided (a):** the
player docs say winter stops rain collection.

## 5. Seasonal water quality

Could summer make surface water worse (stagnant, algae) or winter make it better? Sampling
(`WaterPurity.sampleAt`) is by source and biome; a season term would make the same pond change grade
through the year.

- (a) **Nothing.** Grades stay a property of the place, which players learn once.
- (b) In summer, a still water source in a swamp or a river-less biome samples one grade worse.

**Recommended and decided (a).** Revisit only if players ask; it touches the purification balance in
[WATER-REFERENCE.md](../mechanics/WATER-REFERENCE.md).

## 6. Changelog and player docs

A changelog entry and a short "Serene Seasons" section on the compatibility page: you drink more in
summer, less in winter, the dry season stops the rain, and the settings that change it. Through the
`write-docs` skill. Add the mod to the Modrinth and CurseForge pages' compatibility lists. Done for the changelog, the
integration page, configuration, installation and the climate section of Thirst and Quenched. The
Modrinth and CurseForge rows are not added yet: each row carries one screenshot of the integration in
real terrain, and it was decided not to take one: the integration adds no block or item, so the
docs page shows the settings screen's Seasons tab (`config/config-seasons.png`) instead.

## 7. Nothing crashes without the mod

`checkOptionalSeam` (it reads the package from the table), `boot.jsonl` with
`-PwithoutOptional=serene-seasons` on one Fabric and one NeoForge node, and `runServer` with the mod on
a 1.21.x node to prove GlitchCore's event bus is never touched. Per the test-only-what-you-touch rule,
the full ten-node run waits for the release.
