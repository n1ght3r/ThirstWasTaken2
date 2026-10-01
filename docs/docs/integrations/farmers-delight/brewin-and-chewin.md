---
title: "Brewin' and Chewin'"
---

![Brewin' and Chewin'](https://i.imgur.com/EFkjwBq.png){.mod-banner}

With Brewin' and Chewin' installed, its brews and soups restore thirst, and water keeps its grade in
the Keg.

::: warning Supported versions
Everything on this page works on Fabric and NeoForge, Minecraft 1.21.1, with
![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FhIu9KJTT%2Ff7c591a80046859d3d45c04ecbbc54d264483d5e.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Brewin' and Chewin'](https://modrinth.com/mod/brewin-and-chewin) 4.5.0. The mod has no build for a
newer Minecraft version.
:::

![A Brewin' and Chewin' Keg beside barrels on a lakeshore, with Jade naming its water Dirty](/screenshots/integrations/brewin-and-chewin/brewin-keg.png)

## Brews and soups

![Brewin' and Chewin' brews in the hotbar, with the Beer tooltip showing its thirst and quenched droplets](/screenshots/integrations/brewin-and-chewin/brewin-drinks.png)

| Item | Thirst | Quenched |
|---|---|---|
| Kombucha | 6 | 8 |
| Beer, Mead, Egg Grog, Glittering Grenadine | 5 | 6 |
| Bloody Mary | 4 | 5 |
| Rice Wine | 3 | 4 |
| Pale Jane, Strongroot Ale, Dread Nog | 3 | 3 |
| Saccharine Rum, Steel-Toe Stout, Red Rum | 2 | 2 |
| Creamy Onion Soup | 4 | 5 |
| Fiery Fondue | 2 | 3 |

The stronger a drink, the less it restores. Vodka, Salty Folly and Withering Dross restore none. A
brew is safe whatever water went into the Keg. Every value can be changed in the
[config](/docs/configuration#drinks-and-foods).

## Water in the Keg

- A bucket or a bottle of water poured into a Keg and taken back out keeps its grade. Without this,
  the Keg handed back Clean water whatever went in.
- A Keg holding water of one grade refuses water of another.
- A Keg picked up with water in it keeps the grade when it is placed again.
- The Keg takes sea water and hands it back still salty, but nothing ferments from it. A brew from it
  would come out safe, which would make the sea drinkable.

With Jade installed, looking at a Keg of water shows its grade under the crosshair.
