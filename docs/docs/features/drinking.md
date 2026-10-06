# Drinking

## What is worth drinking

| Item | Thirst | Quenched |
|---|---|---|
| Any potion, water bottles included | 6 | 8 |
| Milk bucket | 6 | 8 |
| Beetroot soup | 5 | 7 |
| Honey bottle | 4 | 6 |
| Melon slice, mushroom stew, rabbit stew | 4 | 5 |
| Terracotta water bowl | 4 | 5 |
| Waterskin, Copper Canteen or Iron Flask, per drink | 4 | 5 |
| Apple, golden apple, enchanted golden apple | 2 | 3 |
| Carrot, golden carrot, beetroot, sweet berries, glow berries | 1 | 2 |

- Thirst past a full bar becomes quenched.
- Plain water cannot be drunk while the bar is full. Potions and food are not blocked.
- Drinks from other mods restore thirst when their mod marks them as drinks, or when a mod or
  [data pack](/docs/developers/data-packs) gives them a value.
- [Farmer's Delight](/docs/integrations/farmers-delight/) has its own values.
- Any item can be given a value in the [config](/docs/configuration#drinks-and-foods).

With AppleSkin installed, tooltips show the values as droplets. Each droplet is two points: filled
droplets for thirst, outlined ones for quenched.

Every item the mod adds is in its own creative tab, and every recipe shows in the recipe book.

## Bowls

1. Three clay balls in a bowl shape make four **clay bowls**.
2. Smelt a clay bowl into a **terracotta bowl**.
3. Use the terracotta bowl on water to fill it. Flowing water works too.

![Three clay balls in a bowl shape make four clay bowls](/screenshots/recipes/clay-bowl-recipe.png)

![A clay bowl firing into a terracotta bowl in a furnace](/screenshots/recipes/furnace-terracotta-bowl.png)

Drinking gives the empty bowl back. A terracotta bowl and a water bucket also craft a water bowl,
but that water is always Dirty.

Glass bottles work as in vanilla: they fill only from a water source block, never from flowing
water. The terracotta bowl, waterskin, Copper Canteen and Iron Flask fill from either.

## Waterskin

![The waterskin recipe uses three leather and one string](/screenshots/recipes/waterskin-recipe.png)

- Holds three drinks. Each restores 4 thirst and 5 quenched.
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

![Leather on top and five copper ingots in a U below make a Copper Canteen](/screenshots/recipes/copper-canteen-recipe.png)

![An iron nugget on top and five iron ingots in a U below make an Iron Flask](/screenshots/recipes/iron-flask-recipe.png)

Hold use on a lit campfire to boil the water inside Pure. The Iron Flask can also go in a furnace.
Times are on the [water purity page](/docs/features/water-purity#boiling-in-a-canteen-or-flask).

## Drinking by hand

Sneak and use an empty hand on water. Each click is one sip. The water keeps its grade, so swamp
water is still risky. It restores less than a bowl. It can be turned off with
[canDrinkByHand](/docs/configuration#candrinkbyhand).

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
| Boil Your Water | Purify water in a furnace or smoker |
| Pure Water | Drink Pure water |
| Salt Water | Drink sea water |
| Dry Heat | Drink in the Nether, or any dimension that boils water away |
