# Minecraft 1.20.1 plan (Fabric and Forge)

Adds two nodes, `1.20.1` (Fabric) and `1.20.1-forge` (MinecraftForge 47), to the one source tree, the
same way every other node is built. This file sets the order of work, what each step needs and how
each one is checked. Once the work is built, how it works goes in the `AGENTS.md` of each area it
touched and in [VERSION-DIFFERENCES.md](VERSION-DIFFERENCES.md).

Written on 2026-09-28 from the tree at `96fdf3e` (release 1.4.1). Library versions below are starting
points and must be checked against Modrinth and the loader mavens when the step starts.

## Why nodes and not a separate module

A copy of the mod for 1.20.1 would have to take every change to core by hand: core is about 8,000 lines
(`src/main/java` and `src/client/java`) and had 84 commits in the three months before this plan. As
nodes, a feature reaches 1.20.1 with the same commit that adds it everywhere else.

The worry with nodes is that `Vanilla` grows too large. The large differences do not belong in
`Vanilla`. They go in their own classes in `platform/`, one per topic, like `DrinkItem`, `PlayerData`
and `SupportedBlock` today. Where a whole loader class differs between generations, it goes in its own
source directory per generation, like `neoforge-fluidhandler` and `neoforge-transfer` today.

The [version policy](../../AGENTS.md#version-policy) applies to 1.20.1 as it does to 1.21.1. A feature
that needs a core-code fork for 1.20.1 retires 1.20.1, not the other way round.

## Decisions

| # | Question | Options | Recommended |
|---|---|---|---|
| D1 | Which loader for the second node | **a.** MinecraftForge 47.x. **b.** NeoForge 47.1 (the 1.20.1 fork) | **a.** Nearly every 1.20.1 modpack runs Forge, and NeoForge 47.1 is no longer maintained |
| D2 | Forge build plugin | **a.** ModDevGradle Legacy (`net.neoforged.moddev.legacyforge`). **b.** ForgeGradle 6 | **a.** Same plugin family and DSL as `build.neoforge.gradle.kts`, and it handles the SRG remap and the mixin refmap |
| D3 | Player data where no attachment API exists | **a.** Fabric: a mixin that saves the value with the player. Forge: a Capability. **b.** Cardinal Components | **a.** Cardinal Components would be a required dependency, which the mod has never had |
| D4 | Java level | **a.** Everything compiled on a 1.20.1 node is Java 17. Integration directories that no 1.20.1 node builds may stay on 21. **b.** The whole tree is Java 17 | **a.** The 1.20.1 node compiles with `release = 17`, so the compiler enforces it |
| D5 | What the Fabric jar claims | **a.** `>=1.20 <=1.20.1`, like the 1.21.1 jar claims 1.21. **b.** 1.20.1 only | **a** if a `runServer` on 1.20 passes, otherwise **b** |

## Phase 0: the seams, on the current ten nodes

No new node yet. Each step keeps behaviour the same, so a failing gametest here is a refactor bug, not a
port bug.

**Status (2026-09-28): built, uncommitted.** Gametests pass on the four check nodes; the full ten-node
run before merging is still open. Where it landed: `platform/ItemWaterData`, `platform/Clientbound`,
`FabricTransfer` for the fluid variant's grade, `loaderOf`/`minecraftOf` in build-logic and
`tools/node_names.py`. NeoForge's `WaterFluids` still names `ThirstComponents` on a `FluidStack`: no
1.20.1 node compiles it, and Forge gets its own in `forge-fluidhandler`.

### 0.1 Java 17

Java 21 features that core uses today:

| What | Where | Change |
|---|---|---|
| `switch` with type patterns (`case WaterQuality.Salt ignored ->`) | `ThirstAdvancements`, `WaterskinItem`, `WaterPurity`, `ThirstTooltip`, `client/compat/JadeIntegration`, `neoforge/WaterFluids` | `if (quality instanceof WaterQuality.Fresh fresh)`, which is Java 16 |
| `Math.clamp` (8 uses) | core | `Mth.clamp` |
| `List.getFirst()` (2 uses) | core | `get(0)` |

Integration code that only 1.21+ nodes build (`brewinandchewin`, `createfly`, `supplementaries` and
others) stays as it is until one of them gains a 1.20.1 node (D4).

The root `AGENTS.md` rule changes from "no language feature newer than Java 21" to "Java 17 in anything
a 1.20.1 node compiles".

### 0.2 Water data on an item

1.20.1 has no data components, only the item's NBT tag. `ThirstComponents` is named 37 times across
core, the loader directories, gametests, datagen and the integrations.

- Add `platform/ItemWaterData`: servings, grade, salty, with `get`, `set`, `clear` and `copy`. It is
  backed by `ThirstComponents` from 1.20.5 and by a `thirstwastaken2` compound tag on 1.20.1.
- Core, the loader directories and the gametests call only `ItemWaterData`. Integrations built only on
  1.21+ nodes may keep naming the components until they gain a 1.20.1 node.
- `WaterPurity.INFO` and `ThirstApi.CACHE` stay keyed by `Item`. The read on the tooltip path must not
  allocate on 1.20.1 either.
- The vanilla components core reads (`POTION_CONTENTS`, `CONSUMABLE`, `CUSTOM_MODEL_DATA`, `ITEM_MODEL`,
  `BLOCK_ENTITY_DATA`) must all be behind `Vanilla` or `DrinkItem`. On 1.20.1 they become `PotionUtils`,
  `Item.Properties.food`, the `CustomModelData` tag and the `BlockEntityTag` tag.

### 0.3 Payloads

`CustomPacketPayload`, `StreamCodec` and `RegistryFriendlyByteBuf` arrived with 1.20.5.

- `ThirstData` and `DrinkValuesPayload` become plain records with `write(FriendlyByteBuf)` and
  `read(FriendlyByteBuf)`. `DrinkValuesPayload` writes items as raw registry ids
  (`BuiltInRegistries.ITEM.getId`) instead of `ByteBufCodecs.registry`.
- Each `Loader` wraps them: from 1.20.5 into a `StreamCodec` through `StreamCodec.of`, on 1.20.1 into its
  own channel (step 1.3 and step 2.3).
- `ThirstData.CODEC`, the save format, stays as it is. `Codec` exists on 1.20.1.

### 0.4 Node names

`-neoforge` is hard-coded as "the other loader" in `settings.gradle.kts`, `stonecutter.gradle.kts`
(`endsWith("-neoforge")`), `build.neoforge.gradle.kts`, `.github/workflows/build.yml`,
`.github/scripts/update_mc_deps.py` (`node_loader`), `tools/release/publish.py`,
`tools/benchmark/bench.py` and `tools/agent/new_world.py`.

- Add `loaderOf(node)` and `minecraftOf(node)` in `build-logic`, next to the `Loader` enum, and a Python
  helper that the scripts share. Every place above asks those.
- Add `FORGE("forge")` to the `Loader` enum, with no node using it yet.

### Check for phase 0

`runGametest` on `1.21.1`, `1.21.1-neoforge`, `26.3.x` and `26.3.x-neoforge`, the oldest and newest node
of each loader. The full ten-node run happens once, before phase 0 is merged. `checkVersionSeam`,
`checkOptionalSeam` and `checkApiSurface` must pass. `update_mc_deps.py --dry-run` must list the same
changes as before the refactor.

## Phase 1: the Fabric node `1.20.1`

### 1.1 The node

- `settings.gradle.kts`: `version("1.20.1", "1.20.1")`.
- `stonecutter.properties.toml`: `[fabric."1.20.1"]` with `mod.mc_compat`, `mod.mc_releases`,
  `deps.fabric_api` (0.92.x+1.20.1), `deps.modmenu` (7.x), `deps.cloth_config` (11.x) and
  `deps.appleskin`/`deps.jade` (1.20.1 uploads, pinned by Modrinth id). Add no integration keys yet.
- `build.gradle.kts`: `requiredJava` is 17 below 1.20.5. The mixin configs get `JAVA_17` through the
  existing `expand`.

### 1.2 Vanilla differences

Fix compile errors by extending `platform/`, never at the call site. Expected differences, confirmed
only when compiling:

| Area | 1.20.1 | Where it goes |
|---|---|---|
| `ResourceLocation.fromNamespaceAndPath` | `new ResourceLocation(ns, path)` | `replacements`, a pure rename |
| Effects as `Holder<MobEffect>` | `MobEffect` itself | `Vanilla` |
| `isSameItemSameComponents` | `isSameItemSameTags` | `replacements` |
| `appendHoverText(stack, TooltipContext, …)` | `(stack, Level, …)` | `mixin/` or `platform/DrinkItem` |
| HUD sprites (`blitSprite`, `textures/gui/sprites/`) | No sprite atlas before 1.20.2: `blit` from a texture sheet | `ClientVanilla`, plus a texture sheet built from the existing sprites |
| Item model definitions (`items/`) | Only models, the same as 1.21.1 today | datagen, already forked for 1.21.1 |

If `Vanilla` or `ClientVanilla` gains a whole topic, it moves into a class of its own
(`platform/Registries`, `client/platform/HudTextures` and so on).

### 1.3 The Fabric loader on 1.20.1

Fabric API on 1.20.1 lacks or differs in four of the modules `src/main/fabric` uses:

| Module today | On 1.20.1 | Where it goes |
|---|---|---|
| `attachment.v1` (player data) | Not present | `src/main/fabric-legacydata`: a mixin on `ServerPlayer` saving `ThirstData.CODEC` in `addAdditionalSaveData`/`readAdditionalSaveData`, copied on `ServerPlayerEvents.COPY_FROM`, synced on join, respawn and dimension change. The current code moves to `src/main/fabric-attachment` |
| `PayloadTypeRegistry` | `ServerPlayNetworking.send(player, id, buf)` and `ClientPlayNetworking.registerGlobalReceiver(id, …)` | the same pair of directories |
| `loot.v3.LootTableEvents` | `loot.v2`, a different `MODIFY` signature | `Loader`, one `//?` |
| `resource.conditions.v1` with `ResourceConditionType` | `ResourceConditions.register(id, Predicate<JsonObject>)` | `Loader`, one `//?` |

Check whether `CommonLifecycleEvents.TAGS_LOADED` and the transfer API's `ContainerItemContext` exist
in the chosen Fabric API build. Both are expected to.

### 1.4 Mixins

Check the eight core mixins against 1.20.1 targets: `BlocksMixin`, `BottleItemMixin`, `BucketItemMixin`,
`CauldronBlockMixin`, `FoodDataMixin`, `LayeredCauldronBlockMixin`, `ItemStackMixin` and `PlayerMixin`.
Do the same for the client and Fabric-client configs. Only the `@Inject` signature forks. The body stays
one line.

### 1.5 Data

- Datagen runs on the new node into `src/main/generated/1.20.1/`. 1.20.1 names its data directories in
  the plural (`recipes`, `advancements`, `loot_tables`, `tags/items`, `tags/blocks`), uses pack format
  15, builds recipes through `Consumer<FinishedRecipe>` and advancements through
  `Consumer<Advancement>`. The providers fork through a datagen-side helper, the same way they already
  do for 26.1 and 26.2.
- Recipes that match water by grade use `fabric:components` today. On 1.20.1 that becomes `fabric:nbt`.
- The hand-written gametest data (`src/gametest/resources/data/c/tags/item`, `.../structure`) needs the
  plural names on 1.20.1. Add a rename in `processGametestResources` for nodes below 1.21, not a second
  copy of the files.
- The mod's own data pack folder `data/<ns>/thirstwastaken2/drinks/` is unaffected.

### 1.6 Gametests

Fabric's own `@GameTest` came with 1.21.5. On 1.20.1 a test uses vanilla's annotation with
`FabricGameTest.EMPTY_STRUCTURE`, the same path 1.21.1 takes today (see
[src/gametest/java/AGENTS.md](../../src/gametest/java/AGENTS.md)).

### Check for phase 1

On `1.20.1`: `build`, `runServer` (prints `initialized for Minecraft 1.20.1`), `runGametest`, `runDatagen`
then `checkDatagen`, `checkLang`, `checkApiSurface`, `checkOptionalSeam` and `checkVersionSeam`. Then
`runClient -Pagent=tools/agent/smoke/boot.jsonl`, and a look at the HUD and a waterskin tooltip in a
dev client.

## Phase 2: the Forge node `1.20.1-forge`

### 2.1 Build

- `settings.gradle.kts`: `version("1.20.1-forge", "1.20.1").buildscript = "build.forge.gradle.kts"`.
  Resolve `net.neoforged.moddev.legacyforge` with `apply false`, next to `net.neoforged.moddev`, and add
  `https://maven.minecraftforge.net/` to the repositories.
- `stonecutter.properties.toml`: `[forge."1.20.1"]` with `deps.forge` (47.x), `mod.mc_compat =
  "[1.20.1,1.20.2)"` and the Forge uploads of AppleSkin, Jade and Cloth Config.
- `build.forge.gradle.kts` starts as a copy of `build.neoforge.gradle.kts`, with these changes:
  - `legacyForge { version = "1.20.1-47.x" }`.
  - `mixin { add(sourceSets.main, "thirstwastaken2.refmap.json") }`.
  - Mixin configs named in the jar manifest (`MixinConfigs`), since Forge 1.20.1 does not read them from
    `mods.toml`.
  - Dependencies remapped through `obfuscation.createRemappingConfiguration`.

  Once both scripts work, whatever they share moves into `gradle/`.

### 2.2 Sources

| Path | What |
|---|---|
| `src/main/forge/java/.../ThirstWasTaken2Forge.java` | `@Mod`, calls `ThirstWasTaken2.initialize` |
| `src/main/forge/java/.../platform/Loader.java` | `DeferredRegister`, `MinecraftForge.EVENT_BUS` events, `SimpleChannel`, player data Capability |
| `src/main/forge/resources/META-INF/mods.toml` | Not `neoforge.mods.toml`. `loaderVersion = "[47,)"` |
| `src/client/forge/java/.../client/platform/ClientLoader.java` | `RegisterGuiOverlaysEvent`, `ConfigScreenHandler.ConfigScreenFactory` |
| `src/main/forge-fluidhandler` | `IFluidHandlerItem` through `AttachCapabilitiesEvent<ItemStack>` and `ForgeCapabilities.FLUID_HANDLER_ITEM` |
| `src/gametest/forge`, `src/dev/forge` | `@GameTestHolder`, `@PrefixGameTestTemplate(false)`. The dev tools' Forge side |

Every NeoForge event core uses has a Forge counterpart: `LootTableLoadEvent`, `OnDatapackSyncEvent`,
`TagsUpdatedEvent`, `RegisterCommandsEvent`, `PlayerInteractEvent` and `TickEvent.ServerTickEvent`
(filtered by phase).

### 2.3 Player data and sync on Forge

A Capability attached in `AttachCapabilitiesEvent<Entity>` saves `ThirstData.CODEC` through
`INBTSerializable`. It is copied in `PlayerEvent.Clone` and sent over the `SimpleChannel` on
`PlayerLoggedInEvent`, `PlayerRespawnEvent` and `PlayerChangedDimensionEvent`. The channel accepts a
missing mod on the other side, like the other loaders, so a vanilla client can still join.

### 2.4 Resources

The Forge node reuses the Fabric `1.20.1` datagen output and translates it, the way NeoForge does:
`fabric:load_conditions` becomes `conditions` with `forge:mod_loaded`, and `fabric:nbt` becomes
`forge:partial_nbt`. `neoForgeJson` becomes one translator with a table per loader, and
`checkNeoForgeResources` becomes a check that runs on both non-Fabric loaders.

### 2.5 Checks and tooling

| Place | Change |
|---|---|
| `checkLoaderSeam` | Also forbids `net.minecraftforge.` |
| `checkOptionalSeam` | Core roots add `src/main/forge`, `src/client/forge` and `src/main/forge-fluidhandler`. The annotation list adds `Lnet/minecraftforge/fml/common/Mod;` |
| `checkDataConditions` | Knows `forge:mod_loaded` |
| `.github/workflows/build.yml` | Reads `[forge."…"]` tables and names them `<version>-forge`. The Forge job runs the same steps as the NeoForge one |
| `update_mc_deps.py` | Forge versions from `maven.minecraftforge.net` (`promotions_slim.json`). Modrinth loader `forge`. Suffix `+forge` |
| `publish.py`, `publish_curseforge.py` | Loader `forge` and game version 1.20.1 |
| `src/watchdog` | Check whether a Forge `-Pagent` run needs it. Forge 1.20.1 fails early the same way FML does |

### Check for phase 2

On `1.20.1-forge`: `build`, `runServer`, `runGametest`, the resource check, `checkOptionalSeam` and
`checkLoaderSeam`, then a dev client boot. Then the full twelve-node run once, before merging.

## Phase 3: integrations on 1.20.1

One at a time, each as a small integration: code, test and changelog, with no plan of its own. Each one
adds `Loader.FORGE` (or `FABRIC`) to its row, sets its deps key in the `1.20.1` tables, and puts its
1.20.1 NBT forks in its own `platform` package.

| Mod | 1.20.1 builds to check | Note |
|---|---|---|
| AppleSkin, Jade, Mod Menu | both loaders, Mod Menu Fabric only | With phase 1 and 2, like every node |
| Farmer's Delight | Forge original, Fabric Refabricated | A large share of 1.20.1 packs |
| Create | Forge (Create 6 has a 1.20.1 Forge build) | Today only `1.21.1-neoforge` builds it |
| Serene Seasons | Forge, Fabric to check | Already on every node |
| Sophisticated, Supplementaries, Cold Sweat, Brewin' and Chewin' | Forge 1.20.1 builds exist | By demand |

## Phase 4: docs and release

- Update `AGENTS.md` (version list, Java rule, node list, integration table), `platform/AGENTS.md`,
  `src/gametest/java/AGENTS.md` and [VERSION-DIFFERENCES.md](VERSION-DIFFERENCES.md).
- Add both nodes to [MANUAL-TESTING.md](MANUAL-TESTING.md) and rows to
  [BENCHMARK-BASELINE.md](benchmark/BENCHMARK-BASELINE.md).
- Update the docs site's install page, the CHANGELOG, and the Modrinth and CurseForge pages
  (the `write-docs` skill).

## Risks

| Risk | What to do |
|---|---|
| HUD icons: 1.20.1 has no GUI sprite atlas | Found in step 1.2. A texture sheet built from the existing sprites, drawn through `ClientVanilla` |
| Recipes that match by grade | `fabric:nbt` and `forge:partial_nbt` match any stack whose tag contains the grade, the same as the components match today. A gametest checks it |
| Forge mixins with SRG names | The refmap from step 2.1. A mixin into a Forge-only class needs `remap = false` |
| `Vanilla` grows | Split it by topic as soon as a topic has more than a few methods, and use per-generation directories for whole loader classes |
| CI time | Two more jobs. Phase 0 keeps them from failing for reasons other than 1.20.1 |
