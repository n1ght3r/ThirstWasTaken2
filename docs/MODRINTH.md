<div align="center">

<br>

[![modrinth](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/modrinth_64h.png)](https://modrinth.com/mod/thirst-was-taken-2)
[![curseforge](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/available/curseforge_64h.png)](https://www.curseforge.com/minecraft/mc-mods/thirst-was-taken-2)
[![ghpages](https://cdn.jsdelivr.net/npm/@intergrav/devins-badges@3/assets/cozy/documentation/ghpages_64h.png)](https://n1ght3r.github.io/ThirstWasTaken2/)

</div>

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
      <img alt="AppleSkin icon" src="https://cdn.modrinth.com/data/EsAfCjCV/icon.png" width="20" height="20" align="absmiddle"> <b><a href="https://modrinth.com/mod/appleskin">AppleSkin</a></b><br>
      <i>Fabric, NeoForge and Forge</i><br><br>
      Displays your Quenched reserve directly on the HUD, with multiple styles available to choose from. Tooltips show exact thirst values for food and drinks.
    </td>
    <td width="45%">
      <img alt="The thirst bar with AppleSkin, cycling through the Diamond, Ice, Gold, AppleSkin and Legacy quenched outlines" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/hud/hud-appleskin.gif" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <img alt="Jade icon" src="https://cdn.modrinth.com/data/nvQzSEkH/b04217bc2b7dc524c4d12f81ff42cc1cefb9b0fc_96.webp" width="20" height="20" align="absmiddle"> <b><a href="https://modrinth.com/mod/jade">Jade 🔍</a></b><br>
      <i>Fabric, NeoForge and Forge</i><br><br>
      Shows the purity grade of water sources, waterlogged blocks, and cauldrons directly under your crosshair.
    </td>
    <td width="45%">
      <img alt="Jade showing the water grade under the crosshair" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/water/jade-water.png" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <img alt="Create icon" src="https://cdn.modrinth.com/data/LNytGWDc/61d716699bcf1ec42ed4926a9e1c7311be6087e2_96.webp" width="20" height="20" align="absmiddle"> <b><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/create">Create</a></b><br>
      <i>1.21.1 NeoForge and 1.20.1 Forge<br>
      26.1.2 and 26.2 Fabric: <img alt="Create Fly icon" src="https://cdn.modrinth.com/data/dKvj0eNn/a1e1ad6f018c3a47cb300edbf0ebebce894bfd45_96.webp" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/create-fly">Create Fly</a></i><br><br>
      Adds a Sand Filter to purify dirty water by one grade. Water also keeps its purity grade through pipes, pumps, tanks, drains, and spouts.
    </td>
    <td width="45%">
      <img alt="Engineer's Goggles showing Murky water entering the Sand Filter and Clean water leaving it" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/integrations/create/create-sand-filter-goggles.png" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <img alt="Sophisticated Backpacks icon" src="https://cdn.modrinth.com/data/TyCTlI4b/e31c7e2f8769d317339e25b2a8d1b40fbf312729_96.webp" width="20" height="20" align="absmiddle"> <b><a href="https://modrinth.com/mod/sophisticated-backpacks">Sophisticated Backpacks</a></b><br>
      <i>NeoForge, every version but 26.3</i><br><br>
      Adds the Drinking Upgrade, which drinks from your backpack when you get thirsty, the cleanest water first. Water keeps its purity grade in the Tank and Pump Upgrades. Also works with <img alt="Sophisticated Storage icon" src="https://media.forgecdn.net/avatars/thumbnails/543/206/64/64/637872959580005837.png" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/sophisticated-storage">Sophisticated Storage</a>.
    </td>
    <td width="45%">
      <img alt="A backpack of water with the Advanced Drinking Upgrade's settings open" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/integrations/sophisticated/sophisticated-drinking-upgrade.png" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <img alt="Supplementaries icon" src="https://cdn.modrinth.com/data/fFEIiSDQ/e9f5f66fa3b67e54acb91258a1428d68311c58bc_96.webp" width="20" height="20" align="absmiddle"> <b><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/supplementaries">Supplementaries</a></b><br>
      <i>1.21.1 Fabric and NeoForge</i><br><br>
      Water keeps its purity grade in Jars, Goblets and Faucets, and sea water stays sea water. A Jar or a Goblet of water can be drunk straight from the block. Faucets fill and empty hanging pots and grade the water they draw from a lake.
    </td>
    <td width="45%">
      <img alt="Five jars of water on a lakeshore, brown, grey, blue, cyan and turquoise, with Jade naming the middle one Clean" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/integrations/supplementaries/supplementaries-jars.png" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <img alt="Kaleidoscope Cookery icon" src="https://media.forgecdn.net/avatars/thumbnails/1361/462/64/64/638884307253099520.png" width="20" height="20" align="absmiddle"> <b><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/kaleidoscope-cookery">Kaleidoscope Cookery</a></b><br>
      <i>1.21.1 NeoForge and 1.20.1 Forge<br>
      Fabric: <img alt="Kaleidoscope Cookery Refabricated icon" src="https://cdn.modrinth.com/data/Ct11Kuii/819ba69579e76715103825ce28b345781b415393.png" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/kaleidoscope-cookery-refabricated">Kaleidoscope Cookery Refabricated</a></i><br><br>
      Teas, milk tea and soups restore thirst. Water keeps its purity grade in the Stockpot and the Teapot, and the Teapot brews nothing from sea water.
    </td>
    <td width="45%">
      <img alt="A Teapot on a lit Stove among teacups in a cherry grove, with Jade naming its water Clean" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/integrations/kaleidoscope-cookery/kaleidoscope-teapot.png" width="100%">
    </td>
  </tr>
</table>

Also works with:

<table>
  <tr>
    <td width="45%"><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/cold-sweat"><img alt="Cold Sweat" src="https://i.imgur.com/N6amWeJ.png" width="180"></a><br><i>1.21.1 NeoForge, 1.20.1 Forge</i></td>
    <td width="55%">Thirst follows the temperature Cold Sweat shows around you, hearths and shade included. Its Waterskin carries a purity grade and quenches thirst, and the Boiler purifies water.</td>
  </tr>
  <tr>
    <td width="45%"><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/serene-seasons"><img alt="Serene Seasons" src="https://cdn.modrinth.com/data/cached_images/ba72bd7e14454054eda390d3a1e42c0be51a810e.png" width="200"></a><br><i>Fabric, NeoForge and Forge</i></td>
    <td width="55%">Thirst drains faster in summer and slower in winter, and tropical biomes dry out in their dry season.</td>
  </tr>
  <tr>
    <td width="45%"><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/features/water-purity#the-four-grades"><img alt="Terralith" src="https://wsrv.nl/?url=https%3A%2F%2Fuser-images.githubusercontent.com%2F63272345%2F224816673-7f074733-da85-4673-a7b0-3362651c4dbd.png&amp;w=600" width="300"></a><br><i>Fabric, NeoForge and Forge</i></td>
    <td width="55%">Water in its Orchid Swamp and Ice Marsh is Dirty, the same as in a vanilla swamp.</td>
  </tr>
</table>

### Delight Ecosystem

<details>
<summary><img alt="Farmer's Delight" src="https://i.imgur.com/wqSocVR.png" width="204" align="absmiddle"> <b>and its addons</b>, click to expand</summary>
<br>

<table>
  <tr>
    <td width="55%">
      <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/"><img alt="Farmer's Delight" src="https://i.imgur.com/wqSocVR.png" width="272"></a><br>
      <i>1.21.1 NeoForge and 1.20.1 Forge<br>
      Fabric: <img alt="Farmer's Delight Refabricated icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2F7vxePowz%2F26e8448993e9bda4dba92b6e7a1a13d9c4333138.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://modrinth.com/mod/farmers-delight-refabricated">Farmer's Delight Refabricated</a></i><br><br>
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

<table>
  <tr>
    <td width="45%"><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/cultural-delights"><img alt="Cultural Delights" src="https://cdn.modrinth.com/data/cached_images/d6a323ce1e69b76f2143b3c0e03bd9ce36fc69e1.png" width="272"></a><br><i>1.21.1 NeoForge</i></td>
    <td width="55%">Its drinks, cucumbers and salads restore thirst, and the Vat brews nothing from sea water.</td>
  </tr>
  <tr>
    <td width="45%"><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/fruits-delight"><img alt="Fruits Delight" src="https://media.forgecdn.net/attachments/765/821/logo2.png" width="272"></a><br><i>1.21.1 NeoForge, 1.20.1 Forge</i></td>
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
</table>

</details>

### Let's Do

<details>
<summary><img alt="Let's Do: Farm &amp; Charm icon" src="https://cdn.modrinth.com/data/HJetCzWo/7c6c372629b3efa41409621631d60df12963f005_96.webp" width="20" height="20" align="absmiddle"> <b>Let's Do: Farm &amp; Charm and its addon</b>, click to expand</summary>
<br>

<table>
  <tr>
    <td width="55%">
      <img alt="Let's Do: Farm &amp; Charm icon" src="https://cdn.modrinth.com/data/HJetCzWo/7c6c372629b3efa41409621631d60df12963f005_96.webp" width="20" height="20" align="absmiddle"> <b><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farm-and-charm/">Let's Do: Farm &amp; Charm</a></b><br>
      <i>1.21.1 Fabric and NeoForge</i><br><br>
      Teas and soups restore thirst. A bucket from the Timber Well gets the grade of the groundwater below it, and water from a Water Trough is Murky, so neither is a free source of clean water.
    </td>
    <td width="45%">
      <img alt="A Timber Well and a Water Trough, both full of water, on a sandy shore" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/integrations/farm-and-charm/farm-and-charm-well.png" width="100%">
    </td>
  </tr>
  <tr>
    <td width="55%">
      <img alt="Let's Do: Candlelight icon" src="https://cdn.modrinth.com/data/qwbArkQk/5e0770c8da0fab82a70bc9c3913c8d3996c53345_96.webp" width="20" height="20" align="absmiddle"> <b><a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farm-and-charm/candlelight">Let's Do: Candlelight</a></b><br>
      <i>1.21.1 Fabric and NeoForge, with Farm &amp; Charm</i><br><br>
      Soups, salads and chocolate mousse restore thirst. Kitchen sinks still fill on their own, but their water is Murky and needs boiling, and they refuse sea water.
    </td>
    <td width="45%">
      <img alt="A Candlelight kitchen sink full of water between counters, beside a stove with a cooking pot" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/integrations/farm-and-charm/candlelight-kitchen-sink.png" width="100%">
    </td>
  </tr>
</table>

</details>

### Versions

<details>
<summary>Which integration works on which loader and version, click to expand</summary>

<table>
  <tr>
    <th>Mod</th>
    <th>Fabric</th>
    <th>NeoForge</th>
    <th>Forge (1.20.1)</th>
  </tr>
  <tr>
    <td><img alt="AppleSkin icon" src="https://cdn.modrinth.com/data/EsAfCjCV/icon.png" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/appleskin">AppleSkin</a><br><img alt="Jade icon" src="https://cdn.modrinth.com/data/nvQzSEkH/b04217bc2b7dc524c4d12f81ff42cc1cefb9b0fc_96.webp" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/jade">Jade 🔍</a><br><img alt="Serene Seasons icon" src="https://cdn.modrinth.com/data/e0bNACJD/f8b292ea53e0a0ea908570defddc48673d16d7d6.png" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/serene-seasons">Serene Seasons</a><br><img alt="Terralith icon" src="https://cdn.modrinth.com/data/8oi3bsk5/1959d924a1088944bbf07a06ba523726112d7e7a_96.webp" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/terralith">Terralith</a></td>
    <td>every version</td>
    <td>every version</td>
    <td>yes</td>
  </tr>
  <tr>
    <td><img alt="Farmer's Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FR2OftAxM%2F8e7aa38ab94d94bb0a2894a218b69beb49002b34.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/">Farmer's Delight</a></td>
    <td>every version (Refabricated)</td>
    <td>1.21.1</td>
    <td>yes</td>
  </tr>
  <tr>
    <td><img alt="Create icon" src="https://cdn.modrinth.com/data/LNytGWDc/61d716699bcf1ec42ed4926a9e1c7311be6087e2_96.webp" width="20" height="20" align="absmiddle"> <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/create">Create</a></td>
    <td>26.1.x, 26.2 (Create Fly)</td>
    <td>1.21.1</td>
    <td>yes</td>
  </tr>
  <tr>
    <td><img alt="Sophisticated Backpacks icon" src="https://cdn.modrinth.com/data/TyCTlI4b/e31c7e2f8769d317339e25b2a8d1b40fbf312729_96.webp" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/sophisticated-backpacks">Sophisticated Backpacks</a><br><img alt="Sophisticated Storage icon" src="https://media.forgecdn.net/avatars/thumbnails/543/206/64/64/637872959580005837.png" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/sophisticated-storage">Sophisticated Storage</a></td>
    <td>–</td>
    <td>1.21.1, 1.21.11, 26.1.x, 26.2</td>
    <td>–</td>
  </tr>
  <tr>
    <td><img alt="Kaleidoscope Cookery icon" src="https://media.forgecdn.net/avatars/thumbnails/1361/462/64/64/638884307253099520.png" width="20" height="20" align="absmiddle"> <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/kaleidoscope-cookery">Kaleidoscope Cookery</a></td>
    <td>every version (Refabricated)</td>
    <td>1.21.1</td>
    <td>yes</td>
  </tr>
  <tr>
    <td><img alt="Rustic Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2Ffoa4fGIH%2Feecc99e281522f2291081c48176f0faa84c107bc.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/rustic-delight">Rustic Delight</a></td>
    <td>1.21.1 and newer</td>
    <td>1.21.1</td>
    <td>–</td>
  </tr>
  <tr>
    <td><img alt="Supplementaries icon" src="https://cdn.modrinth.com/data/fFEIiSDQ/e9f5f66fa3b67e54acb91258a1428d68311c58bc_96.webp" width="20" height="20" align="absmiddle"> <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/supplementaries">Supplementaries</a><br><img alt="Brewin' and Chewin' icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FhIu9KJTT%2Ff7c591a80046859d3d45c04ecbbc54d264483d5e.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/brewin-and-chewin">Brewin' and Chewin'</a><br><img alt="Ocean's Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FDGiq4ZSW%2F949ba66d6fffb5a984fbb70e3ef4a51f15be3191.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/oceans-delight">Ocean's Delight</a><br><img alt="Let's Do: Farm &amp; Charm icon" src="https://cdn.modrinth.com/data/HJetCzWo/7c6c372629b3efa41409621631d60df12963f005_96.webp" width="20" height="20" align="absmiddle"> <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farm-and-charm/">Let's Do: Farm &amp; Charm</a><br><img alt="Let's Do: Candlelight icon" src="https://cdn.modrinth.com/data/qwbArkQk/5e0770c8da0fab82a70bc9c3913c8d3996c53345_96.webp" width="20" height="20" align="absmiddle"> <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farm-and-charm/candlelight">Let's Do: Candlelight</a></td>
    <td>1.21.1</td>
    <td>1.21.1</td>
    <td>–</td>
  </tr>
  <tr>
    <td><img alt="Fruits Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2Fg6sbyCTu%2F4ecc5d554f260b876d21c427aa6c2bdf4457fd5c.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/fruits-delight">Fruits Delight</a></td>
    <td>–</td>
    <td>1.21.1</td>
    <td>yes</td>
  </tr>
  <tr>
    <td><img alt="Cold Sweat icon" src="https://cdn.modrinth.com/data/uXhSmPjd/bf55420556c30d44d2f5cf7b8915705b9214b4ef.png" width="20" height="20" align="absmiddle"> <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/cold-sweat">Cold Sweat</a><br><img alt="Cultural Delights icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2FYttyNOFA%2Fd857243f0e7dedd3d7f552c4371326773629e42e.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/cultural-delights">Cultural Delights</a><br><img alt="Expanded Delight icon" src="https://wsrv.nl/?url=https%3A%2F%2Fcdn.modrinth.com%2Fdata%2Fe9V6wFcR%2F4cbbace573b20628290929948a77c74d95ed7a70.png&amp;trim=1&amp;w=96&amp;h=96&amp;fit=contain&amp;cbg=00000000&amp;output=png" width="24" height="24" align="absmiddle"> <a href="https://n1ght3r.github.io/ThirstWasTaken2/docs/integrations/farmers-delight/expanded-delight">Expanded Delight</a></td>
    <td>–</td>
    <td>1.21.1</td>
    <td>–</td>
  </tr>
</table>

On Minecraft 1.20.1, the drinks and meals of every addon above restore thirst wherever the addon has
them, marked or not. What the table leaves out there, and why:

- Brewin' and Chewin': its 1.20.1 Keg is an older design, so water poured into it does not keep its
  grade.
- Expanded Delight: its 1.20.1 build is Fabric only, and there the Cooking Pot still cooks its two soups
  from sea water.
- Cultural Delights: its 1.20.1 build has no Vat, so there is nothing to change.

</details>

## Settings Screen
Change every setting in game, with a live preview of the thirst bar. Open it through <img alt="Mod Menu icon" src="https://cdn.modrinth.com/data/mOgUt4GM/5a20ed1450a0e1e79a1fe04e61bb4e5878bf1d20.png" width="20" height="20" align="absmiddle"> <a href="https://modrinth.com/mod/modmenu">Mod Menu</a> on Fabric, or the Config button in the Mods list on NeoForge and Forge.

<p align="center">
  <img alt="The ThirstWasTaken2 settings screen, every page and tab: a slider and a switch on the Thirst page, the Water tabs, the Sickness tables for each difficulty, the AppleSkin preview changing outline and its tooltip droplets switched off and on, the item list scrolling to a modded group under its pinned heading, Mod Items and Containers" src="https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/config/config-showcase.gif" width="100%">
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
| 1.20.1 (Fabric and Forge) | Latest | Active |

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

**Does it work in Peaceful mode?**  
Yes. Thirst refills on its own, unless [`thirstDepletionInPeaceful`](https://n1ght3r.github.io/ThirstWasTaken2/docs/configuration#thirstdepletioninpeaceful) is on. Pairs well with <img alt="Peaceful Hunger icon" src="https://cdn.modrinth.com/data/NGEcCZ3C/4f2503680e564e8d2ad5e252993a44af2c61acc3_96.webp" width="20" height="20" align="absmiddle"> [Peaceful Hunger](https://modrinth.com/mod/peaceful-hunger).

**Can I use this in a modpack?**  
Yes. You are free to include Thirst Was Taken 2 in any public or private modpack.

**Supported languages:**  
English, Vietnamese, Simplified Chinese, Traditional Chinese, French, Japanese, Korean, Polish, and Russian.

![Item tooltips in Simplified Chinese](https://raw.githubusercontent.com/n1ght3r/ThirstWasTaken2/main/docs/public/screenshots/water/chinese-tooltips.png)

Want to improve or add a translation? [Open a pull request](https://github.com/n1ght3r/ThirstWasTaken2).

## Credits

* Based on [Thirst Was Taken](https://modrinth.com/mod/thirst-was-taken) by [ghen](https://github.com/ghen-git), under the MIT License.
* Hanging Pot models adapted from [Dehydration](https://github.com/Globox1997/Dehydration) by [Globox1997](https://github.com/Globox1997), under the GPL-3.0.
* Parched effect icon inspired by [Yet Another Thirst](https://modrinth.com/mod/yet-another-thirst) by [minhnh303](https://github.com/minhnh303), redrawn from scratch.
* Licensed under the [GPL-3.0](https://github.com/n1ght3r/ThirstWasTaken2/blob/main/LICENSE) from 1.0.7. See [CREDITS.md](https://github.com/n1ght3r/ThirstWasTaken2/blob/main/CREDITS.md).
