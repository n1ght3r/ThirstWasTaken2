---
outline: [2, 3]
---

# Configuration

Settings live in `config/thirstwastaken2.json`, written on first launch. They can also be changed in
game: through Mod Menu on Fabric, or the Mods list on NeoForge.

![The config screen, with a tab for each group of settings on the left](/screenshots/config/config-screen.png)

- Pick a group from the tabs on the left, or type in the search box to find a setting on any page.
  A longer page is split into smaller tabs along its top.
- A setting you have changed is shown in amber. The arrow button next to it puts it back.
- **Reset to Defaults** resets the whole page, **Done** saves, **Cancel** discards.
- A file edited by hand is read on the next start.

## Thirst

### thirstDepletionModifier

Default `1.2`, shown as `120%`. The base drain speed, before biome changes. `0` stops thirst draining.

### thirstDepletionInPeaceful

Default `false`. When off, thirst refills on its own on Peaceful.

### preventSprintingWhenThirsty

Default `true`. Stops sprinting at 6 thirst or below.

### dehydrationHaltsHealthRegen

Default `true`. Stops natural healing until thirst is nearly full. See
[Running low](/docs/features/thirst-and-quenched#running-low).

### quenchedHealthRegen

Default `0.5`. How fast quenched heals while thirst is full, as a share of how fast saturation
heals: `1.0` is as fast, `0` turns it off. See
[Healing](/docs/features/thirst-and-quenched#healing).

### quenchedHealMinFood

Default `10`. The food level, in half shanks, quenched needs before it heals. `10` is five shanks,
`0` lets it heal even while starving.

### coldSweatClimate

Default `true`. With Cold Sweat installed, the drain follows the temperature Cold Sweat measures around
the player, hearths and shade included, instead of the biome's. Does nothing without Cold Sweat, and the settings screen only shows it
when Cold Sweat is installed. See
[Cold Sweat](/docs/integrations/cold-sweat#climate).

### sereneSeasonsClimate

Default `true`. With Serene Seasons installed, the drain follows the season, and tropical biomes follow
their wet and dry seasons. Shown on the Seasons tab, which only appears with Serene Seasons. See
[Serene Seasons](/docs/integrations/serene-seasons).

### seasonDrainSpring, seasonDrainSummer, seasonDrainAutumn, seasonDrainWinter

Defaults `1.0`, `1.15`, `1.0` and `0.9`, shown as percentages from `25%` to `400%`. The drain speed in
the middle of each season, with Serene Seasons installed. Between two seasons the speed moves from one
to the other a little each day.

## Water

![The Water page, split into Drinking, Quenched, Sea Water and Rain and Dripstone tabs](/screenshots/config/config-water.png)

### defaultPurity

Default `2`, Clean. The grade for water that has none, such as drinks from other mods.

### canDrinkByHand

Default `true`. Sneak and use an empty hand on water to drink.

### quenchedPercent

Default `[0, 50, 100, 100]`. How much of a drink's quenched water of each grade gives, Dirty first,
then Murky, Clean and Pure. Bad water fills the bar but does not keep it full. Shown as four sliders.

### enableSeaWater

Default `true`. Ocean and beach water is salty. Off, it is graded like any other water.

### seaWaterNauseaSeconds and seaWaterParchedSeconds

Default `8` and `30`. How long a drink of sea water gives Nausea and Parched. `0` gives none.

### enableRainCollection

Default `true`. Rain fills hanging pots, and rain in a cauldron gets `rainwaterPurity`. Off, pots
ignore rain and rain in a cauldron has no grade, so it counts as `defaultPurity`.

### rainwaterPurity and dripstonePurity

Default `2`, Clean, and `3`, Pure. The grade of collected rain and of water a pointed dripstone drips
into a cauldron.

## Sickness

### sicknessEffects

Default: the tables in [Drinking bad water](/docs/features/water-purity#drinking-bad-water). For each
difficulty and each grade, a list of effects a drink can give. Each effect has its own chance, so one
drink can give several, or none.

| Field | Meaning |
|---|---|
| `effect` | The effect id, such as `minecraft:poison` or `thirstwastaken2:upset_stomach`. Effects from other mods work too |
| `chance` | `0` to `100`, the percent chance per drink |
| `seconds` | `1` to `600`, how long it lasts |
| `level` | `1` to `10`, the effect level |

The Sickness page of the config screen has a tab per difficulty. Each tab lists the grades, from Dirty to Pure, with their
effects. Effects can be added by id, edited or removed there, and each grade can be reset on its own.

```json
"sicknessEffects": {
  "normal": {
    "dirty": [
      { "effect": "minecraft:nausea", "chance": 100, "seconds": 7, "level": 1 },
      { "effect": "minecraft:poison", "chance": 25, "seconds": 20, "level": 1 }
    ],
    "pure": [
      { "effect": "minecraft:regeneration", "chance": 10, "seconds": 5, "level": 1 }
    ]
  }
}
```

A difficulty or grade left out of the file gets its default effects. An empty list gives nothing. An
effect from a mod that is not installed is skipped.

### extendSicknessEffects

Default `true`. A drink that gives an effect the player already has adds its time to what is left, up
to twice the time in the table, and keeps the higher level. Off, the longer of the two is kept.

## AppleSkin

These settings are client-side and only used while AppleSkin is installed. The exhaustion strip
follows AppleSkin's **Food Exhaustion HUD Underlay** setting.

![The AppleSkin page, with a live preview of the thirst bar and a drink's tooltip](/screenshots/config/config-appleskin.png)

### appleskinQuenchedOverlay

Default `DIAMOND`. The quenched outline colour: `DIAMOND`, `ICE`, `GOLD`, `APPLESKIN` or `LEGACY`, the
blue outline of the original Thirst Was Taken. `OFF` hides the outline and the exhaustion strip.

### appleskinTooltipDroplets

Default `true`. Shows the thirst and quenched droplets in tooltips.

## Item values

### drinks and foods

Two lists of item ids and their thirst and quenched. The **Item Values** page shows one row per item,
with a box for each number, a switch that puts the item in `itemBlacklist`, and a row to add an item.
In the file they look like this:

```json
"drinks": {
  "minecraft:potion": [6, 8],
  "thirstwastaken2:terracotta_water_bowl": [4, 5]
}
```

Ids for mods that are not installed are ignored. Add entries to support another mod.

### itemBlacklist

Empty by default. Items listed here restore nothing.

### enableDrinkTagMatching

Default `true`. Items their mod marks as drinks restore `drinkTagValue`. Items in `drinks` or `foods`
keep their own value.

### drinkTagValue

Default `[6, 8]`, the same as a water bottle.

### enableKeywordMatching

Default `false`. Guesses a value from the item id, so a `strawberry_juice` from any mod counts as a
drink. Guesses can be wrong, but it covers a large modpack quickly.

### drinkKeywords, soupKeywords and fruitKeywords

Words matched against the item id, separated by `|`. Matches are worth `keywordDrinkValue`,
`keywordSoupValue` or `keywordFruitValue`. Drinks are checked first, then soups, then fruit.

### keywordBlacklist

Words that stop a guess, so `melon_seed` is not treated as fruit. Only applies to guesses.

## Mod items

![The Mod Items page on its Items tab: one switch per item, under a note that changes apply after /reload](/screenshots/config/config-mod-items.png)

For a modpack that brings its own canteen or pot. A switch that is off stops the item being crafted
and hides it from the creative tab. Items that already exist keep working, and the item stays in the
game, so worlds that hold one still load.

Changes apply after `/reload`, or on rejoining a singleplayer world. A dedicated server reads the file
only on start, so it needs a restart. Recipe viewers such as JEI and EMI still list a switched-off
item.

### enableBowls

Default `true`. The Clay Bowl, the Terracotta Bowl and the filled bowl, together, since one is no use
without the others. Off also removes boiling water in a bowl.

### enableWaterskin

Default `true`. The Waterskin.

### enableCopperCanteen

Default `true`. The Copper Canteen.

### enableIronFlask

Default `true`. The Iron Flask, and cleaning its water in a furnace.

### enableCopperHangingPot and enableIronHangingPot

Default `true`. The Copper Hanging Pot and the Iron Hanging Pot, one switch each.

## Containers

![The Containers page on its Boil in Hand tab, beside the Capacity and Hanging Pots tabs](/screenshots/config/config-containers.png)

### copperCanteenCapacity and ironFlaskCapacity

Default `4` and `6`, from `1` to `6`. How many drinks each holds when full. One that already holds
more keeps its water but takes no more.

### enableBoilingInHand

Default `true`. Holding use with a Copper Canteen or Iron Flask on a lit campfire boils its water.

### copperCanteenBoilSeconds and ironFlaskBoilSeconds

Default `3` and `4`. Seconds each drink takes to boil over a campfire.

### copperHangingPotBoilSeconds and ironHangingPotBoilSeconds

Default `4` and `6`. Seconds each drink in a hanging pot takes to boil.
