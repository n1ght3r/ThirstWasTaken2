# Let's Do: Meadow integration plan

What ThirstWasTaken2 should do with [Let's Do: Meadow](https://modrinth.com/mod/lets-do-meadow) (mod id
`meadow`, package `net.satisfy.meadow`), the alpine mod of the Let's Do collection: pine forests and
limestone, wooly cows and water buffalo, cheese, a wooden bucket, a wooden cauldron and a Cooking Pot
(`cookpot`) that cooks over a fire with water in it. This file sets the order of work, what each step
needs and how each one is checked. Once the work is built, how it works goes in
`src/main/meadow/AGENTS.md`.

Written on 2026-10-21 from:

- the repository [Let-s-Do-Collection/Meadow](https://github.com/Let-s-Do-Collection/Meadow), branch
  `1.21.1` at `0c87d86` (2026-10-18), which is the released `1.4.10`;
- the Modrinth project `lets-do-meadow` and the two `1.4.10` jars.

## Which build for which node

**Only the two 1.21.1 nodes.** The 1.20.1 line stopped at `1.3.25` (2025-04-26) and is not updated any
more, so `1.20.1` and `1.20.1-forge` are left out on purpose, and there is no build newer than 1.21.1.

| Node | Build | Modrinth id | Requires on `runClient` |
|---|---|---|---|
| `1.21.1` | `1.4.10` Fabric | `CgcJkvI8` | Architectury API (`>=13.0.8`, ours is 13.0.11) and Cloth Config, both already there; Foundation `1.0.9`, nested in its jar |
| `1.21.1-neoforge` | `1.4.10` NeoForge | `7lmxOKYR` | Architectury API, already there; Foundation, nested |
| every other node | none | — | — |

## What Meadow already does for thirst

Nothing. Its jar names no thirst mod and none of its items is in `c:drinks`.

## Where water lives in Meadow

| Block / item | Water in | Out | What happens to the grade today |
|---|---|---|---|
| **Wooden bucket** (`wooden_bucket`, `WoodenBucket`, not a `BucketItem`) | scoops a water source, any water | `wooden_water_bucket` | **unstamped**, so sea water comes back as `defaultPurity`. Core's `BucketItemMixin` never sees it |
| **Wooden water bucket** in a vanilla cauldron | Meadow puts it in vanilla's `EMPTY` map, and the wooden bucket in `WATER` | a vanilla water cauldron, or a wooden water bucket drawn | core's deferred transfer would carry the grade, but `WaterPurity.resolve` does not know the two ids, so the cauldron is left unset and the drawn bucket unstamped |
| **Wooden cauldron** (`wooden_cauldron`, `wooden_water_cauldron`, `wooden_powder_snow_cauldron`) | a water bucket or a wooden water bucket; rain (Meadow's own chance), a dripstone. A water bottle is refused by Meadow's own inverted check (`!= null \|\|`), its bug | a water bottle, a water bucket or a wooden water bucket drawn | the wooden water cauldron is a `LayeredCauldronBlock` built in Meadow's registry, so on 1.21.1 it carries no `purity` property, and core only handles `Blocks.CAULDRON` and `WATER_CAULDRON`. **Sea water poured in comes back fresh**, at `defaultPurity` |
| **Cooking Pot** (`cookpot`, `CookingCauldronBlockEntity.interact`) | `meadow:water` from the data file `meadow/cooking_liquid/water.json`: a water bucket or wooden water bucket (4 units), a water bottle (1) | food: roasts, cooked meat, beetroot soup, rabbit stew, rennet, alpine salt, leather, two milks | the pot keeps only "water" and an amount, so **sea water cooks** like fresh water |
| Watering can | a water source | waters crops | not drinkable, nothing to grade |
| Milk Can, Cheese Form, Fondue Pot | milk, cheese | — | no water |

## The drinks and foods

In `ThirstConfig` by id: a `meadowDrinks` entry merged with `putMissing`.

| Group | Items | Proposed (thirst, quenched) |
|---|---|---|
| Milk buckets, drunk like the milk bucket | `wooden_milk_bucket`, `wooden_sheep_milk_bucket`, `wooden_buffalo_milk_bucket`, `wooden_goat_milk_bucket`, `wooden_warped_milk_bucket`, `wooden_grain_milk_bucket`, `wooden_amethyst_milk_bucket` | 4, 0, as `minecraft:milk_bucket` |
| Left out | the cheeses, sandwiches, rolls, sticks, cheesecake, tart, buffalo meat and ham: solid food. Beetroot soup and rabbit stew are vanilla's, valued already | — |

## Status

| # | Item | Kind | Nodes | Status |
|---|---|---|---|---|
| 1 | The mod on the `runClient` classpath | build | both 1.21.1 | **Done** (2026-10-21) |
| 2 | Thirst values for the milk buckets | data | all (config) | **Done** (2026-10-21), as proposed |
| 3 | The wooden bucket keeps the grade | code | both 1.21.1 | **Done** (2026-10-21) |
| 4 | Decision: the wooden cauldron | decision, then code | both 1.21.1 | **Decided** (2026-10-21): (a), and **done** |
| 5 | Decision: sea water in the Cooking Pot | decision, then code | both 1.21.1 | **Decided** (2026-10-21): (a), and **done** |
| 6 | What happens, per path, in game | investigation | both 1.21.1 | **Done** (2026-10-21): every case below passes on both |
| 7 | Nothing crashes, with or without the mod | test | both 1.21.1 | `checkOptionalSeam` clean; `boot.jsonl` with Meadow loaded passes on both (2026-10-21); without it still to run |
| 8 | Changelog, player docs, store pages | docs | — | **Done** (2026-10-21), no pictures yet |

## 1. The mod on the `runClient` classpath

`deps.meadow` in `[fabric."1.21.1"]` and `[neoforge."1.21.1"]`, pinned by Modrinth id, and in
`MODRINTH_DEPS`. A row in the integration table (`meadow`, both loaders, its mixin config). Mixed into,
so `modCompileOnly` on Fabric, `compileOnly` on NeoForge, not transitive. Foundation is nested in its
jar; nothing here names a Foundation class, so it is not unpacked onto the compile classpath.
`-PwithoutOptional=meadow` leaves it out. Off in `runClient` by default, like the other Let's Do mods
with world generation (see the "heavy mods off" rule).

## 2. Thirst values

`meadowDrinks` in `config/itemvalues/LetsDoItemValues`, merged with `putMissing`, and checked by a
gametest beside the other Let's Do ones.

## 3. The wooden bucket keeps the grade

- `WaterPurity.resolve` learns `meadow:wooden_water_bucket` as a water container graded like the water
  bucket (three servings, no thirst value, vessel `meadow:wooden_bucket`), and `meadow:wooden_bucket` as
  one that draws from a full cauldron (vessel `meadow:wooden_water_bucket`), as Miner's Delight's
  copper cup. That alone makes vanilla's cauldron keep the grade both ways: Meadow puts both items in
  vanilla's interaction maps.
- `WoodenBucketMixin` on `WoodenBucket.use`: clear `FillCapture` at the head, capture on
  `getPlayerPOVHitResult`, stamp the one `new ItemStack` that method builds, the filled wooden water
  bucket. The core bucket mixin's shape, without its slice: the emptying branch builds its empty bucket
  in `getEmptySuccessItem`, another method.

## 4. Decision: the wooden cauldron

The wooden water cauldron is a `LayeredCauldronBlock` Meadow builds itself, and its interactions are
Meadow's own maps, so core's cauldron code passes it by.

- **(a) Standing water, like Farm & Charm's Water Trough.** Sea water poured into any of the three
  wooden cauldrons is refused (the click is spent); whatever is drawn out of the wooden water cauldron
  is `StandingWater.STANDING`'s grade, Murky, whatever went in or whether rain filled it. A wooden tub is
  an open barrel, and nothing gets better by going through it. One mixin on `AbstractCauldronBlock`
  (Minecraft's, so remapped), applied only with Meadow, that recognises Meadow's three blocks.
  Recommended.
- (b) A real grade. Give the wooden water cauldron the `purity` property (a `@WrapOperation` on the
  `new LayeredCauldronBlock` in Meadow's registry, as `BlocksMixin` does for vanilla) and teach core's
  `WaterInteractions` a second pair of cauldrons and Meadow's interaction maps. More faithful, much more
  code in core for one mod.
- (c) Nothing: sea water in, fresh water out.

## 5. Decision: sea water in the Cooking Pot

- **(a) Refused.** A salty water container on the pot spends the click and stays in the hand, as
  Bountiful Fares' Fermentation Vessel. Fresh water of any grade still cooks: what comes out is food,
  as Farm & Charm's pot. Recommended.
- (b) Taken. Meadow's own salt recipe uses limestone and calcite, so there is no sea water recipe to
  keep.

## 6. Investigation

A script `tools/agent/integrations/meadow.jsonl`, written for the finished behaviour:

| Case | Expected |
|---|---|
| Every id in step 2, the three cauldrons, the pot | all exist |
| A wooden milk bucket from thirst 4 | 8 |
| A wooden bucket scooping ocean water, then river water | Salty, then the river's grade |
| A Salty wooden water bucket on a vanilla cauldron, a glass bottle drawn | a Salty bottle |
| A Salty water bucket on a wooden cauldron | refused, the bucket stays full |
| A Pure water bucket into a wooden cauldron, a glass bottle drawn | a Murky bottle |
| A Salty water bucket on the Cooking Pot | refused; a fresh one fills it |

Checked on 2026-10-21 with `tools/agent/integrations/meadow.jsonl`, on `1.21.1` and `1.21.1-neoforge`:
every case passes as expected, plus a Dirty wooden water bucket pouring into and drawing back out of a
copper hanging pot. The Copper Distiller was not driven; it reads the same `WaterPurity.vessel` as the
hanging pot.

## 7. Optional seam

`tools/agent/smoke/boot.jsonl` comes up and stays up with `-PwithoutOptional=meadow`.
`checkOptionalSeam` and the other checks pass on both nodes.

## 8. Docs

`CHANGELOG.md` (Unreleased), a site page under the Let's Do section, its sidebar entry, rows in
`docs/docs/installation.md`, the Modrinth and CurseForge tables, the root `AGENTS.md`, the plans
`README.md` and `SALT-WATER-REFUSALS.md`.

## Not planned

- **1.20.1, Forge, and any version past 1.21.1.** The 1.20.1 line is no longer updated.
- **Fixing Meadow's water bottle on the wooden cauldron.** Its check is inverted, so a bottle never
  pours in; that is Meadow's bug to fix, and nothing here depends on it.
- **The Fondue Pot and the Milk Can**: milk and cheese, no water.
