[![modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_64h.png)](https://modrinth.com/mod/thirst-was-taken-2)
[![curseforge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_64h.png)](https://www.curseforge.com/minecraft/mc-mods/thirst-was-taken-2)
[![ghpages](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/ghpages_64h.png)](https://n1ght3r.github.io/ThirstWasTaken2/)

## Thirst and Quenched
* **Thirst Bar:** Depletes as you run, jump, mine, and fight.
* **Quenched Buffer:** Works like saturation. It drains first, and [heals](https://n1ght3r.github.io/ThirstWasTaken2/docs/configuration#quenchedhealthregen) you while thirst is full.
* **Dehydration:** Low thirst prevents sprinting and natural health regeneration. Empty thirst causes steady damage.
* **Environment:** Hot biomes like deserts and the Nether deplete thirst faster. Fire Resistance and Fire Protection reduce heat drain.

<div align="center">
<table>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/hud/thirst-food-bars.png" alt="The thirst bar above the hunger bar" width="372"></td>
    <td align="center"><img src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/water/water-tooltips.gif" alt="A water bottle tooltip showing each grade" width="272"></td>
  </tr>
</table>
</div>

## Water Quality
| Grade | Common Sources | Effects |
| :--- | :--- | :--- |
| **Dirty** | Swamps, stagnant pools | Often causes [Upset Stomach](https://n1ght3r.github.io/ThirstWasTaken2/docs/features/water-purity#upset-stomach) or [Poisoning](https://n1ght3r.github.io/ThirstWasTaken2/docs/features/water-purity#poisoning), worse on harder difficulties |
| **Murky** | Standard rivers, lakes, caves | Can cause [Upset Stomach](https://n1ght3r.github.io/ThirstWasTaken2/docs/features/water-purity#upset-stomach) or [Poisoning](https://n1ght3r.github.io/ThirstWasTaken2/docs/features/water-purity#poisoning) |
| **Clean** | Mountain rivers, deep aquifers, boiled water | Rarely makes you ill, high hydration |
| **Pure** | Glaciers, rain cauldrons, refined drinks | Completely safe, maximum hydration |
| **Salty** | Oceans and beaches | Cannot quench thirst; makes you [Parched](https://n1ght3r.github.io/ThirstWasTaken2/docs/features/water-purity#parched) |

<div align="center">
<table>
  <tr>
    <th align="center"><img src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/icons/upset-stomach.png" alt="Upset Stomach effect icon" width="36" align="absmiddle"> <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/features/water-purity#upset-stomach">Upset Stomach</a></th>
    <th align="center"><img src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/icons/parched.png" alt="Parched effect icon" width="36" align="absmiddle"> <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/features/water-purity#parched">Parched</a></th>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/hud/upset-stomach-hud.png" alt="The thirst bar in green while Upset Stomach" width="372"></td>
    <td align="center"><img src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/hud/parched-hud.png" alt="The thirst bar in dry sand colours while Parched" width="372"></td>
  </tr>
</table>
</div>

## Purification
* **Boiling:** Smelt water bottles, bowls, or buckets in a furnace or over a campfire to raise their purity grade.
* **Hanging Pots:** Hang a Copper or Iron Hanging Pot over a lit campfire to boil a bucket of water into Pure water.
* **Rain and Dripstone:** Cauldrons placed under open rain or pointed dripstone automatically fill with clean water.

<div align="center">
<table>
  <tr>
    <th align="center">Iron Hanging Pot</th>
    <th align="center">Furnace</th>
  </tr>
  <tr>
    <td align="center"><img src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/water/iron-hanging-pot.png" alt="Iron Hanging Pot boiling water over a campfire" width="300"></td>
    <td align="center"><img src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/water/furnace-clean-water.png" alt="Purifying Water" width="380"></td>
  </tr>
</table>
</div>

## Early Game Gear and Drinking
* **Terracotta Bowls:** Mold clay into bowls and fire them in a furnace to scoop water early on.
* **Waterskin:** Holds 3 servings of water in a single slot. Intelligently mixes water grades.
* **Drink by Hand:** Sneak and right-click any fresh water block to drink directly without a container.

<div align="center">
<table>
  <tr>
    <th align="center">Clay Bowl</th>
    <th align="center">Waterskin</th>
  </tr>
  <tr>
    <td align="center"><img alt="Three clay balls in a bowl shape make four Clay Bowls" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/recipes/clay-bowl-recipe.png" width="280"><br><img src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/recipes/furnace-terracotta-bowl.png" alt="Firing a Clay Bowl into a Terracotta Bowl" width="280"></td>
    <td align="center"><img alt="Three leather and a string make a Waterskin" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/recipes/waterskin-recipe.png" width="280"></td>
  </tr>
</table>
</div>

## Mid Game Gear
* **Copper Canteen:** Holds 4 servings. Hold it over a lit campfire to boil the water inside to Pure.
* **Iron Flask:** Holds 6 servings. Boils slower over a campfire, but also works in a furnace.

<div align="center">
<table>
  <tr>
    <th align="center">Copper Canteen</th>
    <th align="center">Iron Flask</th>
  </tr>
  <tr>
    <td align="center"><img alt="Five copper ingots and a leather make a Copper Canteen" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/recipes/copper-canteen-recipe.png" width="300"></td>
    <td align="center"><img alt="Five iron ingots and an iron nugget make an Iron Flask" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/recipes/iron-flask-recipe.png" width="264"></td>
  </tr>
</table>
</div>

## Mod Compatibility

<table>
  <tr>
    <td width="55%">
      <b><a href="https://www.curseforge.com/minecraft/mc-mods/appleskin">AppleSkin</a></b><br>
      <i>Fabric and NeoForge</i><br><br>
      Displays your Quenched reserve directly on the HUD, with multiple styles available to choose from. Tooltips show exact thirst values for food and drinks.
    </td>
    <td width="45%">
      <img alt="The thirst bar with AppleSkin, cycling through the Diamond, Ice, Gold, AppleSkin and Legacy quenched outlines" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/hud/hud-appleskin.gif" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <b><a href="https://www.curseforge.com/minecraft/mc-mods/jade">Jade (WAILA)</a></b><br>
      <i>Fabric and NeoForge</i><br><br>
      Shows the purity grade of water sources, waterlogged blocks, and cauldrons directly under your crosshair.
    </td>
    <td width="45%">
      <img alt="Jade showing the water grade under the crosshair" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/water/jade-water.png" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <b><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/create">Create</a></b><br>
      <i>26.1.2 and 26.2 Fabric: <a href="https://modrinth.com/mod/create-fly">Create Fly</a><br>
      1.21.1 NeoForge: <a href="https://www.curseforge.com/minecraft/mc-mods/create">Create</a></i><br><br>
      Adds a Sand Filter to purify dirty water by one grade. Water also keeps its purity grade through pipes, pumps, tanks, drains, and spouts.
    </td>
    <td width="45%">
      <img alt="Engineer's Goggles showing Murky water entering the Sand Filter and Clean water leaving it" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/integrations/create/create-sand-filter-goggles.png" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <b><a href="https://www.curseforge.com/minecraft/mc-mods/sophisticated-backpacks">Sophisticated Backpacks</a></b><br>
      <i>NeoForge, every version but 26.3</i><br><br>
      Adds the Drinking Upgrade, which drinks from your backpack when you get thirsty, the cleanest water first. Water keeps its purity grade in the Tank and Pump Upgrades. Also works with <a href="https://www.curseforge.com/minecraft/mc-mods/sophisticated-storage">Sophisticated Storage</a>.
    </td>
    <td width="45%">
      <img alt="A backpack of water with the Advanced Drinking Upgrade's settings open" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/integrations/sophisticated/sophisticated-drinking-upgrade.png" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <b><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/supplementaries">Supplementaries</a></b><br>
      <i>1.21.1 Fabric and NeoForge</i><br><br>
      Water keeps its purity grade in Jars, Goblets and Faucets, and sea water stays sea water. A Jar or a Goblet of water can be drunk straight from the block. Faucets fill and empty hanging pots and grade the water they draw from a lake.
    </td>
    <td width="45%">
      <img alt="Five jars of water on a lakeshore, brown, grey, blue, cyan and turquoise, with Jade naming the middle one Clean" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/integrations/supplementaries/supplementaries-jars.png" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <b><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/kaleidoscope-cookery">Kaleidoscope Cookery</a></b><br>
      <i>Fabric: <a href="https://modrinth.com/mod/kaleidoscope-cookery-refabricated">Kaleidoscope Cookery Refabricated</a><br>
      1.21.1 NeoForge: <a href="https://www.curseforge.com/minecraft/mc-mods/kaleidoscope-cookery">Kaleidoscope Cookery</a></i><br><br>
      Teas, milk tea and soups restore thirst. Water keeps its purity grade in the Stockpot and the Teapot, and the Teapot brews nothing from sea water.
    </td>
    <td width="45%">
      <img alt="A Teapot on a lit Stove among teacups in a cherry grove, with Jade naming its water Clean" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/integrations/kaleidoscope-cookery/kaleidoscope-teapot.png" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <b><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/cold-sweat">Cold Sweat</a></b><br>
      <i>1.21.1 NeoForge</i><br><br>
      Thirst follows the temperature Cold Sweat shows around you, hearths and shade included. Its Waterskin carries a purity grade and quenches thirst, and the Boiler purifies water.
    </td>
    <td width="45%">
      <img alt="Cold Sweat's body temperature gauge between the hearts and the thirst bar, over a snowy taiga" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/integrations/cold-sweat/cold-sweat-hud.png" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/"><img alt="Farmer's Delight" src="https://i.imgur.com/wqSocVR.png" width="272"></a><br>
      <i>Fabric: <a href="https://modrinth.com/mod/farmers-delight-refabricated">Farmer's Delight Refabricated</a><br>
      1.21.1 NeoForge: <a href="https://www.curseforge.com/minecraft/mc-mods/farmers-delight">Farmer's Delight</a></i><br><br>
      Soups, stews, and drinks restore thirst. The Cooking Pot purifies water to Pure grade. The Nourishment effect pauses thirst depletion.
    </td>
    <td width="45%">
      <img alt="A Farmer's Delight Cooking Pot boiling water bottles to Pure" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/integrations/farmers-delight/farmers-delight-cooking-pot.png" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/brewin-and-chewin"><img alt="Brewin' and Chewin'" src="https://i.imgur.com/EFkjwBq.png" width="298"></a><br>
      <i>1.21.1 Fabric and NeoForge</i><br><br>
      Brews and soups restore thirst, less the stronger the drink. Water keeps its purity grade in the Keg, and nothing ferments from sea water.
    </td>
    <td width="45%">
      <img alt="A Brewin' and Chewin' Keg beside barrels on a lakeshore, with Jade naming its water Dirty" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/integrations/brewin-and-chewin/brewin-keg.png" width="100%">
    </td>
  </tr>
</table>

Also works with:

<table>
  <tr>
    <td width="45%"><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/cultural-delights"><img alt="Cultural Delights" src="https://cdn.modrinth.com/data/cached_images/d6a323ce1e69b76f2143b3c0e03bd9ce36fc69e1.png" width="272"></a><br><i>1.21.1 NeoForge</i></td>
    <td width="55%">Its drinks, cucumbers and salads restore thirst, and the Vat brews nothing from sea water.</td>
  </tr>
  <tr>
    <td width="45%"><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/fruits-delight"><img alt="Fruits Delight" src="https://media.forgecdn.net/attachments/765/821/logo2.png" width="272"></a><br><i>1.21.1 NeoForge</i></td>
    <td width="55%">Its juices, teas, jellos, popsicles and juicy fruits restore thirst, and sea water makes no juice.</td>
  </tr>
  <tr>
    <td width="45%"><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/oceans-delight"><img alt="Ocean's Delight" src="https://i.imgur.com/OqgniyH.png" width="272"></a><br><i>1.21.1 Fabric and NeoForge</i></td>
    <td width="55%">Its Guardian Soup, braised sea pickle and seagrass salad restore thirst.</td>
  </tr>
  <tr>
    <td width="45%"><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/expanded-delight"><img alt="Expanded Delight" src="https://wsrv.nl/?url=https%3A%2F%2Fmedia.forgecdn.net%2Fattachments%2F1067%2F753%2Fsome-logo.png&amp;n=-1" width="272"></a><br><i>1.21.1 NeoForge</i></td>
    <td width="55%">Its juices, goat milk, soups and salads restore thirst, and the Cooking Pot cooks nothing from sea water.</td>
  </tr>
  <tr>
    <td width="45%"><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/rustic-delight"><img alt="Rustic Delight" src="https://cdn.modrinth.com/data/cached_images/6ecede4d7053895d6f424894b35007d35a5b883b.png" width="242"></a><br><i>Fabric and 1.21.1 NeoForge</i></td>
    <td width="55%">Its coffees, soups and bell peppers restore thirst.</td>
  </tr>
  <tr>
    <td width="45%"><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/serene-seasons"><img alt="Serene Seasons" src="https://cdn.modrinth.com/data/cached_images/ba72bd7e14454054eda390d3a1e42c0be51a810e.png" width="200"></a><br><i>Fabric and NeoForge</i></td>
    <td width="55%">Thirst drains faster in summer and slower in winter, and tropical biomes dry out in their dry season.</td>
  </tr>
</table>

## Settings Screen
Change every setting in game, with a live preview of the thirst bar. Open it through <a href="https://www.curseforge.com/minecraft/mc-mods/modmenu">Mod Menu</a> on Fabric, or the Config button in NeoForge's Mods list.

<p align="center">
  <img alt="The ThirstWasTaken2 settings screen: a slider and a switch on the Thirst page, the Water tabs, the AppleSkin preview changing outline and its tooltip droplets switched off and on, the item list scrolling to a modded group under its pinned heading, and the Containers page" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/config/config-showcase.gif" width="100%">
</p>

## Version Support

| Minecraft version | Mod version | Support status |
|---|---|---|
| 26.3 | Latest | Active |
| 26.2 | Latest | Active |
| 26.1.x | Latest | Active |
| 1.21.11 | Latest | Active |
| 1.21.1 | Latest | Active |
| 1.21 (Fabric only) | Latest | Active |

## For Developers

**Data Packs:** Give any item a thirst value with one JSON file in `data/<namespace>/thirstwastaken2/drinks/`, no dependency needed:

```json
{
  "values": {
    "mymod:lemonade": { "thirst": 6, "quenched": 8 },
    "mymod:iced_tea": { "thirst": 8, "quenched": 12 }
  }
}
```

**Java API:** Read and change thirst, react to drinking and check water purity, on every loader and version:

```java
// Salty snacks restore no thirst.
ThirstEvents.DRINK.register((player, stack, drink) -> {
    if (stack.is(MyItems.SALTY_SNACK)) drink.cancel();
});

// Give a player a bottle of Pure water.
player.addItem(ThirstApi.waterBottle(ThirstApi.maxPurity()));
```

See the [data pack](https://n1ght3r.github.io/ThirstWasTaken2/docs/developers/data-packs) and [Java API](https://n1ght3r.github.io/ThirstWasTaken2/docs/developers/java-api) guides.

## Quick FAQ

**Will it be backported to 1.20.1 or Forge?**  
No. There are no plans to support 1.20.1 or Forge.

**Does it work in Peaceful mode?**  
Yes. Thirst refills on its own, unless [`thirstDepletionInPeaceful`](https://n1ght3r.github.io/ThirstWasTaken2/docs/configuration#thirstdepletioninpeaceful) is on. Pairs well with [Peaceful Hunger](https://modrinth.com/mod/peaceful-hunger).

**Can I use this in a modpack?**  
Yes. You are free to include Thirst Was Taken 2 in any public or private modpack.

**Supported languages:**  
English, Vietnamese, Simplified Chinese, Traditional Chinese, French, Japanese, Korean, Polish, and Russian.

![Item tooltips in Simplified Chinese](https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/water/chinese-tooltips.png)

Want to improve or add a translation? [Open a pull request](https://github.com/n1ght3r/ThirstWasTaken2).

## Credits

* Based on [Thirst Was Taken](https://www.curseforge.com/minecraft/mc-mods/thirst-was-taken) by [ghen](https://github.com/ghen-git), under the MIT License.
* Hanging Pot models adapted from [Dehydration](https://github.com/Globox1997/Dehydration) by [Globox1997](https://github.com/Globox1997), under the GPL-3.0.
* Parched effect icon inspired by [Yet Another Thirst](https://github.com/minhnh303/Yet-Another-Thirst) by [minhnh303](https://github.com/minhnh303), redrawn from scratch.
* Licensed under the [GPL-3.0](https://github.com/n1ght3r/ThirstWasTaken2/blob/main/LICENSE) from 1.0.7. See [CREDITS.md](https://github.com/n1ght3r/ThirstWasTaken2/blob/main/CREDITS.md).
