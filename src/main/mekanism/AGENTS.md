# src/main/mekanism — water through Mekanism on NeoForge

[Mekanism](https://modrinth.com/mod/mekanism) (mod id `mekanism`, package `mekanism`) moves water in
tanks, pipes and machines as NeoForge `FluidStack`s. The plan, its decisions and what was found in game
are [docs/dev/integration/storage/MEKANISM-INTEGRATION.md](../../../docs/dev/integration/storage/MEKANISM-INTEGRATION.md).

**Built on `1.21.1-neoforge` only**, Mekanism's newest build (10.7.19). Its 1.20.1 build for Forge 47 has
the same two mixins in [src/main/mekanismforge](../mekanismforge/AGENTS.md). What it does:

- **Electric Pump**: every bucket it takes from the world is stamped with the grade of the block it came
  from, so a pump in the sea fills its tank with sea water rather than Clean water.
- **Rotary Condensentrator**: water condensed from water vapour is Pure, whatever went in. Sea water
  boiled to vapour and condensed again is fresh: Mekanism's way of distilling, at the cost of power.

Nothing else needs code. Mekanism's tanks, pipes, Dynamic Tank and portable tanks compare fluid stacks
with `isSameFluidSameComponents`, so a grade travels with the water and two grades never merge.

```
mekanism/java/com/thirstwastaken2/mekanism/
  MekanismPresence       whether each target class and method is still there, read off the class file
  MekanismMixinPlugin    applies each mixin only while its target declares what it injects into
  mixin/ElectricPumpMixin   suck(BlockPos, boolean, boolean): stamps each getOutput with SampledWater
  mixin/RotaryRecipeMixin   BasicRotaryRecipe.getFluidOutput: condensed water comes out Pure
mekanism/resources/
  thirstwastaken2.mekanism.mixins.json
```

## How it works

- **The pump builds its stack twice.** `suck` calls `getOutput` once to ask whether the block is
  wanted, then picks the block up and calls it again. The second time the block is gone, so the mixin
  keeps the first sample for that position. With `pumpInfiniteFluidSources` off and water source
  conversion on, the block is never picked up and both calls see it.
- **Sampling is `SampledWater`**, from `src/main/neoforge`, shared with Create and Sophisticated: a
  cauldron's stored grade, else `WaterPurity.sampleAt`. It is asked only for a water source the pump is
  about to take, once a bucket.
- **Grades are kept apart.** The pump compares each stack with the last one it took, components
  included, so a pump holding Murky water leaves a Clean source in the world, as it leaves lava when it
  holds water. On a lake of one grade nothing changes.
- **Every rotary recipe that condenses into water** is distilled, another mod's or a data pack's
  included. The recipe hands out a copy, so stamping it touches nothing it keeps. Heavy water is not in
  `minecraft:water` and is left alone.

## How it stays optional

1. **Build.** Only where `deps.mekanism` is set does the NeoForge script add this directory and append
   the mixin config and an optional `mekanism` dependency to the built manifest, from
   [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   It is a `compileOnly`; on `runClient` it is off by default. Uncomment its `runClientMod` line in
   `build.neoforge.gradle.kts` to work on it.
2. **Runtime gate.** `MekanismMixinPlugin` answers from class files, never loading a class, and skips a
   mixin whose target lost a method, with a warning.

## Checking it

- `checkOptionalSeam` passes, and the node builds and boots without Mekanism.
- [tools/agent/integrations/mekanism-water.jsonl](../../../tools/agent/integrations/mekanism-water.jsonl)
  checks a pump on sea water, a pipe into a Basic Fluid Tank, a pump on a plains pool and the Rotary
  Condensentrator. Its header says what each line should read.
