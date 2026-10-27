# Gensokyo Delight ~ Youkais' Feasts integration plan

What ThirstWasTaken2 should do with
[Gensokyo Delight ~ Youkais' Feasts](https://modrinth.com/mod/gensokyo-delight-youkais-feasts) (mod id
`youkaisfeasts`, package `dev.xkmc.youkaishomecoming`), the Japanese food, tea and sake addon for
Farmer's Delight by the team behind Fruits Delight. This file sets the order of work, what each step
needs and how each one is checked. Once the work is built, how it works goes in
[src/main/youkaisfeasts/AGENTS.md](../../../../src/main/youkaisfeasts/AGENTS.md).

Written on 2026-10-27 from the two jars, decompiled:

- `1.1.2` for NeoForge 1.21.1 (Modrinth `nPxyA8G2`, 2026-10-22);
- `0.5.2` for Forge 1.20.1 (Modrinth `omvhS3Wy`, 2026-10-22; the same file is also tagged NeoForge 1.20.1,
  which this mod does not build).

## Which build for which node

| Node | Build | Modrinth id | Requires on `runClient` |
|---|---|---|---|
| `1.21.1-neoforge` | `1.1.2` | `nPxyA8G2` | Farmer's Delight `[1.2.6,)`. L2 Core, L2 Serial, L2 Modular Blocks, L2 Harvester, L2 Damage Tracker, Registrate and TerraBlender are nested in its jar |
| `1.20.1-forge` | `0.5.2` | `omvhS3Wy` | Farmer's Delight `[1.20.1-1.3,)`. L2 Library and its siblings, Registrate, TerraBlender and MixinExtras are nested |
| every other node | none | — | — |

There is **no Fabric build and nothing past 1.21.1**. Both builds ship the same 300-odd item ids.

The mod is a subset of Youkais' Homecoming (mod id `youkaishomecoming`), which shares its package; the two
cannot be in one pack. That mod is not covered here.

## What the mod already does for thirst

`compat/thirst/ThirstCompat` listens for `dev.ghen.thirst...RegisterThirstValueEvent`, the **upstream**
Thirst Was Taken's event, gated on its mod id `thirst`. With us it never runs, so today none of its drinks
restores anything: none is tagged `c:drinks`. Its values, on the same 0-20 scale:

| Upstream call | Items | Value |
|---|---|---|
| `addDrink` | every `YHDrink`: 11 teas, Green Water, 8 sakes, mead, 3 grape juices, 5 wines | 8, 13 |
| | Milk Popsicle, Big Popsicle, Avgolemono, Shirayuki | 6, 10 |
| | Higan, Poor God, Power, Mushroom, Miso and Seafood Miso soup | 8, 13 |
| | with Fruits Delight: Lemon Black Tea, Moon Rocket, Peach Tapioca | 8, 13 |

These are the values before our purification rework. The proposal below puts each on our bands
(`DefaultItemValues`): a prepared non-alcoholic drink or soup 6, 4; light alcohol 5, 2; wine 3, 1;
cold sweets and wet produce 2, 0 or 1, 0.

## Where water lives in the mod

| Path | Code | What happens to the grade |
|---|---|---|
| **Iron pots**: a water bottle or bucket on an empty Small Iron Pot, Short Iron Pot or Stockpot starts a soup | `IronBowlBlock.startCooking` asks `util/WaterConsumer.isWaterContainer(stack, amount)`: a water bucket, a water potion, or any fluid handler that drains plain water | the pot block is replaced by its cooking pot; the water is gone and the soup is its own item, so **the grade is dropped**. **Sea water is accepted** today |
| **Kettle**: water poured in, then four tea leaves brew 1000 mB of tea | `KettleBlock.useItemOn` (`onClick` on 1.20.1) → `FluidItemTile.addFluidOrItem`: a fluid handler item through `FluidUtil.interactWithFluidHandler`, or a water potion as 250 mB of `Fluids.WATER` | NeoForge's bucket wrapper hands out plain water, so the kettle's tank never sees the grade. Tea is its own item. **Sea water is accepted** |
| **Fermentation Tank**: 1000 mB of water plus rice, honey, grapes or soybeans ferments into sake, mead, wine or soy sauce | the same `FluidItemTile.addFluidOrItem`, from `FermentationTankBlock` | the same: the grade is dropped and **sea water is accepted** |
| **Farmer's Delight Cooking Pot** | `bamboo_mizuyokan` asks for `l2core:potion` water (L2 Library's `PotionIngredient` on 1.20.1) | L2's ingredient reads only `potion_contents`, so **sea water is accepted**. Fruits Delight's integration already refuses it, but only when Fruits Delight is installed |
| **Steamer** | `ClickAddWaterMethod`: a water bucket sets the pot's `water` blockstate | steam, not a drink: the steamed food is solid. Left alone |
| **Copper Tank** | fills itself from a water source above it and heats the water for the steamer | a heat store, nothing to drink. Left alone |
| **Create** (`1.21.1-neoforge` has Create): filling and emptying recipes for every bottled drink, and pipes into the kettle and tank | NeoForge `FluidStack`s | a pumped grade rides on the fluid component, but the recipes test the fluid alone. Not covered, as for Fruits Delight |

## The drinks and foods

In `ThirstConfig` by id, like Fruits Delight's: a `youkaisFeastsDrinks` / `youkaisFeastsFoods` pair in
`config/itemvalues/FarmersDelightAddonItemValues`, merged with `putMissing`. Proposed (thirst, quenched):

| Group | Items | Proposed |
|---|---|---|
| Teas and juices, a bottle | `green_tea`, `white_tea`, `black_tea`, `oolong_tea`, `dark_tea`, `yellow_tea`, `cornflower_tea`, `tea_mocha`, `saidi_tea`, `sakura_honey_tea`, `genmai_tea`, `green_water`, `lemon_black_tea`, `moon_rocket`, `black_grape_juice`, `red_grape_juice`, `white_grape_juice` | 6, 4, a prepared drink |
| Light alcohol | `mead`, `mio` (a sparkling sake in a bottle) | 5, 2, as Brewin' and Chewin's mead |
| Sake and wine | `kiku`, `hakutsuru`, `kappa_village`, `suigei`, `dassai`, `tengu_tango`, `full_moons_eve`, `red_wine`, `white_wine`, `burgundy`, `champagne`, `van_allen` | 3, 1, as Brewin' and Chewin's wines and rice wine |
| Soups and stews, a bowl | `miso_soup`, `seafood_miso_soup`, `poor_god_soup`, `higan_soup`, `mushroom_soup`, `power_soup`, `potato_soup`, `borscht`, `tofu_crab_stew`, `avgolemono`, `signature_mushroom_stew`, `hokkaido_salmon_hotpot` | 6, 4, as Farmer's Delight's stews |
| Cold sweets | `milk_popsicle`, `big_popsicle`, `shaved_ice_over_rice`, `shirayuki`, `peach_tapioca` | 2, 0, as Farmer's Delight's melon popsicle |
| Porridge | `tutu_congee` | 2, 0, as Brewin' and Chewin's grits |
| Produce | `cucumber` | 2, 0, as Cultural Delights' |
| | `cucumber_slice`, `black_grape`, `red_grape`, `white_grape` | 1, 0, a slice or a berry |
| Left out | soy sauce and mayonnaise (condiments in a bottle), the ice cube (an ingredient), matcha, sushi, dango, mochi, the dry dishes and the placeable feasts | — |

The drinks are safe whatever water went into them, as tea is everywhere else.

## Decision

Sea water makes no drink and no soup, as with Brewin' and Chewin's keg, Cultural Delights' vat and
Fruits Delight's juice. Fresh water of any grade is unchanged. Three mixins, every one named by string so
nothing compiles against the mod:

| Mixin | Target | Effect |
|---|---|---|
| `WaterConsumerMixin` | `util.WaterConsumer.isWaterContainer` | false for a salty stack: an iron pot takes no sea water |
| `FluidTileSeaWaterMixin` | `KettleBlock` and `FermentationTankBlock`, at their call of `FluidItemTile.addFluidOrItem` | a salty stack is refused there (`CONSUME`): nothing is poured in and no item slot takes it |
| `WaterBottleIngredientMixin` | L2 Core's `PotionIngredient.test` (L2 Library's on 1.20.1) | a salty bottle is not a water bottle; the same as Fruits Delight's, so it applies with either mod |

`FluidItemTile.addFluidOrItem` itself is a static method of an interface, which Forge 47's Mixin 0.8.5
cannot inject into, hence the call sites. Both are mod-named methods on 1.20.1 (`onClick`, through L2
Modular Blocks) and Mojang-named on NeoForge 1.21.1, so no target needs a refmap entry.

## Status

| # | Item | Kind | Nodes | Status |
|---|---|---|---|---|
| 1 | The mod on the `runClient` classpath, the deps keys and `MODRINTH_DEPS` | build | both | **Done** (2026-10-27) |
| 2 | Thirst values | data | all (config) | **Done** (2026-10-27), values as proposed |
| 3 | The integration directory, its row and the three mixins | code | both | **Done** (2026-10-27) |
| 4 | In-game check | test | `1.21.1-neoforge`, `1.20.1-forge` | **Done** (2026-10-27), both pass |
| 5 | Nothing crashes without the mod: `checkOptionalSeam`, `checkLoaderSeam`, `checkVersionSeam` | test | both | **Done** (2026-10-27) |
| 6 | Changelog, the site's page, installation tables, store pages | docs | — | **Done** (2026-10-27) |

### 1. Build

`deps.youkais_feasts` in `[neoforge."1.21.1"]` and `[forge."1.20.1"]`, `ModrinthDep("youkais_feasts",
"gensokyo-delight-youkais-feasts", by_id=True)`, and one `runClientMod` line per loader script beside Fruits
Delight's. No `compileOnly`. `-PwithoutOptional=youkais-feasts` leaves it out.

### 2. Values

The table above. A gametest beside `fruitsDelightDrinksAreMergedIntoAnOlderConfig` checks the defaults,
that the condiments are left out, and that a merge keeps a player's value. Run with
`-Ptests=ThirstApiGameTest` on `1.21.1-neoforge`.

### 4. In game

A fresh sky-floor world. For plain, Dirty and sea water:

- a water bucket on a Kettle and on an open Fermentation Tank: the plain and Dirty buckets come back empty,
  the sea water one stays full;
- a water bottle on a Small Iron Pot: the plain and Dirty bottles turn it into a Cooking Small Iron Pot, the
  sea water one does not;
- the Bamboo Mizuyokan in a heated Cooking Pot from a plain bottle cooks; from a sea water bottle it does not;
- every id in step 2 exists (`/give`);
- Green Tea and Miso Soup restore 6 from thirst 4.

**Found** on 2026-10-27 with
[youkais-feasts.jsonl](../../../../tools/agent/integrations/youkais-feasts.jsonl) on `1.21.1-neoforge` and
[youkais-feasts-1.20.1.jsonl](../../../../tools/agent/integrations/youkais-feasts-1.20.1.jsonl) on
`1.20.1-forge`, every case as planned on both: plain and Dirty buckets fill the Kettle (1000 mB of water
in its tank) and the Fermentation Tank, the sea water bucket stays full and the tank empty; plain and
Dirty bottles start a Small Iron Pot and the sea water one does not; Bamboo Mizuyokan cooks from a plain
bottle and not from a sea water one; Green Tea and Miso Soup take thirst 4 to 10 and quenched 0 to 4.

Lemon Black Tea, Moon Rocket and Peach Tapioca exist only with Fruits Delight installed (the mod's
`FruitsDelightCompatDrink` and `FruitsDelightCompatFood`), so the script leaves them out of its id check.
Their values stay in the config and match nothing until Fruits Delight is there.

A first run aimed at pitch 45 from 1.5 blocks: the ray passed beside the short kettle and the buckets
poured onto the floor, which reads as a fill. The scripts now stand next to the block at pitch 50 and
clear the air before each case.

## Not planned

- **The mod's own Thirst compat.** It references `dev.ghen.thirst` classes; we do not provide them.
- **Pipes into the kettle and tank, and Create's filling recipes.** They test the fluid alone; refusing sea
  water there would be a change to every tank. Same as Fruits Delight's Create mixer.
- **The Steamer and the Copper Tank**, which make steam and heat, not a drink.
- **Youkais' Homecoming**, the larger mod. Its ids are `youkaishomecoming:`; it can follow this one if asked.
- **Fabric and newer Minecraft versions**, until the mod publishes one.
