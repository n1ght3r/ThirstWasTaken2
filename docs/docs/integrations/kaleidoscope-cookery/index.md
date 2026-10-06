# Kaleidoscope Cookery

With Kaleidoscope Cookery installed, its teas and soups restore thirst, and water keeps its grade in
the Stockpot and the Teapot.

::: warning Supported versions
Everything on this page works on NeoForge, Minecraft 1.21.1, and Forge, Minecraft 1.20.1, with
![](https://media.forgecdn.net/avatars/thumbnails/1361/462/64/64/638884307253099520.png){.mod-icon} [Kaleidoscope Cookery](https://modrinth.com/mod/kaleidoscope-cookery) 1.5.1, and on Fabric, every
supported Minecraft version, with
![](https://cdn.modrinth.com/data/Ct11Kuii/819ba69579e76715103825ce28b345781b415393.png){.mod-icon} [Kaleidoscope Cookery Refabricated](https://modrinth.com/mod/kaleidoscope-cookery-refabricated). The
official Fabric build stopped at 1.0.1 and has no Teapot: with it, only the teas and soups count.
:::

![A Teapot on a lit Stove among teacups in a cherry grove, with Jade naming its water Clean](/screenshots/integrations/kaleidoscope-cookery/kaleidoscope-teapot.png)

## Teas and soups

![Kaleidoscope Cookery teas in the hotbar, with the Sakura Fubuki tooltip showing its thirst and quenched droplets](/screenshots/integrations/kaleidoscope-cookery/kaleidoscope-teas.png)

| Item | Thirst | Quenched |
|---|---|---|
| Clay Pot Milk Tea, Butter Tea, Wheat Aroma Oolong Tea, Tieguanyin, Biluochun, Dong Ding Oolong Tea, Sakura Fubuki, Flower Tea, Pork Bone Soup, Seafood Miso Soup, Fearsome Thick Soup, Mutton and Radish Soup, Wild Mushroom Rabbit Soup, Pufferfish Soup, Borscht, Beef Meatball Soup, Chicken and Mushroom Stew, Laba Congee, Donkey Soup, Tomato Beef Brisket Soup | 6 | 4 |
| Beef Noodle, Lamb Hui Noodles, Udon Noodle | 3 | 2 |
| Mystery Tea | 3 | 1 |
| Tomato | 2 | 0 |

Tea is brewed from boiled water, so a cup is safe whatever water went into the Teapot. Dishes eaten
straight off a placed block are solid food and restore no thirst. Every value can be changed in the
[config](/docs/configuration#drinks-and-foods).

## Water in the Stockpot and the Teapot

- A bucket of water poured into a Stockpot or a Teapot and taken back out keeps its grade. Without
  this, both handed back Clean water whatever went in.
- A Teapot picked up with water in it keeps the grade when it is placed again.
- An empty Teapot dipped into water grades it where it lies, the way filling a bucket there does.
- Water dripping into a Teapot from pointed dripstone is Pure, as it is in a cauldron. Only the
  Minecraft 1.20.1 and 1.21.1 builds of Kaleidoscope Cookery let dripstone fill a Teapot.
- The Stockpot and the Teapot take sea water, from a bucket or, for the Teapot, straight from the sea.
  A bucket taken back out is still sea water.
- The Teapot brews no tea from sea water. Tea from it would come out safe, which would make the sea
  drinkable.

With Jade installed, looking at a Stockpot or a Teapot of water shows its grade under the crosshair.
