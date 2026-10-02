# Cultural Delights integration plan

What ThirstWasTaken2 should do with [Cultural Delights](https://modrinth.com/mod/cultural-delights)
(mod id `culturaldelights`, package `com.baisylia.culturaldelights`), the Farmer's Delight addon of
foods from around the world, which since 0.18 also brews drinks in a vat. This file sets the order of
work, what each step needs and how each one is checked. Once the work is built, how it works goes where
the steps below say.

Written on 2026-09-26 from:

- the Modrinth project `YttyNOFA`: `0.18.1-1.21.1` for NeoForge (`jL5hP1qm`, 2026-09-14) and `0.17.7`
  for Fabric (`O8lDk3qa`, 2025-08-04), both jars read with `javap` and their data;
- the repository [Baisylia/Cultural-Delights-1.21](https://github.com/Baisylia/Cultural-Delights-1.21),
  `main` at `69838dc` (2026-05-22), which is **0.17.8**. The 0.18 source (the vat, the drinks, cheese,
  beans, sausages) is **not published**, so everything about the vat below is from the jar.

## Which build for which node

| Node | Build | Modrinth id | Requires on `runClient` |
|---|---|---|---|
| `1.21.1-neoforge` | `0.18.1-1.21.1` | `jL5hP1qm` | Farmer's Delight `[1.3.0,)` (we pin `1.21.1-1.3.4`), **Cook's Collection** `[0.5.0,)` (`z1YUDQXH`, 0.6.1, NeoForge only), NeoForge `[21.1.247,)` (we pin a later build). Supplementaries is optional |
| `1.21.1` (Fabric) | `0.17.7` | `O8lDk3qa` | Farmer's Delight Refabricated (`*`), Fabric API. No vat and no drinks: foods only |
| every other node | none | — | — |

The two loaders are **not the same mod**. The Fabric 0.17.7 is a separate port with the 0.17 item set;
the drinks, the vat and cheese exist on NeoForge only. Everything below that concerns drinks or the
vat applies to `1.21.1-neoforge` alone.

## Where water lives in Cultural Delights

No block holds water as a fluid. Water enters as a **water bucket item** matched by the tag
`c:buckets/water`, in three places:

| Path | Recipes | What happens to the grade |
|---|---|---|
| **Vat** (`VatBlockEntity`, an `ItemStackHandler` of ingredient slots, a container slot and an output; type `culturaldelights:aging`, split into fermenting, distilling when heated, chilling when cold) | beer, wine, glow wine, mead, apple cider, gin, lemon liqueur, vinegar, acid, tofu (fermenting); vodka, whiskey, rum, tequila, brandy (distilling); cola (chilling). The cocktails (mojito, margarita, bloody mary) start from a spirit, not water | the bucket is an ingredient; `craftItem` takes it out and drops `getCraftingRemainingItem`, an empty bucket. **The grade is dropped** and a sea water bucket brews like any other: our stamped buckets are still `minecraft:water_bucket`, which is in `c:buckets/water` |
| **Cooking pot** | `milk_from_beans`: water bucket + three beans → milk bucket | dropped; the pot is heated. Sea water makes milk too |
| **Shapeless crafting** | `corn_dough`: water bucket + three corn (both loaders; on Fabric under the `culturalrecipes` namespace) | dropped; the dough is cooked into tortillas afterwards |

There is no tank, so there is nothing to keep a grade in and no Jade line to add. The one question is
sea water (step 4).

## What Cultural Delights says about its drinks

The fifteen alcoholic drinks are tagged `c:drinks/alcohol`. Our `c:drinks` matching reads the tag
`c:drinks` itself; whether NeoForge 1.21.1's `c:drinks` includes `#c:drinks/alcohol` decides whether
they already restore `drinkTagValue` (6, 8). Step 3 checks. `cola`, `ginger_beer`, `butterbeer`,
`acid` and `vinegar` are in no drinks tag. All drinks are Farmer's Delight `DrinkableItem`s in a glass
bottle, one drink per bottle.

## The drinks and foods

In `ThirstConfig` by id: a `culturalDelightsDrinks` / `culturalDelightsFoods` pair merged with
`putMissing`, as for Kaleidoscope Cookery and Brewin' and Chewin'. The same id on both loaders is the
same item, so one list serves both; an id the Fabric port lacks matches nothing. Proposed (thirst,
quenched), on Brewin' and Chewin's principle that a drink restores less the stronger it is, and
spirits nothing:

| Group | Items | Proposed |
|---|---|---|
| Soft | `cola` | 6, 8, as a bottle of water |
| | `ginger_beer`, `butterbeer` | 5, 6 |
| Light alcohol | `beer`, `mead`, `apple_cider` | 5, 6, as Brewin' and Chewin's beer and mead |
| | `bloody_mary` | 4, 5, as Brewin' and Chewin's |
| | `mojito`, `margarita` (ice, lime) | 4, 5 |
| Wine | `wine`, `glow_wine` | 3, 4, as Brewin' and Chewin's wines |
| Strong | `lemon_liqueur` | 2, 2 |
| Spirits | `tequila`, `gin`, `brandy`, `vodka`, `whiskey`, `rum` | 0, 0: listed as zero rather than left out, so the `c:drinks/alcohol` tag cannot give them 6 if `c:drinks` includes it |
| Not drinks | `acid`, `vinegar` | 0, 0, for the same reason if a pack tags them |
| Watery food | `cucumber` | 3, 4 |
| | `cut_cucumber` | 1, 2 |
| | `hearty_salad` | 4, 5, as Farmer's Delight's mixed salad |
| | `creamed_corn`, `poached_eggplants` | 2, 3 |
| Left out | `pickle`, `cut_pickle`, `pickled_egg` (salty), the curries, rolls, tacos, wraps, breads, cheese, sausages and the other dry foods | — |

`culturaldelights:apple_cider` is not Farmer's Delight's `apple_cider` (8, 13): it is alcoholic and
brewed in the vat, hence the lower value.

## Status

**Scope, decided 2026-09-26: NeoForge only.** The Fabric port has not been updated since 0.17.7 and has
no vat and no drinks, so no Fabric node builds or runs it. The ids in `ThirstConfig` are common code
and still match the port's foods where a player installs it; nothing else reaches Fabric.

| # | Item | Kind | Nodes | Status |
|---|---|---|---|---|
| 1 | Build dependency and gate | build | `1.21.1-neoforge` | **Done** (2026-09-26) |
| 2 | Thirst values for the drinks and foods | data | all (config) | **Done** (2026-09-26), values as proposed |
| 3 | What the vat does with graded and sea water | investigation | `1.21.1-neoforge` | **Done** (2026-09-26), by the agent script of step 4 |
| 4 | Sea water in the vat | decision | `1.21.1-neoforge` | **Decided** (2026-09-26): (b), as recommended, and **done** |
| 5 | Changelog and player docs | docs | — | **Done** (2026-09-26) |
| 6 | Nothing crashes without the mod | test | `1.21.1-neoforge` | **Done** (2026-09-26) |

## 1. Build dependency and gate (done)

- A row in [the integration table](../../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt):
  `dir = "culturaldelights"`, `depsKey = "deps.cultural_delights"`, NeoForge only, mixin config
  `thirstwastaken2.culturaldelights.mixins.json`, `neoForgeDependencies = listOf("culturaldelights")`.
- `deps.cultural_delights = "jL5hP1qm"` and `deps.cooks_collection = "z1YUDQXH"` in
  `[neoforge."1.21.1"]`, both in `MODRINTH_DEPS` pinned by id. No Fabric table sets either.
- `compileOnly` and two `runClientMod` lines in `build.neoforge.gradle.kts`.
- `CulturalDelightsPresence` and `CulturalDelightsMixinPlugin`, as in
  [src/main/culturaldelights/AGENTS.md](../../../../src/main/culturaldelights/AGENTS.md). The gate looks
  for the vat's class in the mod's jar, so a 0.17 under the same id turns the integration off.

## 2. Thirst values (done)

`culturalDelightsDrinks` and `culturalDelightsFoods` in `ThirstConfig`, the table above as proposed,
merged into an older config with `putMissing`. The spirits, acid and vinegar are listed as zero, which
the lookup treats as "restores nothing" ahead of any tag. `culturalDelightsDrinksAreMergedIntoAnOlderConfig`
checks the defaults, the zeros, that pickles are left out, and that a merge keeps a player's value.

## 3. and 4. Sea water in the vat (done)

Chosen: (b). `VatSeaWaterMixin` makes the vat's private static `hasRecipe(VatBlockEntity)` answer false
at its head while any slot holds a salty stack. The tick resets the progress on false, so a vat already
brewing stops. `VatWater` reads the vat as a vanilla `Container`, so it names nothing of the mod's.
`milk_from_beans` and `corn_dough` are left alone.

**Checked** with [tools/agent/integrations/cultural-delights.jsonl](../../../../tools/agent/integrations/cultural-delights.jsonl)
on `1.21.1-neoforge`: beer brews from a plain and from a Dirty bucket, with no grade on the beer;
nothing brews from a sea water bucket, which stays in its slot; swapped for a plain bucket, the same vat
brews.

## 5. Docs (done)

`CHANGELOG.md` (Unreleased), [the site's page](../../../docs/integrations/farmers-delight/cultural-delights.md) and its
sidebar entry, the NeoForge row in `docs/docs/installation.md`, and a line under the Modrinth and
CurseForge compatibility tables. A picture of the Vat in a village was taken and later dropped as
showing only the mod's own blocks; the site's page opens with Cultural Delights' own banner, linked
straight from its Modrinth page (`cdn.modrinth.com`) rather than copied into the repository.

## 6. Optional seam (done)

`checkOptionalSeam` and `checkLoaderSeam` pass on `1.21.1-neoforge`, and `runGametest` passes (182).
`tools/agent/smoke/boot.jsonl` with `-PwithoutOptional=culturaldelights` comes up and stays up.

## Not planned

- **Intoxication and thirst.** Cultural Delights' own `IntoxicationEffect` already costs the player;
  as with Brewin' and Chewin', the low values of step 2 are the cost we add.
- **Cook's Collection's own items.** A required dependency of 0.18, with its own foods; a separate plan
  if it has drinks worth a value.
- **Supplementaries compat of the mod** (`SupplementariesCompat`): nothing to do with water.
- **The Fabric port (0.17.7).** Not updated since 0.17, with no vat and no drinks.
- **Other Minecraft versions**, until the mod publishes one.
