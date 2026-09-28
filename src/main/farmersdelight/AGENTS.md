# src/main/farmersdelight — Refabricated's Cooking Pot, on Fabric

Everything else Farmer's Delight does with this mod is data and registry ids in core; see
[compat/AGENTS.md](../java/com/thirstwastaken2/compat/AGENTS.md#farmers-delight). This directory
only fixes one thing in Farmer's Delight Refabricated, the Fabric port.

Refabricated's `CookingPotRecipe.matches` counts the pot's ingredients by item id through vanilla's
`StackedContents` and never asks a Fabric custom ingredient to test the stack. The mod's purification
recipes take `fabric:nbt` (1.20.1) or `fabric:components` ingredients, so on every Fabric node any bottle
matched: salt water, or a Potion of Healing, boiled into Pure water. The original mod on NeoForge and
Forge tests every ingredient through `RecipeMatcher` and needs nothing.

This directory is **compiled by every Fabric node that sets `deps.farmersdelight`**, from its row in
[the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt),
which also lists its mixin config in `fabric.mod.json`.

```
java/com/thirstwastaken2/farmersdelight/
  FarmersDelightPresence     the gate: Refabricated's recipe and RecipeWrapper classes, as resources
  FarmersDelightMixinPlugin  applies the mixin only when the gate passes
  CookingPotMatching         the second half of the match, for a recipe with a tested ingredient
  platform/PotRecipes        a recipe's ingredients: getIngredients up to 1.21.1, input from 1.21.11
  mixin/CookingPotRecipeMixin
```

## The fix

`CookingPotRecipeMixin` is a `@ModifyReturnValue` on `matches`. When Refabricated said yes and one of
the recipe's ingredients `requiresTesting()` (Fabric's `FabricIngredient`), `CookingPotMatching` also
pairs every filled ingredient slot, the first six, with a different ingredient that `test`s it. Six at
most, so it tries every pairing with a bit mask and allocates nothing. A recipe of plain items and tags
is left to Refabricated's own answer.

It asks the ingredients rather than the recipe's id, because a recipe does not know its id from 1.21.2.
That makes it right for any mod's recipe with a custom ingredient, not only this mod's.

From 26.1 the class also has a bridge `matches(RecipeInput, Level)`, so the mixin names the target's
descriptor there, in Mojang names since 26.x Fabric is not remapped. Before 26.1 there is one `matches`,
whose descriptor is in intermediary names, so the name alone is given. `RecipeWrapper.getItem` is the
same call on every version: a `Container` on 1.20.1, a `RecipeInput` after.

## Testing

The gametests run without Farmer's Delight. Checked on 2026-09-28 in a dev client, the salty bottle
staying in the pot where the murky one boiled Pure:
[tools/agent/integrations/farmers-delight-1.20.1.jsonl](../../../tools/agent/integrations/farmers-delight-1.20.1.jsonl)
on `1.20.1` (every check passes), and
[tools/agent/integrations/farmers-delight.jsonl](../../../tools/agent/integrations/farmers-delight.jsonl)
on `1.21.1` (`saltStays` passes) and `26.3.x`. That script is written for `1.21.1-neoforge`: on the
Fabric nodes its thirst reads name the player `Dev`, which a Fabric dev client is not, and on 26.3 its
slot checks do not match the byte `Slot` and the bottle waiting in slot 6 for a glass bottle, so read
`saltAfter` there. The same 26.3 run with the mixin switched off boiled the salty bottle to purity 3.
