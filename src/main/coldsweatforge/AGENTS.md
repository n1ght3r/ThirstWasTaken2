# src/main/coldsweatforge — Cold Sweat on Forge 1.20.1

The Cold Sweat integration of [src/main/coldsweat](../coldsweat/AGENTS.md) on MinecraftForge 47, for
Cold Sweat's 1.20.1 build. Read that guide first: the climate, the waterskin, the Boiler, the campfire,
the furnace and smoker recipes and the hot drinks behave the same, for the same reasons. This file only
records what differs on Forge 1.20.1.

This directory is **only compiled by nodes that set `deps.cold_sweat` on Forge**: its row in
[the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt)
shares the deps key with the NeoForge one and lists only `Loader.FORGE`. Today that is `1.20.1-forge`,
with Cold Sweat 2.4.3.2 (`VCWBWOkR`). It is a directory of its own rather than a fork of
`src/main/coldsweat`, as `createforge` is of `create`: the presence check names the loader's mod list,
the waterskin's tank branch its fluid API, the entrypoint its entrypoint API, and the recipes and food
data its conditions, so every file but two would fork.

```
java/com/thirstwastaken2/coldsweatforge/
  ColdSweatPresence      the gate: FML's mod file for `cold_sweat`, a marker class in it, and each
                         mixin target's methods, read off the class file under either of two names
  ColdSweatEntrypoint    an @IntegrationEntrypoint Runnable; Forge 47 takes one @Mod class per mod id
  ColdSweatMixinPlugin   applies the mixins only when the gate passes and their methods are there
  ColdSweatClimate       hands core the world temperature; the capability is a LazyOptional here
  WaterskinWater         the grade a skin is filled with, on Forge's FluidStack; stripping an empty one
  BoiledWater            the NeoForge copy unchanged
  mixin/                 the five NeoForge mixins, split by what is remapped
resources/
  thirstwastaken2.coldsweatforge.mixins.json
  data/thirstwastaken2/recipes/cold_sweat/           furnace and smoker recipes for the skin, 1.20.1's
                                                     plural folder and the mod's own NBT recipe types
  data/thirstwastaken2/cold_sweat/item/food/         Cold Sweat food data: hot drinks warm
```

Cold Sweat's 1.20.1 classes are the 1.21.1 ones in NBT: the same `WaterskinItem.getFilledItem`,
`useOn` lambda, `FilledWaterskinItem.getCraftingRemainingItem`, `BoilerBlockEntity.tick` and
`checkForItems`, `BoilerContainer$2`, and the same branches keyed on the other port's mod id `thirst`,
dead with this mod installed. Its campfire mixin `@ModifyArg`s the same drop in `cookTick` that
`CampfireWaterskinMixin` wraps.

## What differs from the NeoForge integration

- **Names at runtime.** A player's Forge runs under SRG names, development under Mojang's, so each
  injection says whether its target is Minecraft's. `useOn`, `canPlaceItemThroughFace`, `mayPlace`,
  `cookTick`, `ItemStack.is` and `Containers.dropItemStack` are, and go through the refmap. `getFilledItem`,
  the lambda, `getCraftingRemainingItem` (Forge's), the Boiler's `tick`, `checkForItems` and
  `hasDrinkables`, and Forge's `IFluidHandler.drain` are not, and are `remap = false`. Class names are
  Mojang's in both. The mixin plugin reads the target's class file, which is in SRG names in a player's
  game, so a Minecraft method is asked for by both names (`useOn` or `m_6225_`).
- **Entrypoint.** `ThirstWasTaken2Forge` runs every `@IntegrationEntrypoint` while it is constructed.
- **The empty skin.** Cold Sweat copies the filled skin's whole tag onto the empty one and removes its
  own keys. `ItemWaterData.clearQuality` takes the grade off and, on 1.20.1, drops the
  `thirstwastaken2` compound once nothing is left in it, so the empty skin stacks with a new one.
- **Recipes** are `thirstwastaken2:smelting` and `smoking` (`platform/NbtRecipes`), since 1.20.1 puts no
  tag on a vanilla cooking result, with a `forge:partial_nbt` ingredient and `conditions`
  (`forge:mod_loaded`). The result is a new skin, so its temperature and sips reset, as on NeoForge.
- **Food data** is a Forge data pack registry on 1.20.1, read by vanilla's `RegistryDataLoader`, which
  Forge 47 patches to skip an entry whose `forge:conditions` fail before decoding it. So each file
  carries `forge:conditions` with `forge:mod_loaded` for Cold Sweat and the drink's mod, the same guard
  `neoforge:conditions` is on NeoForge. `checkDataConditions` counts it as a gate.

## Compiling against Cold Sweat

The Modrinth jar is in SRG names, so it is `modCompileOnly` and remapped, with no transitive
dependencies. Its run line in `build.forge.gradle.kts` is commented out, as on NeoForge; uncomment it to
work here.

## Testing

The gametests run without Cold Sweat and prove the node still loads without it.
[tools/agent/integrations/cold-sweat-1.20.1.jsonl](../../../tools/agent/integrations/cold-sweat-1.20.1.jsonl)
is `cold-sweat.jsonl` in NBT, with the freezing case left out; its header says how to run it. Run on
2026-10-01, every check passed:

- a plains source fills a Murky skin, a sea source a salty one, and the tooltip shows the grade;
- a Dirty three-layer cauldron gives a Dirty skin and keeps two layers; the last layer of a Pure
  cauldron gives a Pure skin and empties the cauldron; pours store the worse grade;
- a crouched Pure sip takes thirst 4 to 10 and quenched 0 to 8; a salty one costs thirst and gives
  Parched II; pouring over yourself restores nothing; each leaves an empty skin with no
  `thirstwastaken2` compound;
- the Boiler takes a Dirty bottle, a Murky skin and a salty bucket from a hopper and leaves them Pure,
  Pure and salty; a Hearth keeps a water bucket in its hopper (it kept a lava bucket too here, so that
  half is only recorded);
- skins off a campfire come out Clean, Pure and salty; a Dirty skin smelts Clean; a canteen still boils
  Pure in hand; at thirst 10 health does not regenerate, at full thirst it does;
- hot cocoa takes the base temperature from 0 to 10, and the gauge sits between the hearts and the
  thirst bar;
- climate modifiers 0.83 in plains, 0.77 in snowy plains, 0.87 in a desert by day and 1.04 by night,
  against 0.93, 0.59 and 1.2 from the biome alone. At y 200 Cold Sweat reads the strips alike, as on
  NeoForge.

A world opened with Cold Sweat and without Farmer's Delight and Kaleidoscope Cookery (`boot.jsonl`,
`-PwithoutOptional=farmersdelight,kaleidoscope_cookery`). The shipped `reobfJar` with Cold Sweat
2.4.3.2 on a production Forge 47.4.10 server from the official installer applied every mixin with no
warning, and from console commands the Boiler, the campfire and the furnace gave the grades above.
