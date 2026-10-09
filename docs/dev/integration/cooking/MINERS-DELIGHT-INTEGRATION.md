# Miner's Delight integration plan

What ThirstWasTaken2 should do with [Miner's Delight](https://modrinth.com/mod/miners-delight), the cave
food addon for Farmer's Delight: copper cups that scoop water like a bucket, soups served in those
cups, a Copper Pot and cave crops. This file sets the order of work, what each step needs and how each
one is checked. Once the work is built, how it works goes where the steps below say.

Written on 2026-10-04 from:

- the repository [SammySemicolon/MinersDelight](https://github.com/SammySemicolon/MinersDelight),
  branch `1.21` (the released `1.4.5`) and branch `1.20` (`1.4.5 backport for 1.20.1`);
- the Modrinth project `miners-delight`, whose newest uploads are `1.4.5` (`YUHbwbgQ`, NeoForge
  1.21.1, 2026-04-29) and `1.20.1-1.4.5-backport` (`p0INUam7`, Forge 1.20.1, 2026-05-27).

## Which build for which node

| Node | Build | Modrinth id | Mod id | Requires on `runClient` |
|---|---|---|---|---|
| `1.21.1-neoforge` | `1.4.5` | `YUHbwbgQ` | `minersdelight` | Farmer's Delight, Lodestone (`lodestonelib`) |
| `1.20.1-forge` | `1.20.1-1.4.5-backport` | `p0INUam7` | `miners_delight` | Farmer's Delight |
| every other node | none | — | — | — |

There is **no Fabric build and nothing newer than 1.21.1**. The two builds have **different mod ids
and namespaces**: `minersdelight:water_cup` on 1.21.1, `miners_delight:water_cup` on 1.20.1. Every id
below is listed under both. The Java package is `com.sammy.minersdelight` on both.

## What Miner's Delight does with water

| Thing | What it is | Matters for thirst |
|---|---|---|
| **Copper Cup** (`copper_cup`) | an empty cup, a small bucket: scoops a source block through `BucketPickup.pickupBlock`, then trades the bucket it got for the cup variant (`cup_variant` data map on 1.21.1, `BUCKET_TO_CUP` on 1.20.1) | yes: it draws water |
| **Water Cup** (`water_cup`) | `CopperCupItem(Fluids.WATER)`: places water like a bucket, pours into a cauldron, holds a full bucket (Create fills and empties it with 1000 mB). Not drinkable | yes: it carries water |
| Milk Cup (`milk_cup`) | Farmer's Delight's `MilkBottleItem`: drunk, clears effects | a drink |
| Powder Snow Cup | places powder snow | no |
| Soup cups (16) | `ConsumableItem`s at half the bowl's nutrition, eaten fast; the bowl's soup traded for its cup through the same data map | foods |
| Copper Pot | a small cooking pot running Farmer's Delight's cooking recipes | no water ingredient in any of its recipes |

**Cauldrons.** `MDCauldronInteractions` puts the Copper Cup into vanilla's `WATER` map (draw a cup from
a full water cauldron) and the Water Cup into the `EMPTY` map (pour into an empty cauldron), with the
same `fillBucket` / `emptyCup` shape as vanilla's buckets.

**Create** (`1.21.1-neoforge` and `1.20.1-forge` have Create): `create:filling` fills a copper cup with
1000 mB of water, `create:emptying` empties a water cup to 1000 mB of water. Both are recipes.

## The holes

Nothing the mod does carries our grade:

| Path | Today | Why |
|---|---|---|
| Cup scoops a sea or swamp source | Water Cup with no grade, read as `defaultPurity` (Clean) | `pickupBlock` returns a plain bucket stack; our stamp is in `BucketItemMixin.use`, which the cup never runs |
| Water Cup poured into an empty cauldron | cauldron with `purity=0`, Clean | the cup is not a water container to `WaterInteractions.transferCauldronPurity` |
| Cup drawn from a graded cauldron | Water Cup with no grade | draining only stamps a glass bottle or a bucket |
| Spout fills a cup from a tank of sea water | Clean Water Cup | `FillingBySpout.fillItem` runs the recipe before `GenericItemFilling`, which is where our Create mixin stamps |
| Item Drain empties a salty cup | ungraded water | `GenericItemEmptyingMixin` stamps only what `isWaterContainer` says is water |

Together: **a copper cup turns sea water into Clean water in two clicks** (scoop, pour into a cauldron,
bottle it). That is the one thing this integration has to close. The Copper Pot and the foods need no
code.

## The design

Treat the Water Cup exactly as a water bucket, which is what it is:

1. **A Water Cup is a water container**, by registry id in `WaterPurity.resolve`, as Farmer's Delight's
   and Cold Sweat's are: grade from the config when unstamped, a tooltip line, `ThirstApi.isWaterContainer`.
   It has no thirst value, like the bucket. This alone closes the pour (step 2 of the table: pouring a
   water container into a cauldron already transfers its grade) and the Item Drain.
2. **A Copper Cup draws from a cauldron like a bucket**: `transferCauldronPurity`'s draining check asks a
   new `WaterPurity.drawsFromCauldron`, true for a glass bottle, a bucket and the copper cup (by id, in
   the same cached `ItemInfo`).
3. **A cup that scoops a source is sampled there**, by a mixin on `CopperCupItem.use` in
   `src/main/minersdelight`, the bucket mixin's two steps: capture on `getPlayerPOVHitResult`, stamp the
   result of `ItemUtils.createFilledResult`. The emptying branch hands back an empty cup, which is not a
   container, so the stamp skips it; that keeps one mixin for both versions without slicing from
   `pickupBlock`, whose signature differs between 1.20.1 and 1.21.1.
4. **A Spout filling by recipe stamps the tank's grade**, a `FillingBySpout.fillItem` wrap beside the
   existing `GenericItemFilling` one in `src/main/create` and `src/main/createforge`. It reaches any
   recipe whose result is a water container, not only the cup.

Sea water then stays sea water through the cup, and a dirty cup stays Dirty: nothing new is refused,
since the cup holds water and does not cook it.

## The drinks and foods

In `ThirstConfig` by id, a `minersDelightDrinks` / `minersDelightFoods` pair merged with `putMissing`,
both namespaces. Proposed (thirst, quenched):

| Group | Items | Proposed |
|---|---|---|
| Milk | `milk_cup` | 6, 8, as Farmer's Delight's milk bottle and the milk bucket |
| Its own soups, a bowl | `cave_soup`, `bat_soup`, `insect_stew` | 4, 5, as Farmer's Delight's stews |
| Soup cups, half a bowl eaten fast | `beef_stew_cup`, `chicken_soup_cup`, `fish_stew_cup`, `baked_cod_stew_cup`, `noodle_soup_cup`, `pumpkin_soup_cup`, `vegetable_soup_cup`, `onion_soup_cup`, `mushroom_stew_cup`, `rabbit_stew_cup`, `cave_soup_cup`, `bat_soup_cup`, `insect_stew_cup` | 2, 3, half of 4, 5 |
| | `bone_broth_cup`, `beetroot_soup_cup` | 3, 4, half of 5, 7 rounded up |
| Left out | `water_cup` (a container, no value, like the water bucket), `powder_snow_cup`, `bowl_of_stuffed_squid`, the plates and every dry food | — |

The purification rework (2026-10-06) rebalanced these with every other mod's: the milk cup is 4, 0,
as milk now is everywhere; the three bowls 6, 4; the soup cups 3, 2, half a bowl rounded up. The
player page and both agent scripts follow; the run notes below keep the values of their day.

## Status

| # | Item | Kind | Nodes | Status |
|---|---|---|---|---|
| 1 | The mod on the classpath | build | `1.21.1-neoforge`, `1.20.1-forge` | **Done** (2026-10-04) |
| 2 | Thirst values | data | all (config) | **Done** (2026-10-04), values as proposed |
| 3 | Water Cup a water container, Copper Cup draws from cauldrons | core | all | **Done** (2026-10-04) |
| 4 | Cup scooping a source is sampled | mixin | both | **Done** (2026-10-04) |
| 5 | Spout recipe fills stamped | mixin | both (Create) | **Done** (2026-10-04), checked on `1.20.1-forge`; 1.21.1 has no recipe to fill by (below) |
| 8 | A refused container leaves a cauldron alone | core | all | **Done** (2026-10-04) |
| 6 | Checked in game | test | both | **Done** (2026-10-04) |
| 7 | Docs | docs | — | **Done** (2026-10-04) |

## 1. The mod on the classpath

`deps.miners_delight` in `[neoforge."1.21.1"]` (`YUHbwbgQ`) and `[forge."1.20.1"]` (`p0INUam7`), the
mod in `MODRINTH_DEPS`, a row in the integration table (`minersdelight`, NeoForge and Forge, its mixin
config, the mod id per loader), `compileOnly` / `modCompileOnly` so the mixin names `CopperCupItem` and
the Forge refmap remaps `use`, and a `runClientMod` line with Lodestone on 1.21.1.

## 2. Thirst values

The table above. A gametest beside `fruitsDelightDrinksAreMergedIntoAnOlderConfig` checks the defaults,
both namespaces, that the water cup has no value, and that a merge keeps a player's value.

## 3. Core: the cup as a bucket

`WaterPurity.resolve` and `drawsFromCauldron` as in the design. Gametests can check the resolve only by
id, since the mod is not on the gametest classpath: a test registry item is not possible, so this is
checked in step 6.

## 4. The scoop

`CopperCupItemMixin` with a `MinersDelightPresence` gate and a mixin plugin that applies it only where
`CopperCupItem` still declares `use` (`m_7203_` under SRG).

## 5. The Spout

`FillingBySpoutMixin` in both Create directories, the same body as `GenericItemFillingMixin.fillItem`.

## 6. In game

An agent script per node, `tools/agent/integrations/miners-delight.jsonl` and `-1.20.1.jsonl`:

| Case | Expected |
|---|---|
| Copper cup on an ocean source | Water Cup, Salty |
| that cup into an empty cauldron | cauldron `purity=5` |
| copper cup on a full Dirty cauldron | Water Cup, Dirty |
| Water Cup tooltip, unstamped (`/give`) | Clean (`defaultPurity`) |
| Every id in step 2 | exists |
| Milk Cup, a soup cup from thirst 4 | 10 and 6 |

Create's Spout and Drain by hand, as the Create notes do, if the agent cannot.

**Run on 2026-10-04**, both scripts pass `--verify` on `1.21.1-neoforge` and `1.20.1-forge`: all 21 ids
exist; a cup from the swamp pool is Dirty (`water_purity` 0, NBT `purity:0` on 1.20.1) and one from the
ocean pool Salty; that salty cup poured into an empty cauldron leaves `purity=5` and hands back the empty
cup; a cup drawn from a `purity=1` cauldron is Dirty and empties it; an unstamped Water Cup's tooltip
reads Clean with no droplet rows; the Milk Cup takes thirst 4 to 10 and quenched to 8, a Beef Stew Cup
4 to 6 and 3. The 1.20.1 client runs in Mojang names, so the SRG side was checked on the built refmap:
`use`, `getPlayerPOVHitResult` and `createFilledResult` map to `m_7203_`, `m_41435_` and `m_41813_`,
the names in the Forge jar's `CopperCupItem`.

**Create, run on 2026-10-04** with Create's `runClientMod` line uncommented for the run:
[miners-delight-create-1.20.1.jsonl](../../../../tools/agent/integrations/miners-delight-create-1.20.1.jsonl)
passes on `1.20.1-forge`. A Spout over a copper cup with a bucket of sea water hands back a salty water
cup, one with Dirty water a Dirty cup; an Item Drain fills with 1000 mB of salty water from a salty cup
and of Dirty water from a Dirty one. On `1.21.1-neoforge` the same Spout left the cup empty and the
Drain took nothing: the mod's 1.21.1 jar keeps both Create recipes under `data/minersdelight/recipes/`,
the pre-1.21 folder, so they never load. That is upstream's to fix; the mixin needs nothing when it does.

`runGametest` passes on `1.21.1-neoforge` (205), `1.20.1-forge` (204) and `26.3.x` (203), and
`checkOptionalSeam`, `checkLoaderSeam`, `checkVersionSeam` and `checkApiSurface` pass.

## 8. A refused container leaves a cauldron alone

Found while writing step 3: core's cauldron transfer is queued before vanilla decides, and ran whether or
not anything was poured. A water cup on a full cauldron, which Miner's Delight does not pour into, still
stored the cup's grade; so did a water bottle on a full cauldron and a terracotta water bowl on any, and
a bucket on a cauldron that was not full stamped some other bottle in the inventory. Only ever worse,
never cleaner, but wrong. Fixed in `WaterInteractions.transferCauldronPurity`, as
[purity/AGENTS.md](../../../../src/main/java/com/thirstwastaken2/purity/AGENTS.md) describes: nothing is
queued without a cauldron interaction for the item (`Vanilla.cauldronHasInteraction`, forked at 26.1),
and the queued transfer needs the cauldron or the hand to have changed. A first try with the second check
alone failed in game: the refused water cup places its water in the world in the same tick, so the hand
changed anyway.

`CauldronGameTest` now pours a real bucket through `gameMode.useItemOn` and adds
`aContainerTheCauldronRefusesChangesNothing` (a bottle on a full cauldron, a bowl) and
`aDrawThatDoesNotHappenStampsNothing`. The benchmark's `cauldron_pour` pours a bucket instead of a bowl.
`runGametest` passes on `26.3.x`, `26.1.x`, `1.21.11`, `1.21.1`, `1.21.1-neoforge`, `1.20.1` and
`1.20.1-forge`; "Set active project to 1.20.1-forge" then "Reset active project" leaves `git diff`
unchanged. Both cup scripts gained the case (a Dirty water cup on a full Pure cauldron leaves it Pure and
comes back empty) and pass on both nodes.

## 7. Docs

`CHANGELOG.md` (Unreleased), a site page under `docs/docs/integrations/farmers-delight/`, its sidebar
entry and `mods.ts`, the rows in `installation.md`, and the store pages (a Farmer's Delight addon).
`src/main/minersdelight/AGENTS.md`, the integration README row, and the root `AGENTS.md` rows.

## Not planned

- **Fabric and newer Minecraft versions**, until the mod publishes one.
- **The Copper Pot.** No recipe in it takes water.
- **A water cup refusing sea water.** It holds water and makes nothing from it; it now keeps the grade.
