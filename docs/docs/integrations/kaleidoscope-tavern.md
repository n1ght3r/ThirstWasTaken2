# ![](https://cdn.modrinth.com/data/r9RZvhiJ/b96860512d16e72dcaa30b15cb0b9b25c18a38e8.png){.mod-icon} Kaleidoscope Tavern

With Kaleidoscope Tavern installed, its wines, cocktails and juices restore thirst, and water keeps its
grade in the Barrel, through the Tap and in a Water Bottle put down as a block.

::: warning Supported versions
NeoForge, Minecraft 1.21.1, and Forge, Minecraft 1.20.1, with
![](https://cdn.modrinth.com/data/r9RZvhiJ/b96860512d16e72dcaa30b15cb0b9b25c18a38e8.png){.mod-icon} [Kaleidoscope Tavern](https://modrinth.com/mod/kaleidoscopetavern) 1.2.0, and Fabric, every
supported Minecraft version, with
![](https://cdn.modrinth.com/data/UMblNdlF/bfeeabb4b3f926a0afbc953490c32892a0999122.png){.mod-icon} [Kaleidoscope Tavern Refabricated](https://modrinth.com/mod/kaleidoscope-tavern-refabricated).
Kaleidoscope Tavern has no NeoForge version for newer Minecraft versions yet. Kaleidoscope Tavern
Refabricated 1.2.0.5 for Minecraft 1.21.11 crashes the game on launch on its own, with or without
ThirstWasTaken2.
:::

## Drinks

| Item | Thirst | Quenched |
|---|---|---|
| Watermelon Juice, Grape Bucket, Ice Grape Bucket, Gold Grape Bucket, Green Grape Bucket, Sweet Berries Bucket, Glow Berries Bucket | 6 | 4 |
| Honey Wine | 5 | 2 |
| Wine, Champagne, Carignan, Sakura Wine, Plum Wine, Ice Wine, Polaris Sweet White, Red Queen, Riesling Dry White, Sunset Glow, Madame Shexiang, Sweet Berry Wine, Sherry, Mother Snow, Luminous Bride, Glowflower Brew, Sauvignon Blanc Dry White, Miner's Star | 3 | 1 |
| White Lady, Emerald, Brass Heart, Godfather, Grasshopper, Screwdriver, Mojito, Allium Garden, Depth Charge, Nether Special, Bloody Mary, Sculk Special, Signature Cocktail | 3 | 1 |
| Mystery Cocktail | 2 | 0 |
| Grape, Ice Grape, Gold Grape, Green Grape | 2 | 0 |
| Vodka, Whiskey, Rum, Brandy, Vinegar | 0 | 0 |

The stronger a drink, the less it restores: a spirit takes as much water as it gives. The brew level
changes a drink's effects, not its thirst. Wine, spirits and cocktails are safe whatever water went
into them. Every value can be changed in the [config](/docs/configuration#drinks-and-foods).

## Water in the Barrel

- A bucket of water poured into the Barrel and taken back out keeps its grade. Two grades poured
  together keep the worse one, as in a cauldron.
- The Barrel won't take sea water. It would otherwise ferment it into rum as safe as any other.
- On Fabric, the Barrel won't take Waterskins, Canteens, Flasks or terracotta bowls, which the Fabric
  version of Kaleidoscope Tavern would hand back as a bucket. Buckets still work. On NeoForge and
  Forge they go in and come out like a bucket.

## The Tap

- A Tap on a water cauldron fills the cauldron or the Empty Bottle below with water of the same grade.
- A Tap on a waterlogged block, such as a slab, draws the water around it, graded where it lies. In the
  sea that is sea water.
- With Kaleidoscope Chinese Food installed, a Tap filling a Kaleidoscope Cookery Stockpot or Teapot
  passes on the grade the same way.

## Bottles and the Shaker

- A Water Bottle put down as a block and picked up or broken again keeps its grade. A sea-water bottle
  comes back salty.
- The Shaker won't take a sea-water bottle. Fresh water of any grade goes in.

With Jade installed, looking at any part of a Barrel of water shows its grade under the crosshair.
