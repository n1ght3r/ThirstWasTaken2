# src/main/nomansland — the milk cauldron in No Man's Land

[No Man's Land](https://modrinth.com/mod/no-mans-land) (mod id `nomansland`, package
`com.farcr.nomansland`) is an Overworld overhaul. A milk bucket poured into a cauldron becomes its milk
cauldron, four levels, each sipped from an empty hand to take off one milk-curable effect. The plan and
its decisions are
[docs/dev/integration/world/NO-MANS-LAND-INTEGRATION.md](../../../docs/dev/integration/world/NO-MANS-LAND-INTEGRATION.md).

**Built on `1.21.1-neoforge` only**, the mod's only build (1.5.12). What it does:

- **Milk cauldron**: each of the four sips restores a quarter of what a milk bucket restores drunk from
  the hand, rounded, at least one point of thirst.

The rest is not here, because it names no class of the mod and ships on every node:

- **Drinks and foods**: ids in `ThirstConfig` (`noMansLandDrinks`, `noMansLandFoods`). The mod tags
  three of them `c:drinks`; the explicit entries win over the tag.
- **Swamps**: Bog, Bayou, Dark Swamp and Blackwater River are optional entries in
  `thirstwastaken2:stagnant_water`, from `ThirstBiomeTagProvider` in `src/datagen`, as Terralith's are.
  Blackwater River is also in `minecraft:is_river`; `sampleAt` checks `stagnant_water` first.
- **Mud Beach**: an optional entry in `thirstwastaken2:sea_water`, a biome tag `WaterPurity.sampleAt`
  reads beside `is_ocean` and `is_beach`. Not `is_beach` itself: vanilla places buried treasure by that
  tag, and other mods read it too.

```
nomansland/java/com/thirstwastaken2/nomansland/
  NoMansLandMixinPlugin     applies the mixin only where MilkCauldron is on the classpath, by resource lookup
  mixin/MilkCauldronMixin   MilkCauldron.useWithoutItem: a quarter of a milk bucket before the level goes down
nomansland/resources/
  thirstwastaken2.nomansland.mixins.json
```

## How it works

- **A sip is found by its `lowerFillLevel`.** `useWithoutItem` ends every sip with the method's one call
  to `lowerFillLevel`, the last level included, which empties the cauldron. Filling a Farmer's Delight
  milk bottle goes through `useItemOn` and is no sip.
- **The milk bucket decides.** The values are `ThirstApi.thirstValues(Items.MILK_BUCKET)`, so a changed or
  blacklisted milk bucket changes the sips too. They go through `ThirstApi.drink`, a drink's cap without
  `ThirstEvents.DRINK`: a sip has no item to report. The client runs the method as well, where `drink`
  does nothing.

## How it stays optional

1. **Build.** Only where `deps.nomansland` is set does the NeoForge script add this directory and append
   the mixin config to the built manifest with `nomansland` as an optional dependency, from
   [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   It is a `compileOnly`; on `runClient` it is off by default, since it rewrites the Overworld. Uncomment
   its `runClientMod` line in `build.neoforge.gradle.kts` to work on it, on a new world. Biolith and
   Mixed Litter are nested in its jar. `-PwithoutOptional=nomansland` leaves it out.
2. **Runtime gate.** `NoMansLandMixinPlugin` answers with a resource lookup, never loading a class.

## Checking it

- `checkOptionalSeam` finds the plugin loaded without the mod, and passes.
- `runGametest` passes. `noMansLandDrinksAreMergedIntoAnOlderConfig` checks the config values.
- `./gradlew ":1.21.1-neoforge:runClient" -Pagent=tools/agent/smoke/boot.jsonl -PwithoutOptional=nomansland`
  comes up and stays up.
