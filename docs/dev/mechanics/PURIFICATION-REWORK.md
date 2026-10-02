# Purification rework: plain heat stops at Clean

**Status: planned, not started.** Agreed on 2026-09-30; to be discussed once more before any code.
Read [WATER-REFERENCE.md](WATER-REFERENCE.md) first: it describes how things are today, and this plan
changes it.

## Why

Progression is flat. A furnace costs eight cobblestone and already takes Murky and Clean water to Pure,
so copper and iron gear only make boiling more convenient and unlock nothing. The furnace's flat 10 s
per item makes a full Iron Flask (six servings) its best input by far, and the campfire's four slots
turn four buckets into twelve servings every 30 s with no fuel, beating everything else.

## The rule

- **Plain heat (furnace, smoker) raises water at most to Clean** (grade 2), for every container:
  bottle, terracotta bowl, bucket, Copper Canteen, Iron Flask.
- **Pure (grade 3) comes only from a dedicated boiling vessel:** the Copper Canteen or Iron Flask
  held on a campfire, the Copper and Iron Hanging Pots, Farmer's Delight's Cooking Pot and Cold
  Sweat's Boiler. These cost metal or a block of their own.
- **The campfire's slots stop purifying water.** They go back to cooking food. The campfire stays a
  water block as the heat under the pots and the vessels held on it.
- The waterskin still cannot be boiled at all. Pure water gets into it only from a clean source: a
  hanging pot, or bottles and buckets boiled elsewhere.

Clean is still worth improving on. On Normal it gives Poisoning 2% and Upset Stomach 10% a drink, and
Pure gives nothing ([WATER-SICKNESS.md](WATER-SICKNESS.md)).

## Where Pure water comes from after

| Source | Why it stays |
|---|---|
| Water sampled in cold mountain biomes above y 100 (`WaterPurity.sampleAt` scores it Pure) | found, not made |
| A cauldron filled by a pointed dripstone (`dripstonePurity`, 3) | **kept on purpose**: a reward for building the drip, and the only unattended Pure without metal |
| Copper Canteen / Iron Flask held on a campfire | dedicated vessel |
| Copper / Iron Hanging Pot | dedicated vessel |
| Farmer's Delight's Cooking Pot, Cold Sweat's Boiler | dedicated blocks |
| Loot: water bottles in five structure chests, Piglin bartering | found, not made |

Everything else stops at Clean: furnace, smoker (and Sophisticated's Smoking upgrades), Cold Sweat's
Waterskin on a campfire or in a furnace. Moving water (Create, Sophisticated tanks and pumps,
Supplementaries jars) keeps its grade and makes nothing Pure, today and after.

## The numbers after

| Method | Time | Servings | Result |
|---|---|---|---|
| Furnace, bottle / bowl | 10 s | 1 | Dirty → Clean, Murky → Clean |
| Furnace, bucket | 10 s | 3 | the same |
| Smoker, any of those | 5 s | 1 or 3 | the same |
| **Furnace, Copper Canteen (new)** | **3 s a serving, 12 s full** | 1 to 4 | the same |
| **Furnace, Iron Flask** | **4 s a serving, 24 s full** (was a flat 10 s) | 1 to 6 | the same |
| Canteen / Flask held on a campfire | 3 s / 4 s a serving | 4 / 6 | Pure (unchanged) |
| Copper / Iron Hanging Pot | 4 s / 6 s a serving | 3 | Pure (unchanged) |
| Campfire slots | removed | | |

Per serving in a furnace a bucket (about 3.3 s), the canteen (3 s) and the flask (4 s) are now close,
so no input dominates. The trade is clear: stand holding use for Pure, or leave it in a furnace and
walk away for Clean.

**The bucket keeps its flat 10 s on purpose.** A player who has found iron and made a bucket has
earned a furnace input close to the canteen and the flask, which nudges them off boiling bottles and
bowls one at a time (10 s a serving). Making it 30 s to match bottles was considered and turned down.

## The terracotta bowl

Bowls are the early, clay-age container, and hard to come by. Two changes make them easier to live
with without letting them replace the carried vessels:

- **A filled Terracotta Water Bowl stacks to 4.** Sixteen was considered and turned down: 64 thirst
  in a slot would leave the waterskin, canteen and flask no purpose. A side benefit: a furnace's
  output slot takes several bowls, so a hopper can feed it; today it stalls after one.
  **To re-decide:** 4 was chosen on a wrong number. A vessel's serving is 4 thirst, not a bottle's 6,
  so a full waterskin is 12 thirst (canteen 16, flask 24), and four bowls (16) beat the waterskin and
  tie the canteen. Three bowls tie the waterskin (12); two (8) stay under it.
- **Empty Terracotta Bowls in village chests**, so a player can find them before they dig clay.
- **The bowl-and-bucket crafting recipe gives a Dirty bowl.** Today it always gives Clean, whatever
  the bucket held, because a crafting recipe cannot see the bucket's grade. With Clean as the
  furnace's ceiling that would turn a Dirty bucket into a Clean bowl with no boiling. The cost: a
  bucket that was already Clean or Pure also comes out Dirty, so the recipe becomes a way to fill
  bowls, not to keep water clean. Pour from a cauldron or pot for that.

## Clean water makes you ill a little less

Clean becomes the most a furnace gives, so it is what most players drink before they have copper or
iron. Its Upset Stomach and Poison chances come down a little; the durations and levels stay:

| Clean, Upset Stomach | Easy | Normal | Hard |
|---|---|---|---|
| Now (`SicknessEffect.defaults()`) | 5% | 12% | 20% |
| After | 3% | 8% | 15% |

| Clean, Poison | Easy | Normal | Hard |
|---|---|---|---|
| Now | 3% | 5% | 10% |
| After | 2% | 3% | 6% |

Numbers to tune. Clean stays worse than Pure (0%), so boiling to Pure is still worth it.

## Steps

### 1. Datagen, `ThirstRecipeProvider`

- `PURIFY_TABLE` `{2, 3, 3}` becomes `{2, 2}`; loops over purity 0 and 1 only. No recipe for Clean
  input any more. Bottle, bowl and bucket keep 10 s in a furnace and 5 s in a smoker.
- Remove `Heat.CAMPFIRE` and `CAMPFIRE_TIME`. The unlock advancements' recipe lists shrink with it.
- Iron Flask: cooking time per recipe is `servings × 4 s` instead of `SMELTING_TIME`.
- Copper Canteen: new smelting recipes, one per fill level 1 to 4 and purity 0 and 1, `servings × 3 s`,
  gated on `enabled(COPPER_CANTEEN)`, with its own unlock. Turn `flaskPurifyRecipes` into one method
  taking the item, capacity and seconds a serving.
- Still smelting only for the two vessels: a smoking recipe is possible but not asked for, and a
  campfire recipe would swallow the vessel into a slot instead of boiling it in hand.
- Update the class Javadoc ("raises two grades and stops at PURIFIED").
- `FarmersDelightRecipeProvider` keeps its Pure result; it loops to `PURIFIED` on its own.

### 2. Datagen 1.20.1, `LegacyRecipeProvider`

Mirror step 1: the table, no campfire, the flask's times, the canteen's recipes.

### 3. Cold Sweat (`src/main/coldsweat`)

Cold Sweat's Waterskin reaches Pure today three ways; all three stop, so it follows the same rule as
every other container.

| Cold Sweat's Waterskin | Now | After |
|---|---|---|
| Furnace / smoker, Dirty | Clean | Clean |
| Furnace / smoker, Murky | Pure | **Clean** |
| Furnace / smoker, Clean | Pure | **no recipe** |
| Campfire (Cold Sweat's own warming recipe) | Dirty → Clean, Murky → Pure | **grade unchanged**, only warmed |

- The hand-written `src/main/coldsweat/resources/data/thirstwastaken2/recipe/cold_sweat/
  purify_waterskin_{0,1,2}_{smelting,smoking}.json`: delete the `_2` pair, and the `_1` pair's
  result `water_purity` becomes 2 instead of 3.
- `CampfireWaterskinMixin` wraps the drop of Cold Sweat's campfire recipe and stamps the skin with
  `BoiledWater`'s campfire rule. With no campfire rule left, it copies the input's grade and salt onto
  the output unchanged. It must still stamp: Cold Sweat's recipe hands back a new skin, which would
  otherwise lose its grade and fall to `defaultPurity`.
- Update `BoiledWater`, `src/main/coldsweat/AGENTS.md` ("Campfire, furnace, smoker" and the in-game
  results list) and the gametest or agent script that checks skins off a campfire.
- The Boiler keeps going to Pure.
- Sophisticated's Smoking upgrades use smoking recipes and follow on their own.

### 4. Terracotta Water Bowl stacks to 4

- `ThirstItems.TERRACOTTA_WATER_BOWL`: `stacksTo(1)` becomes `stacksTo(4)`, and rewrite the comment
  above it (it says a filled bowl does not stack, and why) with the numbers from "The terracotta
  bowl" above.
- Drinking one from a stack must leave the rest and hand the empty bowl back into the inventory, on
  every version. 1.21.1's `DrinkItem.finishUsingItem` goes through `ItemUtils.createFilledResult`,
  which does; check the newer versions' consumable remainder does the same.
- Bowls of different grades must not merge. They carry `water_purity`, so they will not; confirm
  filling a bowl on top of a partial stack from a source of another grade.
- Everywhere that fills a bowl (water source, cauldron, hanging pot, the bucket recipe) hands one
  filled bowl per empty one, as now; check none of them assumes a stack of one.

### 5. Empty terracotta bowls in village chests

- `compat/LootIntegration`: a pool on the five village house chests (plains, desert, savanna, snowy,
  taiga), empty `TERRACOTTA_BOWL`, 1 to 4, in about a third of chests (to tune).
- Skip the pool while the config switches the terracotta bowl off (`ThirstConfig.isItemEnabled`), as
  the creative tab does.
- `LootGameTest` and `compat/AGENTS.md` ("One extra pool is appended to five vanilla chest tables").

### 6. The bowl recipe and Clean's sickness

- `ThirstRecipeProvider`: `bowlResult(2)` becomes `bowlResult(0)` in the `terracotta_water_bowl`
  recipe (both branches), and the comment above it says why. Same in `LegacyRecipeProvider`.
- `SicknessEffect.defaults()`: the Clean line's `upset(...)` and `poison(...)` chances on easy,
  normal and hard to the tables above.
- A fresh config takes the new defaults, but an existing `thirstwastaken2.json` keeps the numbers it
  wrote. Check whether `ThirstConfig` migrates untouched defaults; if not, say so in the CHANGELOG
  (reset the sickness page to get them).
- `WATER-SICKNESS.md`'s chance table is already out of date against the code (it shows Normal Clean
  as 0 / 2 / 10); rewrite it from `defaults()`.

### 7. Regenerate and test

- `runDatagen` on a Fabric node, then `checkDatagen` and `checkNeoForgeResources`. The
  `purify_water_*_campfire` files and the `*_2_*` files disappear from every `src/main/generated/`.
- Gametests: `PurificationGameTest` (Murky → Clean, no Clean → Pure, a bottle does not go into a
  campfire slot), `CanteenGameTest` (the canteen smelts, both vessels stop at Clean, the flask's
  time follows its fill), the advancement test if it names a campfire recipe, a stack of four
  bowls drunk one at a time, a village chest rolling bowls, and the bowl recipe giving Dirty.
- Nodes to run: `26.3.x`, `1.21.1-neoforge` (Cold Sweat, Farmer's Delight), `1.20.1` (legacy
  datagen). Not all ten.

### 8. Docs

- `WATER-REFERENCE.md`: the purification table, "Where Pure water comes from today", and the sickness
  tables for Clean. Drop its "describes the game as the code stands today" warning about this plan.
- Player site: `docs/docs/features/water-purity.md` (lines about a campfire's thirty seconds and the
  flask in a furnace), `drinking.md` (bowls stack to 4, found in village chests),
  `integrations/cold-sweat.md`.
- CHANGELOG through the `write-docs` skill, as a balance change: water already Pure stays Pure.

## Known limit

Recipe times are written at datagen from the default `copperCanteenBoilSeconds` (3) and
`ironFlaskBoilSeconds` (4). Changing those in the config changes boiling in hand only, not the
furnace. Making the furnace follow them needs a mixin on the furnace's cooking time; not planned.

## Still open

- **A config switch letting plain heat reach Pure**, for modpacks that want the old way. Possible with
  a resource condition like `itemEnabled`, at the cost of a second set of recipes, a widget and lang
  keys in all nine files. Leaning no.
- **Smoker for the vessels?** Not planned; the smoker stays for bottles, bowls and buckets.
