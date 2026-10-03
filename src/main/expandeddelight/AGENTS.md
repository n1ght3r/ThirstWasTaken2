# src/main/expandeddelight — sea water in Expanded Delight's Cooking Pot soups

[Expanded Delight](https://modrinth.com/mod/expanded-delight) (mod id `expandeddelight`, package
`ianm1647.expandeddelight`) cooks Asparagus Soup and Cinnamon Apples in Farmer's Delight's Cooking Pot
from a water bucket. The recipe's ingredient matches the item alone, so a bucket of sea water cooked
too, into a soup that restores thirst. This directory refuses it, as Brewin' and Chewin's keg, Cultural
Delights' vat and Fruits Delight's juice do. A small integration, so there is no plan file.

**Built on `1.21.1-neoforge` only**, the mod's one build for a version this mod supports. Its 1.20.1
build is Fabric only, so on the `1.20.1` node its drinks restore thirst through config ids but the
Cooking Pot still cooks those two from sea water; a port needs a Fabric side of this guard. What it does:

- **no Cooking Pot recipe takes sea water**: a salty bucket, bottle or bowl in any of the six
  ingredient slots matches nothing, so the pot does not cook. The one exception is a recipe that asks
  for sea water itself, the salt boiled from a sea water bottle that replaces Hearth and Harvest's (see
  `src/main/hearthandharvest`);
- **fresh water of any grade** cooks as before, and the grade goes: the soup is its own item.

The drink and food values are not here. They are ids in `ThirstConfig`, common code that names no class
of the mod.

```
expandeddelight/java/com/thirstwastaken2/expandeddelight/
  ExpandedDelightPresence         the gate: the mod in FML's list, and the target method read off its class file
  ExpandedDelightMixinPlugin      applies the mixin only where the gate allows it
  mixin/CookingPotSeaWaterMixin   CookingPotRecipe.matches(RecipeWrapper, Level): false when an ingredient slot is salty, unless the recipe asks for it
expandeddelight/resources/
  thirstwastaken2.expandeddelight.mixins.json
```

## How it works

- **The pot.** `CookingPotRecipe` has two `matches`; the `RecipeInput` one only casts and forwards to the
  `RecipeWrapper` one, which reads slots 0 to 5. The mixin answers false at its head when any of those
  stacks has `water_salty: true`. It is on Farmer's Delight's class, so it reaches every Cooking Pot
  recipe, not only Expanded Delight's; none of them should take sea water, and the pot's own
  purification recipes already refused it by their ingredients. A recipe asks for sea water when one of
  its ingredients takes the salty stack and would not take the same stack fresh; that recipe is let
  through. Only the salt recipe does today, and it is why both mods together still make salt.
- **Why here and not in core.** Only Expanded Delight's recipes put a water bucket in the pot, so the
  mixin applies only with it installed. Core's Farmer's Delight support stays data only.
- **By string.** The mixin names its target with `targets = "..."` and its method by descriptor, and its
  handler takes only Minecraft and NeoForge types. The mod is on `runClient` only, with no
  `compileOnly`.

## How it stays optional

1. **Build.** Only where `deps.expanded_delight` is set does `build.neoforge.gradle.kts` add this
   directory and append the mixin config to the built manifest with `expandeddelight` as an optional
   dependency, from [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   `-PwithoutOptional=expandeddelight` (or `expanded-delight`) leaves the mod out of `runClient`.
2. **Runtime gate.** `ExpandedDelightPresence` reads FML's list of mod files and looks for the target on
   the classpath, never loading a class. A class or method gone upstream is logged once and the mixin
   skipped.
3. **Mixin plugin.** `shouldApplyMixin` asks the gate, then whether the method is still declared.

## Checking it

- `checkOptionalSeam` finds the plugin and the gate loaded without the mod, and passes.
- `runGametest` passes unchanged: the mod is never on its classpath.
  `expandedDelightDrinksAreMergedIntoAnOlderConfig` checks the config values.
- What it does is checked in a real client with
  [tools/agent/integrations/expanded-delight.jsonl](../../../tools/agent/integrations/expanded-delight.jsonl),
  whose header says how to run and verify it.
