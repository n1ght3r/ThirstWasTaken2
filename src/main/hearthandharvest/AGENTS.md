# src/main/hearthandharvest — water grades in Hearth and Harvest's tanks, sink, jug and cask

[Hearth and Harvest](https://modrinth.com/mod/hearth-and-harvest) (mod id `hearthandharvest`, package
`alabaster.hearthandharvest`) is a Farmer's Delight addon with a Sink, Jugs, Troughs, a Sprinkler, a
Stomping Basin and Casks. Every one of its tanks poured water in as plain water and drew plain water
out, so a Dirty or salty bottle came back Clean; the Sink filled itself from nothing; a Jug scooped the
sea as fresh water; and the Cask aged sea water into mead. This directory fixes all of that. The plan
and its decisions are in
[docs/dev/integration/cooking/HEARTH-AND-HARVEST-INTEGRATION.md](../../../docs/dev/integration/cooking/HEARTH-AND-HARVEST-INTEGRATION.md).

**Built on `1.21.1-neoforge` only**, the mod's build 1.3.4. Its 1.20.1 build is the early mod and has
its own directory, [`hearthandharvestforge`](../hearthandharvestforge/AGENTS.md). What it does:

- **a tank keeps the grade** poured into it: Dirty in, Dirty out; sea water in, sea water out. Two
  grades never share a tank, as in Create's and Sophisticated's tanks: a bottle of another grade is
  refused;
- **the Sink gives Murky water**, the grade Candlelight's kitchen sink and Farm & Charm's trough give,
  whether it filled itself or was poured into, and refuses sea water. It stays an endless tap;
- **a Jug scooping a source samples it** where it lies, like any water collected from the world;
- **a wet sponge stomped gives Murky water**, and **rain in a Trough is rainwater** (Clean by default);
- **the Cask ages nothing from sea water**; fresh water of any grade still ages.

The salt recipe is not here: `minecraft:salt_from_bottle`, Hearth and Harvest's Cooking Pot recipe, is
replaced by a data file core's datagen writes for 1.21.1 (`FarmersDelightRecipeProvider`), which boils
only sea water into salt. The row in the integration table orders this mod after Hearth and Harvest
(`loadAfter`) so that the file wins; the agent check proves it does. Expanded Delight's sea water guard
lets that one recipe through (see [its AGENTS.md](../expandeddelight/AGENTS.md)). The drink and food
values are ids in `ThirstConfig`.

```
hearthandharvest/java/com/thirstwastaken2/hearthandharvest/
  HearthAndHarvestPresence        the gate: each target class and method read off its class file
  HearthAndHarvestMixinPlugin     applies each mixin only while its target declares every method it needs
  HearthWater                     STANDING (Murky), stamped(), and settle() for water saved unstamped
  StampingTank                    a tank seen through a pour: water filled in is stamped first; sink() also refuses salt
  mixin/FluidHandlingMixin        HHFluidHandling.testEmptying/testFilling: pour and draw with the grade
  mixin/SinkBlockMixin            BasinBlock: refuse salty containers, a sink's tank is Murky, its self-fill Murky
  mixin/SprinklerBlockMixin       SprinklerBlock.useItemOn: FluidUtil replaced by useOnTank, so it keeps the grade
  mixin/JugBlockItemMixin         JugBlockItem.use: the scooped water sampled where it lies
  mixin/TroughBlockEntityMixin    TroughBlockEntity.serverTick: rain stamped with rainwaterPurity
  mixin/StompingBasinRecipeMixin  StompingBasinRecipe.getResultFluid: water (the sponge) is Murky
  mixin/CaskBlockEntityMixin      CaskBlockEntity.getMatchingRecipe: no recipe while a salty container is in
hearthandharvest/resources/
  thirstwastaken2.hearthandharvest.mixins.json
```

## How it works

- **One class for every tank.** The Sink, Jug, Trough and Stomping Basin call `HHFluidHandling.useOnTank`,
  which asks `testEmptying` (container into tank) and `testFilling` (tank into container). It knew a
  water bottle by `potion_contents` alone and poured it in as `new FluidStack(WATER, 250)`, and NeoForge's
  bucket wrapper does the same for a bucket. `FluidHandlingMixin` wraps both:
  - `testEmptying` hands the original an **unstamped** copy of a water container, so their bottle test and
    the bucket wrapper still know it, against a `StampingTank` that stamps whatever is filled in with the
    container's grade. The tank's simulated fill therefore compares the water it will really get, and the
    water handed back is stamped too. The mod's own waterskin, canteens and bowls (`WaterContainerFluids.handles`)
    carry the grade on their capability already and go through untouched;
  - `testFilling` stamps what is drawn with the tank's water, `WaterFluids.quality`, Clean for water with
    no grade.
- **The Sprinkler** called `FluidUtil.interactWithFluidHandler` instead. It goes through `useOnTank` now,
  so it keeps the grade like the rest; a glass bottle draws from it too, which it could not before.
- **The Sink.** Its tank is wrapped in `StampingTank.sink`, so anything poured in becomes Murky and sea
  water is refused, including from a jug item whose fluid is salty; a salty stack in hand is refused at
  the head of `useItemOn` as well, so the click is spent rather than passed on. Its self-fill in `tick` is
  stamped Murky. `settle` restamps water saved unstamped before this, which would otherwise refuse every
  stamped fill and leave the sink playing its fill sound for ever.
- **The Jug** item builds `new FluidStack(type, 1000)` from the source it scoops; the mixin stamps that
  local with `WaterPurity.sampleAt`, on the server only. A jug holding one grade refuses another, and the
  mod then says the jug is full.
- **The Cask** finds its recipe through the private `getMatchingRecipe`; answering empty there while a
  salty container is in an input slot stops ageing and leaves the bottle where it is.
- **Compiled against the mod.** The mixins name its classes, so `build.neoforge.gradle.kts` has it as
  `compileOnly`. Every injection point was checked against the 1.3.4 jar's bytecode.

## Not covered

- **Juice and wine fluids.** Hearth and Harvest moves them through the same tanks; they are not water and
  the mixins leave them alone.
- **A refused bottle on a Stomping Basin** is put into the basin as an item, which the basin does with any
  item a tank refuses. Nothing is lost.
- **1.4.0**, unreleased, adds Kegs and a goat milk fluid. Re-check the plan's list when it ships.

## How it stays optional

1. **Build.** Only where `deps.hearth_and_harvest` is set does `build.neoforge.gradle.kts` add this
   directory and append the mixin config to the built manifest, with `hearthandharvest` as an optional
   dependency ordered before this mod, from
   [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   `-PwithoutOptional=hearth-and-harvest` leaves the mod out of `runClient`.
2. **Mixin plugin.** `shouldApplyMixin` asks `HearthAndHarvestPresence` whether the target class is on
   the classpath and still declares each method, reading the class file without loading it. A method gone
   upstream is logged once and that mixin skipped.
3. **Nothing else names the mod.** `HearthWater` and `StampingTank` name only NeoForge and core types, and
   are loaded only from the mixins.

## Checking it

- `checkOptionalSeam` and `checkLoaderSeam` pass; `-PwithoutOptional=hearth-and-harvest,cold-sweat` with
  `tools/agent/smoke/boot.jsonl` comes up and stays up.
- `runGametest` passes unchanged: the mod is never on its classpath.
  `hearthAndHarvestDrinksAreMergedIntoAnOlderConfig` checks the config values.
- What it does is checked in a real client with
  [tools/agent/integrations/hearth-and-harvest.jsonl](../../../tools/agent/integrations/hearth-and-harvest.jsonl),
  whose header says how to run and verify it. It passed whole on 2026-10-03.
