# Water reference

Every way to get water, what each grade of water does when drunk, by difficulty, and how the ways of
cleaning it compare.

**It describes the game as the code stands today.** The planned rework (plain heat stops at Clean, no
campfire slots, lower Clean sickness) is in [PURIFICATION-REWORK.md](PURIFICATION-REWORK.md); update
this file when it lands. The design of the sickness itself is in [WATER-SICKNESS.md](WATER-SICKNESS.md).

Every number below is a default. Most are in the config (`ThirstConfig`); the ones that are not say so.

## Grades

Fresh water has four grades; sea water is a different kind of water, not a grade.

| Grade | Number | Quenched given (`quenchedPercent`) |
|---|---|---|
| Dirty | 0 | 0% |
| Murky | 1 | 50% |
| Clean | 2 | 100% |
| Pure | 3 | 100% |
| Sea water | salt | none, see [Sea water](#sea-water) |

Anything holding water with no grade stamped on it counts as `defaultPurity`, Clean.

## What a drink gives

A thirst bar is 20.

| Drink | Thirst | Quenched (before the grade's cut) |
|---|---|---|
| Water bottle | 6 | 8 |
| Terracotta Water Bowl | 4 | 5 |
| One serving of a Waterskin, Copper Canteen or Iron Flask | 4 | 5 |
| A sip by hand from a water block (`canDrinkByHand`) | 3 | 2 (not in the config) |
| Milk bucket, for comparison | 6 | 8 |

| Carried vessel | Servings | Thirst when full |
|---|---|---|
| Waterskin | 3 (fixed) | 12 |
| Copper Canteen | 4 (`copperCanteenCapacity`) | 16 |
| Iron Flask | 6 (`ironFlaskCapacity`) | 24 |

A bucket is three servings wherever it is poured, the same rate a cauldron uses.

## Where water comes from

### Water in the world

`WaterPurity.sampleAt` scores the water block from 0 (clean) to 100 (filthy), then grades the score.
It is sampled only when water is collected, drunk or looked at with Jade.

| Biome | Base score |
|---|---|
| Swamp, Mangrove Swamp, and modded swamps such as Terralith's and No Man's Land's (`thirstwastaken2:stagnant_water`, checked before river) | 85 |
| Jungle, Savanna, Badlands | 70 |
| Anything else | 55 |
| River | 42 |
| Mountain (`#is_mountain`) | 28 |
| Ocean, Beach, and `thirstwastaken2:sea_water` (No Man's Land's Mud Beach) | sea water, not scored (`enableSeaWater`) |

A fluid in `thirstwastaken2:pure_water` is Pure wherever it lies, before the sea check and the score:
Spelunkery's Spring Water, as optional entries. See [src/main/spelunkery/AGENTS.md](../../../src/main/spelunkery/AGENTS.md).

| Modifier | Score |
|---|---|
| Biome base temperature 1.5 or more | +10 |
| Biome base temperature 0.15 or less | −10 |
| Above y 100 or below y 32 | −5 |
| Flowing water, not a source | −5 |
| Mud or mangrove roots within 2 blocks | +15 |
| Farmland or a composter within 2 blocks | +10 |

| Score | Grade |
|---|---|
| 0 to 15 | Pure |
| 16 to 35 | Clean |
| 36 to 65 | Murky |
| 66 to 100 | Dirty |

What that gives in practice:

- **Pure:** only cold mountains high up: frozen peaks, jagged peaks or snowy slopes above y 100
  (28 − 10 − 5 = 13).
- **Clean:** other mountains above y 100, cold rivers (42 − 10 = 32), deep aquifers in a mountain.
- **Murky:** most plains, forests and rivers, ordinary lakes.
- **Dirty:** swamps, jungles, savannas, badlands, and water next to mud or a farm.

None of these are in the config; they are constants in `WaterPurity`.

### Water that fills a cauldron on its own

| Source | Grade | Config |
|---|---|---|
| Rain into a cauldron or a hanging pot | Clean | `rainwaterPurity`, off with `enableRainCollection` |
| A pointed dripstone dripping into a cauldron | Pure | `dripstonePurity` |

Both keep the worse of what the cauldron held and what fell in. A cauldron always keeps the worse of
two waters mixed; a carried vessel averages them.

### Other mods' water blocks

| Block | Grade drawn | Notes |
|---|---|---|
| Timber Well (Farm & Charm) | the groundwater it pumps, sampled at the source block | rain only: `rainwaterPurity`; a beach well gives sea water |
| Water Trough (Farm & Charm) | Murky | whatever was poured in; sea water refused |
| Kitchen sink (Candlelight) | Murky | fills from nothing; sea water refused |

See [src/main/farmandcharm/AGENTS.md](../../../src/main/farmandcharm/AGENTS.md).

### Loot

A water bottle in a pool added to mineshaft, bastion, Nether fortress, shipwreck supply and dungeon
chests: one in two chests gives 1 to 3 bottles, Clean or Pure in equal odds. Piglin bartering can
give the same bottles, Clean twice as often as Pure. See `compat/LootIntegration`.

Desert pyramid chests and desert and savanna village house chests have a pool of their own: one in two
gives 1 to 3 bottles, Dirty or Murky in equal odds. The dry surface is where water is scarcest, so
water is there to find, but not water to drink without boiling it.

### Villager trades

| Villager | Level | Price | Gives | Uses before restock |
|---|---|---|---|---|
| Leatherworker | Novice | 3 emeralds | an empty Waterskin (none while `enableWaterskin` is off) | 12 |
| Cleric | Novice | 1 emerald | a Clean water bottle | 16 |

Each joins the two novice trades vanilla draws from its pool, so about half of all novice
leatherworkers, and two clerics in three, offer it. See `compat/TradeIntegration`.

## Cleaning water

"Up two grades" is `PURIFY_TABLE` in `ThirstRecipeProvider`: Dirty becomes Clean, Murky and Clean
become Pure. Only the Copper Distiller takes the salt out of sea water, and it makes any water Pure;
see [DISTILLATION-PLAN.md](DISTILLATION-PLAN.md). With Spelunkery, a furnace boils a sea water bucket
down to a salt bucket, and nothing to drink.

| Method | Time | Servings | Result | Fuel | Notes |
|---|---|---|---|---|---|
| Furnace, bottle or bowl | 10 s | 1 | up two grades | yes | `SMELTING_TIME` |
| Furnace, bucket | 10 s | 3 | up two grades | yes | one item, so the furnace's cheapest input per serving after the flask |
| Furnace, Dirty bucket to Pure | 20 s | 3 | Pure | yes | two passes |
| Smoker, any of those | 5 s | 1 or 3 | up two grades | yes | `SMOKING_TIME`; also Sophisticated's Smoking upgrades |
| Campfire, four slots | 30 s | up to 12 | up two grades | no | `CAMPFIRE_TIME`; the highest throughput of anything |
| Iron Flask, furnace | 10 s | 1 to 6 | up two grades | yes | one recipe per fill level; the canteen has none |
| Copper Canteen, held on a campfire | 3 s a serving, 12 s full | 4 | Pure | no | hold use the whole time; `copperCanteenBoilSeconds` |
| Iron Flask, held on a campfire | 4 s a serving, 24 s full | 6 | Pure | no | `ironFlaskBoilSeconds` |
| Copper Hanging Pot | 4 s a serving, 12 s full | 3 | Pure | no | over a lit campfire; `copperHangingPotBoilSeconds` |
| Iron Hanging Pot | 6 s a serving, 18 s full | 3 | Pure | no | the same; `ironHangingPotBoilSeconds` |
| Copper Distiller | 8 s a serving, 72 s full | up to 9 | Pure, sea water included | furnace fuel | coal runs ten servings; the cooling tub filled once; a bucket of sea water leaves one salt when another mod has salt; `distillerServingSeconds`, `distillerTankServings` |
| Cooking Pot (Farmer's Delight) | 10 s | 1 | Pure | heat below | only with Farmer's Delight |
| Boiler (Cold Sweat) | 10 s a grade, 30 s Dirty to Pure | up to 27 | up one grade a pass, to Pure | yes | only with Cold Sweat |
| Cold Sweat's Waterskin, furnace or smoker | 10 s / 5 s | 1 | up two grades | yes | hand-written recipes in `src/main/coldsweat` |
| Cold Sweat's Waterskin, campfire | 60 s | 1 | up two grades | no | Cold Sweat's own recipe; `CampfireWaterskinMixin` stamps the grade |
| Teapot (Kaleidoscope Cookery) | 12 s | 4 teacups | safe tea, not water | heat below, a tea bag | a teacup restores its fixed value whatever the grade; sea water refused |
| Cooking Pot (Farm & Charm, and Candlelight's) | 45 s | 1 jug of tea | safe tea, not water | heat below, a glass bottle | takes a water bucket of any grade; sea water refused |
| Tea Kettle (HerbalBrews) | 25 ticks a tea | 1 cup | safe tea, not water | a stove below, blaze powder for heat, a glass bottle | takes a water bucket or any bottle of any grade; sea water refused |

The Waterskin cannot be boiled at all. Clean water gets into it only from something already clean: a
hanging pot, a cauldron, or bottles and buckets boiled elsewhere.

Moving water (Create, Sophisticated tanks and pumps, Supplementaries jars) keeps its grade and cleans
nothing.

### Where Pure water comes from today

Cold mountain water above y 100, a dripstone cauldron, Spelunkery's Spring Water, underground and Nether loot, and every row above that ends in Pure or
"up two grades" from Murky or Clean. That last part is why plain heat is being capped; see
[PURIFICATION-REWORK.md](PURIFICATION-REWORK.md).

## Drinking bad water

Each line below rolls on its own, so one drink can give more than one. "Taste" is Nausea for 7 s,
always. Pure water gives nothing on any difficulty. The tables are `sicknessEffects` in the config;
`SicknessEffect.defaults()` holds these.

Read as: chance, time, level.

### Peaceful

| | Dirty | Murky | Clean |
|---|---|---|---|
| Taste | 100% | 100% | — |
| Upset Stomach | — | — | — |
| Poison | — | — | — |

### Easy

| | Dirty | Murky | Clean |
|---|---|---|---|
| Taste | 100% | 100% | — |
| Upset Stomach | 65%, 45 s, I | 35%, 45 s, I | 5%, 20 s, I |
| Poison | 25%, 10 s, I | 10%, 10 s, I | 3%, 5 s, I |

### Normal

| | Dirty | Murky | Clean |
|---|---|---|---|
| Taste | 100% | 100% | — |
| Upset Stomach | 75%, 60 s, **II** | 50%, 60 s, I | 12%, 30 s, I |
| Poison | 35%, 20 s, I | 18%, 20 s, I | 5%, 8 s, I |

### Hard

| | Dirty | Murky | Clean |
|---|---|---|---|
| Taste | 100% | 100% | — |
| Upset Stomach | 78%, 90 s, **II** | 66%, 90 s, **II** | 20%, 45 s, I |
| Poison | 45%, 30 s, I | 30%, 30 s, I | 10%, 12 s, I |

- **Drinking again while ill** (`extendSicknessEffects`, on): the same effect adds the line's time to
  what is left, up to twice the line's time, and keeps the higher level. Off, vanilla's rule: the
  longer of the two is kept.
- **Every fresh drink still restores thirst**; the grade only cuts its quenched (Dirty none, Murky
  half) and brings the chance of illness.
- A line naming an effect no mod registers is skipped, so a pack can list another mod's effect.

### Sea water

Salt water never restores thirst. It adds 8 exhaustion to the thirst bar (not in the config), Nausea
for 8 s (`seaWaterNauseaSeconds`) and Parched II for 30 s (`seaWaterParchedSeconds`). The sickness
tables above do not apply to it.

## Decisions already made

These came from the hanging pot's design and still hold.

- **A hanging pot holds one bucket, three servings.** It is smaller than a cauldron, so three buckets
  looked wrong next to one. It was nine servings at first.
- **A bucket is three servings, never nine.** Otherwise water would multiply through a cauldron: three
  bottles make a bucket, and that bucket would give nine bottles. That matters in the Nether.
- **Nothing can be poured into a pot where water evaporates**, as in the Nether. The water hisses away
  like a bucket emptied there, but the player keeps it.
- **Boiling is timed per serving**, as a furnace times each item. Pouring more in adds only the new
  servings' time; what has boiled is kept, and water already Pure counts as boiled. A pot's water is one
  batch with one grade, so no serving can be taken out Pure while the rest is still boiling.
- **Copper boils faster, iron holds more** (the canteen and the flask), and copper heats faster than
  iron (the pots), after the metals themselves. What makes the Iron Hanging Pot worth its iron is still
  open: more heat sources (magma, fire, lava) or keeping its heat about 5 s after the fire goes out.
  Copper oxidising and boiling slower as it weathers was set aside: three more textures and blocks.
