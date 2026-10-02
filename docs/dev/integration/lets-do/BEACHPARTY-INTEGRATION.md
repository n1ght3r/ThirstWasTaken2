# Let's Do: Beachparty integration plan

What ThirstWasTaken2 should do with [Let's Do: Beachparty](https://modrinth.com/mod/lets-do-beachparty)
(mod id `beachparty`, package `net.satisfy.beachparty`), the beach mod of the Let's Do collection: palm
trees and coconuts, beach furniture and swimwear, a Palm Bar that mixes cocktails and a Mini Fridge
that freezes water. This file sets the order of work, what each step needs and how each one is checked.
Once the work is built, how it works goes where the steps below say.

Written on 2026-10-02 from:

- the repository [Let-s-Do-Collection/Beachparty](https://github.com/Let-s-Do-Collection/Beachparty),
  branch `1.21.1` at `2b48c91` (2026-09-19), which is the released `2.1.5`;
- the Modrinth project `GyKzAh3l`.

## Which build for which node

**Only the two 1.21.1 nodes.** The 1.20.1 line stopped at `2.0.3` (2025-08-05) and is not updated any
more, so `1.20.1` and `1.20.1-forge` are left out on purpose, and there is no build newer than 1.21.1.

| Node | Build | Modrinth id | Requires on `runClient` |
|---|---|---|---|
| `1.21.1` | `2.1.5` Fabric | `jeZE0Qav` | Architectury API, already there for Farm & Charm; Trinkets `3.10.0` (`JagCscwi`) |
| `1.21.1-neoforge` | `2.1.5` NeoForge | `HN4rzKRl` | Architectury API, already there; Curios `9.5.1+1.21.1` (`yohfFbgD`) |
| every other node | none | — | — |

Modrinth lists Trinkets (Fabric) and Curios (NeoForge) as required. Neither manifest asks for them, but
step 1 found both are needed all the same: the Fabric client entrypoint names Trinkets' renderer
unconditionally (`ClassNotFoundException: dev.emi.trinkets.api.client.TrinketRenderer`), and on NeoForge
the items implement Curios' `ICurioItem`. Both are on `runClient`, pinned by id.

## What Beachparty already does for thirst

Nothing. Its jar names no thirst mod and none of its items is in `c:drinks`.

## Where water lives in Beachparty

| Block / path | Water in | Out | What happens to the grade today |
|---|---|---|---|
| **Mini Fridge** (`mini_fridge`) | `mini_fridge_freezing` recipes: a `minecraft:potion` → `ice`, a `water_bucket` → `powder_snow_bucket` | ice, powder snow | the grade is lost into a block that holds none. Ice broken in place leaves a source block, sampled where it lies like any other, so freezing sea water launders nothing that pouring the bucket out would not |
| **Palm Bar** (`palm_bar`) | `palm_bar_mixing` recipes: fruit, `minecraft:ice`, honey bottle, milk bucket, sugar | the six cocktails | no water ingredient: ice is a block item, not a water container |
| **A water bottle used on sand, gravel or a hay bale** (Beachparty's `PotionItemMixin`, `PotionItem.useOn`) | any `Potions.WATER` bottle | sand waves, sand, a wet hay bale; an empty bottle back | consumes the water, keeps nothing. Every grade and sea water work, which is fine |
| Placed cocktail (`CocktailBlock`, `STAGE 3..0`) | — | an empty hand sips: an effect, the stage down by one, three sips, then the glass breaks | **no thirst**: the sips go through the block, not the item |

## The drinks and foods

In `ThirstConfig` by id: a `beachpartyDrinks` / `beachpartyFoods` pair merged with `putMissing`. The
cocktails are `DrinkBlockItem`s with a food component (1 nutrition) and a 45-second effect. Proposed
(thirst, quenched):

| Group | Items | Proposed |
|---|---|---|
| Cocktails, a glass | `coconut_cocktail`, `sweetberries_cocktail`, `cocoa_cocktail`, `pumpkin_cocktail`, `honey_cocktail`, `melon_cocktail` | 7, 10: fruit and ice, a little under Farmer's Delight's juices (8, 13) |
| Fruit | `coconut_open` (half a coconut, its water in it) | 4, 5, as a melon slice |
| Left out | `coconut` (thrown, not eaten), `raw_mussel_meat`, `cooked_mussel_meat`, `message_in_a_bottle` (a letter) | — |

## Status

| # | Item | Kind | Nodes | Status |
|---|---|---|---|---|
| 1 | The mod on the `runClient` classpath | build | both 1.21.1 | **Done** (2026-10-02), with Trinkets and Curios |
| 2 | Thirst values for the cocktails and coconut | data | all (config) | **Done** (2026-10-02), values as proposed |
| 3 | Sipping a placed cocktail restores thirst | decision, then code | both 1.21.1 | **Decided** (2026-10-02): (a), and **done** |
| 4 | What happens, per path, in game | investigation | both 1.21.1 | **Done** (2026-10-02) |
| 5 | Nothing crashes without the mod | test | both 1.21.1 | **Done** (2026-10-02) |
| 6 | Changelog, player docs, store pages | docs | — | **Done** (2026-10-02), two pictures each |

## 1. The mod on the `runClient` classpath

`deps.beachparty` in `[fabric."1.21.1"]` and `[neoforge."1.21.1"]`, pinned by Modrinth id, and in
`MODRINTH_DEPS`. If step 3 needs code, a row in the integration table (`beachparty`, both loaders, its
mixin config); if it is decided (c), no row and no directory, only the `runClientMod` lines, as
Candlelight has. Architectury's `runClientMod` line names Beachparty's names too, so it stays when Farm &
Charm is left out. `-PwithoutOptional=beachparty` leaves it out.

## 2. Thirst values

`beachpartyDrinks` and `beachpartyFoods` in `ThirstConfig`, merged with `putMissing`, and checked by a
gametest beside Farm & Charm's.

## 3. Decision: sipping a placed cocktail

One cocktail item drunk from the hand is one drink. Placed, the same glass gives three sips, each with a
30-second effect. Options:

- **(a) Each sip is a third of the cocktail.** A mixin on `CocktailBlock.useWithoutItem`, where the stage
  goes down, restores a third of the item's own values (`ThirstApi.thirstValues(block.asItem())`,
  rounded, at least 1). Three sips are about one glass, so placing it is a way to share it, not to
  triple it. Recommended.
- (b) Each sip is a whole cocktail. Placing one glass would be worth three.
- (c) Nothing. A placed cocktail is decoration with an effect.

## 4. Investigation

A script `tools/agent/integrations/beachparty.jsonl`, written for the finished behaviour:

| Case | Expected |
|---|---|
| Every id in step 2, the bar, the fridge | all exist |
| A coconut cocktail and an opened coconut from thirst 4 | 11 and 8 |
| A placed cocktail sipped three times from thirst 4 | per step 3 |
| A water bottle used on sand, with our grade on it | sand waves, an empty bottle, as without us |
| A water bottle and a sea water bottle in the Mini Fridge | ice from both |

## 5. Optional seam

`tools/agent/smoke/boot.jsonl` comes up and stays up with `-PwithoutOptional=beachparty`, and with
`-PwithoutOptional=farm-and-charm` while Beachparty is loaded. If step 3 adds code, `checkOptionalSeam`
and the other checks pass on both nodes.

## 6. Docs

`CHANGELOG.md` (Unreleased), a site page under the Let's Do section, its sidebar entry, rows in
`docs/docs/installation.md`, the Modrinth and CurseForge tables, the root `AGENTS.md`.

## Not planned

- **1.20.1, Forge, and any version past 1.21.1.** The 1.20.1 line is no longer updated.
- **Refusing sea water in the Mini Fridge.** Ice holds no grade, and the water it leaves is sampled
  where it melts, so nothing is laundered that pouring the bucket out would not launder.
- **A drink from the coconut tree** (`hanging_coconut`): it drops coconuts, nothing to drink.
