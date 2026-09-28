# src/datagen — the generators for every datapack and asset JSON

Every recipe, advancement, tag, damage type, item model and model definition the mod ships is
written by the code in this directory. Nothing under `src/main/generated/` is edited by hand.

```bash
./gradlew ":26.3.x:runDatagen"
```

That empties `src/main/generated/<minecraft version>/` and writes it again, so what is on disk
afterwards is exactly what the generators produce — a file edited by hand is reverted and a file no
longer generated is gone. Commit what it changes. (Emptying first is deliberate: datagen's hash cache
records what it last wrote, not what is on disk, so without it neither of those two would be noticed.)

```bash
./gradlew ":26.3.x:checkDatagen"
```

That regenerates and then fails if the result differs from what is committed, which is what catches
a generator edited without regenerating, and a generated file edited by hand. It asks `git status`,
so it reports a file that is merely staged as changed too — which is the right answer, because the
index then holds something the generators did not produce.

## How it is wired

This is an ordinary Gradle source set, like the gametests and the dev tools, declared by
`fabricApi.configureDataGeneration` in `build.gradle.kts`. `thirstwastaken2-datagen` is its own small
mod (`src/datagen/resources/fabric.mod.json`), so none of it can reach a published jar, and
`ThirstDatagen.getEffectiveModId` is what makes everything it writes land in the `thirstwastaken2`
namespace rather than its own.

It runs as a **client**, because the item model providers live in `net.minecraft.client.data`. Strict
validation is off: every advancement the mod awards by id has a single `minecraft:impossible`
criterion, which strict validation reads as unreachable.

**The output directory is keyed by Minecraft version, not by build node**, and is a resource root of
`main`, so the jar picks it up with no further wiring. Every loader on one Minecraft version reads
the same directory. Only the Fabric node writes it: the providers extend Fabric API's, and a second
generator per loader would only be a second way for the files to drift. The `.cache/` beside the
output is datagen's own hash cache and is gitignored.

## Why the versions produce different bytes

The generators are one body of code; the serializers are not. A field whose value equals its codec's
default is omitted, and which fields have defaults changed between versions, so the same generator
writes `"category": "misc"` on 26.1 and omits it on 26.2. None of that is a difference in behaviour:
the codec that omits a field on write supplies the same value on read.

Two of these are worth knowing before reading a diff and thinking something broke:

- **`cookingtime` is omitted when it is the default**, which is 200 for smelting and 100 for
  smoking and campfire cooking. The campfire recipes are 600, so they always write it; the smelting
  ones are 200 and the smoking ones 100, so on 26.2 neither does.
- **Before 26.1 a recipe result is a live `ItemStack`**, whose components serialize as the delta from
  the item's own defaults. `ThirstItems.TERRACOTTA_WATER_BOWL` defaults to grade 3, fresh, custom
  model data `[0, 3]`, so on 1.21.11 a bowl result that sets exactly those writes no components at
  all. The stack the furnace hands out is the same either way. 26.1's `ItemStackTemplate` records the
  patch verbatim, which is why the newer nodes spell it out.

## Version conditionals

The same discipline as `src/main`, and the same threshold syntax. Two kinds of difference show up
here, and they are handled differently:

- **A pure rename** goes in `replacements` in `stonecutter.gradle.kts`, not in a `//?` block. The
  Fabric data generation API renamed `FabricDataOutput` to `FabricPackOutput` and `FabricTagProvider`
  to `FabricTagsProvider` for 26.1, and 26.2 moved the advancement trigger classes out of
  `net.minecraft.advancements` into `triggers` and `predicates` without renaming one of them. The
  code names the newest spelling and the replacements supply the older one.
- **A shape change** gets a `//?` block. Six in `ThirstRecipeProvider` are for 1.21.11's result
  type and cooking recipe constructors, and one in `ThirstModelProvider` for its texture wrapper;
  the rest are for 1.21.1, below.

**No block comments inside a `//?` block**: a disabled branch is itself one block comment, and a
nested `*/` ends it early. The 1.21.1 blocks use line comments for that reason.

**1.20.1 has providers of its own** for recipes and advancements, in `src/datagen/legacy`:
`LegacyRecipeProvider` and `LegacyAdvancementProvider`. It writes both through types the later
providers share nothing with, and its recipes need the mod's `NbtRecipes` types for a result that
carries water. `build.gradle.kts` compiles that directory there in place of `ThirstRecipeProvider`,
`ThirstAdvancementProvider` and `FarmersDelightRecipeProvider`. A recipe added to one side is added
to the other. `DataDirectories` spells the plural directories 1.20.1 reads.

`builder(TagKey)` rather than `tag(TagKey)` is deliberate — it is Fabric's and needs no conditional
from 1.21.11 on. Vanilla's `tag` only exists from 26.2. On 1.21.1 Fabric still called it
`getOrCreateTagBuilder`, which is the one tag branch in each tag provider.

## What 1.21.1 writes differently

The generators are the same; the formats they write to are older.

- **No item model definitions.** `assets/…/items/` arrived in 1.21.4, so on 1.21.1
  `ThirstItemModelDefinitionProvider` holds no class at all and is not registered. The two items
  that dispatch get an `overrides` list in their own `models/item/*.json` instead, one entry per
  `custom_model_data` value from 1 up, with the first variant as the model's own texture. The game
  takes the last override the value reaches, so the list is in rising order.
- **Custom model data is one integer**, not a float list; `Vanilla.modelSelector` writes it, and the
  recipe results carry it in that form.
- **No legacy sea-water bottle and bucket definitions**, since no 1.21.1 stack ever pointed at them.
- **The advancement background is a texture path**, `minecraft:textures/block/terracotta.png`.
- **Ingredients are `{"item": …}` objects.** Nothing branches for this; the codec writes it.
- **The filled-bowl crafting recipe is built by hand.** 1.21.1's shapeless builder cannot give its
  result components, so the recipe and its unlock are written the way the builder would write them.
  The unlock comes out byte-identical to 1.21.11's.
- **Recipes are known by id, not by registry key**, so `recipe(name)` returns one or the other.

## The providers

| Provider | Writes |
|---|---|
| `ThirstRecipeProvider` | `data/…/recipe/`, and the `advancement/recipes/misc/` unlocks with them |
| `FarmersDelightRecipeProvider` | the two Cooking Pot recipes and their unlocks, as JSON with a load condition |
| `ThirstAdvancementProvider` | `data/…/advancement/`, the mod's own tab |
| `ThirstDamageTypeProvider` | `data/…/damage_type/dehydrate.json` |
| `ThirstDamageTypeTagProvider` | `data/minecraft/tags/damage_type/bypasses_armor.json` |
| `ThirstBiomeTagProvider` | `data/…/tags/worldgen/biome/stagnant_water.json` |
| `ThirstBlockLootProvider` | `data/…/loot_table/blocks/`, as JSON through vanilla's codec |
| `ThirstModelProvider` | `assets/…/models/item/`, the definitions for the mod's own items, and through `HangingPotModels` the pot's blockstate and generated block models |
| `ThirstItemModelDefinitionProvider` | the two legacy definitions in `assets/…/items/` that have no item, kept for sea-water bottles and buckets saved up to 1.4; 1.21.4 and later |

`FarmersDelightRecipeProvider` is a plain `DataProvider` because nothing of Farmer's Delight is on
the datagen classpath, so there is no recipe class to hand a builder. It encodes the same ingredient
and result `ThirstRecipeProvider` uses with vanilla's codecs, which is what keeps the component format
right per version, and prepends `fabric:load_conditions`.

A new provider has to be added to `ThirstDatagen.onInitializeDataGenerator` or it never runs, and
nothing fails to tell you so.

## The Mod Items condition

Every recipe that makes, fills or cleans one of the mod's own items carries
`{"condition": "thirstwastaken2:item_enabled", "item": <that item>}`, and so does its unlock, so a pack
that switches the item off in the config loses both on the next data load. The bottle and bucket
purification recipes carry none. `Recipes` is a static class and cannot call the outer provider's
`withConditions`, so the provider hands it `this::withConditions` and `Recipes.enabled(item)` wraps the
output with it; every `save` and `accept` for one of the mod's items goes through that. The Cooking Pot
bowl recipe adds the same condition to its `fabric:all_mods_loaded` list by hand.

The condition class is `platform/ItemEnabledCondition` in `src/main/fabric`, which datagen can see. A
new recipe for one of the mod's items goes through `enabled(...)` too, and `ModItemsGameTest` lists the
files each switch should take away.

## What is not generated

Textures, the hanging pot's three Blockbench models, `icon.png`, `font/droplets.json`, the nine `lang/` files and
`thirstwastaken2.mixins.json` all stay hand-written in `src/main/resources`, and `fabric.mod.json` in
`src/main/fabric/resources`. The lang files
deliberately so: eight of the nine are translations, and `en_us` is edited alongside them.

## The hanging pot's blockstate

`HangingPotModels` builds the blockstate as JSON, because its builder classes changed in 1.21.5 and
again in 26.1. From 1.21.5 the JSON is parsed with the version's own codec and handed over as a
definition; before it the JSON is handed over as it is. There is no block tag: the Create Fly Sand
Filter already ships `mineable/pickaxe.json` by hand, and a generated copy of the same path fails
`processResources` on the nodes that build it, so the pot is soft enough to break by hand instead.

## Two rules that outlive this file

- **Renaming an advancement means renaming the constant in `ThirstAdvancements` with it**, or the
  advancement silently stops being awarded. `AdvancementGameTest` is what catches that.
- **The purity table lives in `ThirstRecipeProvider.PURIFY_TABLE` and nowhere else.** The Java side
  has no idea the purification recipes exist; changing the table changes all eighteen files at once,
  which is the whole reason they are generated.
