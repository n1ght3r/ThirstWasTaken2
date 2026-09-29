# Installation

## Download

Download the mod from [Modrinth](https://modrinth.com/mod/thirst-was-taken-2). It is also on
[CurseForge](https://www.curseforge.com/minecraft/mc-mods/thirst-was-taken-2).

## Supported versions

Every Minecraft version and loader gets its own file. The suffix on the file name is the Minecraft
version, NeoForge files end in `-neoforge` and Forge files in `-forge`, for example
`ThirstWasTaken2-1.0.9+1.21.11.jar` and `ThirstWasTaken2-1.0.9+1.21.11-neoforge.jar`.

### Fabric

| Minecraft | File suffix | Fabric Loader | Fabric API | Java |
|---|---|---|---|---|
| 26.3 | `+26.3` | 0.19.5 or newer | 0.161.0+26.3 or newer | 25 |
| 26.2 | `+26.2` | 0.19.5 or newer | 0.161.0+26.2 or newer | 25 |
| 26.1, 26.1.1, 26.1.2 | `+26.1.2` | 0.19.5 or newer | 0.155.3+26.1.2 or newer | 25 |
| 1.21.11 | `+1.21.11` | 0.19.5 or newer | 0.141.6+1.21.11 or newer | 21 |
| 1.21, 1.21.1 | `+1.21.1` | 0.19.5 or newer | 0.116.17+1.21.1 or newer | 21 |
| 1.20.1 | `+1.20.1` | 0.19.5 or newer | 0.92.12+1.20.1 or newer | 17 |

Fabric API is required and must match the Minecraft version.

### NeoForge

| Minecraft | File suffix | NeoForge | Java |
|---|---|---|---|
| 26.3 | `+26.3-neoforge` | 26.3.0.22-beta or newer | 25 |
| 26.2 | `+26.2-neoforge` | 26.2.0.88 or newer | 25 |
| 26.1, 26.1.1, 26.1.2 | `+26.1.2-neoforge` | 26.1.2.109 or newer | 25 |
| 1.21.11 | `+1.21.11-neoforge` | 21.11.45 or newer | 21 |
| 1.21.1 | `+1.21.1-neoforge` | 21.1.251 or newer | 21 |

NeoForge needs nothing else. The 1.21.1 file does not run on 1.21. NeoForge has only beta builds for
26.3 so far, so that file asks for one.

### Forge

Minecraft 1.20.1 runs on Forge rather than NeoForge.

| Minecraft | File suffix | Forge | Java |
|---|---|---|---|
| 1.20.1 | `+1.20.1-forge` | any 47 build, tested with 47.4.10 | 17 |

Forge needs nothing else.

The Java version is the minimum on every loader. The runtime Minecraft ships with is enough.

### Minecraft 1.20.1 to 1.21.1

- The droplets in item tooltips have a shadow.

## Compatible mods

All optional except Fabric API. Versions are listed in the order 26.3, 26.2, 26.1.x, 1.21.11, 1.21.1,
1.20.1.

### Fabric

| Mod | Versions built with | What it adds |
|---|---|---|
| ![](https://cdn.modrinth.com/data/P7dR8mSH/icon.png){.mod-icon} [Fabric API](https://modrinth.com/mod/fabric-api) | 0.161.0+26.3, 0.161.0+26.2, 0.155.3+26.1.2, 0.141.6+1.21.11, 0.116.17+1.21.1, 0.92.12+1.20.1 | Required. |
| ![](https://cdn.modrinth.com/data/mOgUt4GM/5a20ed1450a0e1e79a1fe04e61bb4e5878bf1d20.png){.mod-icon} [Mod Menu](https://modrinth.com/mod/modmenu) | 21.0.0, 20.0.3, 18.0.2, 17.0.1, 11.0.5, 7.2.2 | A Config button for the [settings screen](/docs/configuration). |
| ![](https://cdn.modrinth.com/data/EsAfCjCV/icon.png){.mod-icon} [AppleSkin](https://modrinth.com/mod/appleskin) | 3.0.10+mc26.3, 3.0.10+mc26.2, 3.0.10+mc26.1.2, 3.0.8+mc1.21.11, 3.0.6+mc1.21, 2.5.2+mc1.20.1 | The quenched outline on the thirst bar, droplet rows in tooltips, and the exhaustion strip. |
| ![](https://cdn.modrinth.com/data/9s6osm5g/ed8a2316cbb6f4fc5f510e8e13a59a85cbbbff4d_96.webp){.mod-icon} [Cloth Config](https://modrinth.com/mod/cloth-config) | 26.3.159, 26.2.155, 26.1.154, 21.11.153, 15.0.140, 11.1.136+fabric | AppleSkin's own settings screen. |
| ![](https://cdn.modrinth.com/data/dKvj0eNn/a1e1ad6f018c3a47cb300edbf0ebebce894bfd45_96.webp){.mod-icon} [Create Fly](https://modrinth.com/mod/create-fly) | 26.2-rc-2-6.0.9-1, 26.1.2-6.0.9-4 | The [Sand Filter](/docs/integrations/create). 26.2 and 26.1.2 only. |
| ![](https://cdn.modrinth.com/data/nvQzSEkH/b04217bc2b7dc524c4d12f81ff42cc1cefb9b0fc_96.webp){.mod-icon} [Jade 🔍](https://modrinth.com/mod/jade) | 26.3.1, 26.2.11, 26.1.11, 21.1.6, 15.10.6, 11.13.3+fabric | The [grade of the water](/docs/features/water-purity#checking-water-with-jade) under the crosshair. Client only. |
| ![](https://cdn.modrinth.com/data/fFEIiSDQ/e9f5f66fa3b67e54acb91258a1428d68311c58bc_96.webp){.mod-icon} [Supplementaries](https://modrinth.com/mod/supplementaries) | 1.21.1-3.9.9 | [Water that keeps its grade](/docs/integrations/supplementaries) in Jars, Goblets and Faucets, and a Jar or Goblet of water that can be drunk. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/Ct11Kuii/819ba69579e76715103825ce28b345781b415393.png){.mod-icon} [Kaleidoscope Cookery Refabricated](https://modrinth.com/mod/kaleidoscope-cookery-refabricated) | 1.5.1.1-fabric+mc26.3, 1.5.1.1-fabric+mc26.2, 1.5.1.1-fabric+mc26.1.2, 1.3.0.9-fabric+mc1.21.11, 1.5.1.1-fabric+mc1.21.1, 1.5.1.1-fabric+mc1.20.1 | Thirst from its [teas and soups](/docs/integrations/kaleidoscope-cookery), and water that keeps its grade in the Stockpot and the Teapot. The official Fabric build stopped at 1.0.1 and only gets the thirst values. |
| ![](https://cdn.modrinth.com/data/e0bNACJD/f8b292ea53e0a0ea908570defddc48673d16d7d6.png){.mod-icon} [Serene Seasons](https://modrinth.com/mod/serene-seasons) | 26.1.2.0.7, 26.1.2.0.6, 21.11.0.4, 10.1.0.9, 9.1.0.3 | Thirst that [follows the season](/docs/integrations/serene-seasons). Needs GlitchCore. |
| ![](https://cdn.modrinth.com/data/7vxePowz/26e8448993e9bda4dba92b6e7a1a13d9c4333138.png){.mod-icon} [Farmer's Delight Refabricated](https://modrinth.com/mod/farmers-delight-refabricated) | 26.3-3.6.27, 26.2-3.6.26, 26.1-3.6.26, 1.21.11-3.6.16, 1.21.1-3.3.6, 1.20.1-2.5.7 | Thirst from its [drinks and meals](/docs/integrations/farmers-delight/), Pure water from the Cooking Pot, and no drain under Nourishment. |
| ![](https://cdn.modrinth.com/data/hIu9KJTT/f7c591a80046859d3d45c04ecbbc54d264483d5e.png){.mod-icon} [Brewin' and Chewin'](https://modrinth.com/mod/brewin-and-chewin) | v4.5.0+1.21.1-fabric | Thirst from its [brews and soups](/docs/integrations/farmers-delight/brewin-and-chewin), and water that keeps its grade in the Keg. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/DGiq4ZSW/949ba66d6fffb5a984fbb70e3ef4a51f15be3191.png){.mod-icon} [Ocean's Delight](https://modrinth.com/mod/oceans-delight) | 1.0.3+fabric.1.21.1 | Thirst from its [Guardian Soup and bowls](/docs/integrations/farmers-delight/oceans-delight). 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/foa4fGIH/eecc99e281522f2291081c48176f0faa84c107bc.png){.mod-icon} [Rustic Delight](https://modrinth.com/mod/rustic-delight) | 1.7.0 (26.3, 26.2, 26.1), 1.7.2 (1.21.11), 1.7.1 (1.21.1) | Thirst from its [coffees, soups and bell peppers](/docs/integrations/farmers-delight/rustic-delight). |

### NeoForge

The settings screen opens from the Config button in NeoForge's Mods list, with no extra mod.

| Mod | Versions built with | What it adds |
|---|---|---|
| ![](https://cdn.modrinth.com/data/EsAfCjCV/icon.png){.mod-icon} [AppleSkin](https://modrinth.com/mod/appleskin) | 3.0.10+mc26.3, 3.0.10+mc26.2, 3.0.9+mc26.1, 3.0.8+mc1.21.11, 3.0.9+mc1.21 | The same as on Fabric. |
| ![](https://cdn.modrinth.com/data/9s6osm5g/ed8a2316cbb6f4fc5f510e8e13a59a85cbbbff4d_96.webp){.mod-icon} [Cloth Config](https://modrinth.com/mod/cloth-config) | 26.2.155, 26.1.154, 21.11.153, 15.0.140 | AppleSkin's own settings screen. Not on 26.3 yet. |
| ![](https://cdn.modrinth.com/data/nvQzSEkH/b04217bc2b7dc524c4d12f81ff42cc1cefb9b0fc_96.webp){.mod-icon} [Jade 🔍](https://modrinth.com/mod/jade) | 26.3.1, 26.2.10, 26.1.10, 21.1.7, 15.10.6 | The same as on Fabric. Client only. |
| ![](https://cdn.modrinth.com/data/LNytGWDc/61d716699bcf1ec42ed4926a9e1c7311be6087e2_96.webp){.mod-icon} [Create](https://modrinth.com/mod/create) | 6.0.10 | The [Sand Filter](/docs/integrations/create). 1.21.1 only. |
| ![](/icons/mods/sophisticated-backpacks.png){.mod-icon} [Sophisticated Backpacks](https://modrinth.com/mod/sophisticated-backpacks) | Core 26.2-1.5.0.2337, 26.1.2-1.5.0.2334, 1.21.11-1.5.0.2340, 1.21.1-1.5.1.2341. Not on 26.3 yet. | The [Drinking Upgrade](/docs/integrations/sophisticated-backpacks), and water that keeps its grade in the Tank and Pump Upgrades. Sophisticated Storage takes the Drinking Upgrade too. |
| ![](https://cdn.modrinth.com/data/fFEIiSDQ/e9f5f66fa3b67e54acb91258a1428d68311c58bc_96.webp){.mod-icon} [Supplementaries](https://modrinth.com/mod/supplementaries) | 1.21.1-3.9.9 | The same as on Fabric. 1.21.1 only. |
| ![](https://media.forgecdn.net/avatars/thumbnails/1361/462/64/64/638884307253099520.png){.mod-icon} [Kaleidoscope Cookery](https://modrinth.com/mod/kaleidoscope-cookery) | 1.5.1-neoforge+mc1.21.1 | Thirst from its [teas and soups](/docs/integrations/kaleidoscope-cookery), and water that keeps its grade in the Stockpot and the Teapot. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/uXhSmPjd/bf55420556c30d44d2f5cf7b8915705b9214b4ef.png){.mod-icon} [Cold Sweat](https://modrinth.com/mod/cold-sweat) | 2.4.3.1 | Thirst that follows [its temperature](/docs/integrations/cold-sweat), a graded Waterskin, and a Boiler that purifies water. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/e0bNACJD/f8b292ea53e0a0ea908570defddc48673d16d7d6.png){.mod-icon} [Serene Seasons](https://modrinth.com/mod/serene-seasons) | 26.1.2.0.7, 26.1.2.0.6, 21.11.0.4, 10.1.0.9 | The same as on Fabric. |
| ![](https://cdn.modrinth.com/data/R2OftAxM/8e7aa38ab94d94bb0a2894a218b69beb49002b34.png){.mod-icon} [Farmer's Delight](https://modrinth.com/mod/farmers-delight) | 1.21.1-1.3.4 | The same as on Fabric. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/hIu9KJTT/f7c591a80046859d3d45c04ecbbc54d264483d5e.png){.mod-icon} [Brewin' and Chewin'](https://modrinth.com/mod/brewin-and-chewin) | v4.5.0+1.21.1-neoforge | The same as on Fabric. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/YttyNOFA/d857243f0e7dedd3d7f552c4371326773629e42e.png){.mod-icon} [Cultural Delights](https://modrinth.com/mod/cultural-delights) | 0.18.1-1.21.1 | Thirst from its [drinks and watery foods](/docs/integrations/farmers-delight/cultural-delights), and a Vat that brews nothing from sea water. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/g6sbyCTu/4ecc5d554f260b876d21c427aa6c2bdf4457fd5c.png){.mod-icon} [Fruits Delight](https://modrinth.com/mod/fruits-delight) | 1.2.14 | Thirst from its [juices, teas and fruit](/docs/integrations/farmers-delight/fruits-delight). 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/DGiq4ZSW/949ba66d6fffb5a984fbb70e3ef4a51f15be3191.png){.mod-icon} [Ocean's Delight](https://modrinth.com/mod/oceans-delight) | 1.0.4 | The same as on Fabric. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/e9V6wFcR/4cbbace573b20628290929948a77c74d95ed7a70.png){.mod-icon} [Expanded Delight](https://modrinth.com/mod/expanded-delight) | 0.1.4 | Thirst from its [juices, soups and salads](/docs/integrations/farmers-delight/expanded-delight), and a Cooking Pot that cooks nothing from sea water. 1.21.1 only. |
| ![](https://cdn.modrinth.com/data/foa4fGIH/eecc99e281522f2291081c48176f0faa84c107bc.png){.mod-icon} [Rustic Delight](https://modrinth.com/mod/rustic-delight) | 1.7.1 | The same as on Fabric. 1.21.1 only. |

### Forge

On Minecraft 1.20.1 only. The settings screen opens from the Config button in Forge's Mods list.

| Mod | Versions built with | What it adds |
|---|---|---|
| ![](https://cdn.modrinth.com/data/EsAfCjCV/icon.png){.mod-icon} [AppleSkin](https://modrinth.com/mod/appleskin) | 2.5.1+mc1.20.1 | The same as on Fabric. |
| ![](https://cdn.modrinth.com/data/9s6osm5g/ed8a2316cbb6f4fc5f510e8e13a59a85cbbbff4d_96.webp){.mod-icon} [Cloth Config](https://modrinth.com/mod/cloth-config) | 11.1.136+forge | AppleSkin's own settings screen. |
| ![](https://cdn.modrinth.com/data/nvQzSEkH/b04217bc2b7dc524c4d12f81ff42cc1cefb9b0fc_96.webp){.mod-icon} [Jade 🔍](https://modrinth.com/mod/jade) | 11.13.3+forge | The same as on Fabric. Client only. |
| ![](https://cdn.modrinth.com/data/LNytGWDc/61d716699bcf1ec42ed4926a9e1c7311be6087e2_96.webp){.mod-icon} [Create](https://modrinth.com/mod/create) | 6.0.8 | The [Sand Filter](/docs/integrations/create). |
| ![](https://cdn.modrinth.com/data/e0bNACJD/f8b292ea53e0a0ea908570defddc48673d16d7d6.png){.mod-icon} [Serene Seasons](https://modrinth.com/mod/serene-seasons) | 9.1.0.3 | The same as on Fabric. |
| ![](https://cdn.modrinth.com/data/R2OftAxM/8e7aa38ab94d94bb0a2894a218b69beb49002b34.png){.mod-icon} [Farmer's Delight](https://modrinth.com/mod/farmers-delight) | 1.20.1-1.3.4 | The same as on Fabric. |
| ![](https://media.forgecdn.net/avatars/thumbnails/1361/462/64/64/638884307253099520.png){.mod-icon} [Kaleidoscope Cookery](https://modrinth.com/mod/kaleidoscope-cookery) | 1.5.1-forge+mc1.20.1 | The same as on NeoForge. |
| ![](https://cdn.modrinth.com/data/g6sbyCTu/4ecc5d554f260b876d21c427aa6c2bdf4457fd5c.png){.mod-icon} [Fruits Delight](https://modrinth.com/mod/fruits-delight) | 1.1.3 | The same as on NeoForge. |

Other food mods usually work as they are. Drinks their mod marks as drinks restore thirst, and any
item can be given a value in [Configuration](/docs/configuration).

## Where the mod goes

Put the jar in the `mods` folder of the server and of every client. A client without the mod does
not see the bar.

## Languages

The mod follows each player's game language. Nine are included: English, French, Japanese, Korean,
Polish, Russian, Vietnamese, Simplified Chinese and Traditional Chinese.

![Item tooltips with the game set to Simplified Chinese](/screenshots/water/chinese-tooltips.png)

## First run

The first launch writes `config/thirstwastaken2.json` with the defaults. Existing worlds work, and
every player starts at full thirst.
