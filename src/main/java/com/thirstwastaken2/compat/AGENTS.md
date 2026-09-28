# compat/

Optional integrations. **No hard dependency ever lands here**: gate on `Loader.isModLoaded`,
keep the touching classes off the load path, and let the mod run identically with none of them
installed.

Note that most third-party support is *not* in this package — food and drink values resolve by
registry id in `ThirstConfig`, and container detection resolves by namespace in `WaterPurity.resolve`.
Neither references a foreign class, so neither needs a gate. Prefer that approach.

## AppleSkin

`AppleSkin` is the presence check common code can ask without touching an AppleSkin class, and the two
settings the mod keeps for it. The quenched outline on the thirst bar and the tooltip droplet rows are
the thirst half of what AppleSkin adds for hunger, so both are only shown while it is loaded. Reading
AppleSkin's own config names its classes, so that part stays client-side in
`client/compat/AppleSkinIntegration`, which is only reached after `AppleSkin.isLoaded()`.

## Jade

`client/compat/JadeIntegration` (client source set, since Jade's tooltip API is client code) shows
the grade of the water under the crosshair: world water, waterlogged blocks and water cauldrons. It is
reached only through the `jade` entrypoint in `fabric.mod.json`, which Jade resolves on the dedicated
server too, so the plugin class itself names no client class outside `registerClient`. Jade toggles it
under `config.jade.plugin_thirstwastaken2.water_purity`. Why client-side sampling is safe is in
[purity/AGENTS.md](../purity/AGENTS.md).

A block that keeps its water somewhere common code may not name, a Supplementaries jar for instance,
is read through `JadeIntegration.addContainer`: an integration adds a reader from its own client entry
point and the overlay stays one plugin with one entry in Jade's settings.

The part of the Jade API it uses is identical on every supported version, so it carries no version
branch. A water cauldron nothing was poured into shows the client's `defaultPurity`, the same
limitation an unstamped item tooltip already has on a server with a different config.

## Farmer's Delight

Three pieces, none of which loads a Farmer's Delight class:

- **Item values** are plain entries in `ThirstConfig`, and `WaterPurity.resolve` still marks its apple
  cider and melon juice as pure containers by id.
- **Cooking Pot recipes** are data: `FarmersDelightRecipeProvider` in `src/datagen` writes
  `cooking_pot_purify_water_bottle` and `_bowl` plus their unlocks, each behind a
  `fabric:all_mods_loaded` condition. `AdvancementGameTest` checks they are skipped without the mod.
- **Nourishment** is `FarmersDelight.isNourished`, which looks the effect up by id through
  `Vanilla.mobEffect` on first use. `ThirstManager.tickPlayer` drops the tick's exhaustion while it
  holds, as the original mod did. Farmer's Delight itself cancels food exhaustion with
  `setExhaustion(0)` on 1.21.1 and with negative `causeFoodExhaustion` calls from 1.21.11, which
  `PlayerMixin` mirrors, so relying on its side would drain thirst on one version and not the other.

Fabric uses Farmer's Delight Refabricated, NeoForge and Forge vectorwing's original; all register the
same ids, recipe type and effect, so nothing here differs per loader. Only `1.21.1-neoforge` sets
`deps.farmersdelight` among the NeoForge nodes; both 1.20.1 nodes set it.

**On 1.20.1** the Cooking Pot recipes come from `LegacyRecipeProvider` instead, the same names and
unlocks with the water as NBT: `fabric:any` over three `fabric:nbt` ingredients, which the Forge node
translates to a plain array of `forge:partial_nbt` (Forge's compound ingredient). Both 1.20.1 pots read
the result through `CraftingHelper.getItemStack(json, true)`, so its `nbt` survives. A 1.20.1 potion has
no crafting remainder, so the pot serves the bottle straight into its output slot. Refabricated's
1.20.1 build nests Fabric ASM and Porting Lib, which `build.gradle.kts` unpacks for the dev client.

The gametests run without Farmer's Delight, so the Cooking Pot and Nourishment are checked in a real
client: `./gradlew ":26.2.x:runClient"` has it on the classpath, and
[tools/agent/integrations/farmers-delight.jsonl](../../../../../../tools/agent/integrations/farmers-delight.jsonl) checks drinking, the
Cooking Pot and Nourishment unattended on `1.21.1-neoforge`;
[tools/agent/integrations/farmers-delight-1.20.1.jsonl](../../../../../../tools/agent/integrations/farmers-delight-1.20.1.jsonl) is the
same on both 1.20.1 nodes, in NBT.

## Create Fly

Not in this package: the Sand Filter extends Create classes, and Create Fly only exists for some of the
Minecraft versions the mod supports, so it has source directories of its own that only those builds
compile. See [src/main/createfly/AGENTS.md](../../../../createfly/AGENTS.md).

## Sophisticated Backpacks

Not in this package either: the Tank upgrade is reached through a mixin into Sophisticated Core, and
only Core's `IFluidHandler` generation (1.21.1) is supported, so it has a source directory of its own.
See [src/main/sophisticated/AGENTS.md](../../../../sophisticated/AGENTS.md).

## Supplementaries

Not in this package either: a jar, a goblet and a faucet all move water through Moonlight Lib's soft
fluids, which are mixed into, and only the two 1.21.1 nodes have a Supplementaries to compile against.
Its Jade half is the one thing that reaches back into this package, through
`JadeIntegration.addContainer`.
It is the one optional integration **both loaders** compile, since Moonlight is multi-loader. See
[src/main/supplementaries/AGENTS.md](../../../../supplementaries/AGENTS.md).

## LootIntegration

`Loader.onLootTable` replacing the original's Forge global loot modifiers (Fabric's
`LootTableEvents.MODIFY` underneath). One extra pool is appended to five vanilla chest tables
(`SIMPLE_DUNGEON`, `ABANDONED_MINESHAFT`, `SHIPWRECK_SUPPLY`, `NETHER_BRIDGE`, `BASTION_OTHER`) and to
`PIGLIN_BARTERING`, with different weights for each case.

The pool is added to a table with one of those ids whoever wrote it, a data pack's replacement
included. The mod used to skip replaced tables, but a loader cannot tell a player's pack from one of
vanilla's own experiment packs, and Fabric API changed its answer between versions: with the Villager
Trade Rebalance experiment on, mineshaft chests held water on 1.21.1 and none on later versions. A
few water bottles are an addition any pack can live with; a rule that differs per version is not.

Water bottles are emitted as `minecraft:potion` + `SetPotionFunction` + the functions
`ItemWaterData.stampFreshLoot` adds, which stamp `water_purity` and `water_salty`, so loot water arrives
already stamped rather than falling back to `defaultPurity`.
