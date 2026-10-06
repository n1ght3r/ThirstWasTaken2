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

Thirst drains a little all the time, about a point a minute even when standing still, and on top of
that from the same actions as hunger: sprinting, jumping, swimming, mining, attacking and taking
damage. Every 4 points of exhaustion costs one point of quenched, or one point of thirst when the
reserve is empty.

With AppleSkin, an exhaustion strip behind the droplets shows how close the next point is. It
follows AppleSkin's **Food Exhaustion HUD Underlay** setting.

Riding a horse, boat or minecart costs nothing. Creative and spectator players are not affected.

| Where | Effect |
|---|---|
| Hotter biome | Drains faster, a little more with every step up in temperature |
| Dry biome, where it never rains | Drains faster |
| Colder biome | Drains slower |
| The Nether, or any dimension where water evaporates | Drains three times as fast |

- With [Cold Sweat](/docs/integrations/cold-sweat#climate) installed (NeoForge 1.21.1, Forge 1.20.1), hot and cold
  follow the temperature Cold Sweat shows around the player instead of the biome's.
- With [Serene Seasons](/docs/integrations/serene-seasons) installed, summer drains faster and winter
  slower, and tropical biomes drain faster in their dry season.
- Fire Resistance halves the drain. Fire Protection slows it further, down to a quarter.
- Nausea adds extra drain while it lasts.
- [Parched](/docs/features/water-purity#parched) adds its own drain, whatever the climate, and the
  droplets turn sandy. Sea water causes it.
- On Peaceful the bar refills on its own, unless the server turns that off.

Every rate is a setting. See [Configuration](/docs/configuration#thirst).

## Healing

Health comes back two ways, and each needs the other bar:

- **Food** heals as in vanilla while the thirst bar is at least half full. Below that, hunger stays
  full but health does not come back, and no food is spent on it.
- **Quenched** heals on its own while thirst is full and the food bar is at least half full, at half
  the speed saturation heals, spending quenched as it goes. Saturation goes first: quenched only heals
  once the food's saturation has run out, so a big meal is spent before the water reserve.

[Upset Stomach](/docs/features/water-purity#upset-stomach) always stops both. The `naturalRegeneration` game rule still applies. The speed and both
halves can be changed, see [Configuration](/docs/configuration#dehydrationhaltshealthregen).

## Running low

- At 6 or below, players cannot start sprinting.
- Below half the thirst bar, food stops healing. Below full, quenched does.

## Hitting zero

An empty bar deals half a heart every two seconds, through armour. The death message reads
`died from dehydration`.

On Easy the damage stops at five hearts. On Normal and Hard it can kill.
