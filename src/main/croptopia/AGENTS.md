# src/main/croptopia — Croptopia's crafting recipes take no sea water

[Croptopia](https://www.curseforge.com/minecraft/mc-mods/croptopia) (mod id `croptopia`, package
`com.epherical.croptopia`) adds dozens of crops and fruit trees and the drinks and dishes made from
them. It holds no water in any block: everything that takes water is a plain crafting recipe. The plan
and its decisions are
[docs/dev/integration/cooking/CROPTOPIA-INTEGRATION.md](../../../docs/dev/integration/cooking/CROPTOPIA-INTEGRATION.md).

**Built on every node Croptopia has a build for**: `26.2.x`, `26.1.x` and `1.21.1` on Fabric and
NeoForge, `1.20.1` on Fabric and `1.20.1-forge`. It has none for 26.3 or 1.21.11, so those nodes do not
set `deps.croptopia`. What it does:

- **Sea water crafts nothing of Croptopia's.** A shaped or shapeless recipe whose result is an item of
  Croptopia's does not match a grid that holds sea water. That covers its water bottle, crafted sixteen
  at a time from `minecraft:water_bucket`, and everything taking `#c:water_bottles`, which holds the
  water bucket too: tea, steamed rice, the soups, dough, noodles, tofu.

Its drinks and foods are not here. They are `ThirstConfig` ids (`croptopiaDrinks`, `croptopiaFoods`),
which reach every node, these two without the mod included.

```
croptopia/java/com/thirstwastaken2/croptopia/
  CroptopiaMixinPlugin           applies the mixin only where Croptopia's common class is on the classpath, by resource lookup
  mixin/ShapedRecipeMixin        ShapedRecipe.matches: a Croptopia result refuses a grid with sea water
  mixin/ShapelessRecipeMixin     the same for ShapelessRecipe
  platform/CroptopiaVanilla      the recipe result and the crafting grid, whose types differ by Minecraft version
croptopia/resources/
  thirstwastaken2.croptopia.mixins.json
```

## How it works

- **By result, not by id.** `matches` does not know its recipe's id, and the result's namespace is what
  makes it Croptopia's. A data pack's recipe for a Croptopia item is covered too, and nothing else is.
- **Worked out once per recipe.** Whether a recipe makes a Croptopia item is stored in a field of the
  recipe on its first successful match: recipes never change, and the crafting grid asks on every click.
- **Only sea water is refused.** Fresh water of any grade still crafts. Croptopia's water bottle is a
  plain item with no grade, so the bucket's grade is lost in it, as in every crafting recipe that takes
  water. Tea is its own item and safe, as every other mod's tea is.
- **Salt.** Croptopia smelts its own water bottle into salt. That bottle can no longer hold sea water, so
  salt comes from fresh water bottles, salt ore and Mountain Salt; nothing of ours cooks that bottle, so
  the furnace has no choice to make.
- **Versions.** 26.1 holds a recipe's result as an `ItemStackTemplate`, earlier versions as an
  `ItemStack`; 1.21 hands `matches` a `CraftingInput`, 1.20.1 a `CraftingContainer`. Both live in
  `platform/CroptopiaVanilla`, and the mixins fork only their signatures.
- **Two mixins, not one with two targets.** The two recipes' `result` fields are separate fields, and
  under Forge 1.20.1's SRG they have different names, which one `@Shadow` cannot map to both.

## How it stays optional

1. **Build.** Only where `deps.croptopia` is set do the loader scripts add this directory and append the
   mixin config to the built manifest (on NeoForge and Forge with `croptopia` as an optional
   dependency), from [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   Nothing is compiled against Croptopia. It and EpheroLib, which it requires, are on `runClient` only,
   from CurseMaven by CurseForge file id, and off by default. `-PwithoutOptional=croptopia` leaves both
   out.
2. **Runtime gate.** The mixin's targets are Minecraft's, always there, so `CroptopiaMixinPlugin` looks
   up `com/epherical/croptopia/CroptopiaCommon.class` (4.1 and later) or `CroptopiaMod.class` (the 1.20.1
   builds) as a resource instead, never loading it.

## Checking it

- `checkOptionalSeam` finds the plugin loaded without the mod, and passes.
- `runGametest` checks the drinks and foods merge into an older config.
- `./gradlew ":<node>:runClient" -Pagent=tools/agent/smoke/boot.jsonl -PwithoutOptional=croptopia`
  comes up and stays up.
- What it does is checked in a real client with
  [tools/agent/integrations/croptopia.jsonl](../../../tools/agent/integrations/croptopia.jsonl).
