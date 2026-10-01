---
title: "Cold Sweat"
---

<img class="mod-banner" alt="Cold Sweat" src="https://i.imgur.com/N6amWeJ.png" width="360">

With Cold Sweat installed, thirst follows the temperature Cold Sweat measures around the player, and
Cold Sweat's own Waterskin carries a water grade and quenches thirst.

::: warning Supported versions
Everything on this page works on NeoForge, Minecraft 1.21.1, with
![](https://cdn.modrinth.com/data/uXhSmPjd/bf55420556c30d44d2f5cf7b8915705b9214b4ef.png){.mod-icon} [Cold Sweat](https://modrinth.com/mod/cold-sweat) 2.4.3.1, and on Forge, Minecraft 1.20.1, with
2.4.3.2. Cold Sweat has no Fabric build and no build for a newer Minecraft version.
:::

![A snowy taiga by a frozen river and ice spikes, with Cold Sweat's body temperature gauge between the hearts and the thirst bar](/screenshots/integrations/cold-sweat/cold-sweat-hud.png)

## Climate

Thirst drains faster in the heat and slower in the cold. Without Cold Sweat that heat is the biome's.
With it, it is the temperature Cold Sweat shows around the player, so a Hearth in the tundra, shade,
night in the desert and altitude all count. The Nether and Fire Resistance work as before.

Turn it off with [`coldSweatClimate`](/docs/configuration#coldsweatclimate).

## The Waterskin

Cold Sweat's Waterskin is a second waterskin next to this mod's, with its own temperature.

![Cold Sweat's filled Waterskin and its tooltip, the grade line stepping through Dirty, Murky, Clean, Pure and Salty](/screenshots/integrations/cold-sweat/cold-sweat-waterskin.gif)

- Filled from water in the world, it takes that water's [grade](/docs/features/water-purity), sea
  water included. Filled from a cauldron or a tank, it takes the grade of the water inside.
- A sip, made while sneaking, restores what a bottle of water does, 6 thirst and 8 quenched, with the
  same chance of sickness for bad water. Sea water restores nothing and makes the player Parched.
- Pouring it over the player, its normal use, restores no thirst.
- Poured into a cauldron, the cauldron keeps the worse of the two grades, as with any container.
- The empty Waterskin left over carries no grade.

The grade shows in the Waterskin's tooltip.

## Purifying water

![A lit Boiler on the snow between a campfire warming two Waterskins and a water cauldron, ice spikes behind](/screenshots/integrations/cold-sweat/cold-sweat-boiler.png)

| Where | What happens |
|---|---|
| Boiler | Takes any water container of this mod, bottles, buckets and bowls too. With fuel, it raises each one a grade every 10 seconds, up to Pure. |
| Campfire | The Waterskin comes off boiled like a bottle: Dirty becomes Clean, anything better becomes Pure. |
| Furnace and Smoker | The Waterskin purifies like a bottle of water. |

Sea water stays sea water in all three. Boiling does not remove salt.

## Hot drinks

Hot Cocoa from Farmer's Delight and the hot teas from Kaleidoscope Cookery warm the player a little
for a minute, when those mods are installed. Plain water changes nothing: the Waterskin already has a
temperature of its own.
