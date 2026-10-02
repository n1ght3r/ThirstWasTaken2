# src/main/sereneseasons — Serene Seasons on every node

[Serene Seasons](https://modrinth.com/mod/serene-seasons), the seasons mod by the Glitchfiend team
(mod id `sereneseasons`, package `sereneseasons`). With it installed, the thirst drain follows the
season: a temperate biome drains faster in summer and slower in winter, and a tropical biome's dry
season counts as dry. Why each choice was made, and what was found in game, is in
[docs/dev/integration/climate/SERENE-SEASONS-INTEGRATION.md](../../../docs/dev/integration/climate/SERENE-SEASONS-INTEGRATION.md).

This directory is **compiled by every node, on every loader**: Serene Seasons ships them all, Forge 47
on 1.20.1 included, and each table of `stonecutter.properties.toml` sets `deps.serene_seasons`. So nothing
in it may name a loader's API (`checkLoaderSeam`), and it has no mixins. Its 1.20.1 builds (9.1) have
the same API as the later ones; only the dimension check differs, as before 26.2.

```
java/com/thirstwastaken2/sereneseasons/
  SereneSeasonsEntrypoint  @IntegrationEntrypoint Runnable; names no Serene Seasons class
  SereneSeasonsPresence    the gate: the mod id, and the API's two classes as resources
  SereneSeasonsClimate     the SeasonalClimate core asks: the drain factor and the tropical humidity
  platform/SeasonsPlatform whether a dimension has seasons, the one call that differs between builds
```

## How it stays optional

1. **Build.** Only when `deps.serene_seasons` is set, the loader scripts add this directory and an
   optional `sereneseasons` dependency to the built `neoforge.mods.toml` or Forge's `mods.toml`, and the Fabric one a
   `thirstwastaken2:integration` entrypoint to `fabric.mod.json`, as
   [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt) says.
2. **Entrypoint.** Being on every loader, it can carry neither `@Mod` nor `ModInitializer`. It is a
   plain `Runnable` marked `@IntegrationEntrypoint` (core `platform/`): Fabric runs it through the
   `thirstwastaken2:integration` entrypoint after `ThirstWasTaken2.initialize`, NeoForge and Forge find it
   in the mod's scan data by the annotation and run it in the same place. `checkOptionalSeam` treats the
   annotation as a root.
3. **Runtime gate.** The entrypoint calls `SereneSeasonsClimate.install()` only after
   `SereneSeasonsPresence.isPresent()`.
4. **Core never names it.** The season reaches the drain through `data/SeasonalClimate`, set with
   `ThirstManager.setSeasonalClimate`. Not API: no `API_VERSION` bump, no promise to other mods.

**Nothing outside this directory may reference a class in it.**

## Climate

`ThirstManager.climateModifier` asks the `SeasonalClimate` twice, only while `sereneSeasonsClimate` is on:

- **Humidity.** In a tropical biome (`SeasonHelper.usesTropicalSeasons`, Serene's
  `#sereneseasons:tropical_biomes`), the middle of the dry season (`MID_DRY`) counts as no
  precipitation and the middle of the wet one (`MID_WET`) as precipitation, the rest as the biome says:
  exactly when Serene Seasons stops and forces rain. So a jungle drains faster mid dry season, and a
  desert (also in that tag) slower mid wet season.
- **A factor after the curve.** Not a temperature: the curve halves anything above 1, so a warmer
  plains would drain *less*. `seasonDrainSpring` … `seasonDrainWinter` (default 1.0, 1.15, 1.0, 0.9,
  clamped 0.25 – 4) hold at the middle of each season and blend in a straight line to the next, from
  `getSeasonCycleTicks` and `getSubSeasonDuration` (the same on every build). Only where Serene Seasons'
  own temperature shifts: a dimension with seasons, a biome not in `#sereneseasons:blacklisted_biomes`,
  not tropical and no warmer than 0.8. Deserts, badlands, jungles and savannas take no factor.
- **Cold Sweat.** On `1.21.1-neoforge` with Cold Sweat and `coldSweatClimate` on, Cold Sweat's world
  temperature already carries its own season offsets (`SereneSeasonsTempModifier`), so a measured
  temperature takes no factor. The humidity still applies.

Serene Seasons' own `biome_temp_adjustment` is not read: it lives in its internal config and only ever
cools. The modifier is cached 20 ticks per player, so this costs two calendar reads a second per player.

Rain collection needs nothing here: Serene Seasons' `ServerLevel.tickPrecipitation` redirect hands
`SNOW` to `handlePrecipitation` in winter and nothing in the dry season, so the Hanging Pot and
cauldrons stop filling by themselves.

## Config screen

The five settings are the Thirst page's Seasons tab, each `.requires("sereneseasons")`. Without the mod
the tab has no settings and is left off, so the page shows no tab strip.

## Testing

The gametests run without Serene Seasons and prove every node loads without it. runClient never loads
it, as with Cold Sweat (it recolours grass and snows on plains, which gets into other tests and
screenshots): uncomment its lines in both loader scripts, Serene Seasons, GlitchCore and on Fabric the
Night Config nested in its jar, only while working on it.
[tools/agent/integrations/serene-seasons.jsonl](../../../tools/agent/integrations/serene-seasons.jsonl)
drives a client with it. Run on 2026-09-26 on `26.3.x` (Fabric) and `1.21.1-neoforge`, every check
passed with the same numbers on both: plains 0.913 in mid spring, 1.045 in mid summer and 0.851 in mid
winter (0.929 in every season without it, by the formula); snowy plains 0.659 in mid summer; a jungle 0.814 in its dry season and
0.746 in its wet one; a desert 1.2 in the dry season and 0.971 in the wet one. `boot.jsonl` with
`-PwithoutOptional=all` came up on `26.3.x-neoforge`.
Run again on 2026-09-28 on `1.20.1` and `1.20.1-forge` (Serene Seasons 9.1.0.3): the same numbers, every
check passed on both. A production Forge 47.4.10 server with the shipped jar, Serene Seasons and GlitchCore
logged "Serene Seasons found, thirst follows the season".
