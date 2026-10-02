# Let's Do: HerbalBrews integration plan

What ThirstWasTaken2 should do with [Let's Do: HerbalBrews](https://modrinth.com/mod/lets-do-herbalbrews)
(mod id `herbalbrews`, package `net.satisfy.herbalbrews`), the tea and coffee mod of the Let's Do
collection: tea bushes, coffee and herbs, a Tea Kettle that brews them, a Jug that holds three cups and
a Cauldron that mixes potions into a Flask. This file sets the order of work, what each step needs and
how each one is checked. Once the work is built, how it works goes where the steps below say.

Written on 2026-10-02 from:

- the repository [Let-s-Do-Collection/HerbalBrews](https://github.com/Let-s-Do-Collection/HerbalBrews),
  branch `1.21.1` at `3b0d146` (2026-09-19), which is the released `1.1.4`;
- the Modrinth project `Eh11TaTm`.

## Which build for which node

**Only the two 1.21.1 nodes.** The 1.20.1 line stopped at `1.0.12` (2025-04-24) and is not updated any
more, so `1.20.1` and `1.20.1-forge` are left out on purpose, and there is no build newer than 1.21.1.
Same situation as [Farm & Charm](FARM-AND-CHARM-INTEGRATION.md).

| Node | Build | Modrinth id | Requires on `runClient` |
|---|---|---|---|
| `1.21.1` | `1.1.4` Fabric | `43g8sVYA` | Architectury API, already there for Farm & Charm |
| `1.21.1-neoforge` | `1.1.4` NeoForge | `kGYbyn9P` | Architectury API, already there |
| every other node | none | — | — |

Written once in Architectury's `common` module, so the classes the integration touches
(`TeaKettleBlockEntity`, `JugBlock`) have the same names and package on both loaders. One integration
directory serves both, `src/main/herbalbrews`.

## What HerbalBrews already does for thirst

Nothing. Its jar names no thirst mod and none of its items is in `c:drinks`. Its teas and coffees
restore nothing with us today; keyword matching would reach the teas only, and it is off by default.

## Where water lives in HerbalBrews

| Block / path | How water gets in | How it gets out | What happens to the grade today |
|---|---|---|---|
| **Tea Kettle** (`tea_kettle`, `copper_tea_kettle`; needs a heat block below, `#herbalbrews:allows_cooking`) | its water slot takes `#herbalbrews:small_water_fill` (+25 of 100: **any** `minecraft:potion`, splash, lingering potion, even a tipped arrow) or `#herbalbrews:large_water_fill` (+50: `minecraft:water_bucket`). `TeaKettleBlockEntity.tick` empties the slot into a plain `waterLevel` int | each recipe spends `fluid_amount` of it (10 for a tea, 0 for milk coffee) | the level keeps no grade. **Sea water fills the kettle** and brews tea. Fresh water of any grade does too, which is right: the kettle boils |
| **Jug** (`jug`, placed) | three teas or coffees (`DrinkBlockItem`) put in by hand | an empty hand drinks the whole jug at once: every effect, with a longer duration, then it is empty | no water, but **the drinks in it restore nothing**: they are drunk through the block, not as items |
| **Cauldron** (`cauldron`) + Herbal Infusion | three `PotionItem`s | a `flask` carrying their combined effects | three water bottles of any grade make a Flask with no effect. A Flask is not water to us |
| Tea cup (a placed tea) | — | breaking it gives the tea back | decorative; nothing is drunk from it |

`#herbalbrews:small_water_fill` holding every potion is HerbalBrews' choice: a Potion of Harming fills
the kettle as water does. It stays (see Not planned). Our waterskin, canteen and flask are not in either
tag, so the kettle does not take them.

## The drinks

In `ThirstConfig` by id, like Farm & Charm: a `herbalBrewsDrinks` merged with `putMissing`. That reaches
every node, matches nothing where the mod is absent, and needs no code of the mod's. Every drink is a
`DrinkBlockItem` with a food component and a glass bottle as its recipe container. Proposed (thirst,
quenched):

| Group | Items | Proposed |
|---|---|---|
| Tea, a cup | `green_tea`, `black_tea`, `lavender_tea`, `yerba_mate_tea`, `oolong_tea`, `rooibos_tea`, `hibiscus_tea` | 6, 9, as Farm & Charm's and Kaleidoscope Cookery's teas |
| Coffee | `coffee` | 5, 8, as Rustic Delight's coffee |
| | `milk_coffee` | 6, 10, as Rustic Delight's milk coffee |
| Left out | `flask` (a potion mix, see above), `herbal_infusion` (an ingredient), the dried leaves, `tea_blossom`, `rooibos_leaf`, `yerba_mate_leaf`, `coffee_beans` (edible, but dry) | — |

No foods: nothing HerbalBrews makes is a wet dish.

## Status

| # | Item | Kind | Nodes | Status |
|---|---|---|---|---|
| 1 | The mod on the `runClient` classpath | build | both 1.21.1 | **Done** (2026-10-02) |
| 2 | Thirst values for the drinks | data | all (config) | **Done** (2026-10-02), values as proposed |
| 3 | Sea water refused by the Tea Kettle | code | both 1.21.1 | **Done** (2026-10-02) |
| 4 | Drinking a placed Jug restores thirst | decision, then code | both 1.21.1 | **Decided** (2026-10-02): (a), and **done** |
| 5 | What happens to a grade, per path, in game | investigation | both 1.21.1 | **Done** (2026-10-02) |
| 6 | Nothing crashes without the mod | test | both 1.21.1 | **Done** (2026-10-02) |
| 7 | Changelog, player docs, store pages | docs | — | **Done** (2026-10-02), two pictures each |

## 1. The mod on the `runClient` classpath

`deps.herbalbrews` in `[fabric."1.21.1"]` and `[neoforge."1.21.1"]`, pinned by Modrinth id (the two
uploads share a version number), and in `MODRINTH_DEPS`. A row in the integration table (`herbalbrews`,
both loaders, its mixin config, `herbalbrews` as a NeoForge optional dependency). `modCompileOnly` on
Fabric, since it is mixed into, `compileOnly` on NeoForge, and one `runClientMod` line each. Architectury
is already on `runClient` for Farm & Charm; it must stay there when Farm & Charm is left out, so its
`runClientMod` line names HerbalBrews' names too. `-PwithoutOptional=herbalbrews` leaves it out.

## 2. Thirst values

`herbalBrewsDrinks` in `ThirstConfig`, the table above, merged with `putMissing` into older configs. A
gametest checks they are merged into an older config, as `farmAndCharmDrinksAreMergedIntoAnOlderConfig`
does.

## 3. Sea water in the Tea Kettle

Sea water is not the water a HerbalBrews recipe asks for, as it is not for Farm & Charm's Cooking Pot
or Brewin' and Chewin's keg. `TeaKettleBlockEntity.tick` asks `waterItem.is(SMALL_WATER_FILL)` and
`is(LARGE_WATER_FILL)` before it empties the slot. A `@WrapOperation` on `ItemStack.is(TagKey)` in
`tick` returns false for a salty stack: the bucket or bottle stays in the water slot, unused, like a
salty bucket in Farm & Charm's pot. The heat item check in the same method goes through the same call,
and no heat item is ever salty, so it is unaffected.

Fresh water of any grade still fills the kettle: it boils, and what comes out is its own item.

## 4. Decision: drinking a placed Jug

Today a Jug of three teas drunk from the block gives their effects and **no thirst**, though the three
teas are gone. Options:

- **(a) Each tea in the jug counts as drunk.** A mixin on `JugBlock.useItemOn` before `clearDrinks`
  hands each stack in `JugBlockEntity.getDrinks()` to `ThirstManager.drinkItem`, the call a tea drunk
  by hand goes through. Three teas from a jug restore what three teas from the hand do. Recommended.
- (b) The jug as one serving, one tea's value. Cheaper to write, but three teas go in.
- (c) Nothing. The jug stays an effect booster; a player who wants thirst drinks the cups.

## 5. Investigation

A script `tools/agent/integrations/herbalbrews.jsonl`, written for the finished behaviour:

| Case | Expected |
|---|---|
| Every id in step 2, the kettle, the jug | all exist |
| A heated kettle, a plain water bucket in its slot, dried green tea and a bottle | the slot empties, green tea is brewed |
| The same with a sea water bucket | the bucket stays in its slot, no tea |
| Green tea and coffee drunk from thirst 4 | 10 and 9 |
| A placed jug of three green teas drunk from thirst 4 | per step 4 |
| A water bottle in the Cauldron's three slots with an infusion | a Flask, as before |

## 6. Optional seam

`checkOptionalSeam`, `checkLoaderSeam`, `checkVersionSeam`, `checkLang` and `checkDataConditions` pass on
both nodes. `tools/agent/smoke/boot.jsonl` comes up and stays up with `-PwithoutOptional=herbalbrews`
and with `-PwithoutOptional=farm-and-charm` (HerbalBrews still loaded, so Architectury must stay).

## 7. Docs

`CHANGELOG.md` (Unreleased), a site page under the Let's Do section beside Farm & Charm, its sidebar
entry, rows in `docs/docs/installation.md`, a row in the Modrinth and CurseForge main tables and a line
in their versions tables, the root `AGENTS.md`, `WATER-REFERENCE.md` and `SALT-WATER-REFUSALS.md`.

## Not planned

- **1.20.1, Forge, and any version past 1.21.1.** The 1.20.1 line is no longer updated.
- **Our containers in the kettle's tags.** Adding the waterskin or canteen would consume them whole.
- **Every potion counting as water** in `small_water_fill`. HerbalBrews' choice, not a thirst question.
- **A grade on the teas.** They are boiled and are their own items, like Farm & Charm's.
- **The Flask.** A mix of potions; three water bottles make a Flask with no effect, which a player has
  no reason to make.
- **The placed tea cup.** Nothing is drunk from it.
