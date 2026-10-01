# Let's Do: Farm & Charm

With Farm & Charm installed, its teas and soups restore thirst, and water from its Timber Well, Water
Trough and kitchen sinks carries an honest grade. Candlelight, its kitchen and dining addon, is covered
here too.

::: warning Supported versions
Fabric and NeoForge, Minecraft 1.21.1: ![](https://cdn.modrinth.com/data/HJetCzWo/7c6c372629b3efa41409621631d60df12963f005_96.webp){.mod-icon} [Let's Do: Farm & Charm](https://modrinth.com/mod/lets-do-farm-charm)
1.1.26 and ![](https://cdn.modrinth.com/data/qwbArkQk/5e0770c8da0fab82a70bc9c3913c8d3996c53345_96.webp){.mod-icon} [Let's Do: Candlelight](https://modrinth.com/mod/lets-do-candlelight-farmcharm-compat)
2.1.13. Both need Architectury API. Their 1.20.1 versions are no longer updated and are not supported.
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

## Kitchen sinks

![A Candlelight kitchen sink full of water between counters, beside a stove with a cooking pot](/screenshots/integrations/farm-and-charm/candlelight-kitchen-sink.png)

Candlelight's kitchen sinks fill from nothing, as they always have. The water they give, in a bottle
or a bucket, is Murky, the same as a trough's, so it still needs boiling. A sink refuses sea water.

![A player at a Candlelight kitchen, holding a bottle just filled at the sink, its tooltip reading Murky](/screenshots/integrations/farm-and-charm/candlelight-sink-murky-bottle.png)

## Cooking

The Cooking Pot, the Stove and the Crafting Bowl, and Candlelight's Cooking Pot, take a water bucket
in their recipes. Any grade of fresh water works, since the food that comes out is its own item. Sea
water does not: a recipe with a bucket of sea water does not start.

## Food and drink

| Item | Thirst | Quenched |
|---|---|---|
| Strawberry, Nettle and Ribwort Tea, a cup or the whole jug | 6 | 9 |
| Barley, Onion, Potato and Simple Tomato Soup, Goulash, Farmer Salad | 4 | 5 |
| Corn Grits | 3 | 4 |
| Tomato | 2 | 3 |
| Lettuce, Strawberry | 1 | 2 |
| Candlelight's Tomato Soup, Mushroom Soup, Salad, Beetroot Salad, Fresh Garden Salad | 4 | 5 |
| Candlelight's Tomato Mozzarella Salad | 3 | 4 |
| Candlelight's Chocolate Mousse | 2 | 3 |

A placed tea jug pours two cups, so drinking the jug whole wastes one. Breads, roasts, pasta and other
dry dishes restore no thirst. Every value can be changed in the [config](/docs/configuration).
