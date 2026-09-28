# Serene Seasons

![Serene Seasons](https://cdn.modrinth.com/data/cached_images/ba72bd7e14454054eda390d3a1e42c0be51a810e.png)

With Serene Seasons installed, thirst follows the season: summer makes players thirstier, winter less so.

::: tip Supported versions
Works on Fabric and NeoForge, on every Minecraft version this mod supports, with
[Serene Seasons](https://modrinth.com/mod/serene-seasons) and the GlitchCore it needs.
:::

## Thirst through the year

In most biomes, each season has its own drain speed. The speed changes a little every day, so there is
no sudden jump when a new season starts.

| Season | Drain speed |
|---|---|
| Spring | Normal |
| Summer | 15% faster |
| Autumn | Normal |
| Winter | 10% slower |

The speeds are for the middle of each season. They stack with the biome's own heat and cold, described
in [Thirst and Quenched](/docs/features/thirst-and-quenched).

## Tropical biomes

Jungles, savannas, deserts, badlands and the other biomes Serene Seasons treats as tropical have a wet
and a dry season instead of four. In the middle of the dry season they count as dry, which drains
thirst faster, and in the middle of the wet season as rainy, which drains it slower. The season speeds
above do not apply to them, since Serene Seasons never warms or cools them.

## Rain

In winter, rain falls as snow, so Hanging Pots and cauldrons stop collecting rainwater until spring.
In the middle of the dry season, tropical biomes get no rain at all. Find a lake or a river, or store
water before the season turns.

## Settings

The Seasons tab of the Thirst page appears when Serene Seasons is installed. It holds
[`sereneSeasonsClimate`](/docs/configuration#sereneseasonsclimate), which turns all of this off, and
the four season speeds.

![The Thirst settings page on its Seasons tab: Serene Seasons Climate on, and the Spring, Summer, Autumn and Winter Drain sliders at 100%, 115%, 100% and 90%](/screenshots/config/config-seasons.png)

With [Cold Sweat](/docs/integrations/cold-sweat) on NeoForge 1.21.1, thirst already follows the
temperature Cold Sweat shows, and that temperature follows the season. The season speeds are not
applied a second time. The wet and dry seasons still count.
