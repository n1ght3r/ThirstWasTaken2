# src/main/java — common code

Runs on both sides but **must not reference `net.minecraft.client`**. Anything client-only belongs in
`src/client/java` (see its own AGENTS.md). Gameplay here is server-authoritative: the server owns
thirst, and the client only receives it through the `PlayerData` sync.

## Where to change what

| Want to change | Go to |
|---|---|
| What an item restores | `config/ThirstConfig.defaultDrinks()` / `defaultFoods()`, read through `api/ThirstApi` |
| What data packs say an item restores, and syncing it to clients | `data/DataPackDrinks`, `data/DrinkValuesPayload` |
| Anything another mod calls | `api/` only: `ThirstApi`, `ThirstEvents`. Public API; see `docs/docs/developers/`, the site's developer pages |
| Drain rate, climate, damage, full-bar drinking rules, hand drinking | `data/ThirstManager` |
| Natural healing: food's thirst and illness gate, and quenched's own heal once saturation is spent | `data/HealthRegen`, from `mixin/FoodDataMixin` and `ThirstManager.tickPlayer` |
| The state record itself (thirst, quenched, exhaustion) | `data/ThirstData` |
| A new config key | `config/ThirstConfig` (field + `sanitize()`), then the client config screen |
| Switching off the mod's own items (Mod Items page) | `config/ThirstConfig.isItemEnabled`, read by the `item_enabled` recipe condition (`platform/Loader.registerResourceConditions`, the generators in `src/datagen`) and by the creative tab |
| Bowls, waterskin, copper canteen, iron flask, creative tab | `item/` (the three carried containers are one class, `WaterskinItem`) |
| The mod's own mob effects (Parched) | `effect/ThirstEffects`; what they do lives where they matter, e.g. Parched's drain in `ThirstManager.tickPlayer` |
| The copper and iron hanging pots: capacity, boiling, filling and drawing | `block/` |
| The copper distiller, a two-block block placed and broken whole like a bed | `block/DistillerBlock`; its models from `tools/distiller/generate_distiller_model.py`; checked in a client by `tools/agent/gameplay/distiller.jsonl` |
| The distiller at work: tanks, fire, distilling to Pure, the slots and hopper faces, the right-click pour and draw | `block/DistillerBlockEntity` on the boiler half, `block/DistillerWater` for which containers it takes and fills, `block/DistillerInteractions.useMachine`. See `docs/dev/mechanics/DISTILLATION-PLAN.md` |
| The distiller's fire drawn, lit and smoking only while fuel burns | the boiler half's `lit` property, set by `DistillerBlockEntity`; `DistillerBlock.burning` for light and smoke; the cold model from the same generator |
| The salt sea water leaves, only when another mod has salt | `block/DistillerSalt` over the `thirstwastaken2:distiller_salt` item tag (`ThirstItemTagProvider`, optional entries only) and `ThirstConfig.distillerSaltItem` |
| The distiller's four parts (pipe, boiler, cooling tub, brick firebox) under one Mod Items switch. Firebox, boiler and tub are blocks too; built in the world, a boiler on a firebox merges into an unpiped boiler half, and the pipe joins it to the tub beside it | `block/DistillerPartBlock`, `block/CoolingTubBlock`, `item/DistillerBoilerItem`, `item/CopperPipeItem`, `config/ThirstConfig.enableCopperDistiller`; models from the same generator, the pipe a sprite from `generate_distiller_sprites.py` |
| The cooling tub's coolant, poured once from any water container, alone or in the machine | `block/DistillerInteractions`, the `cooled` property |
| Anything about water cleanliness | `purity/` (has its own AGENTS.md) |
| A vanilla behaviour hook | `mixin/` (has its own AGENTS.md) |
| Loot, optional mod integrations | `compat/` (has its own AGENTS.md) |
| Any line the mod adds to a tooltip | `tooltip/ThirstTooltip` (see the rules below) |
| An advancement the mod awards | `advancement/ThirstAdvancements`, plus its JSON in `data/thirstwastaken2/advancement/` |
| `/thirst` | `command/ThirstCommands` |
| Dev-only tooling such as `/thirst benchmark` | `src/dev/java` (own AGENTS.md), gated on `ThirstWasTaken2.DEV` |

## Init order

`ThirstWasTaken2.initialize` is the only entry point, called by the loader's entrypoint class
(`ThirstWasTaken2Fabric` in `src/main/fabric`, `ThirstWasTaken2NeoForge` in `src/main/neoforge`), and the
order matters:
`ThirstConfig.load()` → `ThirstData.register()` → `ThirstBlocks.register()` →
`ItemWaterData.register()` → `ThirstItems.register()` → `ThirstItems.registerCreativeTab()` →
`ThirstEffects.register()` → `LootIntegration.register()` → `TradeIntegration.register()` →
`Loader.registerResourceConditions()` → events.
Nothing in this source set may import a mod loader's API; it goes through `platform/Loader` (see
`platform/AGENTS.md`).

The five registration calls go through `Loader.onRegister`, one per registry. Fabric runs them on the
spot; a loader that freezes its registries before mods start runs them later, from its own registration
phase, so they must not depend on anything `initialize` does after them.

`ThirstItems.register()` is empty on purpose: calling it triggers the class's static initializer, which
is where the items are built and registered. **Nothing may touch a `ThirstItems` field before it runs**,
from a mixin, `ThirstApi` or a static field elsewhere, because an item cannot be built before its
registry accepts entries. `ThirstComponents` builds its types with the class but registers them only in
`register()`, which `ItemWaterData.register()` calls, so touching one of its fields early is harmless. The
items' properties name the components, so components are registered first.

Events registered there, in registration order per event:

- `Loader.onServerTickEnd` → `ThirstManager.tick`, then `WaterInteractions.tick` (drains the deferred
  queue).
- `Loader.onUseBlock` → `ThirstManager.drinkByHand`, `HangingPotInteractions.use`,
  `WaterInteractions.emptyWaterskinOnBlock`,
  `WaterInteractions.fillWaterskinFromCauldron`, `WaterInteractions.transferCauldronPurity`. A
  handler that returns anything but `PASS` stops the rest, which is why `transferCauldronPurity`
  deliberately returns `PASS` and defers its work.
- `Loader.onUseItem` → `WaterInteractions.fillFromWater`.
- `Loader.onRegisterCommands` → `ThirstCommands.register`.
- `Loader.onTagsLoaded` → `ThirstApi.clearCache`, because an item's value can come from the `c:drinks`
  tag and tags are rebound on every reload and server join; and → `DistillerSalt.clearCache`, for the
  distiller's salt comes from a tag too.
- `Loader.onServerDataReload` → `DataPackDrinks.reload`, which parses the data pack files and drops the
  `ThirstApi` cache itself when the values changed. `DataPackDrinks.registerPayload` declares
  `DrinkValuesPayload` through `Loader.clientboundPayload`, received by `DataPackDrinks.receive`, and `Loader.onDataPackSync` →
  `DataPackDrinks.sync` sends it to each player on join and after `/reload`, because the tooltip
  resolves on the client.

## Invariants worth not breaking

- **`api/` is a promise to other mods.** Its public signatures name only Minecraft, JDK and `api` types
  (`checkApiSurface` fails the build otherwise), a signature changes only after a deprecation, and an
  addition bumps `ThirstApi.API_VERSION`. The code behind it forwards to the internal classes; keep the
  behaviour in those.
- **`ThirstEvents` costs nothing unless someone listens.** The tick and the drink ask `hasListeners()`
  before building anything, and a new event on a hot path does the same.

- **One write point for player state.** `ThirstManager.set` only. `ThirstData` is a record, so mutate
  by deriving (`drink`, `addExhaustion`, `consumeExhaustion`, `withLevels`, `withEnabled`) and write
  only when the value actually changed — every write is a sync packet. `tickPlayer` accumulates into
  a local `updated` and writes once.
- **Vanilla exhaustion is buffered, not written.** `PlayerMixin` calls
  `ThirstManager.mirrorExhaustion`, which only adds the raw amount to the player's
  `data/ExhaustionTracker`. `tickPlayer` applies the total once, together with the Hunger effect
  refund, so a player who sprints, jumps and fights in the same tick still costs one packet at most.
  `addExhaustion` writes immediately and is only for one-off sources such as salt water.
- **Exhaustion is written in steps.** `tickPlayer` only writes when exhaustion crosses a quarter point
  (`ThirstManager.SYNC_STEP`), spends a point, or something else in the record changes. The rest waits
  in `ExhaustionTracker.unsynced` and is added to the next write, so the drain stays exact while the
  client receives exhaustion in quarter steps. Ticks that write nothing build no record either. Keep
  that fast path when changing the tick: it is where the per-player cost went.
- **The exhaustion modifier is cached per player for 20 ticks** on the same tracker. Reading armour
  protection builds a loot context for every enchantment on every equipped item. A dimension or
  config change recomputes it straight away; anything else (biome, armour, Fire Resistance) may lag
  by up to a second, which exhaustion accumulating over many seconds hides.
- **Per-`Item` caches, never per-call string work.** `ThirstApi.CACHE` and `WaterPurity.INFO` are
  `ConcurrentHashMap`s keyed by `Item` identity. `ThirstApi` drops its cache when
  `ThirstConfig.generation()` changes; `WaterPurity.INFO` never invalidates, so it may only hold
  facts that cannot change at runtime (registry id, not config values — those resolve to
  `PURITY_FROM_CONFIG` and are looked up per call).
- **`ThirstConfig.get()` is cheap, `sanitize()` is where clamping lives.** Compiled regex patterns are
  transient fields rebuilt by `sanitize()`; never compile a pattern on a call path.
- **Side checks.** Interaction callbacks fire on both sides. Return `InteractionResult.SUCCESS` on the
  client for the swing animation and `SUCCESS_SERVER` from the server branch;
  `ThirstManager.drinkByHand` and `WaterInteractions.fillFromWater` are the reference shape.
- **Sounds from a server-only path need `level.playSound(null, ...)`.** `Player#playSound` excludes the
  player themselves, so the drinker would hear nothing — see the comment in
  `ThirstManager.drinkByHand`.

## The hanging pots

`block/HangingPotBlock` is adapted from Dehydration's campfire cauldron (GPL-3.0; see `CREDITS.md`).
The copper and iron pots are two registrations of it that differ in look, sound, recipe and the
boil time and capacity they read from the config whenever needed (`copperHangingPotBoilSeconds`
and `ironHangingPotBoilSeconds`, 3 and 4 by default; `copperHangingPotCapacity` and
`ironHangingPotCapacity`, 3 and 6, up to 64), so code that asks whether a block is a pot checks
`instanceof HangingPotBlock`, never one of the two. Its quality is in `WaterPurity.BLOCK_PURITY`,
like a cauldron, and `HangingPotInteractions` does all the filling and drawing itself, inline,
because vanilla has no interaction for the block to defer to. It refuses every pour where
`Vanilla.waterEvaporates`, so a pot never holds water in the Nether. Keep that handler ahead of
`emptyWaterskinOnBlock`: a sneaking player's waterskin pours into the pot rather than onto the ground.

`HangingPotBlockEntity` holds the servings and the boiling steps done, and is storage only: **no
ticker**. The blockstate's `level` is only how full the pot looks, in thirds (`FILLS`), so 64
servings need no more blockstates than three. A pot saved before the block entity existed reads its
servings from that `level` on first access, which counted servings when every pot held three.
Boiling runs on scheduled ticks, `STEPS_PER_SERVING` per serving, so the time is per serving like a
furnace's per item; every write schedules the next step while there is something to boil over a lit
campfire, and `supportChanged` schedules one when the campfire below is lit again. A step that finds
the fire out schedules nothing, so a pot with nothing to do costs nothing. Anything that adds water
goes through `HangingPotBlock.pour`, which keeps what has boiled and counts Clean and Pure water as
boiled; drawing goes through `draw`; `setWater` sets a pot outright and starts its count at nothing.
A pot over a lowered capacity keeps its water and takes none until it has room (`room`).

## Tooltip lines

Every line the mod adds to an item tooltip goes through `tooltip/ThirstTooltip.appendTo`, and they
are grouped in three tiers. A new item follows the same tiers, so that a tooltip stays scannable no
matter how many lines it grows.

| Tier | Colour | Answers | Examples |
|---|---|---|---|
| What the item is | `ChatFormatting.GRAY` | how it works, what state it is in | `Contains 3/3 drinks`, `Smelt to hold water` |
| What it holds | `WaterPurity.purityColor`, salt's cream | is this worth drinking | `Murky`, `Salty` |
| What drinking does | the droplet font, no colour of its own | how much it restores | the thirst and quenched rows, AppleSkin only |

- **Grey is for describing the item, never for a value the player weighs.** A grade or a restored
  amount has to stand out from the grey; if a new line is something the player compares between two
  stacks, it belongs in the second or third tier, with a colour of its own.
- **Keep a grey line shorter than the item's name.** A hint that widens the tooltip box drags the eye
  away from the name and the grade. `Smelt to hold water` replaced a line twice that long for exactly
  this reason.
- **Do not invent a second palette.** Water colours live in `WaterPurity.purityColor` and salt's line
  colour next to it. Anything about water quality reuses those.
- **The third tier needs AppleSkin.** It is the thirst half of AppleSkin's food rows, so without it
  (or with `appleskinTooltipDroplets` off) `appendTo` stops after the second tier. Anything that must
  see the rows regardless, such as a gametest or the benchmark, calls `appendTo(stack, tooltip, true)`.
- **Tiers keep their order, and a tier may cancel the ones below it.** Salt water prints its own line
  and returns, because droplet rows under it would promise thirst it does not restore.
- **Lines are rebuilt every frame a stack is hovered.** Build a constant once and hand out `copy()`,
  as `CLAY_BOWL_HINT` and the droplet rows do; other mods are free to restyle a line they receive.
- **A new line means a new `tooltip.thirstwastaken2.*` key in all nine lang files.** Every one is
  required, and `checkLang` fails on a missing one; see `src/main/resources/AGENTS.md`.

## Divergences from upstream live as comments

Where behaviour intentionally differs from the original Forge mod, the reason sits next to the code
(`climateModifier`, `canDrinkWater`, `WaterPurity.at`). Keep that habit: a new divergence gets a comment
at the divergence, not only a note in the docs.
