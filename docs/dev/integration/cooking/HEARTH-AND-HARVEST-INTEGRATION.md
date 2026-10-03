# Hearth and Harvest integration plan

What ThirstWasTaken2 should do with [Hearth and Harvest](https://modrinth.com/mod/hearth-and-harvest)
(mod id `hearthandharvest`, package `alabaster.hearthandharvest`), the homesteading addon for Farmer's
Delight: juices, wines and casks, jugs, troughs, a sink, a sprinkler, a stomping basin and salt. This
file sets the order of work, what each step needs and how each one is checked. Once the work is built,
how it works goes where the steps below say.

Written on 2026-10-03 from:

- the repository [AlabasterLeking/Hearth-And-Harvest](https://github.com/AlabasterLeking/Hearth-And-Harvest):
  - branch `NeoForge-1.21.1` at `09793769` (2026-09-06), the last commit with `mod_version=1.3.4`,
    the released build. The branch head, `2430be7c` (2026-10-02), is an unreleased `1.4.0` with Kegs
    and a goat milk fluid; see [What 1.4.0 changes](#what-140-changes);
  - branch `Forge-1.20.1` at `e97c54dd` (2026-09-14), the released `1.0.12c`;
  - branch `Fabric-1.21.1` at `786e3ee9` (2025-04-10): a Gradle setup and nothing else, never released;
- the Modrinth project `8EEEXOzj`.

## Which build for which node

| Node | Build | Modrinth id | Requires on `runClient` |
|---|---|---|---|
| `1.21.1-neoforge` | `1.3.4` (2026-09-08) | `7nTyt27O` | Farmer's Delight `[1.3.0,)`, which the node already runs for Fruits Delight |
| `1.20.1-forge` | `1.0.12c` (2026-09-14) | `Lc4WN5U1` | Farmer's Delight `[1.3.0,)`, as above |
| every other node | none | — | — |

There is **no Fabric build and nothing newer than 1.21.1**. Both lines are still updated, so both get
the integration. The two are different code: 1.0.12c is the early mod (a cask, a jug, a wine rack, fewer
items), 1.3.4 the full one. Code for 1.20.1, if any is needed, goes in its own directory
(`hearthandharvestforge`), as Create and Cold Sweat do.

## What Hearth and Harvest already does for thirst

On 1.21.1, `integration/ThirstWasTakenCompat` listens for `dev.ghen.thirst...RegisterThirstValueEvent`,
gated on `ModList.isLoaded("thirst")`: the **upstream** Thirst Was Taken. Our mod id is
`thirstwastaken2`, so with us that code never runs. Its juices are not tagged `c:drinks`, so today every
Hearth and Harvest drink restores nothing. The 1.20.1 build has no thirst compat at all.

Its values (thirst, quenched), on the same 0-20 scale as ours, are generous:

| Upstream | Items | Value |
|---|---|---|
| wines | the 8 `*_wine` | 10, 14 |
| strong drinks | `mead`, `root_beer`, `hard_cider`, `moonshine` | 12, 18 |
| juices | 6 of the 7 `*_juice` (not `sweet_berry_juice`) | 8, 12 |
| milk | `goat_milk_bottle` | 6, 8 |

A wine is worth more than a water bottle there, and moonshine the most of all. We give Brewin' and
Chewin's wines 3, 4 and its rum 2, 2, so the proposal below keeps the juices and milk and scales the
alcohol to ours.

## Where water lives in Hearth and Harvest (1.3.4)

Almost every block that holds a fluid goes through one class, `common/fluid/HHFluidHandling`.
`useOnTank` is what a click on the Sink, the Jug, the Trough and the Stomping Basin calls; it asks
`testEmptying` (item into tank) and `testFilling` (tank into item):

- a **water bottle** is recognised by `potion_contents` alone and poured in as
  `new FluidStack(Fluids.WATER, 250)`, **with no components**: the grade or salt is dropped;
- a **glass bottle** on a tank of water comes back as `PotionContents.createItemStack(POTION, WATER)`,
  **unstamped**, which reads as `defaultPurity` (Clean);
- anything else goes through the item's fluid capability (`FluidUtil.tryFluidTransfer`). NeoForge's
  bucket wrapper takes and gives plain water, so a bucket loses its grade too. Our waterskin, canteen,
  flask and bowls carry the grade on their capability already (see `purity/AGENTS.md`).

So **every tank launders**: Dirty or sea water poured in comes back Clean. On top of that:

| Block / item | What it does with water | Problem |
|---|---|---|
| **Sink** (`basin`, `BasinBlock`, 1000 mB) | refills itself by 250 mB every 3 s while not powered, **from nothing** (`BasinBlock.tick`) | an endless Clean tap, the same hole as Candlelight's kitchen sink |
| **Trough** (`trough`, 1000 mB) | rain adds 4 mB a second (`TroughBlockEntity`); it is for animals, but `useOnTank` lets a bottle draw from it | rain water comes out as `defaultPurity`, not as our `rainwaterPurity`. Both are Clean by default, so it only matters if a pack changes one |
| **Jug** (`jug`, block and item, 8000 mB, any fluid) | the jug **item** scoops a source block from the world (`JugBlockItem.use`) with `new FluidStack(fluidState.getType(), 1000)` | **no sampling**: ocean water scooped into a jug pours out as Clean fresh water. The worst hole, since it needs nothing but a jug and the sea |
| **Stomping Basin** (8000 mB, 32000 combined) | recipe `stomping/sponge`: a wet sponge gives 1000 mB of water | the sponge holds no grade (vanilla), so a sponge soaked in the ocean is squeezed into Clean water |
| **Sprinkler** (10 000 mB, water only) | waters crops; filled through `FluidUtil.interactWithFluidHandler`, which also lets a bucket draw back out | through the bucket wrapper: a Dirty or salty bucket in, a Clean bucket out |
| **Cask** (aging) | `aging/mead`, `aging/moonshine`, `aging/root_beer` take a water bottle as a `neoforge:components` ingredient on `potion_contents` | a partial match: **any grade and sea water** age into a drink |
| Cooking Pot (Farmer's Delight) | `minecraft:salt_from_bottle`: one water bottle → 2 salt; `minecraft:salt_from_bucket`: a water bucket → 8 salt | **collides with our `cooking_pot_purify_water_bottle`**, also one water bottle and nothing else. See below |
| Crafting | `flour_dough_from_water_bottle`, `flour_dough_from_water_bucket`, `tortilla` | dough and tortillas, cooked afterwards; not drinks. Left alone, as with Farm & Charm's dough |
| Watering Can, Tree Tapper | the can keeps a `water_level` counter, not a fluid; the tapper holds only sap | nothing a player drinks |

Hearth and Harvest also makes plain water bottles stack to 16 (`WaterBottleMixin`, config
`STACK_WATER_BOTTLES`, on by default), testing `potion_contents` only. Our stamped bottles stack with
bottles of the same grade only, since their components differ. Step 3 checks nothing of ours assumes a
bottle stack of 1.

### The salt recipe collision

Both recipes are `farmersdelight:cooking`, one ingredient, no container:

| Recipe | Ingredient | Result |
|---|---|---|
| ours, `thirstwastaken2:cooking_pot_purify_water_bottle` | a water bottle with `water_purity` 0-2 and `water_salty: false` | a Pure water bottle |
| theirs, `minecraft:salt_from_bottle` | 1.21.1: any `minecraft:potion` whose `potion_contents` is water. 1.20.1: **any `minecraft:potion`** | 2 salt |

A Dirty, Murky or Clean bottle matches both, and which one the pot cooks depends on recipe order, not on
anything the player can see. A Pure bottle and a sea water bottle match only theirs. Purifying in a
Cooking Pot is one of our main paths, so this is the first thing step 3 measures.

## The drinks and foods

In `ThirstConfig` by id, like Fruits Delight: a `hearthAndHarvestDrinks` / `hearthAndHarvestFoods` pair
merged with `putMissing`. It reaches both nodes with one list (1.20.1's ids are a subset), matches
nothing where the mod is absent, and needs no code of the mod's. Proposed (thirst, quenched):

| Group | Items | Proposed |
|---|---|---|
| Juices, a bottle | `blueberry_juice`, `cherry_juice`, `raspberry_juice`, `red_grape_juice`, `green_grape_juice`, `sweet_berry_juice`, `glow_berry_juice` | 8, 13, as Farmer's Delight's `melon_juice` |
| Milk | `goat_milk_bottle`, `chocolate_milk_bottle` | 6, 8, as Farmer's Delight's `milk_bottle` |
| Light alcohol | `mead`, `hard_cider`, `root_beer` | 5, 6, as Brewin' and Chewin's beer and mead |
| Wine | `blueberry_wine`, `cherry_wine`, `raspberry_wine`, `red_grape_wine`, `green_grape_wine`, `sweet_berry_wine`, `glow_berry_wine`, `melon_wine` | 3, 4, as Brewin' and Chewin's wines |
| Spirit | `moonshine` | 2, 2, as Brewin' and Chewin's rum |
| Syrup | `syrup_bottle` | 2, 3, as Rustic Delight's syrup |
| Bowls | `corn_stew`, `onion_soup` (1.20.1 only, its own id) | 4, 5, as Farmer's Delight's stews |
| Fruit | `blueberries`, `raspberry`, `cherry`, `red_grapes`, `green_grapes` | 1, 2, as sweet berries |
| | `baked_apple`, `caramel_apple` | 2, 3, as an apple |
| Left out | the jams and `peanut_butter` (in a jar), pickles, cheese, `sap_bucket`, `cooking_oil`, pies, cakes, cookies, candy, the sandwiches, `mashed_potatoes`, `macaroni_and_cheese`, `biscuits_and_gravy` and the other dry foods | — |

`root_beer` is aged in a cask like the others; if it carries no Drunk effect in game it moves up to
6, 8 in step 3. The aged wines keep their value whatever their vintage. Step 3 confirms every id
resolves on both nodes.

## Status

| # | Item | Kind | Nodes | Status |
|---|---|---|---|---|
| 1 | The mod on the `runClient` classpath | build | `1.21.1-neoforge`, `1.20.1-forge` | **Done** (2026-10-03) |
| 2 | Thirst values for the drinks and foods | data | all (config) | **Done** (2026-10-03), values as proposed |
| 3 | What happens to a grade, per path; which salt recipe wins | investigation, then check | both | **Done** (2026-10-03), as the check of steps 4 to 7 |
| 4 | Tanks keep the grade (`HHFluidHandling`) | decision, then code | `1.21.1-neoforge` | **Decided** (2026-10-03): (a), and **done** |
| 5 | Water from nothing or from the world: Sink, Jug item, sponge, rain | decision, then code | `1.21.1-neoforge` | **Decided** (2026-10-03): as proposed, and **done** |
| 6 | Sea water refused in the cask | code | `1.21.1-neoforge` | **Done** (2026-10-03); 1.20.1 needs none |
| 7 | The salt recipe collision | decision, then data | both | **Decided** (2026-10-03): (a), and **done**; a mixin on Forge, see below |
| 8 | Ask upstream to target `thirstwastaken2` | outreach | — | Not done: optional, for the user to raise |
| 9 | Nothing crashes without the mod | test | both | **Done** (2026-10-03) |
| 10 | Changelog, player docs, store pages | docs | — | **Done** (2026-10-03) |
| 11 | The Jug keeps the grade on Forge 1.20.1 | code | `1.20.1-forge` | **Done** (2026-10-03), after the first pass |

How it works now is in [src/main/hearthandharvest/AGENTS.md](../../../../src/main/hearthandharvest/AGENTS.md)
and [src/main/hearthandharvestforge/AGENTS.md](../../../../src/main/hearthandharvestforge/AGENTS.md).

## 1. The mod on the `runClient` classpath

`deps.hearth_and_harvest = "7nTyt27O"` in `[neoforge."1.21.1"]` and `"Lc4WN5U1"` in `[forge."1.20.1"]`,
the mod in `MODRINTH_DEPS` of `.github/scripts/update_mc_deps.py`, and a `runClientMod` line in
`build.neoforge.gradle.kts` and `build.forge.gradle.kts` beside Farmer's Delight's. A row in the
integration table once step 4 brings code (`compileOnly` then too, since the mixins target
`HHFluidHandling` and `JugBlockItem`). `-PwithoutOptional=hearth-and-harvest` leaves it out.

**Check:** both nodes' `runClient` come up with the mod listed, no errors from either mod at load.
Done as written; the mod is `compileOnly` on `1.21.1-neoforge` only, since nothing compiles against it on
Forge.

## 2. Thirst values

`hearthAndHarvestDrinks` and `hearthAndHarvestFoods` in `ThirstConfig`, the table above, merged with
`putMissing`. A gametest in the shape of `fruitsDelightDrinksAreMergedIntoAnOlderConfig`: the defaults,
that jam and the dry foods are left out, and that a merge keeps a player's value.

**Check:** `runGametest` on `1.21.1`, `1.21.1-neoforge` and `1.20.1-forge`. Passed (200, 200, 199) with
`hearthAndHarvestDrinksAreMergedIntoAnOlderConfig`. `root_beer` stays at 5, 6: it was not drunk in game.

## 3. Investigation

An agent script, `tools/agent/integrations/hearth-and-harvest.jsonl`, on `1.21.1-neoforge` with Cold
Sweat left out, and the salt and id cases again on `1.20.1-forge`:

| Case | Expected today |
|---|---|
| A Dirty bottle and a sea water bottle into a Sink, a Jug and a Trough, then a glass bottle out | Clean, both: the laundering above |
| A Dirty and a salty bucket into the Sprinkler, then an empty bucket out | Clean, both |
| The Jug item on an ocean source, then a bottle from the placed jug | Clean fresh water |
| An empty Sink left 15 s, then a bottle | Clean, from nothing |
| A wet sponge from the ocean stomped, then a bottle | Clean |
| Mead in a cask with a plain, a Dirty and a sea water bottle | mead from all three |
| A Dirty, a Pure and a sea water bottle alone in a heated Cooking Pot | **unknown for Dirty**: salt or Pure water. Pure and sea water: salt |
| A water bucket alone in the pot | 8 salt |
| A stack of 16 graded bottles in a furnace and a smoker | purified one by one, nothing lost |
| Every id in step 2 | `/give` takes it |
| Drinking from thirst 4, quenched 0 | juice to 12 and 12, wine to 7 and 4, the tooltips show the droplets; whether `root_beer` makes the player Drunk |

**Check:** the script passes whole and this section records what it found.

The table above was written before the code, from the source and the 1.3.4 jar's bytecode, and the code
was built against it; the script was written with the fixes in, so it checks them. On 2026-10-03,
[hearth-and-harvest.jsonl](../../../../tools/agent/integrations/hearth-and-harvest.jsonl) on
`1.21.1-neoforge` and [hearth-and-harvest-1.20.1.jsonl](../../../../tools/agent/integrations/hearth-and-harvest-1.20.1.jsonl)
on `1.20.1-forge` pass whole:

| Case | Found |
|---|---|
| Every id in step 2 | exists on both nodes (1.20.1 has its subset) |
| A Sink left 10 s, then a glass bottle | Murky |
| A salty bottle on the Sink | refused, kept in hand |
| A Pure bottle into the Sink, then drawn back | taken; Murky comes back |
| A Dirty bottle into a Trough, a Clean one after it, then a draw | Dirty taken, Clean refused, Dirty drawn |
| A salty bottle into an empty Trough, then a draw | salty |
| A Dirty bucket into the Sprinkler, then an empty bucket | a Dirty bucket |
| The Jug item on an ocean source | the jug holds sea water |
| A wet sponge stomped in a Stomping Basin, then a bottle | Murky |
| Mead's ingredients in a Cask, with a Murky bottle and with a salty one | the first ages, the second does not start |
| A Murky, a sea water and a Pure bottle in heated Cooking Pots | Pure water; two salt; the Pure bottle stays, so the replacement won (1.21.1) or the mixin holds (1.20.1) |
| On 1.20.1, a Dirty bucket into a placed Jug, a Clean one after it, then an empty bucket; a salty one into a new Jug | Dirty taken, Clean refused, Dirty drawn; salty drawn (added later, see "The Jug on Forge") |
| Drinking from thirst 4 | cherry juice to 12 and 12, red grape wine to 7 and 4 (1.21.1); cherry juice to 12 on 1.20.1, where a juice is a food and needs hunger |

What the first runs found: on Forge the replacement file lost to Hearth and Harvest's own and the Pure
bottle boiled into salt, so step 7 changed there (below). Expanded Delight, also on the 1.21.1 client,
refused the sea water bottle in the pot; step 7 says how. The furnace stack case was not run: Hearth and
Harvest's stacking leaves our stamped bottles alone, since they stack only with their own grade.

## 4. Decision: tanks keep the grade

All four tanks go through `HHFluidHandling.testEmptying` and `testFilling`, so the fix is one mixin
pair there, in `src/main/hearthandharvest`, in the shape of Sophisticated's `WaterQualityFluidHandler`:

- `testEmptying`: hand the original an **unstamped** copy of a water container (so their
  `isWaterBottle` still knows it, and NeoForge's bucket wrapper still drains it), then stamp the
  returned `FluidStack` with the container's quality through `WaterFluids.stamp`. Our own containers
  already carry the grade on their capability and are passed as they are;
- `testFilling`: after the original, stamp the returned item with `WaterFluids.quality` of the tank's
  water, if it is water.

The tank's `FluidTank` compares with `isSameFluidSameComponents`, so two grades and sea water never
share a Hearth and Harvest tank, the same rule as Create's and Sophisticated's. Unstamped water already
in a tank from an older world reads as `defaultPurity` and stays that way.

The Sprinkler does not use `HHFluidHandling`; it calls `FluidUtil.interactWithFluidHandler` directly.
Options: **(a)** wrap that call in `SprinklerBlock.useItemOn` the same way; **(b)** refuse drawing water
back out of a sprinkler, which is a crop block and has no reason to be a tap; **(c)** nothing, since
taking water back out of a sprinkler is a detour few players take. Proposed: **(a)**, so every Hearth and
Harvest tank follows one rule.

**Check:** the step 3 cases expect the grade back: Dirty in, Dirty out; sea water in, salty out; a
Dirty bottle into a Clean jug refused.

**Decided (a) and done.** `FluidHandlingMixin` wraps both methods; the pour is simulated against a
`StampingTank` that stamps what it is filled with, since Hearth and Harvest checks a pour with plain water
first, which a graded tank would refuse. The Sprinkler's `FluidUtil` call is replaced by `useOnTank`, so a
glass bottle now draws from it too. Passed as in step 3.

## 5. Decision: water from nothing or from the world

Step 4 keeps a grade that exists. Four places make water without one:

| Source | Proposed |
|---|---|
| Sink self-fill (`BasinBlock.tick`) | stamp it Murky, the `StandingWater.STANDING` grade the Candlelight sink and the Farm & Charm trough already hand out. `StandingWater` moves from `src/main/farmandcharm` to core `purity/` so both integrations share it |
| Jug item scooping a source (`JugBlockItem.use`) | stamp `incoming` with `WaterPurity.sampleAt(level, pos)`, as any other collection from the world: ocean water is salty, a swamp Dirty |
| Wet sponge in the Stomping Basin | stamp the recipe's `result_fluid` Murky: a sponge is not a filter. Where to hook is for step 3 to find (the basin's recipe output) |
| Rain into a Trough | stamp `WaterPurity.rainwaterPurity()`, as a cauldron filling in the rain |

The other options, for the record: (b) the Sink stops filling itself, which takes away what the block
is for; (c) nothing, which makes a Hearth and Harvest kitchen a free Clean tap and the jug a desalinator.

One thing to settle in code: a Sink holding poured Clean water refuses its own Murky self-fill (different
components), and `BasinBlock.tick` plays its sound whether or not the fill took. Either skip the tick when
the fill would not take, or accept a sink that stays as it was poured.

**Check:** the step 3 cases: the sink gives Murky, the jug from the ocean gives sea water, the ocean
sponge gives Murky, a rain-filled trough gives Clean.

**Decided as proposed and done**, with two changes. `StandingWater` stayed where it is: the integration has
its own `HearthWater.STANDING`, the same Murky, rather than moving a class another integration owns. And
the open question is settled by the sink holding nothing but its own water: everything poured into it
becomes Murky, sea water is refused, and water saved unstamped is restamped first, so the self-fill always
takes. Rain in a trough was not run in game; the mixin is one line on the rain fill.

## 6. Sea water refused in the cask

Mead, moonshine and root beer take a water bottle by `potion_contents` alone. As with Brewin' and
Chewin's keg, Cultural Delights' vat and Fruits Delight's juices, **sea water makes no drink**, and
fresh water of any grade does, since the drink is aged (and alcohol is safe whatever went into it, the
rule B&C follows). A mixin on the cask's recipe match (`CaskBlockEntity`, where it looks the recipe up)
refuses an input slot holding a salty water container; step 3 names the method. The bottle stays in its
slot and nothing ages.

On 1.20.1 the cask's mead takes a honey bottle, not water; step 3 checks no other aging recipe there
takes water, and if none does, 1.20.1 needs no code at all.

**Check:** the step 3 cask case: mead from the plain and the Dirty bottle, nothing from the salty one.
A line in `docs/dev/mechanics/SALT-WATER-REFUSALS.md`.

**Done**, on `CaskBlockEntity.getMatchingRecipe`. 1.0.12c has only the honey bottle mead, so 1.20.1 needs
nothing.

## 7. Decision: the salt recipe collision

Options:

- **(a) Salt from sea water only, by bottle.** Ship our own `data/minecraft/recipe/salt_from_bottle.json`
  with a `mod_loaded` condition on `hearthandharvest`, whose ingredient asks for `water_salty: true`.
  Fresh bottles then match only our purify recipe, sea water only theirs, and boiling sea water down to
  salt is what it is in life. Ours must load after Hearth and Harvest's for the override to win: an
  optional `hearthandharvest` dependency with `ordering = "AFTER"` in both loader manifests, which the
  integration table row can write. The bucket recipe stays as it is: nothing of ours collides with it.
- **(b) Ours always wins.** A mixin in Farmer's Delight's Cooking Pot recipe lookup that prefers our
  recipe when both match. Touches every Farmer's Delight pack, for one collision.
- **(c) Nothing.** A player purifying Dirty water in a Cooking Pot may get salt instead, depending on
  recipe order.

Proposed: **(a)**. It changes Hearth and Harvest's balance a little (fresh bottles stop giving salt), but
salt still comes from buckets, salt caverns and salt blocks, and it is the one option that makes both
recipes readable to the player. On 1.20.1 the override has the 1.20.1 recipe format (`recipes/`, the
Forge `forge:nbt`-style partial ingredient); `checkDataConditions` covers both.

**Check:** step 3's pot cases: Dirty bottle → Pure water, sea water bottle → 2 salt, Pure bottle →
nothing, water bucket → 8 salt. Run on both nodes.

**Decided (a) and done**, differently per loader:

- **1.21.1.** The replacement file as written, from `FarmersDelightRecipeProvider`, generated for 1.21.1
  only, with `ordering = "AFTER"` from the row's new `loadAfter`. It wins: the Pure bottle stays in the pot.
- **Forge 1.20.1.** The replacement lost whatever the ordering, so it was dropped. In its place
  `src/main/hearthandharvestforge` has one mixin on Farmer's Delight's `CookingPotRecipe.matches`: the
  recipe `minecraft:salt_from_bottle` fails when a water container in the pot is not salty. Same result.
- **Expanded Delight** refuses sea water in every Cooking Pot recipe, which also stopped the salt. Its
  mixin now lets through a recipe that asks for sea water itself, an ingredient that takes the salty stack
  and not the same stack fresh. `expanded-delight.jsonl` still passes.
- `farmers-delight.jsonl` and its 1.20.1 twin put a salty bottle in the pot and expect it to stay, so they
  now run with `-PwithoutOptional=hearth-and-harvest`.

The bucket case was not run; its recipe is untouched.

## 8. Optional: ask upstream to target `thirstwastaken2`

Hearth and Harvest already ships Thirst compat, for the upstream mod. A data pack file in its own jar
(`data/hearthandharvest/thirstwastaken2/drinks/hearthandharvest.json`, see
[data-packs.md](../../../docs/developers/data-packs.md)) would give its values with no class reference.
Our config still wins over it. Not needed for anything above.

## 9. Optional seam

`checkOptionalSeam`, `checkLoaderSeam` and `checkDataConditions` pass on both nodes, and
`tools/agent/smoke/boot.jsonl` with `-PwithoutOptional=hearth-and-harvest,cold-sweat` comes up and stays
up on `1.21.1-neoforge` and `1.20.1-forge`. **Done** (2026-10-03).

## 10. Docs

`CHANGELOG.md` (Unreleased), a page under `docs/docs/integrations/farmers-delight/` and its sidebar
entry, the NeoForge and Forge rows in `docs/docs/installation.md`, the Modrinth and CurseForge
compatibility tables, `src/main/hearthandharvest/AGENTS.md` once there is code, and the root `AGENTS.md`
integration row. Other versions ignore Hearth and Harvest, which has no build for them.

**Done** (2026-10-03).

## The Jug on Forge (done, 2026-10-03)

Left open in the first pass: on 1.20.1 the placed Jug moves water through item fluid capabilities, and
Forge's bucket wrapper pours and draws plain water, so a Dirty bucket came back Clean. `JugBlockMixin` in
`src/main/hearthandharvestforge` wraps the held container's handler so that the grade crosses it both
ways, as on the 1.21.1 tanks. Checked by the 1.20.1 script.

## What 1.4.0 changes

The branch head (unreleased, `mod_version=1.4.0`) adds a `KegBlockEntity` with data-driven aging, a goat
milk fluid and a goat milk bucket, and reworks the cask; `HHFluidHandling` itself is unchanged. When it
is released: re-run step 3, check the keg (whether any keg recipe takes water, and whether it uses
`useOnTank`), add `goat_milk_bucket` at 6, 8 if it is drinkable, and move the pin.

## Not planned

- **The Watering Can.** It keeps a counter, not water, and nothing drinkable comes out.
- **Dough and tortillas from a water bottle.** Not drinks, and cooked afterwards; Farm & Charm's dough
  was left alone the same way.
- **Hearth and Harvest's own Thirst compat.** It references `dev.ghen.thirst` classes; we do not provide
  them, and faking the upstream mod id would break the real upstream mod in the same pack.
- **Hearth and Harvest's water bottle stacking.** Its own feature; step 3 only checks it breaks nothing.
- **Fabric and newer Minecraft versions**, until the mod publishes one.
