---
title: "Let's Do: Farm & Charm"
---

![Let's Do: Farm & Charm](https://lets-do.ch/assets/mod-logos/farm_and_charm.webp){.mod-banner}

With Farm & Charm installed, its teas and soups restore thirst, and water from its Timber Well and
Water Trough carries an honest grade. Its kitchen addon has [a page of its own](./candlelight).

::: warning Supported versions
Fabric and NeoForge, Minecraft 1.21.1, with ![](https://cdn.modrinth.com/data/HJetCzWo/7c6c372629b3efa41409621631d60df12963f005_96.webp){.mod-icon} [Let's Do: Farm & Charm](https://modrinth.com/mod/lets-do-farm-charm)
1.1.26, which needs Architectury API. Its 1.20.1 version is no longer updated and is not supported.
:::

![A Timber Well and a Water Trough, both full of water, on a sandy shore](/screenshots/integrations/farm-and-charm/farm-and-charm-well.png)

## Timber Well

A Timber Well pumps groundwater from a water source up to six blocks below it. A bucket drawn from it
gets the grade of that groundwater, the same grade a bottle filled at the source would get. A well in a
swamp gives Dirty water, and a well by the sea gives sea water.

A well with no water below fills only with rain, and gives rain water, Clean by default.

![A player at a Timber Well on a sandy shore, holding the bucket just drawn from it, its tooltip reading Salty](/screenshots/integrations/farm-and-charm/farm-and-charm-well-salty-bucket.png)

This well stands on a beach, so the lake water under it counts as sea water, and so does every bucket
it gives.

## Water Trough

Water drawn from a Water Trough is always Murky, whatever was poured in. Water standing open in a
trough is not clean, and a long trough never runs dry, so it cannot be a free source of clean water.
Boil it before drinking.

A trough refuses sea water.

## Cooking

The Cooking Pot, the Stove and the Crafting Bowl take a water bucket in their recipes. Any grade of
fresh water works, since the food that comes out is its own item. Sea water does not: a recipe with a
bucket of sea water does not start.

## Food and drink

| Item | Thirst | Quenched |
|---|---|---|
| Strawberry, Nettle and Ribwort Tea, a cup or the whole jug | 6 | 9 |
| Barley, Onion, Potato and Simple Tomato Soup, Goulash, Farmer Salad | 4 | 5 |
| Corn Grits | 3 | 4 |
| Tomato | 2 | 3 |
| Lettuce, Strawberry | 1 | 2 |

A placed tea jug pours two cups, so drinking the jug whole wastes one. Breads, roasts, pancakes and
other dry dishes restore no thirst. Every value can be changed in the [config](/docs/configuration).
