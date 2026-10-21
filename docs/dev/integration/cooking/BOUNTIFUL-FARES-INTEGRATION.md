# Bountiful Fares integration plan

What ThirstWasTaken2 should do with [Bountiful Fares](https://modrinth.com/mod/bountiful-fares), the
vanilla-style food mod by Heccology: ceramic cups that scoop water like a glass bottle, teas brewed from
a cup of water, ciders, wines and mead from a Fermentation Vessel, and stews, medleys and fruit. This
file sets the order of work, what each step needs and how each one is checked. Once the work is built,
how it works goes in [src/main/bountifulfares/AGENTS.md](../../../../src/main/bountifulfares/AGENTS.md).

Written on 2026-10-21 from:

- the repository [Heccology/Bountiful-Fares](https://github.com/Heccology/Bountiful-Fares), default
  branch `1.21-ML` (the released 3.x, one Architectury-style common module for both loaders);
- the Modrinth project `bountiful-fares`, whose newest uploads are `3.0.13` for Fabric (`7SStqCZU`) and
  NeoForge (`w8uM2PS4`), 1.21.1, 2026-09-18, and the jars of both, read with `javap`.

## Which build for which node

Only builds whose Minecraft version is still updated are integrated.

| Node | Build | Modrinth id | Requires on `runClient` |
|---|---|---|---|
| `1.21.1` | `3.0.13` (Fabric) | `7SStqCZU` | NexusLib 1.2.0 (`15AB8yjE`), Cloth Config, Fabric API |
| `1.21.1-neoforge` | `3.0.13` (NeoForge) | `w8uM2PS4` | NexusLib 1.2.0 (`KZh9Z9Sd`) |
| every other node | none | — | — |

- **1.21.1 is the only line still updated**: 3.0.0 to 3.0.13 between 2025-09 and 2026-09, on both loaders.
- 1.20.1, 1.20.2 and 1.20.4 (Fabric only) stopped at 1.2.1 on 2024-06-01, and their branches are marked
  deprecated. Not integrated, as Farm & Charm's dead 1.20.1 line is not.
- There is no build for 1.21.11 or 26.x.
- One mod id, `bountifulfares`, and one package, `net.hecco.bountifulfares`, on both loaders. Its Fabric
  and NeoForge uploads share a version number, so both are pinned by Modrinth id, and so is NexusLib.

## What Bountiful Fares does with water

| Thing | What it is | Matters for thirst |
|---|---|---|
| **Cup** (`bountifulfares:jar`) | `CupItem`: an empty ceramic cup, a glass bottle. `use` casts `getPlayerPOVHitResult(SOURCE_ONLY)` and, on any water, trades itself for a water cup through `turnCupIntoItem` → `ItemUtils.createFilledResult`. In vanilla's `WATER` cauldron map, drawing one level | yes: it draws water |
| **Water Cup** (`water_cup`) | `WaterCupItem`: drunk like a water bottle and hands back the cup; on dirt makes mud. In the `EMPTY` and `WATER` cauldron maps, pouring one level | yes: it carries water and is drunk |
| A dispenser with cups | fills a cup from the water in front of it, as vanilla's does with bottles | the same hole as vanilla's bottle dispenser, which core does not stamp either |
| **Fermentation Vessel** | a block: right-click a `bountifulfares:fermentation_water_sources` item (water bottle, water bucket, water cup, Miner's Delight's water cup) into it while `EMPTY` and it becomes `WATER`; then a fruit or honey bottle ferments into a cider, wine or mead. The water is a blockstate, not a stack | yes: it takes water |
| Teas (6) | `*_tea_bottle`, named "Tea Cup": shapeless crafting, tea leaves or a flower plus a water cup. Drunk, clear an effect | drinks; the recipe takes water |
| Ciders (3), wines (2), mead, maple mead | fermented in the vessel | drinks |
| Coconut milk bottle | fermented from a coconut half; tagged `c:foods/milk` | a drink |
| Compotes (5) | `EdibleJarItem`: stewed fruit eaten from a cup | foods |
| Stews (7), medleys (7), fruit | bowl foods and raw fruit | foods |
| Sponge Cake | shaped crafting with three water buckets | takes water |

## The holes

| Path | Today | Why |
|---|---|---|
| Cup scoops a sea or swamp source | Water Cup with no grade, read as `defaultPurity` (Clean) | `CupItem.use` is its own, not our `BottleItemMixin` |
| Water cup poured into a cauldron | cauldron unset, Clean | the water cup is not a water container to `transferCauldronPurity` |
| Cup drawn from a graded cauldron | ungraded water cup | draining stamps only a container `WaterPurity` knows |
| Drinking a water cup | restores nothing, no sickness, drinkable at a full bar | no thirst value, not plain water |
| Sea water cup or bottle into the Fermentation Vessel | ferments cider from sea water | the vessel reads only the item tag |
| Sea water cup crafted into a tea, sea water buckets into a Sponge Cake | a fresh drink or cake | crafting matches the item alone |

Together: **a cup turns sea water into Clean water in two clicks** (scoop, pour into a cauldron, bottle
it), and every drink of the mod restores nothing.

## Decisions

1. **The water cup is a water bottle in a cup.** A water container, plain water, one serving, graded
   like the bottle, swapping with the empty cup (`WaterPurity.resolve`, by id under `bountifulfares`). It
   then takes part everywhere a container does with no further code: tooltips, sickness, the full-bar rule,
   cauldrons, the hanging pot and the distiller (both through `WaterPurity.Vessel`). Its thirst value is
   `plainWaterValue`; an entry under `drinks` is needed only so `thirstValues` resolves the item at all,
   as `minecraft:potion` has one.
2. **The empty cup draws from a full cauldron**, as the glass bottle does (`drawsWater`).
3. **A cup scooping a source is sampled there**, with core's `FillCapture`: one mixin on `CupItem.use`
   that captures the ray and stamps the third argument of `turnCupIntoItem`. Compiled against the mod,
   so it names `CupItem`; gated by a mixin plugin that reads the class file without loading it, as Miner's
   Delight's does.
4. **The Fermentation Vessel refuses sea water.** A mixin at the head of `useItemOn` spends the click when
   the vessel is `EMPTY` and the held stack is salty. Its grade does not matter otherwise: a fermented
   drink is safe whatever water went in, as in Brewin' and Chewin's keg. The vessel stores no stack, so
   there is nothing to stamp.
5. **Sea water crafts nothing of the mod's**, as with Croptopia: two mixins on `ShapedRecipe.matches` and
   `ShapelessRecipe.matches` return false when the recipe makes a `bountifulfares` item and the grid holds a
   salty stack. Grades below salt are not refused: tea is a drink with a value of its own, and Croptopia's
   tea is not refused for Dirty water either.
6. **The dispenser stays as vanilla's is.** Core does not stamp a bottle a dispenser fills; a cup filled
   there is unstamped the same way. Closing that is a core change for both, not this integration.
7. **Values**, by id in `config/itemvalues/OtherModItemValues`, on the scales already used:
   - teas 6, 4 (every other mod's tea); ciders 6, 4 (Farmer's Delight's apple cider);
   - mead and maple mead 5, 2, wines 3, 1 (Brewin' and Chewin's); coconut milk 4, 0 (milk);
   - compotes 3, 2 (half a bowl, Miner's Delight's cups);
   - stews 6, 4 (every stew); medleys and the sea salad 2, 0 (Farmer's Delight's mixed salad);
   - oranges, lemons, plums, hoary apples, passion fruit and spongekin slices 2, 0 (an apple, a melon
     slice); elderberries and lapisberries 1, 0 (a sweet berry);
   - left out: candies, breads, cookies, stuffed potatoes, the chows, pickles, foul flesh, citrus essence.
     The maple mead is registered as `nomansland:maple_mead_bottle` and only exists with No Man's Land.

## Steps

| Step | Where | Checked by |
|---|---|---|
| 1. Deps: `deps.bountiful_fares` and `deps.nexuslib` on both 1.21.1 tables, `MODRINTH_DEPS`, `compileOnly` and `runClientMod` lines | `stonecutter.properties.toml`, `build.gradle.kts`, `build.neoforge.gradle.kts`, `update_mc_deps.py` | `./gradlew ":1.21.1:build"`, `":1.21.1-neoforge:build"` |
| 2. A row in the integration table | `Integrations.kt` | `./gradlew -p build-logic test` |
| 3. Water cup and cup in `WaterPurity.resolve` | core | the agent script below |
| 4. Values and their merge into an older config | `ThirstConfig`, a gametest in `ThirstApiGameTest` | `runGametest -Ptests=ThirstApiGameTest` on `1.21.1` |
| 5. `CupItemMixin`, `FermentationVesselBlockMixin`, the two recipe mixins, the plugin and its gate | `src/main/bountifulfares` | `checkOptionalSeam`, `runServer`, the agent script |
| 6. In game: scoop sea water, pour it into a cauldron, try the vessel and a tea with it; scoop river water and drink it | `tools/agent/integrations/bountiful-fares.jsonl` | `runClient -Pagent=` on both nodes |
| 7. Docs: changelog, installation tables, an integration page, the store pages' compatibility list | `docs/`, `CHANGELOG.md` | `write-docs` skill |

## What was found in game

Checked on 2026-10-21 with `tools/agent/integrations/bountiful-fares.jsonl`, on `1.21.1` (Fabric) and
`1.21.1-neoforge`, Bountiful Fares 3.0.13 and NexusLib 1.2.0. Every check passed on both:

- Every id `ThirstConfig` lists exists (`/give` takes all 42, the cup included).
- A cup scooping a swamp source comes out Dirty, one scooping an ocean source salty.
- A salty water cup poured into an empty cauldron leaves sea water (`purity=5`) and hands back the cup;
  a cup drawn from a full Dirty cauldron comes out Dirty and the cauldron drops to level 2.
- A Dirty water cup fills an empty hanging pot by one serving and draws it back out Dirty.
- An empty Fermentation Vessel refuses a salty water cup, which stays in the hand, and takes a fresh one
  (`fermentation_stage=water`).
- A Crafter makes green tea from tea leaves and a Dirty water cup, and nothing from a salty one.
- A Pure water cup and a green tea each restore 6 and 4 from thirst 4, quenched 0. The water cup's
  tooltip shows Pure and the plain water droplets.

Without the mod, `runGametest -Ptests=ThirstApiGameTest` on `1.21.1` passed all 29, the new merge test
included, and `checkOptionalSeam` passed on both nodes.
