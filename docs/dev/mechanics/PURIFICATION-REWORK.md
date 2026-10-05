# Purification rework: boiling stops at Clear, only distilling makes Pure

**Status: planned, not started.** [WATER-REFERENCE.md](WATER-REFERENCE.md) describes the game today;
this plan changes it. Grade names below are the new ones: today's Clean is Clear here.

## Goal

Each step of progression does one thing of its own:

| Step | How | Gives |
|---|---|---|
| 1 | No metal: water as found, rain, loot, a cleric's trade | whatever it is |
| 2 | Boiling: Copper Canteen or Iron Flask held on a campfire, Copper and Iron Hanging Pots | Clear |
| 3 | Distilling: the Copper Distiller, any water, sea water included | Pure |

Today a furnace (eight cobblestone) already makes Pure, so copper and iron unlock nothing and the
distiller is only for sea water.

## Names and colours

Grades: **Dirty → Murky → Clear → Pure**. Only grade 2 changes, Clean to Clear: boiled water is free of
germs but not of what is dissolved in it, and still carries a small risk, so the name says how it
looks, not that it is safe. The scale's name, wherever a player reads it, is **water quality**, not
water purity (it also covers salt water). On the site, Pure is the game's name for distilled water,
not a claim that it is healthier than other safe water.

Colours read brighter and more vivid the safer the water, warm to cool, salt off the ramp:

| Grade | Tooltip | Sprite | Reads as |
|---|---|---|---|
| Dirty | `0xB0632E` | `0x5E3E20` | mud brown |
| Murky | `0xBDB878` | `0x808C4C` | olive: silt and algae |
| Clear | `0x8FA6B4` | `0x3F76E4` | tooltip a greyed blue; sprite vanilla's water blue |
| Pure | `0x4FD6FF` | `0x3FB4E8` | the brightest of the scale |
| Salt | `0xE6DFC8` | `0x25817A` | unchanged |

- Clear and Pure must be easy to tell apart, colour-blind players included: these give a ΔE of about
  28 (22 in a red-green simulation), and every tooltip colour keeps 4.5:1 contrast.
- The tooltip is `WaterPurity.purityColor`. The sprites are the four `terracotta_water_bowl_purity_*`
  and the four animated `copper_hanging_pot_water_purity_*` textures: recolour the water, keep the
  shading. Anything that later shows water by grade uses the sprite column.
- Tune the hex in game before settling.

**Renamed** (text only):

| What | From | To |
|---|---|---|
| Grade lang keys | `thirst.purity.dirty`, `.slightly_dirty`, `.acceptable`, `.purified` | `thirst.water.dirty`, `.murky`, `.clear`, `.pure` |
| Quenched config lang key | `quenched_percent_clean` | `quenched_percent_clear` |
| Config lang keys | `default_purity`, `rainwater_purity`, `dripstone_purity` | `*_quality`, values "… Quality" |
| Sophisticated button | `upgrades.buttons.min_purity` | `min_quality`, "Drinks %s water or better" |
| Sickness key in `thirstwastaken2.json` | `clean` | `clear`, migrated |
| Values only | "Water Purity" (Jade), "The water is boiled clean" | "Water Quality", "The water has boiled" |

**Kept**, because worlds, configs, other mods or the API store them: the `water_purity` and
`water_salty` components, the `purity` blockstate, the `purified_water` advancement, the Jade plugin id,
`drink_min_purity`, the `pure_water` tag, config field names, `ThirstApi` and `WaterPurity`.

**Migration:** in `sanitizeSickness`, a difficulty's table with `clean` and no `clear` is old; move
`clean` to `clear`. The new names never write `clean`, so no version field is needed.

## Real-world basis

Player-facing text about treating water stays within this:

| Real world | Source | In game |
|---|---|---|
| Boiling kills bacteria, viruses and parasites (1 minute at a rolling boil) | CDC | boiling gives Clear |
| Boiling leaves chemicals, heavy metals and salt, more concentrated as water boils off; algal toxins survive it | CDC, EPA | boiling stops at Clear, sea water stays salty |
| Distillation removes germs and most chemicals, not some volatile organics | CDC | the distiller gives Pure from any water |

The game simplifies one thing: Clear's small, immediate sickness risk stands in for harm that is really
long-term. The site says so in a line.

Sources: [CDC, water in an emergency](https://www.cdc.gov/water-emergency/about/index.html),
[EPA, emergency disinfection](https://www.epa.gov/ground-water-and-drinking-water/emergency-disinfection-drinking-water),
[CDC, home water treatment](https://www.cdc.gov/drinking-water/about/about-home-water-treatment-systems.html),
[CDC, backcountry treatment](https://stacks.cdc.gov/view/cdc/12378),
[EPA, cyanotoxins](https://www.epa.gov/sites/default/files/2017-06/documents/cyanotoxin-management-drinking-water.pdf).

## Rules

- **Heat sets water to Clear**, whatever heats it: a held vessel, a hanging pot, Farmer's Delight's
  Cooking Pot, the furnace and smoker (only with the switch). Clear and Pure water is not boiled, and
  nothing lowers a grade.
- **The distiller sets any water to Pure**, sea water included, and is the only thing that makes sea
  water drinkable.
- **No furnace or smoker water recipes** by default; `enableFurnaceBoiling` brings them back, to Clear.
- **Campfire slots never boil water**, switch or not.
- **Waterskins cannot be boiled** (the mod's and Cold Sweat's), switch or not.

Every treatment after the rework (today's are in WATER-REFERENCE.md, "Input to output, by grade"):

| Method | Rule | Dirty | Murky | Clear | Pure | Salt |
|---|---|---|---|---|---|---|
| All heat, as above | to Clear | Clear | Clear | not boiled | kept | refused, or salt where a mod makes it |
| Boiler (Cold Sweat) | +1 a pass, to Clear | Murky, then Clear | Clear | kept | kept | refused |
| Cold Sweat's Waterskin on a campfire | to Clear | Clear | Clear | kept | kept | refused |
| Copper Distiller | to Pure | Pure | Pure | Pure | Pure | Pure |
| Sand Filter (Create, Create Fly) | +1 a pass, to Pure | Murky | Clear | Pure | Pure | passes salty |
| Tanks, pumps, jars, Spout, Item Drain | pass through | kept | kept | kept | kept | kept |

**The Sand Filter is the one exception to "only the distiller makes Pure"**, unchanged: it needs a
running Create setup, Dirty to Pure takes three filters in series, and it never touches salt. Pure from
sand is a gameplay allowance, not a real-world claim.

**Other Pure sources** stay: cold mountain water above y 100, Spelunkery's Spring Water, loot, and a
dripstone cauldron (about 6.5 minutes a serving, too slow to compete; `dripstonePurity` set to 2 makes
it Clear).

## Balance

| Grade | Quenched | Upset Stomach (Easy / Normal / Hard) | Poison |
|---|---|---|---|
| Dirty | 0% | unchanged | unchanged |
| Murky | 50% | unchanged | unchanged |
| Clear | **85%** (100% today) | **3 / 8 / 15%** (5 / 12 / 20% today) | **none** (3 / 5 / 10% today) |
| Pure | 100% | none | none |

| Container | Servings | Boils |
|---|---|---|
| Waterskin | **5** (3 today) | no |
| Copper Canteen | 4 | to Clear, on a campfire |
| Iron Flask | 6 | to Clear, on a campfire |

- The Waterskin keeps its four sprites: 0 empty, 1 or 2 `waterskin_1`, 3 or 4 `waterskin_2`, 5 full.
  `MAX_CAPACITY` stays 6; old skins need no migration.
- A filled Terracotta Water Bowl stacks to 4. Empty Terracotta Bowls appear in village chests.
- The bowl-and-bucket crafting recipe gives a Dirty bowl: a crafting recipe cannot read the bucket's
  grade.
- `boil_water` is earned by having a Copper Canteen, Iron Flask or either Hanging Pot in the inventory
  (`inventory_changed`). `purified_water` points at the distiller.
- The distiller stays at 8 s a serving.

With the switch on:

| Method | Time | Servings |
|---|---|---|
| Furnace, bottle or bowl | 10 s | 1 |
| Furnace, bucket | 10 s | 3 |
| Smoker, any of those | 5 s | 1 or 3 |
| Furnace, Copper Canteen | 3 s a serving | 1 to 4 |
| Furnace, Iron Flask | 4 s a serving | 1 to 6 |

## Steps

### 1. Names and colours

Ships on its own; it changes no behaviour.

- `WaterPurity.purityKey` and `purityColor`; `ConfigCategory`, `ConfigEntry.grade` and
  `DrinkingUpgradeTab` use the renamed keys.
- `SicknessEffect.GRADES` becomes `{"dirty", "murky", "clear", "pure"}`; the migration in
  `sanitizeSickness`.
- Recolour the eight water textures; check in a client.
- Nine lang files: renamed keys, grade 2 retranslated (Vietnamese: Trong; "Chất lượng nước" for the
  scale). `checkLang`.
- `TooltipGameTest`: check the four grade keys by name, since salt now shares the `thirst.water.`
  prefix. A gametest for the migration.
- Dev docs and Javadoc that name a grade; the site (rename `features/water-purity.md` to "Water
  quality", keeping its URL), store pages, screenshots; CHANGELOG ("Clean is now called Clear").

### 2. Cap heat at Clear

- A constant `WaterPurity.BOILED = 2`. `WaterskinItem` and `HangingPotBlock` boil only below it and
  finish at it. `DistillerWater.PURE` stays `MAX`.
- Cold Sweat (`coldsweat`, `coldsweatforge`): `BoiledWater` stops at Clear for the Boiler and the
  campfire skin.
- `ThirstApi`: check what it documents as boiling's result; bump `API_VERSION` only if a method is
  added.

### 3. Config

- `enableFurnaceBoiling`, off; a toggle in the Water page's `water.collected` section, lang keys.
- `quenchedPercent` default `{0, 50, 85, 100}`; `SicknessEffect.defaults()` for Clear as in Balance.
- `thirstwastaken2:furnace_boiling` resource condition per loader, next to `ItemEnabledCondition`.
- Existing configs keep their numbers; the CHANGELOG says to reset the Water and sickness pages.

### 4. Waterskin

`CAPACITY` 5, `ThirstModelProvider.waterskinVariants()` maps five fills onto four sprites.

### 5. Datagen (`ThirstRecipeProvider`, and `LegacyRecipeProvider` for 1.20.1)

- `PURIFY_TABLE` becomes `{2, 2}` (Dirty and Murky only), every recipe gated on `furnace_boiling`.
- Remove campfire recipes (`Heat.CAMPFIRE`).
- Iron Flask `servings × 4 s`; new Copper Canteen recipes `servings × 3 s`, one method for both.
- `FarmersDelightRecipeProvider` gives Clear.
- Bowl recipe gives Dirty (`bowlResult(0)`).
- `boil_water`'s new criterion in `ThirstAdvancementProvider` and `LegacyAdvancementProvider`.

### 6. Cold Sweat

Delete the six `purify_waterskin_*` furnace and smoker recipes in `src/main/coldsweat`; update its
`AGENTS.md` and the checks that expect Pure from a skin or the Boiler.

### 7. Bowls

- `TERRACOTTA_WATER_BOWL` `stacksTo(4)`. Drinking from a stack returns one empty bowl; bowls of
  different grades do not merge; every filler handles a stack.
- A loot pool of 1 to 4 empty bowls in village house chests, skipped while the bowl is disabled.
  `LootGameTest`.

### 8. Regenerate and test

- `runDatagen`, `checkDatagen`, `checkNeoForgeResources`, `checkDataConditions`.
- Gametests, switch off: no furnace water recipe; vessels and pots end Clear and leave Pure alone; the
  distiller gives Pure; Clear quenches 85% and never poisons; 5-serving waterskin; `boil_water` from a
  canteen. Switch on: Murky to Clear, canteen and flask timed by fill. Cold Sweat; bowl stacks, loot
  and recipe.
- Nodes: `26.3.x`, `1.21.1-neoforge`, `1.20.1`, `1.20.1-forge`.

### 9. Docs

`WATER-REFERENCE.md` (then drop its "today" warning), `WATER-SICKNESS.md`, `DISTILLATION-PLAN.md`, the
site's water, drinking, Cold Sweat, Farmer's Delight and configuration pages, and the CHANGELOG as a
balance change.

## Open

- **Is the start too hard?** Before copper there is no way to treat water. If it proves harsh, a
  terracotta pot on a campfire (to Clear) could fill the gap.
