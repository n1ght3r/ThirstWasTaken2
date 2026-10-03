# Croptopia integration plan

What ThirstWasTaken2 should do with [Croptopia](https://www.curseforge.com/minecraft/mc-mods/croptopia)
(mod id `croptopia`, package `com.epherical.croptopia`), a farming mod: about sixty crops, thirty fruit
trees, and over two hundred foods and drinks made from them in the crafting grid with its Cooking Pot,
Frying Pan, Food Press, Mortar and Pestle and Knife, all of which are crafting tools, not blocks. This
file sets the order of work, what each step needs and how each one is checked. Once the work is built,
how it works goes in [src/main/croptopia/AGENTS.md](../../../../src/main/croptopia/AGENTS.md).

Written on 2026-10-03 from:

- the repository [ExcessiveAmountsOfZombies/Croptopia](https://github.com/ExcessiveAmountsOfZombies/Croptopia),
  branches `v4-1.21.1` (4.2.4), `v4` (4.3.2, 26.1.2) and `v4-26.2` (4.3.2);
- the released jars of every build below, the 1.20.1 ones included, which have no branch on GitHub;
- the CurseForge project `croptopia` (415438). **Croptopia is not on Modrinth.**

## Which build for which node

| Node | Build | CurseForge file id | Requires on `runClient` |
|---|---|---|---|
| `26.2.x` | `croptopia-fabric-26.2-4.3.2` | `8908617` | EpheroLib `epherolib-fabric-26.2-1.3.0` (`8544496`) |
| `26.2.x-neoforge` | `croptopia-neoforge-26.2-4.3.2` | `8908615` | EpheroLib `epherolib-neoforge-26.2-1.3.0` (`8544497`) |
| `26.1.x` | `croptopia-fabric-26.1.2-4.3.2` | `8908639` | EpheroLib `epherolib-fabric-26.1-1.3.0` (`7937978`) |
| `26.1.x-neoforge` | `croptopia-neoforge-26.1.2-4.3.2` | `8908643` | EpheroLib `epherolib-neoforge-26.1-1.3.0` (`7937982`) |
| `1.21.1` | `croptopia-fabric-1.21.1-4.2.4` | `7958878` | EpheroLib `EpheroLib-FABRIC-1.2.0-1.21.1` (`6379763`) |
| `1.21.1-neoforge` | `croptopia-neoforge-1.21.1-4.2.4` | `7958876` | EpheroLib `EpheroLib-1.21.1-NEO-FORGE-1.2.0` (`5872111`) |
| `1.20.1` | `Croptopia-1.20.1-FABRIC-4.0.1` | `8066551` | EpheroLib `EpheroLib-1.20.1-FABRIC-1.2.0` (`4949797`) |
| `1.20.1-forge` | `Croptopia-1.20.1-FORGE-4.0.1` | `8066550` | EpheroLib `EpheroLib-1.20.1-FORGE-1.2.0` (`4889101`) |
| `26.3.x`, `1.21.11` and their NeoForge nodes | none | — | — |

The 26.1.2 build needs Minecraft 26.1.2 exactly, which is what the `26.1.x` nodes compile and run
against. Patchouli is optional on NeoForge and is not pinned.

## What Croptopia already does for thirst

Nothing. Its jars name no thirst mod, and it tags no item `c:drinks`, so no drink of it restores thirst
here without values of our own. Its item ids are the same in every build above; 26.1 and 26.2 add
dishes (kimchi, the pickles, bibimbap, `bibim_naengmyeon`) but no drink.

## Its drinks and watery food

Every drink is `croptopia.items.Drink`, a plain `Item` with food and the drink animation; most hand back
a glass bottle.

| Kind | Ids | Proposed thirst, quenched | Like |
|---|---|---|---|
| Juice | `apple_juice`, `cranberry_juice`, `grape_juice`, `melon_juice`, `orange_juice`, `pineapple_juice`, `saguaro_juice`, `tomato_juice`, `lemonade`, `limeade` | 8, 13 | Farmer's Delight's juice |
| Smoothie | `banana_smoothie`, `strawberry_smoothie`, `fruit_smoothie`, `kale_smoothie` | 7, 10 | Beachparty's cocktails |
| Milkshake | `chocolate_milkshake` | 8, 12 | Fruits Delight's milkshake |
| Tea | `tea` | 6, 9 | a tea cup |
| Coffee | `coffee` | 5, 8 | HerbalBrews' and Rustic Delight's coffee |
| Latte | `pumpkin_spice_latte` | 6, 10 | milk coffee |
| Milk | `soy_milk`, `horchata` | 6, 8 | a bottle of milk |
| Beer, mead | `beer`, `mead` | 5, 6 | Brewin' and Chewin's |
| Wine | `wine` | 3, 4 | Brewin' and Chewin's |
| Spirit | `rum` | 2, 2 | Brewin' and Chewin's saccharine rum |
| Soup, stew, salad | `leek_soup`, `pumpkin_soup`, `potato_soup`, `beef_stew`, `nether_wart_stew`, `borscht`, `goulash`, `chicken_and_dumplings`, `tofu_and_dumplings`, `chicken_and_noodles`, `bibim_naengmyeon`, and the cucumber, caesar, leafy, veggie and beetroot salads | 4, 5 | Farmer's Delight's stews and mixed salad |
| Fruit salad | `fruit_salad` | 6, 8 | Farmer's Delight's |
| Sorbet | `kiwi_sorbet` | 7, 9 | the melon popsicle |
| Ice cream, yoghurt | six ice creams, `yoghurt` | 2, 3 | glow berry custard |
| Melons | `cantaloupe`, `honeydew` | 4, 5 | a melon slice |
| Cucumber | `cucumber` | 3, 4 | Cultural Delights' |
| Tomato, tree fruit | `tomato`, `orange`, `grapefruit`, `peach`, `pear`, `plum`, `nectarine`, `apricot`, `mango`, `pineapple`, `kiwi`, `starfruit`, `dragonfruit`, `persimmon` | 2, 3 | an apple |
| Berries, sour fruit, leaves | `grape`, `strawberry`, `blueberry`, `blackberry`, `raspberry`, `cranberry`, `currant`, `elderberry`, `cherry`, `kumquat`, `lemon`, `lime`, `lettuce`, `celery` | 1, 2 | a sweet berry, a cabbage leaf |

Left out: `water_bottle` and `milk_bottle`, which are ingredients that cannot be drunk; the pickles and
kimchi, which are salty; jams, pies, breads and everything fried, roasted or baked.

## Where water lives in Croptopia

No block of Croptopia holds water. Water enters only through crafting recipes, all of them
`minecraft:crafting_shaped` or `crafting_shapeless`:

| Recipe | Water in | Out | What happens today |
|---|---|---|---|
| `shaped_water_bottle` | `minecraft:water_bucket`, matched by item | **16** `croptopia:water_bottle`, a plain item with no grade, and the empty bucket | **a sea water bucket makes sixteen ordinary water bottles**, and from them anything below |
| `shaped_tea` | `#c:water_bottles` | `croptopia:tea` | the tag holds `croptopia:water_bottle` **and `minecraft:water_bucket`**, so a sea water bucket brews tea |
| `steamed_rice`, `shaped_potato_soup`, `shaped_steamed_broccoli`, `shaped_steamed_green_beans`, `dough`, `noodle`, `tofu`, `soy_sauce`, `horchata`, `mead`, `rum`, the two puddings | `#c:water_bottles` | food and drinks | the same |
| `salt_from_water_bottle`, `salt_from_smoking_water_bottle` | `croptopia:water_bottle` in a furnace or smoker | `croptopia:salt` | nothing of ours cooks that bottle, so no clash. Salt from fresh water is odd but harmless, and salt also comes from salt ore and Mountain Salt |

The grade of a fresh bucket is lost in every one of these, as in every crafting recipe of every mod
that takes water: a crafting recipe cannot see components on a `#c:water_bottles` match.

## Status

| # | Item | Kind | Nodes | Status |
|---|---|---|---|---|
| 1 | The mod on the `runClient` classpath | build | the eight above | **Done** (2026-10-03) |
| 2 | Thirst values for its drinks and food | data | every node (ids) | **Done** (2026-10-03) |
| 3 | Sea water in its crafting recipes | decision, then code | the eight above | **Decided** (2026-10-03): (a), and **done** |
| 4 | What happens, per path, in game | investigation | 1.21.1 and later | **Done** on `1.21.1` (2026-10-03), every case as expected; the other nodes and the 1.20.1 crafting cases by hand are open |
| 5 | Nothing crashes without the mod | test | the eight above | `checkOptionalSeam` and `checkVersionSeam` pass on all eight, `runGametest` on `1.21.1` (2026-10-03); the boot smoke is open |
| 6 | Changelog, player docs, store pages | docs | — | **Done** (2026-10-03) |

## 1. The mod on the `runClient` classpath

`deps.croptopia` and `deps.epherolib` in the eight loader tables of `stonecutter.properties.toml`,
pinned by CurseForge file id, and resolved from [CurseMaven](https://cursemaven.com) as
`curse.maven:croptopia-415438:<id>` and `curse.maven:epherolib-885449:<id>`, a repository each loader
script declares for the group `curse.maven` alone. `update_mc_deps.py` reads Modrinth and cannot see
them, so a bump is by hand. A row in the integration table (`croptopia`, all three loaders, its mixin
config, `croptopia` as an optional dependency). Nothing is compiled against Croptopia. Its
`runClientMod` lines are commented out by default, like Vinery's: dozens of crops and fruit trees in
worldgen slow every client start. `-PwithoutOptional=croptopia` leaves it and EpheroLib out.

## 2. Thirst values

`croptopiaDrinks` and `croptopiaFoods` in `ThirstConfig`, merged with `putMissing` like every other
mod's, with the values in the table above. Ids alone, so they reach every node, even 26.3 and 1.21.11
should Croptopia ever build there, and match nothing where the mod is absent. Gametest
`croptopiaDrinksAreMergedIntoAnOlderConfig`.

## 3. Decision: sea water in its crafting recipes

- **(a) Refuse sea water.** A mixin on `ShapedRecipe.matches` and `ShapelessRecipe.matches`: a recipe
  whose result is an item of Croptopia's does not match a grid holding sea water
  (`WaterPurity.isSalty`). Fresh water of any grade still crafts, since what a recipe makes is its own
  item, as tea is. The same rule as Farm & Charm's recipes and Fruits Delight's water bottle
  ingredient. Recommended.
- (b) Nothing: a sea water bucket keeps making sixteen water bottles and tea.

Decided (a). Matched by result, not by recipe id, since `matches` does not know its id: that also
covers a data pack's recipe for a Croptopia item, and touches no other mod's recipe. Whether a recipe
makes a Croptopia item is worked out on its first match and kept in a field of the recipe. The result is
an `ItemStackTemplate` from 26.1 and an `ItemStack` before, and `matches` takes a `CraftingInput` from
1.21 and a `CraftingContainer` on 1.20.1: both differences live in
`com.thirstwastaken2.croptopia.platform.CroptopiaVanilla`.

Ruled out: replacing `shaped_water_bottle.json` from our jar with a component-aware ingredient. On
Fabric 1.21.1 nothing orders one mod's data over another's (see Spelunkery's plan), and on 1.20.1 there
are no components to ask for.

Not planned: carrying the bucket's grade into Croptopia's water bottle, so that salt could come from sea
water alone. The bottle is a plain item that nothing drinks; grading it would mean a crafting result
hook on four versions for a recipe whose only use of the grade would be salt.

## 4. Investigation

A script `tools/agent/integrations/croptopia.jsonl`, written for the finished behaviour. Crafting goes
through a Crafter, which asks the same `matches` a crafting table does, so the script runs on 1.21.1
and later; on `1.20.1` and `1.20.1-forge` the crafting cases are checked by hand at a crafting table.

| Case | Expected |
|---|---|
| The ids the script uses exist | all exist |
| A Clean and a Dirty fresh bucket with four glass | sixteen water bottles each |
| A sea water bucket with four glass | nothing; the Crafter keeps its grid |
| Tea leaves over a sea water bucket | nothing |
| Tea leaves over Croptopia's water bottle | tea |
| Orange juice, tea, wine, leek soup, a cucumber, from thirst 4 | 12/12, 10/9, 7/4, 8/5, 7/4 |

Run on `1.21.1` on 2026-10-03, every case as expected: the Clean and the Dirty bucket each crafted
sixteen water bottles, the sea water bucket and the sea water tea crafted nothing, the water bottle tea
crafted, and every drink and food came out at the numbers above. The soup is not always edible, so the
script's Hunger lasts long enough to empty a full food bar first.

## 5. Optional seam

`tools/agent/smoke/boot.jsonl` comes up and stays up with `-PwithoutOptional=croptopia`.
`checkOptionalSeam` and `checkVersionSeam` pass on every node that builds the directory.

## 6. Docs

`CHANGELOG.md` (Unreleased), a site page `docs/docs/integrations/croptopia.md`, its sidebar entry and
`mods.ts` row, rows in `docs/docs/installation.md`, the Modrinth and CurseForge pages, the root
`AGENTS.md`, `SALT-WATER-REFUSALS.md` and `tools/agent/AGENTS.md`.

## Not planned

- **26.3 and 1.21.11.** Croptopia has no build for them. The thirst values reach them anyway, as ids.
- **Its Cooking Pot and other utensils.** They are crafting tools that stay in the grid, not blocks, so
  nothing of them holds or heats water.
