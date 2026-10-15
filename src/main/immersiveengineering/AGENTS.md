# src/main/immersiveengineering — water through Immersive Engineering on NeoForge

[Immersive Engineering](https://modrinth.com/mod/immersiveengineering) (mod id `immersiveengineering`,
package `blusunrize.immersiveengineering`) moves water in pumps, pipes, barrels and machines as NeoForge
`FluidStack`s. The plan, its decisions and what was found in game are
[docs/dev/integration/storage/IMMERSIVE-ENGINEERING-INTEGRATION.md](../../../docs/dev/integration/storage/IMMERSIVE-ENGINEERING-INTEGRATION.md).

**Built on `1.21.1-neoforge` only**, IE's newest build (12.4.2). Its 1.20.1 build for Forge 47 has the
pump and Bottling Machine mixins in [src/main/immersiveengineeringforge](../immersiveengineeringforge/AGENTS.md).
What it does:

- **Fluid Pump**: every bucket it takes from the world is stamped with the grade of the block it came
  from, on both of its paths: a pool it drains block by block (`Utils.drainFluidBlock`) and an infinite
  pool it draws from without taking a block (`gatherInfiniteFluidFromWorld`).
- **Bottling Machine**: a glass bottle filled by recipe comes out with the grade of the water spent.
- **Jerrycan and IE's other fluid items** (`IEItemFluidHandler`): top up only with water of the same
  grade. IE compared fluids alone there, so Murky water poured into a can of Pure became Pure.

Nothing else needs code. IE's barrels, Fluid Tank multiblock and the pump's own tank are NeoForge
`FluidTank`s, which compare components; pipes pass stacks on with `copyWithAmount`, which keeps them;
recipes that use water (Cloche, Mixer, concrete) take it by tag, whatever its grade.

```
immersiveengineering/java/com/thirstwastaken2/immersiveengineering/
  ImmersiveEngineeringPresence     whether each target class and method is still there, read off the class file
  ImmersiveEngineeringMixinPlugin  applies each mixin only while its target declares what it injects into
  mixin/FluidPumpMixin             tickServer's drainFluidBlock, gatherInfiniteFluidFromWorld's new FluidStack
  mixin/BottlingProcessMixin       keeps the grade at the drain, stamps getRecipeItemOutputs
  mixin/ItemFluidHandlerMixin      IEItemFluidHandler.fill: same fluid and same grade
immersiveengineering/resources/
  thirstwastaken2.immersiveengineering.mixins.json
```

## How it works

- **Sampling is `SampledWater`**, from `src/main/neoforge`, shared with Create, Sophisticated and
  Mekanism: a cauldron's stored grade, else `WaterPurity.sampleAt`. It is asked only for a water block
  the pump is about to take, once a bucket: every 40 ticks on a pool it drains, every 20 on an infinite
  one. The block is sampled before `drainFluidBlock` runs, since an executing call picks it up.
- **Grades are kept apart.** The pump's tank refuses a bucket of another grade, so a pump holding
  Murky water waits on a Clean block until its tank has emptied into its outputs, as it waits on any
  other fluid.
- **The Bottling Machine spends its water partway through a process** and hands the bottle out at the
  end. The grade is kept from the drain in a `@Unique` field, not saved: a process loaded from a save
  after its drain reads the tank instead. Only an output that is a water container is stamped, on a
  copy. A container the machine fills through its fluid capability (a bucket, a waterskin) takes no
  recipe; `WaterContainerFluids` stamps it.
- **`getRecipeItemOutputs` is targeted by full descriptor**: it overrides a generic method, so the
  compiler adds a bridge with the same name that would be injected twice.

## How it stays optional

1. **Build.** Only where `deps.immersive_engineering` is set does the NeoForge script add this
   directory and append the mixin config and an optional `immersiveengineering` dependency to the built
   manifest, from [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   It is a `compileOnly`; on `runClient` it is off by default. Uncomment its `runClientMod` line in
   `build.neoforge.gradle.kts` to work on it.
2. **Runtime gate.** `ImmersiveEngineeringMixinPlugin` answers from class files, never loading a
   class, and skips a mixin whose target, or the method it wraps in another class (`Owner#method`),
   lost a method, with a warning.

## Checking it

- `checkOptionalSeam` passes, and the node builds and boots without IE.
- [tools/agent/integrations/immersive-engineering.jsonl](../../../tools/agent/integrations/immersive-engineering.jsonl)
  checks both pump paths on sea water, a pump in plains, the Jerrycan on a barrel of another grade and
  the Bottling Machine. Its header says what each line should read.
