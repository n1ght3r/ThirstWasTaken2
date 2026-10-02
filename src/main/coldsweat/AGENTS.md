# src/main/coldsweat — Cold Sweat on NeoForge 1.21.1

[Cold Sweat](https://modrinth.com/mod/cold-sweat), the body temperature mod (mod id `cold_sweat`,
package `com.momosoftworks.coldsweat`). With it installed, the thirst drain follows the world
temperature Cold Sweat measures around the player, and Cold Sweat's own waterskin carries a grade and
quenches when drunk. Cold Sweat's Boiler purifies water, its Waterskin purifies on a campfire, in a
furnace and in a smoker, and hot drinks from two other mods warm the player. Why each choice was made,
and what was found in game, is in
[docs/dev/integration/climate/COLD-SWEAT-INTEGRATION.md](../../../docs/dev/integration/climate/COLD-SWEAT-INTEGRATION.md).

This directory is **only compiled by NeoForge nodes that set `deps.cold_sweat`** in
`stonecutter.properties.toml`. Today that is `1.21.1-neoforge`, with Cold Sweat 2.4.3.1: it has no
Fabric build and nothing past 1.21.1. Its jar is in Mojang names, so the mixins need no `remap = false`.
The same integration on Forge 1.20.1 is [src/main/coldsweatforge](../coldsweatforge/AGENTS.md); a
change here goes there too.

```
java/com/thirstwastaken2/coldsweat/
  ColdSweatPresence      the gate: FML's mod file for `cold_sweat`, a marker class in it, and each
                         mixin target's methods, read off the class file
  ColdSweatEntrypoint    a second @Mod class for the mod id; names no Cold Sweat class
  ColdSweatMixinPlugin   applies the mixins only when the gate passes and their methods are there
  ColdSweatClimate       hands core the world temperature, through ThirstManager.setClimateTemperature
  WaterskinWater         the grade a waterskin is filled with, and stripping it off an empty one
  BoiledWater            what the Boiler takes and raises, and the campfire's rule for the skin
  mixin/                 WaterskinItemMixin (filling), FilledWaterskinItemMixin (the empty skin),
                         BoilerBlockEntityMixin and BoilerSlotMixin (the Boiler),
                         CampfireWaterskinMixin (vanilla's campfire, for the skin)
resources/
  thirstwastaken2.coldsweat.mixins.json
  data/thirstwastaken2/recipe/cold_sweat/            furnace and smoker recipes for the skin
  data/thirstwastaken2/cold_sweat/item/food/         Cold Sweat food data: hot drinks warm
```

## How it stays optional

1. **Build.** Only when `deps.cold_sweat` is set, `build.neoforge.gradle.kts` adds this directory and
   appends the mixin config and an optional `cold_sweat` dependency to the built `neoforge.mods.toml`, as
   [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt) says.
2. **Runtime gate.** `ColdSweatEntrypoint` calls `ColdSweatClimate.install()` only after
   `ColdSweatPresence.isPresent()`, and the mixin plugin asks the same gate.
3. **Core never names it.** The climate reaches core through a `ToDoubleFunction<Player>` seam in
   `ThirstManager`, which is not API. The waterskin is known to core by registry id only:
   `WaterPurity.resolve` treats `cold_sweat:filled_waterskin` as a water container, and `ThirstConfig`'s
   default drinks give it a bottle's value, 6 and 8.

**Nothing outside this directory may reference a class in it.**

## Climate

`ThirstManager.climateModifier` takes `Trait.WORLD` in place of `Biome#getBaseTemperature`; the
humidity, the curve, the harshness softening, the scorching override, Fire Resistance and Fire
Protection are unchanged. The value is read through `EntityTempManager.getTemperatureCap`, not
`Temperature.get`: a player Cold Sweat keeps no temperature for answers NaN and drains by the biome,
where `Temperature.get` would answer 0, freezing. The `coldSweatClimate` config toggle turns it off. The
modifier is cached 20 ticks per player, so this costs one capability read a second per player.

## The waterskin

- **Filling.** `WaterskinItem.getFilledItem` is stamped at its return, server side, with
  `WaterPurity.sampleAt` of the block. A cauldron is lowered *before* that call, and its last layer
  takes the stored grade with it, so `useOn`'s head notes the cauldron's grade first; a tank's water
  carries its grade on the `FluidStack`, so the `drain` inside `useOn`'s lambda notes that. Both notes
  live in a `ThreadLocal` in `WaterskinWater` and are cleared at `useOn`'s return.
- **Pouring into a cauldron** needs nothing here: once the filled skin is a water container, core's
  `WaterInteractions.transferCauldronPurity` stores the worse of the two, as for every pour.
- **Drinking** needs nothing here either: `ItemStackMixin.finishUsingItem` drinks every item with a
  thirst value, so a crouched sip (Cold Sweat's default secondary action) quenches with its grade's
  sickness, and salt water parches. Pouring it over yourself goes through `use`, never
  `finishUsingItem`, and restores nothing. It is not "plain water", so a full bar stops neither.
- **The empty skin** comes from `getCraftingRemainingItem` on every path (recipe, pour, last sip,
  dispenser). Cold Sweat copies every component onto it; the mixin strips `water_purity` and
  `water_salty` again.
- **Sprites.** None: Cold Sweat's waterskin has its own model, and `syncModel` leaves a modded
  container's model alone. The tooltip's grade line is the only sign.

## The Boiler

Cold Sweat's own purifying branch is keyed on the other port's mod id and never runs here.
`BoilerBlockEntityMixin` takes its place: both item checks (a hopper's `canPlaceItemThroughFace`, and
`BoilerSlotMixin` on the menu's anonymous slot class) also let in anything `WaterPurity.isWaterContainer`
answers yes for; at the tail of the Boiler's tick, on Cold Sweat's own beat (200 ticks divided by its
`TEMP_RATE`) and only with fuel, every fresh container in slots 1 to 9 goes up one grade. Setting
`hasDrinkables`, there and in `checkForItems`, keeps the Boiler lit and burning fuel while it works, as
Cold Sweat's own branch does. Salt water is let in and never raised.

## Campfire, furnace, smoker

Cold Sweat already has a campfire recipe for any filled skin (it warms it), which hands back a new skin
without our components. A second recipe of ours for the same input would leave the pick to load order,
so there is none: `CampfireWaterskinMixin` wraps the drop in `CampfireBlockEntity.cookTick` and, when
the result is an unstamped water container of the same item as a stamped input (`@Local(ordinal = 0)`
is the input stack), stamps it with the bottle's campfire rule. It touches nothing already stamped, so
this mod's own campfire recipes pass through unchanged. The furnace and smoker recipes are ordinary JSON,
the bottle's grades and times, with a `neoforge:mod_loaded` condition; datagen is Fabric only, so they
are hand-written.

## Hot drinks

Cold Sweat food data, `+10` base temperature for 1200 ticks, on Farmer's Delight's hot cocoa and the
hot Kaleidoscope Cookery teas. Plain water gets none: the waterskin already carries a temperature.

**Every file needs `neoforge:conditions`** with `mod_loaded` for `cold_sweat` and for each mod whose
items it names. Cold Sweat loads these from any namespace, but honours `required_mods` before decoding
only in its own: in ours an item id that does not exist fails the whole registry and no world loads.
That was 1.3.0 and 1.4.0 without Farmer's Delight. `checkDataConditions` now fails the build on it.

## Testing

The gametests run without Cold Sweat and prove the node loads without it.
[tools/agent/integrations/cold-sweat.jsonl](../../../tools/agent/integrations/cold-sweat.jsonl) drives
a client with it. Run on 2026-09-26, every check passed:

- a source in plains fills a Murky skin, a source in an ocean a salty one, and the tooltip shows the grade;
- a Dirty three-layer cauldron gives a Dirty skin and keeps two layers; the last layer of a Pure
  cauldron gives a Pure skin and leaves an empty cauldron;
- a Dirty skin poured into an empty cauldron stores Dirty, a Pure one on top keeps it Dirty;
- a crouched Pure sip takes thirst 4 to 10 and quenched 0 to 8; a salty one restores nothing and gives
  Parched II; pouring over yourself restores nothing; each leaves an empty skin with no grade;
- the thirst bar and Cold Sweat's gauge do not overlap;
- the Boiler takes a Dirty bottle, a Murky skin and a salty bucket from a hopper and leaves them Pure,
  Pure and salty after 700 ticks; a Hearth keeps a water bucket in its hopper and takes lava;
- skins off a campfire come out Clean (from Dirty), Pure (from Murky) and salty; a Dirty skin smelts
  Clean; a canteen still boils Pure in hand;
- at thirst 10 health does not regenerate, at full thirst it does;
- hot cocoa takes the base temperature from 0 to 10;
- with `coldSweatClimate` false the climate modifiers are the biome's: 0.929, 0.586 and 1.2.

The climate readings, at y 200 on narrow strips given their biome by `fillbiome`: 0.89 in plains, 0.91
in snowy plains, 1.03 in a desert by day and 0.83 by night, against 0.93, 0.59 and 1.2 from the biome
alone. Cold Sweat blends the biomes around the player and cools with altitude, so strips this narrow and
high all read alike; the day and night difference is the one that shows the source works.
