# src/main/youkaisfeasts — sea water in Youkais' Feasts' kettle, tank and pots

[Gensokyo Delight ~ Youkais' Feasts](https://modrinth.com/mod/gensokyo-delight-youkais-feasts) (mod id
`youkaisfeasts`, package `dev.xkmc.youkaishomecoming`) brews tea in a Kettle, ferments sake, wine and mead
in a Fermentation Tank, and starts its soups by pouring water into an iron pot. All of them restore
thirst, and all took sea water. This directory refuses it, as Brewin' and Chewin's keg, Cultural Delights'
vat and Fruits Delight's juice do. The plan and its decisions are in
[docs/dev/integration/cooking/YOUKAIS-FEASTS-INTEGRATION.md](../../../docs/dev/integration/cooking/YOUKAIS-FEASTS-INTEGRATION.md).

**Built on `1.21.1-neoforge` and `1.20.1-forge`**, the mod's two builds for a version this mod supports
(1.1.2 and 0.5.2). What it does:

- **a sea water bucket or bottle goes into no Kettle and no Fermentation Tank** by hand: the use is
  refused and the container stays full;
- **a sea water bucket or bottle starts no soup** in the Small Iron Pot, Short Iron Pot or Stockpot;
- **a bottle of sea water is not a water bottle** to a recipe, so no Bamboo Mizuyokan in the Cooking Pot;
- **fresh water of any grade** is unchanged, and the grade goes: tea, sake and soup are their own items.

Not covered: pipes into the kettle and tank, and Create's filling recipes, which test the fluid alone. The
Steamer and the Copper Tank take water for steam and heat, and nothing drinkable comes of it.

The drink and food values are not here. They are ids in `config/itemvalues/FarmersDelightAddonItemValues`,
common code that names no class of the mod.

```
youkaisfeasts/java/com/thirstwastaken2/youkaisfeasts/
  YoukaisFeastsPresence             the gate: a class of the mod on the classpath, and each target method read off its class file
  YoukaisFeastsMixinPlugin          applies each mixin only where the gate allows it
  platform/YoukaisFeastsNames       the one name that differs: the blocks' use method, useItemOn or onClick
  mixin/WaterConsumerMixin          WaterConsumer.isWaterContainer: false for a salty stack (the iron pots)
  mixin/FluidTileSeaWaterMixin      KettleBlock and FermentationTankBlock, at their call of FluidItemTile.addFluidOrItem: CONSUME for a salty stack
  mixin/WaterBottleIngredientMixin  L2's PotionIngredient.test: false for a salty water bottle, as Fruits Delight's
youkaisfeasts/resources/
  thirstwastaken2.youkaisfeasts.mixins.json
```

## How it works

- **Iron pots.** `IronBowlBlock.startCooking` replaces the empty pot with its cooking pot once
  `util/WaterConsumer.isWaterContainer(stack, amount)` says the stack is water: a water bucket, a water
  potion, or a fluid container that drains plain water. Every iron pot asks only that, so
  `WaterConsumerMixin` answers false at its head for a salty stack.
- **Kettle and Fermentation Tank.** Both hand a held stack to the static
  `FluidItemTile.addFluidOrItem`, which pours a bucket through the loader's bucket wrapper (plain water
  whatever the bucket held), a water bottle as 250 mB of plain water, or else puts the stack in an item
  slot. A static interface method is beyond Forge 47's Mixin 0.8.5, so `FluidTileSeaWaterMixin` injects at
  the two calls, and returns `CONSUME` for a salty stack: nothing poured, nothing slotted, and the blocks'
  other uses (opening the tank, emptying by hand, the kettle's handle) are untouched.
- **Water bottle ingredient.** The same mixin as Fruits Delight's, on the L2 Core (L2 Library on 1.20.1)
  nested in this mod's jar. Each integration applies its own copy only when its own mod is present, so
  either mod alone is covered; with both, the second check finds nothing left to refuse.
- **By string.** Every target is named with `targets = "..."` and every handler takes only Minecraft
  types, so nothing compiles against the mod and it is on `runClient` only. On 1.20.1 the use method is L2
  Modular Blocks' `onClick` and the others are the mod's own, never remapped; the descriptors name only
  Mojang class names, which Forge keeps. `remap = false` throughout.

## How it stays optional

1. **Build.** Only where `deps.youkais_feasts` is set do `build.neoforge.gradle.kts` and
   `build.forge.gradle.kts` add this directory and append the mixin config to the built manifest with
   `youkaisfeasts` as an optional dependency, from
   [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   `-PwithoutOptional=youkais-feasts` leaves the mod out of `runClient`.
2. **Runtime gate.** `YoukaisFeastsPresence` looks up `WaterConsumer`, and each target, on the classpath
   as a resource, never loading a class. Youkais' Homecoming, the larger mod this one is cut from, has the
   same package and so passes the gate too; nothing has been checked against it. A class or method gone
   upstream is logged once and its mixin skipped.
3. **Mixin plugin.** `shouldApplyMixin` asks the gate, then whether the method is still declared.

## Checking it

- `checkOptionalSeam` finds the plugin and the gate loaded without the mod, and passes.
- `runGametest` passes unchanged: the mod is never on its classpath.
  `youkaisFeastsValuesAreMergedIntoAnOlderConfig` checks the config values.
- What it does is checked in a real client with
  [tools/agent/integrations/youkais-feasts.jsonl](../../../tools/agent/integrations/youkais-feasts.jsonl)
  and, on `1.20.1-forge`,
  [tools/agent/integrations/youkais-feasts-1.20.1.jsonl](../../../tools/agent/integrations/youkais-feasts-1.20.1.jsonl),
  whose headers say how to run and verify them.
