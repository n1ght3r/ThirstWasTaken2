# src/main/fruitsdelight — sea water in Fruits Delight's juice and cauldrons

[Fruits Delight](https://modrinth.com/mod/fruits-delight) (mod id `fruitsdelight`, package
`dev.xkmc.fruitsdelight`) makes orange, lemon and pear juice from a water bottle at a crafting table, and
turns a full water cauldron into a lemonade or fruit cauldron whose chain ends in jello, which restores
thirst. Both took sea water. This directory refuses it, as Brewin' and Chewin's keg and Cultural
Delights' vat brew nothing from it. The plan and its decisions are in
[docs/dev/integration/FRUITS-DELIGHT-INTEGRATION.md](../../../docs/dev/integration/FRUITS-DELIGHT-INTEGRATION.md).

**Built on `1.21.1-neoforge` and `1.20.1-forge`**, the mod's two builds for a version this mod supports
(1.2.14 and 1.1.3). What it does:

- **a bottle of sea water is not a water bottle** to a recipe: no juice from it;
- **a full cauldron of sea water takes no lemon slice and no jam**, by hand or by dispenser;
- **fresh water of any grade** still makes juice and fruit cauldrons, and the grade goes: the juice is
  its own item, as tea is.

Not covered: Create's mixer on the same node mixes juice from 250 mB of water in a basin, and tests the
fluid alone with NeoForge's `SizedFluidIngredient`. Refusing sea water there would be a change to every
Create recipe that takes water, so it is left.

The drink and food values are not here. They are ids in `ThirstConfig`, common code that names no class
of the mod.

```
fruitsdelight/java/com/thirstwastaken2/fruitsdelight/
  FruitsDelightPresence            the gate: the mod in FML's list, and each target method read off its class file
  FruitsDelightMixinPlugin         applies each mixin only where the gate allows it
  mixin/WaterBottleIngredientMixin L2 Core's PotionIngredient.test: false for a salty stack when the potion is water
  mixin/FruitCauldronMixin         FDCauldronInteraction.perform: false on a cauldron holding sea water
fruitsdelight/resources/
  thirstwastaken2.fruitsdelight.mixins.json
```

## How it works

- **Juice.** The juice recipes are vanilla shapeless recipes whose water is L2 Core's
  `PotionIngredient.of(Potions.WATER)`, a custom ingredient that reads only the stack's
  `potion_contents`. `WaterBottleIngredientMixin` answers false at its head for a stack with
  `water_salty: true` when the ingredient's potion is water. L2 Core is nested in Fruits Delight's jar,
  so this reaches any other L2 mod's water bottle recipe too, where sea water should not count either.
- **Cauldrons.** Fruits Delight puts its lemon slice and jam into vanilla's water cauldron interaction
  map, each an `FDCauldronInteraction` that checks only the level. Its `perform` is what both
  `interact` and the dispenser behaviour call. `FruitCauldronMixin` answers false at its head when
  `WaterPurity.storedQuality(state)` is salt; the mod's own cauldrons carry no purity value, so only a
  vanilla water cauldron of sea water is refused.
- **By string.** Both mixins name their targets with `targets = "..."`, and their handlers take only
  Minecraft types. L2 Core is not a dependency one can compile against without unpacking the nested jar,
  so nothing here does: the mod is on `runClient` only, with no `compileOnly`.

## How it stays optional

1. **Build.** Only where `deps.fruits_delight` is set do `build.neoforge.gradle.kts` and
   `build.forge.gradle.kts` add this directory and append the mixin config to the built manifest with `fruitsdelight` as an optional
   dependency, from [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   `-PwithoutOptional=fruitsdelight` (or `fruits-delight`) leaves the mod out of `runClient`.
2. **Runtime gate.** `FruitsDelightPresence` looks up a class of the mod, and each target, on the
   classpath as a resource, never loading a class, so both loaders compile it. A class or method gone upstream is logged once and its mixin
   skipped.
3. **Mixin plugin.** `shouldApplyMixin` asks the gate, then whether the method is still declared.

## Checking it

- `checkOptionalSeam` finds the plugin and the gate loaded without the mod, and passes.
- `runGametest` passes unchanged: the mod is never on its classpath.
  `fruitsDelightDrinksAreMergedIntoAnOlderConfig` checks the config values.
- `./gradlew ":1.21.1-neoforge:runClient" -Pagent=tools/agent/smoke/boot.jsonl -PwithoutOptional=fruitsdelight,cold-sweat`
  comes up and stays up.
- What it does is checked in a real client with
  [tools/agent/integrations/fruits-delight.jsonl](../../../tools/agent/integrations/fruits-delight.jsonl),
  whose header says how to run and verify it.
- On 1.20.1 the juice takes its water through L2 Library's `PotionIngredient`, whose field is a plain
  `Potion` beside a bridge `test(Object)`, so `WaterBottleIngredientMixin` forks its target and names the
  descriptor. Forge's annotation processor fails on a string target it cannot find; the Forge script
  makes that a warning, as Loom does.
  [tools/agent/integrations/fruits-delight-1.20.1.jsonl](../../../tools/agent/integrations/fruits-delight-1.20.1.jsonl)
  passed on `1.20.1-forge` on 2026-09-28: all 53 ids exist, plain and Dirty bottles make orange juice
  and a sea water one does not, a lemon slice makes lemonade of a plain and a Dirty cauldron and not of
  sea water, and juice, jello and an orange restore 8, 3 and 2.
