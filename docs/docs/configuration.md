---
outline: [2, 3]
---

# Configuration

Settings live in `config/thirstwastaken2.json`, written on first launch. They can also be changed in
game, through Mod Menu on Fabric or the Mods list on NeoForge.

![The settings screen, every page and tab: sliders and switches, the Water tabs, the Sickness tables, the AppleSkin preview, the item list, Mod Items and Containers](/screenshots/config/config-showcase.gif)

- A changed setting shows in amber; the arrow beside it puts it back.
- **Reset to Defaults** resets the page, **Done** saves, **Cancel** discards.
- A file edited by hand is read on the next start.

## The file

Every setting at its default. The long lists are cut short here; see
[sicknessEffects](#sicknesseffects) and [drinks and foods](#drinks-and-foods).

```json
{
  "thirstDepletionModifier": 1.2,
  "thirstDepletionInPeaceful": false,
  "preventSprintingWhenThirsty": true,
  "canDrinkByHand": true,
  "dehydrationHaltsHealthRegen": true,
  "foodHealMinThirstPercent": 50,
  "quenchedHealMinFoodPercent": 50,
  "coldSweatClimate": true,
  "sereneSeasonsClimate": true,
  "seasonDrainSpring": 1.0,
  "seasonDrainSummer": 1.15,
  "seasonDrainAutumn": 1.0,
  "seasonDrainWinter": 0.9,
  "appleskinQuenchedOverlay": "DIAMOND",
  "appleskinTooltipDroplets": true,
  "defaultQuality": 2,
  "plainWaterValue": [6, 4],
  "plainWaterDrinkTicks": 32,
  "quenchedPercent": [0, 25, 50, 100],
  "enableSeaWater": true,
  "seaWaterNauseaSeconds": 8,
  "seaWaterParchedSeconds": 30,
  "enableRainCollection": true,
  "rainwaterQuality": 2,
  "dripstoneQuality": 3,
  "enableBoilingInHand": true,
  "enableFurnaceBoiling": true,
  "waterskinCapacity": 4,
  "copperCanteenCapacity": 4,
  "ironFlaskCapacity": 6,
  "terracottaWaterBowlStackSize": 3,
  "copperCanteenBoilSeconds": 2,
  "ironFlaskBoilSeconds": 3,
  "copperHangingPotBoilSeconds": 3,
  "ironHangingPotBoilSeconds": 4,
  "copperHangingPotCapacity": 3,
  "ironHangingPotCapacity": 6,
  "distillerServingSeconds": 8,
  "distillerTankServings": 9,
  "sicknessEffects": {
    "normal": {
      "dirty": [
        { "effect": "thirstwastaken2:upset_stomach", "chance": 75, "seconds": 45, "level": 2, "group": "illness" },
        { "effect": "minecraft:poison", "chance": 20, "seconds": 20, "level": 1, "group": "illness" }
      ]
    }
  },
  "extendSicknessEffects": true,
  "enableDrinkTagMatching": true,
  "drinkTagValue": [6, 4],
  "enableKeywordMatching": false,
  "keywordBlacklist": "dried|candied|leaf|leaves|gummy|crate|jam|sauce|bucket|seed|cookie|pie|bush|sapling|bean|curry|cake|candy",
  "drinkKeywords": "drink|juice|tea|soda|coffee|wine|beer|cider|yogurt|milkshake|smoothie",
  "soupKeywords": "soup|stew|porridge",
  "fruitKeywords": "fruit|berry|berries|grape|orange|peach|pear|coconut|lemon|melon|cherry|apple",
  "keywordDrinkValue": [6, 4],
  "keywordSoupValue": [6, 4],
  "keywordFruitValue": [2, 0],
  "itemBlacklist": [],
  "drinks": {
    "minecraft:potion": [6, 8],
    "minecraft:milk_bucket": [4, 0]
  },
  "foods": {
    "minecraft:apple": [2, 0]
  },
  "enableBowls": true,
  "enableWaterskin": true,
  "enableCopperCanteen": true,
  "enableIronFlask": true,
  "enableCopperHangingPot": true,
  "enableIronHangingPot": true,
  "enableCopperDistiller": true,
  "distillerSaltItem": ""
}
```

::: tip Updating from an older version
An existing config file keeps its values, so most new defaults only take effect after a reset. Reset
the **Thirst**, **Water**, **Sickness**, **Item Values** and **Containers** pages on the settings
screen, or delete the file to take every new value at once. `defaultPurity`, `rainwaterPurity` and `dripstonePurity` are read once and renamed to
`defaultQuality`, `rainwaterQuality` and `dripstoneQuality`.
:::

## Thirst

### thirstDepletionModifier

Default `1.2` (`120%`). The base drain speed. `0` stops thirst draining.

### thirstDepletionInPeaceful

Default `false`. Off, thirst refills on its own on Peaceful.

### preventSprintingWhenThirsty

Default `true`. No sprinting at 6 thirst or below.

### dehydrationHaltsHealthRegen

Default `true`. Food only heals with the thirst bar at `foodHealMinThirstPercent` or more. See
[Healing](/docs/features/thirst-and-quenched#healing).

### foodHealMinThirstPercent

Default `50`, from `0` to `100`. How full the thirst bar has to be for food to heal.

### quenchedHealMinFoodPercent

Default `50`, from `0` to `100`. How full the food bar has to be for quenched to heal. It replaces
`quenchedHealMinFood`, which counted half shanks.

### coldSweatClimate

Default `true`. With Cold Sweat, the drain follows Cold Sweat's temperature instead of the biome's.
See [Cold Sweat](/docs/integrations/cold-sweat#climate).

### sereneSeasonsClimate

Default `true`. With Serene Seasons, the drain follows the season. See
[Serene Seasons](/docs/integrations/serene-seasons).

### seasonDrainSpring, seasonDrainSummer, seasonDrainAutumn, seasonDrainWinter

Defaults `1.0`, `1.15`, `1.0`, `0.9`, from `25%` to `400%`. The drain speed in each season.

## Water

### defaultQuality

Default `2`, Clean. The grade for water that has none, such as drinks from other mods.

### canDrinkByHand

Default `true`. Sneak and use an empty hand on water to sip it, or use an empty hand on a water
cauldron to drink from it.

### plainWaterValue

Default `[6, 4]`. Thirst and quenched one drink of water restores, from a bottle, bowl, waterskin,
canteen or flask alike, before its grade's share of quenched.

### plainWaterDrinkTicks

Default `32`, from `8` to `100`. Ticks a drink from a bowl, waterskin, canteen or flask takes. A water
bottle always takes vanilla's 32.

### quenchedPercent

Default `[0, 25, 50, 100]`. How much of a drink's quenched each grade gives, Dirty to Pure.

### enableSeaWater

Default `true`. Ocean and beach water is salty.

### seaWaterNauseaSeconds and seaWaterParchedSeconds

Default `8` and `30`. How long sea water gives Nausea and Parched. `0` gives none.

### enableRainCollection

Default `true`. Rain fills hanging pots and cauldrons with `rainwaterQuality` water.

### rainwaterQuality and dripstoneQuality

Default `2`, Clean, and `3`, Pure. The grade of rain and of dripstone water.

## Sickness

### sicknessEffects

Default: the tables in [Drinking bad water](/docs/features/water-purity#drinking-bad-water). For each
difficulty and grade, a list of effects a drink can give, each with its own chance.

| Field | Meaning |
|---|---|
| `effect` | Effect id, from any mod, such as `minecraft:poison` |
| `chance` | `0` to `100`, percent per drink |
| `seconds` | `1` to `600` |
| `level` | `1` to `10` |
| `group` | Optional. Effects with the same group share one roll per drink, so a lower chance comes only with a higher one. Without a group an effect rolls on its own |

A difficulty or grade left out gets its defaults, an empty list gives nothing, and an effect from a
mod that is not installed is skipped.

### extendSicknessEffects

Default `true`. The same effect again makes it last longer: Upset Stomach by half the new time, up to
one and a half times the table's, any other effect by the new time, up to twice the table's. Off, the
longer one is kept.

## AppleSkin

Client-side, and only used with AppleSkin installed.

### appleskinQuenchedOverlay

Default `DIAMOND`. The quenched outline: `DIAMOND`, `ICE`, `GOLD`, `APPLESKIN`, `LEGACY` or `OFF`.

### appleskinTooltipDroplets

Default `true`. Thirst and quenched droplets in tooltips.

## Item values

### drinks and foods

Item ids and their `[thirst, quenched]`. Edit them on the **Item Values** page, or add entries to
support another mod. Ids for mods that are not installed are ignored.

### itemBlacklist

Empty by default. Items listed here restore nothing.

### enableDrinkTagMatching

Default `true`. Items their mod marks as drinks restore `drinkTagValue`, unless listed in `drinks`
or `foods`.

### drinkTagValue

Default `[6, 4]`, the same as a prepared drink such as a juice or tea.

### enableKeywordMatching

Default `false`. Guesses a value from the item id, so any `strawberry_juice` counts as a drink.

### drinkKeywords, soupKeywords and fruitKeywords

Words matched against the id, worth `keywordDrinkValue`, `keywordSoupValue` or `keywordFruitValue`.

### keywordBlacklist

Words that stop a guess, so `melon_seed` is not fruit.

## Mod items

A switch that is off stops the item being crafted and hides it from the creative tab; existing ones
keep working. Applies after `/reload`, or a restart on a dedicated server.

### enableBowls

Default `true`. The Clay Bowl, Terracotta Bowl and filled bowl, together.

### enableWaterskin

Default `true`. The Waterskin.

### enableCopperCanteen

Default `true`. The Copper Canteen.

### enableIronFlask

Default `true`. The Iron Flask.

### enableCopperHangingPot and enableIronHangingPot

Default `true`. One switch for each hanging pot.

### enableCopperDistiller

Default `true`. The Copper Distiller and its four parts, together.

## Containers

Every count here is from `1` to `64`. Lowering one never takes water away: a container that holds
more keeps it and can be drunk or drawn from, but takes nothing more until it has room.

### waterskinCapacity, copperCanteenCapacity and ironFlaskCapacity

Default `4`, `4` and `6`. Drinks each holds when full.

### terracottaWaterBowlStackSize

Default `3`. How many filled water bowls of the same water stack together, each still one drink. Needs
a restart, and the server and players must use the same value. Water bottles don't stack.

### copperHangingPotCapacity and ironHangingPotCapacity

Default `3` and `6`. Drinks each hanging pot holds. A bucket is three.

### enableBoilingInHand

Default `true`. Hold use with a Copper Canteen or Iron Flask over a lit campfire to boil it.

### enableFurnaceBoiling

Default `true`. Furnaces and smokers boil water bottles, water bowls and water buckets, and furnaces
the Copper Canteen and Iron Flask. Off, they boil no water. Applies after `/reload`.

### copperCanteenBoilSeconds and ironFlaskBoilSeconds

Default `2` and `3`, from `1` to `60`. Seconds per drink over a campfire or in a furnace.

### copperHangingPotBoilSeconds and ironHangingPotBoilSeconds

Default `3` and `4`, from `1` to `60`. Seconds per drink in a hanging pot.

### distillerServingSeconds

Default `8`, from `1` to `60`. Seconds the [Copper Distiller](/docs/features/water-purity#copper-distiller)
takes per drink while its fire burns.

### distillerTankServings

Default `9`. Drinks the distiller's boiler and basin each hold. Three make a bucket, so below three
it takes no buckets.

### distillerSaltItem

Default `""`. The item id of the salt the distiller leaves from sea water, for a pack with more than
one salt mod, for example `"croptopia:salt"`. Empty picks one of the installed mods' salts. Not on
the config screen.
