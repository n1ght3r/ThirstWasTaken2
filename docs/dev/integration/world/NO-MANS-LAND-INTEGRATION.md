# No Man's Land integration plan

What ThirstWasTaken2 should do with [No Man's Land](https://modrinth.com/mod/no-mans-land) (mod id
`nomansland`, package `com.farcr.nomansland`), an Overworld overhaul: every vanilla biome redone, new
biomes (Bog, Bayou, Maple Forest, three new rivers), new mobs, tree tapping into cauldrons, and a set of
foods with a Farmer's Delight side. This file sets the order of work, what each step needs and how each
one is checked. Once the work is built, how it works goes where the steps below say.

Written on 2026-10-03 from:

- the repository [Monad-Modding/No-Mans-Land](https://github.com/Monad-Modding/No-Mans-Land), `main`
  at `e35f56a` (2026-10-02), which is `1.6.0-RC1`, one step ahead of the release;
- the Modrinth project `no-mans-land`, whose newest release is `1.5.12` (2026-07-02).

Ids were read from `main`. Step 1 checks each one against the released `1.5.12` jar, since an RC can add
items the release lacks.

## Which build for which node

**Only `1.21.1-neoforge`.** Every release, `1.0.0` to `1.5.12`, is NeoForge 1.21.1 and nothing else, and
the mod's FAQ says no other loader or version is planned. Fabric players are pointed at Kilt, which this
mod does not build or test against.

| Node | Build | Modrinth id | Requires on `runClient` |
|---|---|---|---|
| `1.21.1-neoforge` | `1.5.12` | `bcszfrVc` | Biolith `3.0.14` (`EAjbdreT`). Mixed Litter is embedded in the jar. NeoForge `21.1.207` or newer; the node has `21.1.252` |
| every other node | none | — | — |

## What No Man's Land already does for thirst

Nothing aimed at a thirst mod: its jar names none. It does tag three items `c:drinks` (`pear_juice`,
`pesto_bottle`, `maple_syrup_bottle`) and one `c:foods/soup` (`witch_stew`), so with
`enableDrinkTagMatching` on, which is the default, all three drinks already restore `drinkTagValue`
(6, 8) today, the pesto included.

## Where water lives in No Man's Land

| Block / path | Water in | Out | What happens to the grade today |
|---|---|---|---|
| **New biomes**, sampled by `WaterPurity.sampleAt` | world water | — | Bog, Bayou and Dark Swamp are in `c:is_swamp` but not in our `stagnant_water`, so their water grades like a forest's, cleaner than a vanilla swamp's. Blackwater River is in both `minecraft:is_river` and `c:is_swamp` and grades as a river. See steps 3 and 4 |
| **Mud Beach** (`mud_beach`), a sub-biome of `minecraft:beach` | the sea at its edge | — | **fresh.** It replaces part of a beach but is not in `minecraft:is_beach`, only `c:is_aquatic`, so sea water drawn there reads as fresh. Tropical Beach is in `is_beach` and reads salty. Frozen Shore replaces Stony Shore, which vanilla does not tag `is_beach` either. See step 4 |
| **Tap on a log or hive** (`TapBlock.tryFill`, data in `data/nomansland/nomansland/tapping/`) | none | honey, maple syrup or resin into an *empty* cauldron | no water. A tap fills only `minecraft:cauldron` or its own cauldron type, so it never touches a water cauldron or our `purity` blockstate |
| **Milk cauldron** (`MilkCauldron`, four levels, a milk bucket fills it) | milk | an empty hand sips: one milk-curable effect removed, a level down | **no thirst**: the sip goes through the block, not the item. See step 5 |
| Honey, maple syrup, resin oil cauldrons | — | bottles out | the bottles are items; the maple syrup one is valued in step 2 |
| **Awkward Residue** in a brewing stand (`addMix(Potions.WATER, AWKWARD_RESIDUE, Potions.AWKWARD)`) | any `Potions.WATER` bottle | Awkward Potion | the same path as Nether Wart. Our bottles are still `Potions.WATER` with a component on them, so every grade and sea water brew, as with Nether Wart |
| Create pump on its milk, honey and resin oil cauldrons (`VanillaFluidTargetsMixin`) | — | milk, honey, resin oil | no water |
| Deep Well, wells in Alchemist Ruins | world water | — | sampled where it lies, like any water |

## The drinks and foods

In `ThirstConfig` by id: a `noMansLandDrinks` / `noMansLandFoods` pair merged with `putMissing`, like the
Farmer's Delight addons. The Farmer's Delight items (`pear_juice`, `pesto_bottle`, `witch_stew` and the
plated meals) exist only when Farmer's Delight is loaded; an id that is absent matches nothing. Proposed
(thirst, quenched):

| Group | Items | Proposed |
|---|---|---|
| Juice | `pear_juice` (Farmer's Delight's `DrinkableItem`, Regeneration) | 8, 13, Farmer's Delight's juices |
| Syrup | `maple_syrup_bottle` (drunk, 40 ticks, honey sound, clears Hunger, Weakness, Slowness) | 4, 6, the honey bottle's. Today the tag gives it 6, 8 |
| Sauce | `pesto_bottle` (a sauce in a bottle, eaten as a drink) | 1, 2: oil and basil, wetter than nothing, drier than Farmer's Delight's tomato sauce (4, 5). Today the tag gives it 6, 8 |
| Stew | `witch_stew` | 4, 5, Farmer's Delight's stews |
| Fruit | `pear`, `syruped_pear`, `honeyed_apple` | 2, 3, the apple's |
| Left out | meats, fish, frog legs, `grilled_mushrooms`, `mashed_potatoes_with_mushrooms`, nuts, `trail_mix`, `hardtack`, `pancake`, the tarts, `pear_cobbler_slice`, `fruit_cake_slice`, `seared_venison`, `stallion_strip`, both pesto pastas, `awkward_residue`, `resin_oil_bottle` (fuel, Flammable) | — |

## Status

| # | Item | Kind | Nodes | Status |
|---|---|---|---|---|
| 1 | The mod on the `runClient` classpath | build | `1.21.1-neoforge` | done: `compileOnly`, and a `runClientMod` line off by default |
| 2 | Thirst values for the drinks, stew and fruit | data | all (config) | done |
| 3 | Bog, Bayou, Dark Swamp in `stagnant_water`; Blackwater River | decision, then data | all (data) | done: (a), Blackwater River in too |
| 4 | Sea water at the Mud Beach | decision, then code | all | done: (a), `thirstwastaken2:sea_water` |
| 5 | Sipping from a milk cauldron restores thirst | decision, then code | `1.21.1-neoforge` | done: (a), `src/main/nomansland` |
| 6 | What happens, per path, in game | investigation | `1.21.1-neoforge` | passed 2026-10-03: `tools/agent/integrations/no-mans-land.jsonl` (ids, drinks, sampling, milk cauldron). The tap, Awkward Residue and the dream were not scripted: none of them touches code of ours |
| 7 | Nothing crashes without the mod | test | `1.21.1-neoforge` | `checkOptionalSeam` and gametests pass; the default `runClient` has no No Man's Land |
| 8 | Changelog, player docs, store pages | docs | — | done |

Decided on 2026-10-03: the three recommendations, 3 (a), 4 (a) and 5 (a). Step 1 found Biolith 3.0.11
and Mixed Litter 1.2.2 nested in the `1.5.12` jar, so neither is a separate `runClient` line, and every
id in step 2 and biome in steps 3 and 4 in it. How it works now is in
[src/main/nomansland/AGENTS.md](../../../../src/main/nomansland/AGENTS.md).

## 1. The mod on the `runClient` classpath

`deps.nomansland` in `[neoforge."1.21.1"]` only, pinned by Modrinth id, with Biolith beside it, and both
in `MODRINTH_DEPS`. Farmer's Delight is already on the node's `runClient`, so its side of the mod loads
too. If step 5 is decided (a), a row in the integration table (`nomansland`, NeoForge only, its mixin
config); otherwise no row and no directory, only the `runClientMod` lines, as Candlelight has.
`-PwithoutOptional=nomansland` leaves it out.

No Man's Land has worldgen of its own, so the dev world in `run/1.21.1-neoforge/` must be a new one for
step 6; an old world keeps its old chunks.

Check here that every id in step 2 and every biome in steps 3 and 4 exists in `1.5.12`.

## 2. Thirst values

`noMansLandDrinks` and `noMansLandFoods` in `ThirstConfig`, merged with `putMissing`, and checked by a
gametest beside Beachparty's. The explicit entries win over the `c:drinks` tag, which is what takes the
syrup and pesto off 6, 8. The ids reach every node and match nothing where the mod is absent.

## 3. Decision: the swamp biomes

`WaterPurity.sampleAt` scores a biome in `stagnant_water` 85, one in `minecraft:is_river` 42, and checks
`stagnant_water` first. The entries go in `ThirstBiomeTagProvider` as optional ones, as Terralith's do,
so the tag loads without the mod on every node.

- **Bog, Bayou, Dark Swamp: in.** Each is a swamp in all but our tag. Not a real choice; listed so the
  table is whole.
- **Blackwater River**, which No Man's Land tags both river and swamp:
  - **(a) In `stagnant_water`.** Its water is the dark, slow swamp river its name says, and it spawns
    the swamp's Billhook Bass. Recommended.
  - (b) Left a river. It still flows, and Desert River and Lush River stay rivers either way.

## 4. Decision: sea water at the Mud Beach

Sea water is decided by `BiomeTags.IS_OCEAN` or `BiomeTags.IS_BEACH`. Adding Mud Beach to
`minecraft:is_beach` from our data is ruled out: vanilla places buried treasure by that tag, and other
mods read it, so a thirst mod would be changing their worldgen.

- **(a) A tag of our own, `thirstwastaken2:sea_water`,** read by `sampleAt` beside the two vanilla tags,
  with `nomansland:mud_beach` as an optional entry. A small core change, and the same tag lets any later
  coastal biome join without code. Recommended. Whether `minecraft:stony_shore` and
  `nomansland:frozen_shore` join it is a separate question: stony shore water reads fresh today, on every
  node, without No Man's Land, so changing it is a core change for every player and is left for its own
  discussion.
- (b) Nothing. Mud Beach water stays fresh, which a player standing on a beach drawing sea water will
  read as a bug.

## 5. Decision: sipping from a milk cauldron

A milk bucket restores 6, 8. Poured in, it is four levels, and each sip from an empty hand takes one.

- **(a) Each sip is a quarter of a milk bucket.** A mixin on `MilkCauldron.useWithoutItem`, where the
  level goes down, restores a quarter of `minecraft:milk_bucket`'s own values (rounded, at least 1), as
  Beachparty's cocktail sips take a third of the glass. Four sips are about one bucket, so the cauldron
  is a way to share milk, not to multiply it. Needs `src/main/nomansland`, NeoForge only, and a row in
  the integration table. Recommended if milk is to count.
- (b) Nothing. The milk cauldron is a cure station, and a bucket still drinks normally.

## 6. Investigation

A script `tools/agent/integrations/no-mans-land.jsonl`, written for the finished behaviour, on a new
world:

| Case | Expected |
|---|---|
| Every id in step 2, the milk cauldron, the tap | all exist |
| A pear juice, a maple syrup bottle, a pesto bottle from thirst 4 | 12, 8, 5 |
| Water sampled in a Bog, a Bayou, a Dark Swamp | the same grade odds as a vanilla swamp |
| Water sampled in a Blackwater River | per step 3 |
| Water sampled at a Mud Beach's sea edge, and at a Tropical Beach | per step 4; salty |
| A milk cauldron sipped four times from thirst 4 | per step 5 |
| A tap on a maple log over an empty cauldron, and over a water cauldron | syrup cauldron; the water cauldron untouched, its `purity` kept |
| Awkward Residue over a graded water bottle and a sea water bottle | an Awkward Potion from both, as with Nether Wart |
| Sleeping into a dream | the HUD hides with the rest of the GUI and comes back on waking; the bar is not drawn over the dream |

## 7. Optional seam

`tools/agent/smoke/boot.jsonl` comes up and stays up with `-PwithoutOptional=nomansland`, and with
Farmer's Delight left out while No Man's Land is loaded. If step 5 adds code, `checkOptionalSeam` and
the other checks pass on the node.

## 8. Docs

`CHANGELOG.md` (Unreleased), a site page for the mod, its sidebar entry, rows in
`docs/docs/installation.md`, the Modrinth and CurseForge tables, the root `AGENTS.md` (integration table
row if step 5 adds code). The page says other versions ignore No Man's Land, which has no build for them.

## Not planned

- **Fabric through Kilt, and any version but 1.21.1.** The mod has no build for them.
- **A water tap.** Tapping is data driven (`nomansland:tapping`), so a sap-water tap could be added, but
  the mod has none and inventing a water source is not this integration's job.
- **Refusing sea water for Awkward Residue.** Vanilla brewing takes sea water bottles with Nether Wart
  already; Awkward Residue is the same mix and stays consistent with it.
- **Thirst on the honey cauldron.** An empty hand takes a honey block out of it, nothing is drunk.
