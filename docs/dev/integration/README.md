# Integration plans

One plan per mod: what it does with water, the decisions taken and what was found in game. Once a plan
is built, how the integration works lives in its source directory's `AGENTS.md`; the plan stays as the
record. Small integrations with only thirst values in `ThirstConfig` have no plan.

| Folder | What is in it |
|---|---|
| [`lets-do/`](lets-do/) | The Let's Do collection, all on Architectury, all on the two 1.21.1 nodes only |
| [`cooking/`](cooking/) | Farmer's Delight addons and Kaleidoscope Cookery: cooking pots, kegs, vats, teapots |
| [`storage/`](storage/) | Mods that move water in tanks, jars and upgrades |
| [`climate/`](climate/) | Mods that change the drain: temperature and seasons |
| [`world/`](world/) | Overworld overhauls: new biomes whose water we grade, and what they add to drink |

| Mod | Plan | Code | Nodes | State |
|---|---|---|---|---|
| Let's Do: Farm & Charm | [lets-do/FARM-AND-CHARM](lets-do/FARM-AND-CHARM-INTEGRATION.md) | `src/main/farmandcharm` | both 1.21.1 | done |
| Let's Do: Candlelight | [lets-do/CANDLELIGHT](lets-do/CANDLELIGHT-INTEGRATION.md) | `src/main/farmandcharm` | both 1.21.1 | done |
| Let's Do: HerbalBrews | [lets-do/HERBALBREWS](lets-do/HERBALBREWS-INTEGRATION.md) | `src/main/herbalbrews` | both 1.21.1 | done |
| Let's Do: Beachparty | [lets-do/BEACHPARTY](lets-do/BEACHPARTY-INTEGRATION.md) | `src/main/beachparty` | both 1.21.1 | done |
| Brewin' and Chewin' | [cooking/BREWIN-AND-CHEWIN](cooking/BREWIN-AND-CHEWIN-INTEGRATION.md) | `src/main/brewinandchewin` | both 1.21.1 | see plan |
| Cultural Delights | [cooking/CULTURAL-DELIGHTS](cooking/CULTURAL-DELIGHTS-INTEGRATION.md) | `src/main/culturaldelights` | `1.21.1-neoforge` | see plan |
| Fruits Delight | [cooking/FRUITS-DELIGHT](cooking/FRUITS-DELIGHT-INTEGRATION.md) | `src/main/fruitsdelight` | `1.21.1-neoforge`, `1.20.1-forge` | see plan |
| Kaleidoscope Cookery | [cooking/KALEIDOSCOPE-COOKERY](cooking/KALEIDOSCOPE-COOKERY-INTEGRATION.md) | `src/main/kaleidoscope` | see plan | see plan |
| Sophisticated Backpacks and Storage | [storage/SOPHISTICATED](storage/SOPHISTICATED-INTEGRATION.md) | `src/main/sophisticated` | NeoForge but 26.3 | see plan |
| Supplementaries | [storage/SUPPLEMENTARIES](storage/SUPPLEMENTARIES-INTEGRATION.md) | `src/main/supplementaries` | both 1.21.1 | see plan |
| Cold Sweat | [climate/COLD-SWEAT](climate/COLD-SWEAT-INTEGRATION.md) | `src/main/coldsweat`, `coldsweatforge` | `1.21.1-neoforge`, `1.20.1-forge` | see plan |
| Serene Seasons | [climate/SERENE-SEASONS](climate/SERENE-SEASONS-INTEGRATION.md) | `src/main/sereneseasons` | every node | see plan |
| Spelunkery | [world/SPELUNKERY](world/SPELUNKERY-INTEGRATION.md) | `src/main/spelunkery` | both 1.21.1 | see plan |

A new plan goes in the folder of its family, gets a row here and a row under "Where to look" in the
root `AGENTS.md`.
