# Water quality

Fresh water has a grade, from Dirty to Pure. Sea water is Salty, has no grade, and never quenches
thirst. Every container shows which one it holds in its tooltip.

![A water bottle tooltip stepping through Dirty, Murky, Clean, Pure and Salty](/screenshots/water/water-tooltips.gif)

## The four grades

From worst to best: **Dirty**, **Murky**, **Clean**, **Pure**. Water gets its grade where it is
collected and keeps it.

| Grade | Usually found in | Drinking it |
|---|---|---|
| **Dirty** | Swamps, jungles, savannas, badlands | Often makes you ill, no quenched |
| **Murky** | Rivers and most other biomes | Can make you ill, half quenched |
| **Clean** | Mountains | Rarely makes you ill |
| **Pure** | Cold peaks | Always safe |
| **Salty** | Oceans and beaches | Never quenches thirst, gives [Parched](#parched) |

How likely each grade is to make you ill is under [Drinking bad water](#drinking-bad-water).

![](https://cdn.modrinth.com/data/8oi3bsk5/1959d924a1088944bbf07a06ba523726112d7e7a_96.webp){.mod-icon} [Terralith](https://modrinth.com/mod/terralith)'s Orchid Swamp and Ice Marsh count as swamps, so their water is Dirty.

- Hot biomes make water worse, cold biomes make it better.
- Water above y 100 or below y 32 is a little cleaner. So is flowing water.
- Mud, mangrove roots, farmland or a composter within two blocks make water worse.

Modpacks can add biomes to the `thirstwastaken2:stagnant_water` tag, coastal biomes whose water is
salty to the `thirstwastaken2:sea_water` tag, and fluids that are always Pure to the
`thirstwastaken2:pure_water` fluid tag. Water with no grade of its own
uses [defaultPurity](/docs/configuration#defaultpurity).

## Checking water with Jade

![Jade showing Murky for the river water under the crosshair](/screenshots/water/jade-water.png)

With ![](https://cdn.modrinth.com/data/nvQzSEkH/b04217bc2b7dc524c4d12f81ff42cc1cefb9b0fc_96.webp){.mod-icon} [Jade](https://modrinth.com/mod/jade) installed, looking at water, a waterlogged block, a water
cauldron or a hanging pot shows its grade, or Salty. On a [Copper Distiller](#copper-distiller) it
shows both tanks and whether the cooling tub is dry. Each can be turned off in Jade's plugin settings.

## Salt water

Oceans and beaches give salt water. It has its own icon and tooltip line. On Minecraft 1.21 and
1.21.1 only the bowl has its own icon.

- Drinking it costs thirst, causes eight seconds of Nausea and 30 seconds of Parched II.
- It cannot be boiled clean. Only a [Copper Distiller](#copper-distiller) makes it drinkable.
- One salty drink makes a whole waterskin, cauldron or hanging pot salty.

### Parched

![Parched effect icon](/icons/parched.png){.effect-icon}

Parched makes thirst drain faster, the way Hunger does for food, and turns the thirst bar the colour
of dry sand.

![The thirst bar in dry sand colours while Parched](/screenshots/hud/parched-hud.png)

## Mixing and cauldrons

- A waterskin takes the average grade of its drinks, rounded down.
- A cauldron keeps the worse grade of what it holds and what is poured in.

A cauldron also fills on its own:

| How it filled | Grade |
|---|---|
| Rain | Clean |
| A pointed dripstone dripping into it | Pure |

Neither improves water already in the cauldron.

## Drinking bad water

Fresh water always quenches thirst. The grade sets the risk, and harder difficulties make it worse.
Pure water is always safe.

Bad water fills the thirst bar but does not last. Dirty water gives no quenched and Murky water half,
so thirst starts dropping again soon after, the way rotten flesh gives almost no saturation.

Dirty and Murky water taste bad: every drink gives seven seconds of Nausea, even on Peaceful. Then each
effect below has its own chance, so one drink can give both, or neither.

| Chance per drink | Dirty | Murky | Clean |
|---|---|---|---|
| Peaceful | none | none | none |
| Easy | 65% Upset Stomach I, 25% Poison | 35% Upset Stomach I, 10% Poison | 5% Upset Stomach I, 3% Poison |
| Normal | 75% Upset Stomach II, 35% Poison | 50% Upset Stomach I, 18% Poison | 12% Upset Stomach I, 5% Poison |
| Hard | 78% Upset Stomach II, 45% Poison | 66% Upset Stomach II, 30% Poison | 20% Upset Stomach I, 10% Poison |

Clean water makes the player ill for less time than Dirty or Murky water, as the tables below show.

Drinking bad water again while an effect from it is still on adds its time again, up to twice as long.
The stronger level is kept. This can be switched off with
[extendSicknessEffects](/docs/configuration#extendsicknesseffects).

Every effect, chance, duration and level can be changed for each difficulty and grade, and any effect
added, with [sicknessEffects](/docs/configuration#sicknesseffects).

### Upset Stomach

![Upset Stomach effect icon](/icons/upset-stomach.png){.effect-icon}

The common one. It never hurts on its own.

- Thirst drains faster, twice as fast at level II.
- The screen warps now and then, about once a minute at level I and twice at level II.
- Food fills less saturation, and drinks less quenched: three quarters at level I, half at level II.
- The thirst bar turns green while it lasts.

| | Easy | Normal | Hard |
|---|---|---|---|
| From Dirty or Murky water | 45 seconds | 60 seconds | 90 seconds |
| From Clean water | 20 seconds | 30 seconds | 45 seconds |

![The thirst bar in green while the player has Upset Stomach](/screenshots/hud/upset-stomach-hud.png)

### Poison

A bad batch. Milk cures it.

| | Easy | Normal | Hard |
|---|---|---|---|
| From Dirty or Murky water | 10 seconds | 20 seconds | 30 seconds |
| From Clean water | 5 seconds | 8 seconds | 12 seconds |

Poison stops at half a heart, so it never kills.

## Cleaning fresh water

Put a water bottle, terracotta water bowl or water bucket in a furnace, a smoker or on a campfire.

![A dirty water bottle comes out of the furnace clean](/screenshots/water/furnace-clean-water.png)

| In | Out |
|---|---|
| Dirty | Clean |
| Murky | Pure |
| Clean | Pure |

A furnace takes ten seconds, a smoker five, a campfire thirty. An Iron Flask works in a furnace
too, however full it is. Other ways to clean water:

- A [Copper Canteen or Iron Flask](#boiling-in-a-canteen-or-flask) boils its water Pure over a
  campfire.
- A [Hanging Pot](#copper-hanging-pot) boils a whole bucket Pure.
- A [Copper Distiller](#copper-distiller) turns any water Pure, sea water included.
- The Farmer's Delight [Cooking Pot](/docs/integrations/farmers-delight/#boiling-water-in-the-cooking-pot)
  makes bottles and bowls Pure in one pass.
- The Create [Sand Filter](/docs/integrations/create#sand-filter) cleans water pumped through it.

## Boiling in a canteen or flask

Hold use on a lit campfire or soul campfire with a [Copper Canteen or Iron Flask](/docs/features/drinking#copper-canteen-and-iron-flask)
that holds water. The progress shows above the hotbar, and when it is done all the water inside is
Pure.

| | Per drink | Full |
|---|---|---|
| Copper Canteen | 3 seconds | 12 seconds |
| Iron Flask | 4 seconds | 24 seconds |

- Letting go keeps the progress. Adding water starts it over.
- Salt water never boils clean.
- Only the Iron Flask goes in a furnace. It raises the water two grades, like a bottle.

## Copper Hanging Pot

![A Copper Hanging Pot of water boiling over a campfire](/screenshots/water/copper-hanging-pot.png)

Placed on a lit campfire or soul campfire, it boils water into Pure water. Placed on the ground, it
stands a block up on its legs, so a campfire can go under it later. Putting any other solid block
under it knocks the pot off.

![A Copper Hanging Pot standing on the grass on its own legs, with room for a campfire under it](/screenshots/water/copper-hanging-pot-ground.png)

![Two sticks and a chain across the top, five copper ingots in a U below, make a Copper Hanging Pot](/screenshots/recipes/copper-hanging-pot-recipe.png)

On Minecraft 1.21 and 1.21.1 the recipe uses a chain instead of an iron chain.

- Holds three servings, like a cauldron. A bucket fills or empties it. A bottle or bowl adds or takes
  one.
- A waterskin fills up from it in one go, as far as the pot has water. Sneak to pour all of it in.
- Each serving takes 4 seconds.
- Adding water only adds that water's time. Putting the fire out pauses the boil.
- The water changes colour with its grade.
- It mixes like a cauldron, and salt water never boils clean.
- Rain fills it slowly.
- Water cannot be poured into it in the Nether.
- Breaking it drops the pot. The water is lost.

### Iron Hanging Pot

![An Iron Hanging Pot of water boiling over a campfire](/screenshots/water/iron-hanging-pot.png)

Works like the Copper Hanging Pot but boils slower: 6 seconds a serving.

![Two sticks and a chain across the top, five iron ingots in a U below, make an Iron Hanging Pot](/screenshots/recipes/iron-hanging-pot-recipe.png)

## Copper Distiller

A two-block still that turns any water into Pure water, sea water included. It burns furnace fuel
and runs on its own, so hoppers can keep it going.

![A Copper Distiller up close, its fire burning: the boiler on its brick firebox, the swan neck pipe running over to the cooling tub, and the tap over the basin](/screenshots/water/copper-distiller-front.png)

It is made of four parts: a Brick Firebox, a Distiller Boiler, a Cooling Tub and a Copper Pipe.

![Three copper ingots in a row make four Copper Pipes](/screenshots/recipes/copper-pipe-recipe.png)

![Seven copper ingots in a ring round a gold ingot, a Copper Pipe in the middle of the top row, make a Distiller Boiler](/screenshots/recipes/distiller-boiler-recipe.png)

![Five Copper Pipes around a barrel, a cauldron below, make a Cooling Tub](/screenshots/recipes/cooling-tub-recipe.png)

![Three smooth stone slabs on top, a campfire in a ring of bricks below, make a Brick Firebox](/screenshots/recipes/brick-firebox-recipe.png)

Craft the four together into a whole distiller, or build it in the world:

![The Distiller Boiler beside a Copper Pipe, the Brick Firebox under the boiler and the Cooling Tub under the pipe, make a Copper Distiller](/screenshots/recipes/copper-distiller-recipe.png)

1. Place the Distiller Boiler on top of the Brick Firebox.
2. Place the Cooling Tub to its right, as seen from the front, facing the same way.
3. Use a Copper Pipe on either of them to join the two.

Before it works, fill the cooling tub once with any water container. Sea water will do. Without it
nothing condenses, and the arrow in the distiller's screen turns red.

![The Copper Distiller's screen: an emptied bucket in the water slot, coal burning, sea water in the boiler tank, Pure water in the basin tank, and a bottle filled from it](/screenshots/water/copper-distiller-gui.png)

- Right-click with an empty hand to open its screen: water in, fuel, an empty container to fill, and
  the two tanks.
- Right-click with a water container to pour it into the boiler, and with an empty one to draw
  Pure water from the basin under the tap. A waterskin, canteen or flask draws on a click and pours
  when sneaking.
- Each serving takes 8 seconds while the fire burns. Coal runs ten servings. The fire only burns
  while there is water to boil and room in the basin, so no fuel is wasted.
- The boiler and the basin each hold 9 servings, three buckets.
- Hoppers feed water in from above and fuel or empty containers from the sides, and take filled
  containers and salt out from below.
- Breaking it drops the distiller and whatever its slots held. The water in its tanks is lost.

With Jade installed, looking at it shows what each tank holds and its grade.

![Jade on a Copper Distiller: two drinks of Salty water in the boiler and four of Pure water in the basin](/screenshots/water/copper-distiller.png)

### Salt

When another mod in the pack adds salt, such as Croptopia, Spelunkery, Expanded Delight or Hearth
and Harvest, every bucket of sea water distilled leaves one of that salt behind. The distiller then
shows a salt slot. Without such a mod there is no salt and no slot. When the salt slot is full, the
sea water waits until it is emptied.
