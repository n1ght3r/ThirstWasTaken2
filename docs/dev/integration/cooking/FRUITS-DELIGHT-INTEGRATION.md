# Fruits Delight integration plan

What ThirstWasTaken2 should do with [Fruits Delight](https://modrinth.com/mod/fruits-delight) (mod id
`fruitsdelight`, package `dev.xkmc.fruitsdelight`), the fruit, juice, jam and jello addon for Farmer's
Delight. This file sets the order of work, what each step needs and how each one is checked. Once the
work is built, how it works goes where the steps below say.

Written on 2026-09-26 from:

- the repository [Minecraft-LightLand/FruitsDelight](https://github.com/Minecraft-LightLand/FruitsDelight),
  branch `1.21` at `f9ec48a` (2026-05-09), which is the released `1.2.14`;
- the Modrinth project `g6sbyCTu`, whose newest 1.21.1 upload is `1.2.14` (`TWbuEFZt`, 2026-05-09).

## Which build for which node

| Node | Build | Modrinth id | Requires on `runClient` |
|---|---|---|---|
| `1.21.1-neoforge` | `1.2.14` | `TWbuEFZt` | Farmer's Delight `[1.2.4,)` (we pin `1.21.1-1.3.4`). L2 Core, L2 Serial, L2 Modular Blocks, L2 Harvester and Registrate are nested in its jar (`jarJar`), and NeoForge loads nested jars itself |
| every other node | none | — | — |

There is **no Fabric build and no build newer than 1.21.1**. Modrinth lists `1.21` as well, but every
1.21.x upload asks for `[1.21.1,1.22)`.

## What Fruits Delight already does for thirst

The mod ships `compat/thirst/ThirstCompat`, behind its own common config `enableThirstCompat` (true by
default). It listens for `dev.ghen.thirst...RegisterThirstValueEvent` and is gated on
`ModList.isLoaded(Thirst.ID)`, the mod id `thirst` of the **upstream** Thirst Was Taken. Our mod id is
`thirstwastaken2`, so with us that code never runs, and today every one of its drinks restores
nothing: the juices are tagged `fruitsdelight:juice`, not `c:drinks`, and keyword matching is off by
default.

Its values are on the same 0-20 scale as ours (thirst, quenched):

| Upstream call | Items | Value |
|---|---|---|
| `addDrink` | every `FDJuice`: the 12 below | 8, 13 |
| `addFood` | orange, lychee, pineapple, kiwi, peach, hamimelon, mango (the fruit item) | 4, 6 |
| | pear | 6, 10 |
| | `baked_pear`, `hamimelon_popsicle`, `kiwi_popsicle` | 8, 13 |
| | `hamimelon_shaved_ice`, `fig_chicken_stew` | 10, 16 |
| | `pear_with_rock_sugar` | 14, 20 |

The drink value matches ours for Farmer's Delight's own `apple_cider` and `melon_juice` (8, 13). The
food values are two to three times what we give comparable food (a melon slice is 4, 5, Farmer's
Delight's stews 4, 5), so the proposal below keeps the drinks and scales the foods to ours.

## Where water lives in Fruits Delight

No block entity holds water, so there is no tank to keep a grade in. Water is used four ways:

| Path | Recipe | What happens to the grade |
|---|---|---|
| **Shapeless crafting with a water bottle** | `orange_juice`, `lemon_juice`, `pear_juice` (`Category.RINSE`, `waterCraft`): fruit, sugar and `l2core` `PotionIngredient.of(Potions.WATER)` | the bottle is consumed and the juice is its own item: **the grade is dropped**. Whether a stamped bottle matches at all depends on how `PotionIngredient` compares (only `potion_contents`, or the whole component map). Step 3 |
| **Farmer's Delight cooking pot** | the teas, `bayberry_soup`, `bellini_cocktail` | no water ingredient: the pot is heated, the recipe asks for fruit, sugar, leaves or ice |
| **Water cauldron** | a full water cauldron + lemon slice → `lemonade_cauldron`; a full water cauldron + a jam bottle → `<fruit>_cauldron`; then heat, sugar, slime ball → jam and jello, taken out with a bottle or a bowl | the cauldron **block is replaced**, so our `purity` blockstate value is gone. Nothing drinkable comes out: jam and jello, both heated. The water is boiled on the way, which is our rule for purification anyway |
| **Create mixing** (`1.21.1-neoforge` has Create on `runClient`) | the `RINSE`, `BOIL` and `SOUP` juices mix 250 mB of `Fluids.WATER` with fruit, the boiled ones heated | the fluid ingredient matches the fluid, not its components, so any grade and possibly sea water mix into juice. Step 3 confirms |

Fruits Delight adds entries to vanilla's `CauldronInteraction.WATER` map (`CauldronRecipe.create`) and
checks `LayeredCauldronBlock.LEVEL == 3`. It never reads our `purity` property, and ours never reads
its cauldrons, so the two only meet where the water cauldron is replaced. Step 3 checks that a graded
full cauldron still takes a lemon slice.

## The drinks and foods

In `ThirstConfig` by id, like Kaleidoscope Cookery and Brewin' and Chewin': a `fruitsDelightDrinks` /
`fruitsDelightFoods` pair merged with `putMissing`. That reaches every node, matches nothing where the
mod is absent, and needs no code of the mod's. Proposed (thirst, quenched):

| Group | Items | Proposed |
|---|---|---|
| Juices and teas, a bottle | `hamimelon_juice`, `kiwi_juice`, `orange_juice`, `lemon_juice`, `pear_juice`, `hawberry_tea`, `mango_tea`, `peach_tea`, `lychee_cherry_tea`, `mangosteen_tea` | 8, 13, as upstream and as Farmer's Delight's juices |
| | `bayberry_soup` (a bottle, sweet and cooked) | 8, 13 |
| | `mango_milkshake` | 8, 12 |
| | `bellini_cocktail` (alcohol, Heal Aura and Nausea) | 5, 6, as Brewin' and Chewin's beer |
| Cold | `hamimelon_shaved_ice` | 8, 10 |
| | `hamimelon_popsicle`, `kiwi_popsicle` | 7, 9, as Farmer's Delight's melon popsicle |
| Jello, a bowl, eaten fast | the 22 `<fruit>_jello` | 3, 4 |
| Fruit | `hamimelon_slice` | 4, 5, as a melon slice |
| | `orange`, `lychee`, `pineapple_slice`, `kiwi`, `peach`, `mango`, `pear` | 2, 3, as an apple |
| | `orange_slice`, `lemon_slice`, `baked_pear` | 1, 2 |
| Bowls | `pear_with_rock_sugar` (a sweet soup) | 6, 8, as Farmer's Delight's fruit salad |
| | `fig_chicken_stew` | 4, 5, as Farmer's Delight's stews |
| | `mango_salad` | 4, 5, as Farmer's Delight's mixed salad |
| | `blueberry_custard` (a glass) | 2, 3, as Farmer's Delight's glow berry custard |
| Left out | the jams (block items in a bottle), `dried_persimmon`, cookies, muffins, pies, tarts, rolls, sheets, sticks, meals, `durian_flesh` and the other dry foods | — |

The ids come from the mod's generated `en_us.json`. `pineapple` is the whole block item; the slice is
what is eaten, so only `pineapple_slice` is listed. Step 3 confirms each id resolves.

## Status

| # | Item | Kind | Nodes | Status |
|---|---|---|---|---|
| 1 | The mod on the `runClient` classpath | build | `1.21.1-neoforge` | **Done** (2026-09-26) |
| 2 | Thirst values for the drinks and foods | data | all (config) | **Done** (2026-09-26), values as proposed |
| 3 | What happens to a grade, per path | investigation | `1.21.1-neoforge` | **Done** (2026-09-26) |
| 4 | A graded or salty water bottle in the juice recipes | decision | `1.21.1-neoforge` | **Decided** (2026-09-26): (b), sea water refused, and **done** |
| 5 | Ask upstream to target `thirstwastaken2` | outreach | — | Not done: optional, and for the user to raise upstream |
| 6 | Changelog and player docs | docs | — | **Done** (2026-09-26) |
| 7 | Nothing crashes without the mod | test | `1.21.1-neoforge` | **Done** (2026-09-26), again after step 4 |

## 1. The mod on the `runClient` classpath (done)

`deps.fruits_delight = "TWbuEFZt"` in `[neoforge."1.21.1"]` and in `MODRINTH_DEPS`, and one
`runClientMod` line in `build.neoforge.gradle.kts`, beside Farmer's Delight's. Step 4 added the row in
the integration table; still no `compileOnly`, since its mixins name their targets by string.
`-PwithoutOptional=fruits-delight` leaves it out.

## 2. Thirst values (done)

`fruitsDelightDrinks` and `fruitsDelightFoods` in `ThirstConfig`, the table above, merged with
`putMissing`. `fruitsDelightDrinksAreMergedIntoAnOlderConfig` checks the defaults, that jam and the dry
foods are left out, and that a merge keeps a player's value. `runGametest` passes (183) on `1.21.1` and
`1.21.1-neoforge`.

## 3. Investigation (done)

[tools/agent/integrations/fruits-delight.jsonl](../../../../tools/agent/integrations/fruits-delight.jsonl),
run on 2026-09-26 on `1.21.1-neoforge` with Cold Sweat left out, passes whole:

| Case | Found |
|---|---|
| Orange juice from a plain, a Dirty and a sea water bottle | **all three accepted**: l2core's `PotionIngredient` tests only `potion_contents`. No glass bottle comes back, as with vanilla's own water bottle recipes |
| Lemon slice on a full plain, Dirty and sea water cauldron | **all three become a lemonade cauldron**; the block is replaced, so the grade goes |
| Create mixing | not run: Create 6 tests a basin's fluid with NeoForge's `SizedFluidIngredient`, on the fluid alone, so every grade and sea water mix into juice |
| Every id in step 2 | exists: `/give` takes each one |
| Drinking from thirst 4, quenched 0 | Orange Juice to 12 and 12 (quenched capped at thirst), Apple Jello to 7 and 4, an Orange to 6 and 3; the tooltips show the droplets |

## 4. Decision (done)

First chosen (a), then changed on 2026-09-26 to **(b)**, so that the three Farmer's Delight addons agree:
sea water makes no safe drink through Brewin' and Chewin's keg, Cultural Delights' vat, or Fruits
Delight. Built as described in [src/main/fruitsdelight/AGENTS.md](../../../../src/main/fruitsdelight/AGENTS.md):

- `WaterBottleIngredientMixin` on L2 Core's `PotionIngredient.test`: a salty bottle is not a water
  bottle, which also reaches other L2 mods' water bottle recipes;
- `FruitCauldronMixin` on `FDCauldronInteraction.perform`: a full cauldron of sea water takes no lemon
  slice and no jam, since the jello at the end of that chain restores thirst.

Create's mixer is not covered; see that file for why. Fresh water of any grade is unchanged.

**Checked** by the agent script, whose sea water cases now expect a refusal: plain and Dirty bottles
make juice and the sea water one does not; plain and Dirty cauldrons become lemonade cauldrons and the
sea water one stays.

## 5. Optional: ask upstream to target `thirstwastaken2`

Fruits Delight lists Thirst Was Taken as an optional dependency on Modrinth. A data pack file in its
own jar (`data/fruitsdelight/thirstwastaken2/drinks/fruitsdelight.json`, see
[data-packs.md](../../../docs/developers/data-packs.md)) would give its values with no class reference
either way. Our config still wins over it. Not needed for anything above.

## 6. Docs (done)

`CHANGELOG.md` (Unreleased), [the site's page](../../../docs/integrations/farmers-delight/fruits-delight.md) and its sidebar
entry, the NeoForge row in `docs/docs/installation.md`, and a line under the Modrinth and CurseForge
compatibility tables. A picture of an orange tree in a village was taken and later dropped as showing
only the mod's own blocks, so neither the site nor the store pages have one.
Step 4 added the root `AGENTS.md` integration row.

## 7. Optional seam (done)

`checkOptionalSeam` and `checkLoaderSeam` pass on `1.21.1-neoforge`, finding the plugin and the gate
loaded without the mod, and `tools/agent/smoke/boot.jsonl` with
`-PwithoutOptional=fruits-delight,cold-sweat` comes up and stays up.

## Not planned

- **Jam and jello cauldrons keeping a grade.** Nothing drinkable comes out, and the jam is heated.
- **Fruits Delight's own Thirst compat.** It references `dev.ghen.thirst` classes; we do not provide
  them, and faking the upstream mod id would break the real upstream mod in the same pack.
- **Fabric and newer Minecraft versions**, until the mod publishes one.
