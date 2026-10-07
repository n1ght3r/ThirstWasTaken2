# Drinking

## What is worth drinking

A drink of water restores 6 thirst, whatever holds it: a bottle, a bowl, a waterskin, a canteen or a
flask. How much quenched it gives depends on its [grade](/docs/features/water-purity).

| Water | Thirst | Quenched |
|---|---|---|
| Dirty | 6 | 0 |
| Murky | 6 | 1 |
| Clean | 6 | 2 |
| Pure | 6 | 4 |

Pure water gives a bigger reserve, not more thirst. Clean and Pure water are both safe.

| Item | Thirst | Quenched |
|---|---|---|
| Any other potion | 6 | 8 |
| Beetroot soup, mushroom stew, rabbit stew | 6 | 4 |
| Milk bucket | 4 | 0 |
| Honey bottle, apple, golden apple, enchanted golden apple, melon slice | 2 | 0 |
| Carrot, golden carrot, beetroot, sweet berries, glow berries | 1 | 0 |

Only water and prepared drinks and soups build a reserve. Solid food helps a little with thirst, never
with quenched.

- Thirst past a full bar is lost, so drinking early wastes some of it.
- Plain water cannot be drunk with a full bar, except Clean or Pure water while quenched is not yet
  full, to build up a reserve before a trip. Potions and food are not blocked.
- A drink takes about a second and a half, as long as a water bottle.
- Drinks from other mods restore thirst when their mod marks them as drinks, or when a mod or
  [data pack](/docs/developers/data-packs) gives them a value.
- [Farmer's Delight](/docs/integrations/farmers-delight/) has its own values.
- Any item can be given a value in the [config](/docs/configuration#drinks-and-foods).

With AppleSkin installed, tooltips show the values as droplets. Each droplet is two points: filled
droplets for thirst, outlined ones for quenched.

Every item the mod adds is in its own creative tab, and every recipe shows in the recipe book.

## Bowls

1. Three clay balls in a bowl shape make three **clay bowls**.
2. Smelt a clay bowl into a **terracotta bowl**.
3. Use the terracotta bowl on water to fill it. Flowing water works too.

![Three clay balls in a bowl shape make three clay bowls](/screenshots/recipes/clay-bowl-recipe.png)

![A clay bowl firing into a terracotta bowl in a furnace](/screenshots/recipes/furnace-terracotta-bowl.png)

Drinking gives the empty bowl back. Filled water bowls stack to three, as long as their water is the
same, and drinking from a stack gives back one empty bowl and keeps the rest. Water bottles don't
stack, as in vanilla. A terracotta bowl and a water bucket also craft a water bowl, but that water is always Dirty.

Glass bottles work as in vanilla: they fill only from a water source block, never from flowing
water. The terracotta bowl, waterskin, Copper Canteen and Iron Flask fill from either.

## Waterskin

![Four leather in a ring make a Waterskin](/screenshots/recipes/waterskin-recipe.png)

- Holds four drinks, one more than a stack of bowls, with no empties to carry.
- Use it on water to fill it in one go. From a water cauldron it takes as many drinks as the
  cauldron holds, up to full.
- In the inventory, right-click it with a water bottle to add one drink, or with a water bucket to
  fill it. The empty container is returned.
- Sneak and use it on a block to pour it out, with the same splash as pouring a water bottle.
- Mixed water takes the average grade, rounded down. One salty drink makes all of it salty.
- Pipes and tanks from other mods can fill and empty it, one whole drink at a time.

## Copper Canteen and Iron Flask

Metal versions of the waterskin that can boil their own water. They fill, pour and mix exactly like
the waterskin, and a bar under the icon shows how full they are.

| | Holds |
|---|---|
| Copper Canteen | 4 drinks |
| Iron Flask | 6 drinks |

![String on top and three copper ingots below make a Copper Canteen](/screenshots/recipes/copper-canteen-recipe.png)

![An iron nugget on top and three iron ingots below make an Iron Flask](/screenshots/recipes/iron-flask-recipe.png)

Hold use on a lit campfire to boil the water inside Clean. Both can also go in a furnace. Times are
on the [water quality page](/docs/features/water-purity#boiling-in-a-canteen-or-flask).

## Drinking by hand

Sneak and use an empty hand on water. Each click is one sip of 3 thirst, half a serving, with quenched
cut by the grade the same way. The water keeps its grade, so swamp water is still risky. It can be
turned off with [canDrinkByHand](/docs/configuration#candrinkbyhand).

## Finding water

Water bottles, Clean or Pure, turn up one to three at a time in:

- Abandoned mineshaft, dungeon, shipwreck supply, nether bridge and bastion chests.
- Piglin bartering, more rarely.

Desert pyramid chests and desert and savanna village houses hold one to three bottles too, but that
water is Dirty or Murky. Boil it before drinking.

Villagers trade for water as well. Some novice Leatherworkers sell an empty Waterskin for 3 emeralds,
and some novice Clerics sell a Clean water bottle for 1 emerald.

## Advancements

| Advancement | How to earn it |
|---|---|
| ThirstWasTaken2 | Start playing |
| First Sip | Drink water for the first time |
| Dirty Water | Drink Dirty water |
| Boil Your Water | Boil water in a furnace or smoker, or make a hanging pot, canteen or flask |
| Pure Water | Drink Pure water |
| Salt Water | Drink sea water |
| Dry Heat | Drink in the Nether, or any dimension that boils water away |
