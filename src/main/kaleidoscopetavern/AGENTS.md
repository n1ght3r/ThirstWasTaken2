# src/main/kaleidoscopetavern — water quality in Kaleidoscope Tavern

[Kaleidoscope Tavern](https://modrinth.com/mod/kaleidoscopetavern) (mod id `kaleidoscope_tavern`,
package `com.github.ysbbbbbb.kaleidoscopetavern`) is Kaleidoscope Cookery's sibling: a barrel that
ferments wine and spirits, a tap, a shaker for cocktails, and bottles that can be put down as blocks.
Every one of them kept a fluid or a block and nothing else, so the grade a container had was lost on
the way through. This directory is where that gets fixed. The plan, the order of work and what was found
are in [docs/dev/integration/cooking/KALEIDOSCOPE-TAVERN-INTEGRATION.md](../../../docs/dev/integration/cooking/KALEIDOSCOPE-TAVERN-INTEGRATION.md).

**Built on the eight nodes that set the key**: `1.21.1-neoforge`, `1.20.1-forge` and the six Fabric
nodes. What it does there:

- **the barrel keeps the grade** of the water poured in, the worse of two grades poured together, and
  hands it back on the container drawn out;
- **the barrel refuses sea water**, which it would otherwise ferment into rum as safe as any other, and
  uses the click up so the bucket is not emptied over its lid;
- **on Fabric the barrel refuses this mod's own containers** (waterskin, canteen, flask, terracotta
  bowls): Refabricated hands back any container that is not a bucket as an empty bucket, and fills a whole
  water bucket for a serving drawn;
- **a tap copies the grade** of the water cauldron it draws from into the cauldron or the placed bottle
  below, and **samples the world's water** behind a waterlogged block, sea water included;
- **a placed water bottle keeps its grade beside it** and drops a bottle of that grade, however it drops;
- **the shaker refuses a sea-water bottle**; fresh water of any grade goes in;
- and **Jade** names the barrel's grade from any of its blocks.

The drink values are not here. They are ids in `ThirstConfig`, common code that names no class of the
mod, so they reach every node. Fermented drinks and cocktails carry no grade and are safe whatever fresh
water went in, as Brewin' and Chewin's are.

The tap into Kaleidoscope Cookery's stockpot and teapot, which only Kaleidoscope Chinese Food adds, is
graded in [src/main/kaleidoscope](../kaleidoscope/AGENTS.md) (`TapCookeryMixin`), since what it writes to
is Cookery's and it needs no class of Tavern's.

## Which build

| Node | Mod | Modrinth project |
|---|---|---|
| `1.21.1-neoforge`, `1.20.1-forge` | the official mod | `kaleidoscopetavern` |
| `1.20.1`, `1.21.1`, `1.21.11`, `26.1.x`, `26.2.x`, `26.3.x` | Refabricated, the Fabric port | `kaleidoscope-tavern-refabricated` |

The official mod has published no NeoForge build past 1.21.1 (its `26.1.2` branch builds one that was
never uploaded), so the other NeoForge nodes do not set the key. Refabricated has the same mod id and the
same package, and everything touched names no loader, so **all three loaders compile this directory**,
and `checkLoaderSeam` keeps it that way. Each key is pinned by Modrinth version id.

On `1.20.1`, `1.21.1` and `1.21.11` Refabricated requires Forge Config API Port,
`deps.forge_config_api_port`, on the `runClient` classpath only, as Kaleidoscope Cookery does; on
`1.20.1` also Reach Entity Attributes, nested in its jar beside Fabric API modules the run already has,
so `build.gradle.kts` unpacks only that one. From 26.1 on it needs neither.

**The `1.21.11` build cannot start.** Refabricated 1.2.0.5's `PlayerMixin` registers an entity data
serializer from `Player`'s static initializer, which current Fabric API refuses, so any 1.21.11 game with
it crashes on launch, with or without this mod. `runtime.kaleidoscope_tavern = false` in that node's table
keeps it compiled against but off `runClient` and `runDatagen` (CI's datagen crashed on it). The
integration is built there and untested in game; remove the key once a fixed build is pinned.

```
kaleidoscopetavern/java/com/thirstwastaken2/kaleidoscopetavern/
  KaleidoscopeTavernPresence     the gate: a classpath probe for the barrel, one per mixin target, and
                                 one for Refabricated's own fluid helper
  KaleidoscopeTavernMixinPlugin  applies each mixin only where the gate allows it
  BarrelWater                    our interface on the barrel: the grade it holds, for the Jade reader
  BarrelLookup                   the barrel's water from any of its blocks, for the Jade reader
  TavernWater                    the only place that reads or writes a grade: barrel, tap, placed
                                 bottle, shaker
  ReturnedWater                  the grade on its way back out of the barrel, per thread
  mixin/BarrelBlockEntityMixin   addFluid, removeFluid, save and load
  mixin/ItemUtilsMixin           stamps the container the barrel hands back through ItemUtils
  mixin/WaterCauldronTapMixin    the tap on a water cauldron
  mixin/WaterloggedTapMixin      the tap on a waterlogged block
  mixin/BottlePlaceMixin         a water bottle shift-placed as a block
  mixin/BottleDropMixin          what a placed water bottle drops (Minecraft's BlockBehaviour)
  mixin/ShakerBlockEntityMixin   the shaker's addIngredient
kaleidoscopetavern/resources/
  thirstwastaken2.kaleidoscopetavern.mixins.json
../../client/kaleidoscopetavern/java/com/thirstwastaken2/client/kaleidoscopetavern/
  KaleidoscopeTavernJade         adds BarrelLookup to client/compat/JadeIntegration's block readers
```

## How a grade moves

**The barrel.** It is three blocks a side, one block entity at the bottom centre, reached from every
click through `BarrelBlock.getBarrelEntity`, so the grade lives in one `@Unique` field there, saved as one
int under `thirstwastaken2:water_quality` (`WaterPurity.storedValue`: 1 to 4 for the grades, 5 for sea
water). It holds four buckets.

- **Read before the call, never after.** `addFluid` empties the container, so the grade is read first
  and kept only when the call returns true: the worse of it and what the barrel held, or the poured grade
  alone into a barrel with no graded water. Another fluid poured in keeps nothing.
- **The field is only trusted while the barrel holds water and is not brewing.** `thirst$heldWater`
  answers null otherwise, which is what is saved, what Jade shows and what a remove call stamps. Once a
  recipe starts, the tank is emptied into the brew and the field means nothing; once the barrel is
  empty, the next fill writes it again.
- **Its tank is read the way the mod's own recipe check reads it.** The tank is NeoForge's or Forge's
  `FluidTank` on the official mod and a Fabric Transfer API storage on Refabricated, so naming it would
  name a loader. Both builds make a `BarrelRecipeContainer` from `getIngredient()` and `getFluid()` and
  ask it for a plain `Fluid`, so the mixin does the same: the source names no loader type, and each node
  compiles the call against its own jar.
- **The tank is read at most once a game tick.** Jade asks every frame while a player looks at the
  barrel, so whether it holds water is kept in two plain fields of the block entity, the answer and the
  tick it was read on, and read again on the next tick or as soon as a fill, a draw or a load (the client's
  sync) goes through the mixin. Whether it brews is read live, first, so a tank emptied into a brew is
  never answered from the cache. No static map: the fields go with the block entity.
- **Out through `ItemUtils.getItemToLivingEntity`**, which both builds hand the filled container to,
  while `ReturnedWater.during` holds the grade. Refabricated builds a fresh bucket there; the official mod
  hands back the container its fluid capability filled. Both are stamped through
  `WaterPurity.setQuality`, so fresh water gets `water_salty: false`.
- **A refused container uses the click up.** `addFluid` returns true without doing anything, so the
  barrel's `useItemOn` answers success. Returning false would make it try `removeFluid`, then pass the
  click on, and a bucket's own use would empty sea water over the lid.
- **Refabricated's barrel turns containers into buckets.** `FluidUtils.emptyItem` hands back
  `onConsumed(result)`, an empty bucket for any fluid container that is not a bucket, and `fillItem` hands
  back `resource.getFluid().getBucket()`, a full bucket, whatever was held out. So a canteen poured in came
  back a bucket, and an empty one held out came back a whole bucket of water for one serving. The barrel
  refuses this mod's containers there (`WaterContainers.handles`), with an action bar message; the
  official mod fills and empties them through their own fluid handler and keeps them.

**The tap.** It never drains its source, by the mod's design; the integration only makes it copy the
right water. Both behaviours rebuild the cauldron below from `WATER_CAULDRON.defaultBlockState()`, whose
`purity` is unset, so each mixin runs at every return of `onEndExtract`, sees whether the block below
changed, and stamps it: the source cauldron's grade, or the world's water sampled at the waterlogged block
(`tapPos.relative(facing.getOpposite())`), the worse of it and what the cauldron below held. An unset
side lets the other decide. A placed empty bottle below becomes a placed water bottle, whose grade goes
where a placed bottle's does.

**The placed water bottle.** It has no block entity, and its loot table drops a plain water bottle, so
the grade is kept beside it in the level, in a `platform/SavedPositions` named
`placed_water_bottles`: one int per position, the same stored value. Written when the mod's shift-use
places one (the bottle in hand is read before it is shrunk; an unstamped one clears the entry) and when
a tap fills one. Read and removed in `BlockBehaviour.getDrops`, which every drop goes through: broken,
blown up, pushed by a piston, and picked up by hand, which the bottle builds from `Block.getDrops`. Only
for the `kaleidoscope_tavern:water_bottle` block, looked up once.

- **What it cannot see.** A bottle removed without dropping anything (creative breaking, an arrow
  shattering it, `/setblock`) leaves its entry, and a water bottle `/setblock` puts in the same place later
  would drop with that grade. Every way the game itself places one, shift-use and the tap, writes the
  entry afresh, so only a command can meet a stale one.

**The shaker.** `#cocktail_ingredient_white` holds any potion, so a water bottle is an ingredient.
A sea-water bottle is refused in `ShakerBlockEntity.addIngredient`, the way the mod refuses a drink below
its brew level, before the bottle is taken.

## How it stays optional

The same three layers as Kaleidoscope Cookery.

1. **Build.** Only where `deps.kaleidoscope_tavern` is set and the loader is in its row does the loader
   script add this directory and append the mixin config to the built manifest (on NeoForge and Forge with
   `kaleidoscope_tavern` as an optional dependency), from
   [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   Fabric needs it remapped, since it is mixed into, so it is `modCompileOnly`; NeoForge takes it as
   `compileOnly`, Forge as a remapped `modCompileOnly` (its jar is in SRG names). All put it on
   `runClient` only, so `runServer` and the gametests run without it, and
   `-PwithoutOptional=kaleidoscope_tavern` (or `kaleidoscope-tavern`, `kaleidoscope-tavern-refabricated`)
   leaves it out of `runClient` as well.
2. **Runtime gate.** `KaleidoscopeTavernPresence` answers every question with a resource lookup, which
   never loads a class, and names no class of the mod's, no Minecraft class and no loader. Each target is
   probed by name, and a missing one logs once and is skipped. `BottleDropMixin` targets a Minecraft
   class, so it is applied whenever the barrel is there.
3. **Mixin plugin.** `KaleidoscopeTavernMixinPlugin.shouldApplyMixin` asks the gate for the mixin's own
   target, so a class renamed upstream only takes its own mixins down.

`KaleidoscopeTavernJade` is a Fabric `jade` entrypoint and a NeoForge and Forge `@WailaPlugin`. Jade
loads it whether or not the mod is installed, so it asks the gate and hands over through a static call
into `BarrelLookup`.

## Names on Fabric

As in Kaleidoscope Cookery: the mod's own methods (`addFluid`, `removeFluid`, `onEndExtract`,
`onRightClickBlock`, `addIngredient`, `getItemToLivingEntity`) are matched by name with `remap = false`,
and every Minecraft name says `remap = true`: `saveAdditional`, `loadAdditional`, `load`, the
`Level.setBlock` call, and `BlockBehaviour.getDrops`. Refabricated's 1.20.1, 1.21.1 and 1.21.11 jars are
in intermediary for Minecraft's names; 26.x has nothing to rewrite. `VanillaBottlePlaceEvent.onRightClickBlock`
is a NeoForge or Forge event handler on the official mod and a `UseBlockCallback` on Refabricated, with
different parameters, so `BottlePlaceMixin` names the method alone and reads the bottle from the one
`ItemStack` local both keep.

## Version forks

Only in the barrel mixin, each body one line:

| Difference | Where |
|---|---|
| `saveAdditional` / `loadAdditional` take a `ValueOutput` / `ValueInput` from 1.21.6 | `BarrelBlockEntityMixin`; `TavernWater.save` / `load` take `putInt` / `getIntOr`, so each branch makes the same call |
| Before 1.20.5 the block entity saves through `saveAdditional(CompoundTag)` and loads through `load(CompoundTag)`, with no registries | `BarrelBlockEntityMixin` |

The placed bottles' store is `platform/SavedPositions`, whose save changed twice; see its Javadoc.
Vanilla's other differences go through `Vanilla` (the action bar, `sendOverlayMessage`, from 26.1).

## Checking it

- `checkOptionalSeam` finds the plugin, the gate and `KaleidoscopeTavernJade` as classes loaded without
  the mod, and passes.
- `runGametest` passes unchanged on every node: the mod is never on its classpath.
- `./gradlew ":<node>:runClient" -Pagent=tools/agent/smoke/boot.jsonl`, with the mod, with
  `-PwithoutOptional=kaleidoscope_tavern` and with `kaleidoscope_tavern,jade`, comes up and stays up.
- What it does is checked in a real client with
  [tools/agent/integrations/kaleidoscope-tavern.jsonl](../../../tools/agent/integrations/kaleidoscope-tavern.jsonl),
  whose header says how to run and verify it, and
  [tools/agent/integrations/kaleidoscope-tavern-1.20.1.jsonl](../../../tools/agent/integrations/kaleidoscope-tavern-1.20.1.jsonl),
  the same in 1.20.1's NBT.
