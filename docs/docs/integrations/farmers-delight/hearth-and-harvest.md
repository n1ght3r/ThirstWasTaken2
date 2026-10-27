# ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2F8EEEXOzj%2Fe5d9aa8bd6bf5dcbd674f08b92957d4b001229e3.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} Hearth and Harvest

With Hearth and Harvest installed, its juices, milks, wines and stews restore thirst, and its sinks,
jugs, troughs and kegs treat water the way the rest of this mod does.

::: warning Supported versions
Everything on this page works with ![](https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2F8EEEXOzj%2Fe5d9aa8bd6bf5dcbd674f08b92957d4b001229e3.png&trim=1&w=96&h=96&fit=contain&cbg=00000000&output=png){.mod-icon .mod-icon-lg} [Hearth and Harvest](https://modrinth.com/mod/hearth-and-harvest) 1.4.3, on NeoForge, Minecraft 1.21.1,
and on Forge, Minecraft 1.20.1. With 1.0.12c on Forge, only the drinks, the salt recipe and the Jug
apply. The mod has no Fabric build and no build for a newer Minecraft version.
:::

## Drinks and foods

| Item | Thirst | Quenched |
|---|---|---|
| Blueberry, Cherry, Raspberry, Red Grape, Green Grape, Sweet Berry and Glow Berry Juice, Chocolate Milk Bottle, Root Beer, Corn Stew, Onion Soup | 6 | 4 |
| Mead, Hard Cider, Beer | 5 | 2 |
| Goat Milk Bottle, Goat Milk Bucket | 4 | 0 |
| Every wine, per glass | 3 | 1 |
| Moonshine | 2 | 1 |
| Syrup Bottle | 2 | 0 |
| Blueberries, Raspberries, Cherries, Red and Green Grapes, Baked Apple, Caramel Apple | 1 | 0 |

Jams, pickles, cheese, brine and other dry foods restore none. Every value can be changed in the
[config](/docs/configuration#drinks-and-foods).

## Water in its blocks

- **Jugs, Troughs, the Sprinkler and the Stomping Basin** keep the grade of the water poured in. Dirty
  water comes back out Dirty, and sea water stays sea water. A tank holding one grade won't take
  another.
- **A Jug scooping water from the world** gets that water's grade, so a jug filled at the coast holds
  sea water.
- **The Sink** fills itself, as before, but its water is Murky, the same as a kitchen sink from other
  mods. It won't take sea water.
- **A wet sponge** squeezed in a Stomping Basin gives Murky water.
- **Rain** collected in a Trough or a Sprinkler is Clean, like rain in a cauldron.
- **A Sprinkler** filling itself from water beside or below it gets that water's grade.
- **The Keg** keeps the grade of water poured in and drawn back out. Fresh water of any grade
  ferments, and any water makes brine, but sea water won't ferment into mead, beer, moonshine or root beer.

## Salt

In a Cooking Pot, only a bottle of sea water boils down to salt. A bottle of Dirty or Murky fresh water is
boiled Clean instead, as it is without Hearth and Harvest. A water bucket still boils into salt.
