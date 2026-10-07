# ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FqMxbM4BQ%2F0d6f967d3ad184dd296c62a9891e2b2b7d45f61d.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} Miner's Delight

With Miner's Delight installed, its soups and milk cup restore thirst, and its copper cups treat water
the way a bucket does.

::: warning Supported versions
Everything on this page works on NeoForge, Minecraft 1.21.1, with
![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FqMxbM4BQ%2F0d6f967d3ad184dd296c62a9891e2b2b7d45f61d.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Miner's Delight](https://modrinth.com/mod/miners-delight) 1.4.5, and on Forge, Minecraft 1.20.1, with
1.20.1-1.4.5-backport. The mod has no Fabric build and no build for a newer Minecraft version.
:::

## Drinks and foods

| Item | Thirst | Quenched |
|---|---|---|
| Cave Soup, Bat Soup, Insect Stew | 6 | 4 |
| Milk Cup | 4 | 0 |
| Every soup and stew cup | 3 | 2 |

A cup of soup is half a bowl, so it restores half as much. The cave foods, plates and sandwiches restore
none. Every value can be changed in the [config](/docs/configuration#drinks-and-foods).

## Copper cups

A Water Cup holds a bucket of water and keeps its grade, like a water bucket. It is not a drink.

- Scooped from a source, it gets that water's grade. A cup filled from the sea holds sea water.
- Drawn from a cauldron, it gets the cauldron's grade, and poured back into a cauldron it gives it.
- With Create on Forge 1.20.1, a Spout filling a cup and an Item Drain emptying one keep the grade
  too. The mod's 1.21.1 build has no working Create recipes for its cups.

Without this, a cup of sea water poured into a cauldron came out as Clean water.
