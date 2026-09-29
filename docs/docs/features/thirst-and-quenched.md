# Thirst and Quenched

![The thirst bar above the food bar, part drained](/screenshots/hud/thirst-food-bars.png)

## The two numbers

- **Thirst** runs from 0 to 20, drawn as ten droplets. New players start full.
- **Quenched** is a reserve on top of thirst, like saturation on top of hunger. It starts at 5 and is
  spent before thirst.

The droplets shake when the reserve is empty, like the hunger bar.

With AppleSkin installed, quenched shows as an outline over the droplets. The colour can be changed
or turned off in [Configuration](/docs/configuration#appleskinquenchedoverlay).

![The thirst bar with AppleSkin, cycling through the Diamond, Ice, Gold, AppleSkin and Legacy quenched outlines](/screenshots/hud/hud-appleskin.gif)

## What drains it

Thirst drains from the same actions as hunger: sprinting, jumping, swimming, mining, attacking and
taking damage. Every 4 points of exhaustion costs one point of quenched, or one point of thirst when
the reserve is empty.

With AppleSkin, an exhaustion strip behind the droplets shows how close the next point is. It
follows AppleSkin's **Food Exhaustion HUD Underlay** setting.

Riding a horse, boat or minecart costs nothing. Creative and spectator players are not affected.

| Where | Effect |
|---|---|
| Hot or dry biome | Drains faster |
| Cold or rainy biome | Drains slower |
| The Nether, or any dimension where water evaporates | Drains much faster |

- With [Cold Sweat](/docs/integrations/cold-sweat#climate) installed (NeoForge 1.21.1), hot and cold
  follow the temperature Cold Sweat shows around the player instead of the biome's.
- With [Serene Seasons](/docs/integrations/serene-seasons) installed, summer drains faster and winter
  slower, and tropical biomes drain faster in their dry season.
- Fire Resistance halves the drain. Fire Protection slows it further, down to a quarter.
- Nausea adds extra drain while it lasts.
- Parched adds extra drain while it lasts, and the droplets turn sandy. Sea water causes it.
- On Peaceful the bar refills on its own, unless the server turns that off.

Every rate is a setting. See [Configuration](/docs/configuration#thirst).

## Healing

While thirst is full, quenched heals you the way saturation does, at half the speed, and
spends quenched to do it. It stacks with healing from food, so with both bars full you heal faster.
It needs at least 5 food shanks, and follows the `naturalRegeneration` game rule. The speed and
the food it needs are settings, see [Configuration](/docs/configuration#quenchedhealthregen).

## Running low

- At 6 or below, players cannot start sprinting.
- Natural healing stops until thirst reaches 19. It then runs slower than usual. Food is not spent
  on healing that did not happen.

## Hitting zero

An empty bar deals half a heart every two seconds, through armour. The death message reads
`died from dehydration`.

On Easy the damage stops at five hearts. On Normal and Hard it can kill.
