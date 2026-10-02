# src/main/brewinandchewin — water quality in Brewin' and Chewin's keg

[Brewin' and Chewin'](https://modrinth.com/mod/brewin-and-chewin) (mod id `brewinandchewin`, package
`umpaz.brewinandchewin`) keeps water in one block, the keg, and fills and empties it through its own
pouring recipes, which build the water from the recipe rather than from the bucket or bottle. So the
grade a bucket had was lost on the way in and on the way out. This directory is where that gets fixed.
The plan, the order of work and what is still to do are in
[docs/dev/integration/cooking/BREWIN-AND-CHEWIN-INTEGRATION.md](../../../docs/dev/integration/cooking/BREWIN-AND-CHEWIN-INTEGRATION.md).

**Built on the two nodes that set the key**, `1.21.1` and `1.21.1-neoforge`: the mod publishes both
loaders, for 1.21.1 only. What it does there:

- **a bucket of water keeps its grade through the keg**: poured in, the keg's water carries it; drawn
  out with a bucket or a glass bottle, the container gets it back;
- **a keg holding one grade refuses another**, as every tank does;
- **a drawn bottle is cookable**: it gets `water_salty: false`, which the keg's own bottle lacked;
- **a graded water bottle goes in**, which the strict bottle recipe refused;
- **sea water** goes in and comes back out salty, and **nothing ferments from it**;
- **fresh water of any grade ferments** into ordinary drinks, with no grade: a brewed drink is safe
  whatever went in, as tea is;
- **a broken keg** keeps its water's grade in the item;
- **with Jade**, looking at a keg of water shows its grade.

The drink and soup values are not here. They are ids in `ThirstConfig`, common code that names no class
of the mod, so they reach every node.

## Which build

| Node | Build | Modrinth id |
|---|---|---|
| `1.21.1` | `v4.5.0+1.21.1-fabric` | `O3PobqCR` |
| `1.21.1-neoforge` | `v4.5.0+1.21.1-neoforge` | `MbcR48Ou` |

Everything touched is in the mod's `common` module, which names neither loader, so **both loaders
compile this directory** and `checkLoaderSeam` keeps it that way. The mod requires Farmer's Delight,
already on both nodes' `runClient`, and Greenhouse Config, nested in its jar. On Fabric that nesting is
two deep (Greenhouse Config's TOML support nests its own Night Config), which is why `nestedMods` in
`build.gradle.kts` follows each jar's `fabric.mod.json` down; NeoForge loads the nested jars itself.

```
brewinandchewin/java/com/thirstwastaken2/brewinandchewin/
  BrewinAndChewinPresence      the gate: a classpath probe per mixin target, and a read of the class
                               file for the methods each mixin needs
  BrewinAndChewinMixinPlugin   applies each mixin only where the gate allows it
  KegWater                     the only place that reads or writes a grade on the keg's fluid
  mixin/KegPouringRecipeMixin  getFluid: the recipe's water with the grade of the container pouring it
  mixin/KegBlockEntityMixin    fluidExtract: the container a pouring recipe hands back, stamped
  mixin/KegBottleMixin         the strict bottle comparisons: a graded bottle matches as its plain self
  mixin/KegFermentingMixin     canFerment: false while the keg holds sea water
  HeldKegWater                 our interface on the keg: the grade it holds, for the Jade reader
  mixin/KegHeldWaterMixin      implements it off the tank
brewinandchewin/resources/
  thirstwastaken2.brewinandchewin.mixins.json
src/client/brewinandchewin/java/com/thirstwastaken2/client/brewinandchewin/
  BrewinAndChewinJade          adds the keg to client/compat/JadeIntegration's containers
```

## How a grade moves

**The grade lives on the keg's fluid, as one component**: `water_purity` for a grade,
`water_salty` for sea water. Those are the components NeoForge's `FluidStack` and Fabric's
`FluidVariant` already carry for this mod, and both loaders' keg tanks turn Brewin' and Chewin's
`AbstractedFluidStack` into one of those. So the tank saves the grade, syncs it, copies it into a
picked-up keg (`copy_drink`) and refuses to mix two grades, with no field or save hook of ours.

- **In.** The keg fills itself from `KegPouringRecipe.getFluid(slot)`, picks the recipe by comparing it
  with the tank, and checks the tank against it before filling. `KegPouringRecipeMixin` answers with the
  recipe's water stamped with the slot's grade, so all three follow the grade at once. **Only a stamped
  container stamps**: a plain bucket pours plain water, and so still tops up a keg filled before the
  integration.
- **Out.** `fluidExtract` builds the container it hands back with `KegPouringRecipe.assemble`.
  `KegBlockEntityMixin` stamps that result with the tank's grade, read before the drain. It is stamped
  even from plain water, as `defaultPurity`, so the bottle gets `water_salty: false`. `fluidExtract`
  also builds a result when a full container is poured in, only to compare it with the one in hand; a
  full water container in hand is therefore left alone, or a plain bottle would stop matching the plain
  bottle the strict bottle recipe expects.
- **Bottles.** The bottle recipe is `strict`. `KegBottleMixin` wraps each
  `ItemStack.isSameItemSameComponents` in `getPouringRecipe`'s filter lambda (`lambda$getPouringRecipe$4`,
  the same synthetic name in both jars) and in `fluidExtract`: a stamped water container in hand also
  matches a plain recipe stack as its unstamped self. Only that direction: a drawn container compared
  with the output slot is stamped itself, so that stays exact.
- **Sea water does not ferment.** Every water recipe matches `#c:water`, which ignores components.
  `KegFermentingMixin` makes `canFerment` answer false while the tank holds sea water. It runs on every
  fermenting tick, so `KegWater.isSalt` reads the stack's patch rather than building a component map.
  It shadows the tank rather than calling the keg: `KegBlockEntity` extends Farmer's Delight's
  `SyncedBlockEntity`, which is not on the compile classpath.
- **Jade.** The keg's tank reaches the client through `writeUpdateTag`, components included, so
  `KegHeldWaterMixin` reads the grade there as the server has it. It is its own mixin, gated on the
  class alone, so the line stays when a method another mixin needs moves upstream.
  `BrewinAndChewinJade` is a Fabric `jade` entrypoint and a NeoForge `@WailaPlugin`; Jade loads it
  whether or not the mod is installed, so it names only `HeldKegWater` and the gate.
- **Both loaders keep a stack's components only when they are a `PatchedDataComponentMap`**, and use a
  stack's `loaderSpecific` over its components when it has one. `KegWater.stamped` builds exactly that,
  with no loader stack.

## What Brewin' and Chewin' does on its own

Found while building this, and not changed by it:

- **A keg holds one bucket** by default in 4.5.0 (`kegCapacity = 81000` droplets in
  `brewinandchewin-common.toml`); the unreleased 5.0.0 branch says 4000 mB. A bucket poured into a
  keg with room fills what fits and is used up whole.
- **A refused container opens the keg's screen.** An agent script has to close it before the next
  click, or every click after lands in the screen.
- **Without the integration a graded water bottle is refused**: the bottle recipe is `strict`. On
  `1.21.1-neoforge`, where Create is on `runClient`, Create's bottle handler took it as `create:potion`
  instead. With `KegBottleMixin` the keg's own recipe matches first.
- **Canteens and waterskins** go through the generic fluid container path, which only tops up a keg
  already holding the same water. Into an empty keg they are refused, and on `1.21.1-neoforge` a Dirty
  canteen with room poured onto a Dirty keg was filled from it instead, emptying the keg.

## How it stays optional

The same three layers as Kaleidoscope Cookery.

1. **Build.** Only where `deps.brewin_and_chewin` is set does the loader script add this directory and
   append the mixin config to the built manifest (on NeoForge with `brewinandchewin` as an optional
   dependency), from [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   Fabric takes it as `modCompileOnly`, since it is mixed into; NeoForge as `compileOnly`. Both put it
   on `runClient` only, so `runServer` and the gametests run without it, and
   `-PwithoutOptional=brewinandchewin` (or `brewin-and-chewin`) leaves it out of `runClient` as well.
2. **Runtime gate.** `BrewinAndChewinPresence` answers with a resource lookup and a read of the class
   file, never loading a class, and names no class of the mod's, no Minecraft class and no loader. A
   target that is gone skips its mixin silently when the mod is absent; a method that is gone from a
   class that is there is logged once.
3. **Mixin plugin.** `shouldApplyMixin` asks the gate for each mixin's own target and method.

## Names on Fabric

The Fabric jar names Minecraft types in intermediary, as Refabricated's do. The mod's own methods and
fields are matched **by name alone, with no descriptor**, and every mixin says `remap = false`, except
`canFerment`, whose full descriptor names only the mod's own types. `KegBottleMixin`'s one Minecraft
target, `isSameItemSameComponents`, says `remap = true` on its `@At`, and Loom writes the intermediary
name into the built jar. Otherwise: the
`assemble` target is `Lumpaz/brewinandchewin/common/crafting/KegPouringRecipe;assemble`, which Loom
reports as "not fully qualified" on every build, as it does Kaleidoscope's `fillFluid`. `fluidExtract`
calls only the one overload.

## Checking it

- `checkOptionalSeam` finds the plugin, the gate and the Jade reader as classes loaded without the
  mod, and passes.
- `runGametest` passes unchanged on both nodes: the mod is never on its classpath.
  `brewinAndChewinDrinksAreMergedIntoAnOlderConfig` checks the config values.
- `./gradlew ":<node>:runClient" -Pagent=tools/agent/smoke/boot.jsonl -PwithoutOptional=brewinandchewin`
  comes up and stays up.
- What it does is checked in a real client with
  [tools/agent/integrations/brewin-and-chewin.jsonl](../../../tools/agent/integrations/brewin-and-chewin.jsonl),
  whose header says how to run and verify it. It passed whole on `1.21.1` and `1.21.1-neoforge` on
  2026-09-25, Jade line included.
