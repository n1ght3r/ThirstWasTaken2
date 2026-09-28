# Create

With Create installed, the mod adds the Sand Filter, and water keeps its grade through Create's
pipes, pumps, Spouts and drains.

::: warning Supported versions
- Fabric, Minecraft 26.2 and 26.1.2: [Create Fly](https://modrinth.com/mod/create-fly).
- NeoForge, Minecraft 1.21.1: [Create](https://modrinth.com/mod/create) 6.0.10.
- Forge, Minecraft 1.20.1: [Create](https://modrinth.com/mod/create) 6.0.8.

Other versions ignore Create.
:::

## Sand Filter

![Engineer's Goggles showing Murky water entering the Sand Filter and Clean water leaving it](/screenshots/integrations/create/create-sand-filter-goggles.png)

Water pumped into the top comes out of the bottom one grade cleaner. Pure stays Pure.

- Filters a bucket every five seconds and holds one bucket on each side.
- Only water goes in. Salt water passes through unchanged.
- Nothing flows through the sides.
- The filter waits while the bottom holds water of a different grade.
- A comparator reads how full the bottom is.
- Engineer's Goggles show both sides and their grades.

![Sand, a Nozzle and a Fluid Tank in a column make a Sand Filter](/screenshots/integrations/create/sand-filter-recipe.png)

The recipe unlocks once a Nozzle is picked up.

## Water grades in Create

- A pump or Hose Pulley grades water from the world like a bottle would. A cauldron gives the grade
  it holds.
- Pouring water into a Basin, Item Drain or tank keeps its grade.
- Filling a bottle or bucket from a Spout or tank gives it the water's grade.
- A Spout fills a waterskin in one go, and a terracotta bowl too. An Item Drain empties both. The
  water keeps its grade, and a waterskin only takes water of the grade it already holds.
- Water of different grades does not mix in one tank or pipe.
