# Purification rework: implementation

How to build [PURIFICATION-REWORK.md](PURIFICATION-REWORK.md). That page owns every number and
behaviour; this one owns the order, where the code goes, and what each step may cost at run time.
When they disagree, the design page wins and this one is fixed.

**Status: steps 0 to 10 done except the played balance tests, uncommitted.** See [Progress](#progress) at the end.

## Rules for every step

- **One step, one shippable state.** Each step builds, passes gametests and leaves the game playable
  on its own. Later steps may change a default an earlier one set, never its shape.
- **Test the nodes a step touches**, not all ten: `26.3.x`, `1.21.1-neoforge`, `1.20.1`,
  `1.20.1-forge` by default (newest, oldest NeoForge, both 1.20.1 loaders, as the design page's
  validation list says), plus any node whose `platform/` or integration directory the step forks.
  Full CI only before release.
- **Version differences** go in `platform/Vanilla` / `ClientVanilla` or a mixin fork, never at the
  call site (`checkVersionSeam`). Core code stays Java 17.
- **Config**: field, clamp in `sanitize()`, widget and reset in `client/config/ConfigCategory` on the
  page of its topic, nine lang files (`checkLang`).
- **Datagen** only through `src/datagen`, then `runDatagen` and `checkDatagen`; never hand-edit
  `src/main/generated/`.
- **Public API** (`com.thirstwastaken2.api`) keeps every signature. A new method bumps
  `API_VERSION`; nothing there gets a `//?`.
- **Docs** for players are written once, in step 9, with the `write-docs` skill.

## Performance budget

The mod's cost today is in [BENCHMARK-BASELINE.md](../benchmark/BENCHMARK-BASELINE.md): about
0.25 us and 7 to 90 B per player per tick, and zero bytes for `thirst_lookup` and
`exhaustion_mirror`. The rework adds per-tick drain, a timed hand drink and heal hooks, so it is the
first change in a while that touches every hot path at once. Rules:

| Path | Runs | Budget |
|---|---|---|
| `ThirstManager.tickPlayer` | every player, every tick | No new allocation. `thirst_tick_idle` B/op and `B/player/tick` stay within the baseline's spread. New state is plain fields on `ExhaustionTracker`, never a map or a record. One `getEffect` per effect per tick, as today |
| Attachment writes (sync packets) | on change | Still one write per tick at most. Baseline drain adds one write per quarter point, about every 75 ticks for an idle player; nothing else may add writes |
| `ThirstApi.thirstValues(ItemStack)`, tooltips | every frame per hovered stack | Zero allocation (`thirst_lookup` = 0). No string, regex or `new ItemStack` per call. Stack-dependent answers come from arrays prebuilt in `sanitize()` |
| Tooltip lines with actual gain | every frame | Component rows cached by value (thirst 0..20 x quenched 0..20 is 441 cells, built lazily), copied out; `tooltip_*` B/op unchanged |
| `FoodDataMixin` heal hooks | per heal (every 10 or 80 ticks) | Arithmetic and one effect lookup; no allocation |
| `WaterPurity.sampleAt` | interaction and Jade only | One heightmap read and a bounded column walk added; `sample_water` B/op unchanged |
| Hanging pots | idle pots | Zero cost when idle, as today: a block entity for storage only, **no ticker**, scheduled ticks only while boiling |
| Recipes | furnace lookups | Fewer recipes than today, not more (see step 7): no per-serving JSON recipes up to 64 |
| Stack size of water bottles | inventory code, very hot | On 1.20.5+ a `max_stack_size` component on the stack, so vanilla does the work. 1.20.1 only: one identity check in a `getMaxStackSize` mixin |

Each step that touches a row above ends with `runBenchmark` (`quick` is enough while iterating,
`standard` x3 through `tools/benchmark/bench.py --repeats 3` before calling the step done) on the
default nodes, compared with `aggregate.py --compare`. A change outside the spread is fixed or
written up here with the reason before moving on.

## Step 0: baseline and scaffolding

- Take a `standard` x3 baseline on `26.3.x` and `26.3.x-neoforge`, which have none, and refresh the
  four default nodes on the current machine. Record it in `BENCHMARK-BASELINE.md` with the mod
  version.
- Add benchmark operations for paths the rework creates, measured before they change so the delta
  is visible: `heal_food` (one saturation heal through `FoodDataMixin`), `thirst_lookup_potion`
  (a non-water potion stack), `sample_water_covered` (water under a roof). Steps 3 to 6 add theirs.
- Add a gametest helper that sets thirst, quenched, food, health and an effect in one call; most
  tests below need it.

Done when: baseline committed, new operations report numbers on the default nodes.

## Step 1: names, colours, config keys

Design page: "Names and colours". Pure rename and art; no gameplay changes.

- `WaterPurity.purityKey` to `thirst.water.dirty/.murky/.clean/.pure`; `purityColor` Murky to
  `0xBDB878`, others unchanged. `TooltipLines` still builds once.
- Lang: the renames in the design table, in all nine files ("Sạch", "Chất lượng nước" in `vi_vn`).
  Jade label "Water Quality", boil message "The water has boiled". Sophisticated `min_quality`.
- `ThirstConfig`: `defaultQuality`, `rainwaterQuality`, `dripstoneQuality`. Migration in `load()`,
  on the parsed `JsonObject` before Gson maps it: copy a legacy key only when the new key is absent,
  then drop the legacy key so the next save writes only the new one. Runs once per load, no cost later.
- ~~Eight textures recoloured (four bowl, four animated pot).~~ Done, then put back on 2026-10-06 at
  the maintainer's call: the water sprites keep their original colours. `tools/recolour_water.py`
  (local, ignored) refuses to run.
- Keep every persisted id the design page lists. Grep for each before finishing.

Tests: config migration gametest (legacy only, new only, both, custom value), `checkLang`, client
look at the tooltip and both sprites on one Fabric and one NeoForge node.

## Step 2: heat cap at Clean

Design page: "Treatment rules". Small, contained, and what everything later assumes.

- `WaterPurity.BOILED = 2`. Two helpers, the only places that know the cap:
  `WaterPurity.boil(quality)` raises a fresh grade below `BOILED` to `BOILED` and leaves anything
  else as it is (salt included), and `boilStep(quality)` gives `max(grade, min(grade + 1, BOILED))`
  for Cold Sweat's Boiler. Every heat source calls one of them: `WaterskinItem` boiling, `HangingPotBlock` (`withPoured` counts Clean and Pure as boiled),
  the Farmer's Delight Cooking Pot recipe output, Cold Sweat Boiler and campfire paths.
- Distiller and Create Sand Filter keep their own rules (Pure, +1 to Pure).
- Remove the deep-aquifer bonus now (one line in `sampleAt`); the covered cap waits for step 6.

Tests: extend `PurificationGameTest`: each heat source on each grade, Clean and Pure not reboiled
or lowered, salt unchanged. No benchmark: nothing on a hot path.

## Step 3: recovery values and the stack-aware lookup

Design page: "Recovery and quality", "Integration and transfer boundaries".

- `ThirstConfig.plainWaterValue = {6, 4}` and `quenchedPercent = {0, 25, 50, 100}`;
  `plainWaterDrinkTicks = 32`. Solid food, milk and prepared drink defaults per the design page.
  Defaults apply to fresh configs; existing explicit values stay.
- `ThirstApi.thirstValues(ItemStack)`: before the `Item` cache, `WaterPurity.isPlainWaterDrink(stack)`
  returns the config's prebuilt array. `minecraft:potion` keeps caching only the item-level answer
  (non-water), so nothing stack-dependent is cached under it. `isPlainWaterDrink` is already
  component reads only; keep it that way.
- Own vessels and bowls take the value from the same array, so all containers restore the same
  serving. Use durations from `plainWaterDrinkTicks`, through `platform/DrinkItem`.
- `ThirstData.drink`: thirst overflow above 20 is discarded for every source; quenched still capped
  at thirst. No public signature changes; `ThirstEvents.DRINK` still sees and may cancel amounts.
- Full-thirst rule: `canDrinkWater(player, stack)` also allows Clean or Pure when quenched < 20.
  The old one-argument method stays, deprecated if public. Automation (Sophisticated's upgrade) and
  previews use the new one.
- Tooltip: the third tier shows the actual gain (caps, grade, Upset Stomach) for the local player.
  Rows come from a lazily filled `Component[21][21]` and are copied out.

Tests: water vs potion vs Awkward potion, each grade, overflow at 18/19/20, quenched cap, event
cancel, top-up rule for Clean/Pure only. Benchmark: `thirst_lookup`, `thirst_lookup_potion`,
`tooltip_*`, `drink_*` within spread.

## Step 4: timed hand drinking (taken back)

Built, then removed at the maintainer's call: drinking by hand stays a sip per click. See Progress.

Design page: "Drinking by hand".

- Vanilla repeats `useItemOn` about every 4 ticks while the key is held. The server keeps, on
  `ExhaustionTracker`, the target `BlockPos` (a mutable copy, not a new object per click), the tick it
  started and the tick it was last refreshed. Three fields, no allocation per click.
- `drinkByHand` starts or refreshes; it gives nothing on click. `tickPlayer` checks one field
  (`handDrinkStart >= 0`) and only then: cancel if not refreshed within 6 ticks, target no longer
  water, out of reach, or the player stopped crouching; complete at 32 ticks with 6 thirst, 0
  quenched and one sickness roll. Sound and splash on start and completion only.
- No offhand second sip (already guarded); no overflow (step 3).
- Check the repeat interval on 1.20.1 and on the newest version in a client before settling the
  6-tick grace: if a version repeats more slowly, the grace comes from `platform/Vanilla`.

Tests: no gain before tick 32, release at 20 gives nothing, moving the target cancels, exactly one
completed drink at 40 held ticks. Benchmark: `drink_by_hand` B/op and `thirst_tick_idle` unchanged.

## Step 5: health gate, reserve bonus, drain

Design page: "Health and drain". The step that moves the per-tick cost, so it is benchmarked hardest.

Healing, all in `FoodDataMixin` and `HealthRegen`:

- Gate: natural food healing needs thirst 20, quenched > 0 and no Upset Stomach, on both heal paths.
  Delete `SLOW_FACTOR`, `allowsSlowHeal`, `NEARLY_HYDRATED` and the `thirst$dehydratedHealTimer`
  field. Blocked heals keep the existing food-exhaustion refund. Two config switches:
  `dehydrationHaltsHealthRegen` (existing) and a new `illnessHaltsHealthRegen`.
- Bonus: inside the redirect, read health before and after the base heal; on success and with a
  reserve of at least 6 (checked before the base heal), heal
  `min(quenchedHealthRegen * actual, missing)` and add `6 * bonus` to a new
  `ExhaustionTracker.healCost` field. `tickPlayer` folds it into its one write, unscaled, as it does
  the heal cost today. No second food cost.
- Delete `HealthRegen.healWithQuenched` and `quenchedHealTimer`. This **removes** a per-tick check
  (food level, `isHurt`, game rule) from `tickPlayer`, which pays for some of what follows.
- `quenchedHealthRegen` default 0.25, new meaning in its lang description and CHANGELOG.

Drain, in `tickPlayer`, three parts kept apart because they scale differently:

| Part | Per tick | Climate and fire relief | Nourishment | `EXHAUSTION` event |
|---|---|---|---|---|
| Activity (`raw` today) | mirrored vanilla | yes | cancels | sees it |
| Baseline | `4 / 1200` (a constant) | yes | no | does not see it; it is not something the player did, like the heal cost |
| Illness | Upset Stomach 16/1200 at I, 32/1200 at II | no | no | sees it |

  With a listener, the event gets activity + illness and its answer is split back in the same
  proportion; without one, no work. Baseline is skipped in creative, spectator, `/thirst enable`
  off and refilling Peaceful.
- `climateModifier`: the monotonic formula from the design page; `thirstDepletionModifier` default
  1.2 applies to the Nether's 3.0 too. Still cached 20 ticks per player; the formula is cheaper than
  today's (no `exp`).
- The baseline means `raw` is never zero for an active player, so `exhaustionModifier` runs every
  tick: it is a cached field read, but check the B/op. The fast path (`sameSyncStep`) still skips the
  write for about 74 of every 75 idle ticks.

Tests: the design page's health list (19/20 thirst, quenched 0/1/5/6, Upset on/off, both paths,
switches off, refunds, starvation, `naturalRegeneration` off), 1 HP base gives 0.25 bonus for 1.5
exhaustion, no bonus on a failed or potion heal, drain rates per part over 1200 ticks, Nourishment
keeps baseline and illness. Benchmark: new `heal_food`; `thirst_tick_idle`, `B/player/tick` and the
200-player tick on every default node. Expected and allowed: idle players now write about once
every 75 ticks; record that number in the baseline so it is not read as a regression later.

## Step 6: sickness and covered water

Design page: "Sickness", and the covered-water rule under "Treatment rules".

- `SicknessEffect.group` (optional string). `WaterSickness.drink` draws one roll per group per drink
  in a tiny local array (at most the number of lines; no map), independent rolls without a group.
  Default Dirty and Murky tables: Upset Stomach and Poison in group `illness`, Poison's chance at or
  under Upset's, enforced in `sanitize()`. Clean, Pure and Peaceful lists empty.
- Resolve each line's effect `Holder` once and keep it on a transient field, reset by `sanitize()`
  and on registry reload, instead of `Identifier.tryParse` per line per drink.
- Duration: Upset Stomach uses `max(R, min(R + D / 2, 1.5 * D))` in ticks; other effects keep today's
  rule. No taste Nausea in defaults.
- Remove Nausea bursts from `tickPlayer` (`burstChance`, the slow-tick roll): another per-tick
  cost gone. `UpsetStomach.SATURATION` to 0.5 and 0.25; quenched uses the same scale.
- Milk does not cure Upset Stomach. This is a loader seam: NeoForge 1.21.1+ effect cures, Forge
  1.20.1 curative items, Fabric a hook where milk clears effects. Behind `platform/Loader`, one
  implementation per loader. Honey and milk still clear Poison.
- Covered water: `sampleAt`, after the salt and `pure_water` checks and only when nothing is stored,
  walks up the water column from `pos` (at most 16 blocks) to its top, then compares with the
  `MOTION_BLOCKING` heightmap through a new `Vanilla.skyAbove(level, pos)`. The client receives that
  heightmap with each chunk, so Jade still shows what the server stamps. Covered water caps at Murky.
  Check that leaves count as cover, as the design page's "under trees" test expects, and use
  `MOTION_BLOCKING_NO_LEAVES` if not.

Tests: shared and independent rolls with forced suppliers, no Poison without Upset, extension cap
and fractional seconds, milk vs honey, cave, roof, tree, placed source, stored treated water,
Spring Water. Benchmark: `sample_water`, `sample_water_covered`, `thirst_tick_idle` (should drop
slightly with bursts gone).

## Step 7: containers, stacks and capacities

Design page: "Containers and crafting", "Container configuration". The widest step; split into
7a to 7d if it grows.

- **7a Carried vessels.** Capacities 4/4/6 from config, 1 to 64. `WaterskinItem.MAX_CAPACITY` to 64
  and the servings component's codec range with it (saved and network). Saved excess above a lowered
  capacity drains but accepts nothing. Sprite: `ceil(3 * min(s, cap) / cap)`. Fluid handlers on both
  loaders already count servings; audit their bounds.
- **7b Stacks of filled bowls** (bottles taken back, see Progress). Terracotta Water Bowl: stack size set on its `Properties`
  at registration from config (restart). ~~Water bottles: on 1.20.5+, `ItemWaterData.setFresh` /
  `setSalty` also set `max_stack_size`, so the limit rides on the stack, syncs by itself and costs
  nothing in inventory code; every place that creates a water bottle already stamps it. On 1.20.1, a
  `getMaxStackSize` mixin that does an item identity check first and reads NBT only for potions.
  Quality differences keep stacks apart by themselves. Brewing stand: insert one bottle per slot
  (manual, shift-click, hopper), a mixin per version through `platform/`. Drinking from a stack
  returns one empty and keeps the rest.~~
- **7c Pots.** Capacities 3/6, up to 64, so `LEVEL` and `BOIL` blockstates go. A block entity stores
  servings, boiled servings, quality and progress; the blockstate keeps a bounded `fill` (0 to 3) for
  the model and the existing `purity` for light-weight reads. Still **no ticker**: scheduled ticks
  only while boiling, as today. Migration: a pot loaded without block entity data takes its contents
  from the old state on first access, no water lost. Fewer blockstates than today (the product of
  level and boil goes), so registry and memory get smaller.
- **7d Distiller tanks** 1 to 64 from config; a tank under 3 refuses a bucket without consuming it.
- Clay bowl recipe yields three; loot: 1 to 4 empty bowls in village houses, gated on the item.

Tests: each setting at 1, default, 64 and out of range; legacy saves; full inventory; brewing with
every insertion route; pot migration from a world saved before the step. Benchmark: `fill_*`,
`drink_*`, `waterskin_mix`, plus a new `inventory_stack_bottles` on 1.20.1 for the mixin.

## Step 8: furnace switch, recipes and advancements

Design page: "Treatment stations", "Advancements" (implementation item 7).

- `enableFurnaceBoiling`, default true, explicit false kept; an `config_enabled` resource condition
  per loader through `Loader.registerResourceConditions`. Delete every `*_campfire` water recipe and
  Cold Sweat's furnace and smoker water recipes. All outputs Clean.
- **Do not generate one recipe per serving count.** Today the flask alone has 18 JSON recipes
  (6 servings x 3 grades); at 64 servings that becomes hundreds, slowing load and every furnace
  lookup. Instead: one recipe per vessel per furnace type that matches any boilable grade and any
  servings, and a mixin on the furnace's total cook time (`getTotalCookTime`, forked per version)
  that returns per-serving time x servings for those recipes. Bottle, bowl and bucket keep plain
  recipes (one per grade below Clean, or one with a grade-agnostic ingredient if the version allows).
  Net result: fewer recipes than today.
- Recipe XP zero. Pot recipes: five ingots, two sticks, one chain. Distiller boiler centre: iron.
  Check recipe patterns for collisions on every node.
- Advancements: `boil_water` also on completing a furnace or smoker water recipe;
  `purified_water` points at the distiller. Both advancement providers.

Tests: recipe output and time for each vessel at 1, default and 64 servings, switch on and off,
existing false config, zero XP, both advancement routes; `checkDatagen`, `checkDataConditions`,
`checkNeoForgeResources`.

## Step 9: documentation

`write-docs` skill: `docs/features/water-purity.md` (URL kept, titled "Water quality"),
`drinking.md`, `thirst-and-quenched.md`, `configuration.md` (which settings to reset to adopt new
defaults), integration pages that name boiling, store pages, screenshots (`capture-screenshots`),
CHANGELOG. Update `WATER-REFERENCE.md`, `WATER-SICKNESS.md`, `DISTILLATION-PLAN.md` and the
AGENTS.md files whose rules changed (purity, the hanging pots, `HealthRegen`). Mark the design page
implemented.

## Step 10: validation and tuning

The design page's "Validation and tuning" list, run in this order: gametests on all ten nodes, a
final `standard` x3 benchmark on every node written into `BENCHMARK-BASELINE.md` as the new
baseline, `docs:build`, then the manual playtests (routes, mining comparison, long trip) recorded in
`MANUAL-TESTING.md`. Tune drain before capacity, as the design page says.

## Order and dependencies

```
0 baseline ── 1 names ── 2 heat cap ─┬─ 3 recovery ── 4 hand drink ── 5 health/drain ── 6 sickness ─┐
                                     └─ 7 containers ── 8 recipes/advancements ─────────────────────┴─ 9 docs ── 10 validation
```

Steps 1 and 2 can ship in a release on their own. 3 to 6 change the survival loop together and
should ship together. 7 and 8 are independent of 3 to 6 and can go before or after them.

## Progress

Stopped on 2026-10-06, mid step 7. Nothing is committed. Last green gametest run: steps 0 to 6 on
`26.3.x` and `1.21.1-neoforge` (290 tests), `1.20.1` and `1.20.1-forge` (289). Datagen was rerun for
26.3, 1.21.1 and 1.20.1 after step 2; the other Minecraft versions (26.2, 26.1.2, 1.21.11) still need
`runDatagen` before their nodes are tested.

### Done

- **Step 0.** Baseline `standard` x3 on the five default nodes in `run/benchmark-sets/rework-step0`
  (not yet written into `BENCHMARK-BASELINE.md`). New benchmark operations `sample_water_covered`
  (a roofed source in the fixture), `thirst_lookup_potion` (`Vanilla.awkwardPotion`), `heal_food`.
  Gametest helper `TestFixtures.setState`. `runGametest` now deletes the gametest run's
  `config/thirstwastaken2.json` first (`gradle/shared.gradle.kts`), so local runs test the defaults
  as CI does.
- **Step 1.** Keys `thirst.water.dirty/murky/clean/pure`, Murky `0xBDB878`, renamed lang keys in all
  nine files, `defaultQuality` / `rainwaterQuality` / `dripstoneQuality` with `ThirstConfig.read` and
  `migrate` (`ConfigMigrationGameTest`). Textures recoloured by `tools/recolour_water.py` (rerunnable).
  Not done: a client look at the sprites and tooltip.
- **Step 2.** `WaterPurity.BOILED`, `boil`, `boilStep`, `boils`; canteen/flask, pots, Cold Sweat
  (both directories) capped at Clean; furnace/smoker recipes Dirty/Murky to Clean, XP 0, no campfire
  recipes (both recipe providers and both advancement providers); Cold Sweat's furnace and smoker
  JSON recipes deleted; deep-aquifer bonus removed.
- **Step 3.** `plainWaterValue {6,4}`, `plainWaterDrinkTicks`, `quenchedPercent {0,25,50,100}`;
  own vessels dropped from `drinks`; all default item values re-banded (see `defaultDrinks` javadoc);
  stack-aware `ThirstApi.thirstValues`; overflow discarded; `canDrinkWater(player, stack)` top-up;
  tooltip rows show the actual gain (`ThirstTooltip.setViewer`). `RecoveryGameTest`.
- **Step 4.** Timed hand drinking on `ExhaustionTracker` (`HandDrinkGameTest`); benchmark op updated. Taken back later, see below.
- **Step 5.** `HealthRegen.blocksFoodHeal` / `heal` with the reserve bonus, `illnessHaltsHealthRegen`,
  `quenchedHealMinFood` removed, baseline/activity/illness drain, `ThirstManager.climateFactor`.
  `HealthRegenGameTest` rewritten, `DrainGameTest`.
- **Step 6.** `SicknessEffect.group` and cached holder, shared rolls, new default tables (no taste
  Nausea), Upset Stomach extension rule, saturation 0.5/0.25, no Nausea bursts, `MilkMixin`, covered
  water cap (`Vanilla.skyAbove`). `CoveredWaterGameTest`. Note: a gametest's area blocks the sky, so
  sampled water in any gametest is at best Murky unless the column is cleared (see `openWater`).
- `WaterQuality.fresh` now returns interned instances.

### Steps 7 and 8, 2026-10-06 (second session)

- `HangingPotGameTest` ported to the block entity, plus migration from an old save, capacities 1 and
  64, a pot over a lowered capacity and the fill in thirds. Old tests moved to the new defaults
  (skin 4, bowls stack 3, fluid API 250 mB a serving fills a skin with 1000 mB, sprite by thirds).
- Brewing stand: vanilla already refuses a bottle slot that holds anything (hopper) and its menu
  slots hold one (shift-click moves one per click), so **no mixin**; `WaterStackGameTest` guards it.
  The distiller's water slot did jam on a stack (it pours only a single item), so its menu slot holds
  one and `canPlaceItem` refuses a filled water slot.
- Tests: vessel capacities 1, 64, clamped and over a lowered capacity; distiller tanks 1 (no bucket,
  slot or hand) and 64.
- Step 8: `thirstwastaken2:config_enabled` (`setting`) on all three loaders plus its build-logic
  translation; every furnace/smoker water recipe carries it for `enableFurnaceBoiling`. One
  smelting recipe per grade for the canteen (new: 2 s a serving) and flask (3 s), any fill, no
  smoker recipe: 18 purify recipes per version instead of 30+. Bottle/bowl 160/80 ticks, bucket
  480/240. `CookingRecipeMixin` also stamps a cooked water bottle's stack size, which a recipe file
  cannot carry. Clay bowl yields 3, waterskin 2 leather + string, canteen 3 copper + string (design
  table), distiller boiler centre iron; pots already matched. `boil_water` also on holding a canteen,
  flask or pot; `purified_water` shows the distiller, both descriptions in nine languages. Datagen
  rerun for every Minecraft version.
- Gametests green: `26.3.x` and `1.21.1-neoforge` 306, `1.20.1` and `1.20.1-forge` 305.
  `checkLang`, `checkDataConditions`, `checkNeoForgeResources`, `checkVersionSeam`,
  `checkOptionalSeam` pass. (1.20.1's brewing menu shift-clicks a potion into a bottle slot only
  when it is alone and sends a stack to the hotbar; the test checks the invariant, not the route.)

Not done: Farmer's Delight Cooking Pot bottles still come out without the stack size component
(its own recipe class); a Cold Sweat Boiler or Create spout bottle likewise unless it goes through
`setQuality`. `checkDatagen` only means something once the generated files are committed.

Benchmark after step 8, `standard` x3 on the five default nodes in `run/benchmark-sets/rework-step8`,
against `rework-step0` (`aggregate.py --compare`): ms/tick at 200 players better on 26.3.x (-60%),
noise elsewhere. Steady tick +1.2 to 1.6 B per player on 1.20.1, 1.21.1-neoforge and 26.3.x, which is
the baseline drain's write about every 75 idle ticks, expected by step 5; `thirst_tick_idle` 0.78 B
on 1.20.1. First touch +40 B once per player (the new `ExhaustionTracker` fields). `thirst_lookup`
and `thirst_lookup_potion` still 0 B; tooltips, drinks and `waterskin_mix` allocate less. Per-op
microsecond changes are inside their spread. Accepted; step 10 writes the new baseline.

### Taken back, 2026-10-06

At the maintainer's call, after step 8:

- **Water bottles do not stack**, to stay out of other mods' way: `waterBottleStackSize`, the
  per-stack `max_stack_size` (`WaterPurity.stampStackSize`, loot, `CookingRecipeMixin`), 1.20.1's
  `ItemStackMixin.getMaxStackSize` and `Vanilla.setMaxStackSize` are gone, with their lang keys.
  Bowls still stack. `WaterStackGameTest` now covers bowls and checks bottles stay at one; the
  distiller's one-container water slot stays, for bowls.
- **The Waterskin's recipe** went back to three leather and a string, then became **four leather in a
  ring** at the maintainer's call (no string).
- **Drinking by hand is a sip per click again** (3 thirst, 2 base quenched cut by the grade): the
  timed drink, its `ExhaustionTracker` fields, `HAND_DRINK_TICKS`, `isDrinkingByHand` and
  `HandDrinkGameTest` are gone; `DrinkingGameTest` and the `drink_by_hand` benchmark op are as before.
- **Healing went back close to the original**: quenched heals on its own again (`healWithQuenched`,
  `quenchedHealthRegen` 0.5), but only once saturation is spent; food needs the thirst bar at
  `foodHealMinThirstPercent` (50), quenched needs a full thirst bar and the food bar at
  `quenchedHealMinFoodPercent` (50); Upset Stomach stops both. The 25% reserve bonus,
  `chargeHealCost` and `quenchedHealMinFood` are gone. `HealthRegenGameTest` rewritten.
- **`drinkTagValue` is 6 / 4**, a prepared drink's, not a potion's 6 / 8; the keyword values follow
  (drink and soup 6 / 4, fruit 2 / 0).
- **No water sprite is recoloured**: the four bowl and four pot textures are the committed ones again.
  The tooltip colours (Murky `0xBDB878`) stay.

### Step 9, 2026-10-06

- Player pages: `water-purity.md` (grades, covered water, sickness, Upset Stomach, heat to Clean,
  furnace and smoker table, canteen, pots, distiller), `drinking.md` (water by grade, item values,
  overflow, top-up, bowls stacking, hand drinking, advancements), `thirst-and-quenched.md` (baseline
  drain, climate, healing gate and reserve bonus), `configuration.md` (the file, every new or renamed
  key, the update tip), the overview, and the value table of every integration page, regenerated from
  `ThirstConfig`'s defaults. Integration pages that named boiling to Pure (Cold Sweat, Farmer's
  Delight, Hearth and Harvest, Spelunkery) say Clean. `docs:build` passes.
- CHANGELOG `[Unreleased]`: summary, Added, Changed, config details. Store pages (Modrinth,
  CurseForge): grades table, purification, containers, Farmer's Delight.
- Dev docs: `WATER-REFERENCE.md` rewritten to the game as built, `WATER-SICKNESS.md` notes what the
  rework replaced, `DISTILLATION-PLAN.md`, the design page marked implemented with the three choices
  taken back, purity, main, datagen and gametest `AGENTS.md`.

Screenshots: the clay bowl, waterskin, copper canteen and distiller boiler recipe images were
retaken on 26.2 through the agent queue, `tools/agent/shots/recipes.jsonl` and `recipe_images.py`.

### Step 10, 2026-10-06

- Gametests on all twelve nodes, 0 failures: 300 on the 26.x and 1.21.1 nodes, 301 on both 1.21.11,
  299 on both 1.20.1.
- `standard` x3 on every node, `run/benchmark-sets/rework-final`, written into
  `BENCHMARK-BASELINE.md` as the new baseline, with what the rework changed against `rework-step0`.
- `docs:build` passes.
- Agent gameplay scripts on 26.3.x, all verified: `canteen` (boiling Clean, both recipes),
  `hanging-pot` (Clean; its last check was stale since 1.6.0's stand and now checks the pot stays),
  `loot-and-boil` (furnace and smoker give Clean, `boil_water`), `parched` (no taste Nausea any more),
  `distiller`, `waterskin-stack`. Their `checks` lines used a form `drive.py` never read and are
  `expect` now.
- `MANUAL-TESTING.md` has a Purification rework section; the Nausea burst items are gone.

Not done: **the played balance tests** in that section (twenty-minute routes, matched mining runs,
a long trip, emergency recovery). They judge balance and need a person playing; record their numbers in
the release PR and tune drain before capacity, as the design page says. Also open: the
`client-sync` script and the integration scripts were not rerun.

### Step 7, where it stopped (first session)

Written and compiling (main, client, dev, datagen on 26.3.x); **not yet gametested**:

- Config: `waterskinCapacity`, `waterBottleStackSize` (since removed), `terracottaWaterBowlStackSize`,
  `copper/ironHangingPotCapacity`, `enableFurnaceBoiling` (field only), `MAX_CONTAINER = 64`, boil
  seconds defaults 2/3/3/4, distiller tank 1..64. Containers page rebuilt with a Stacks tab; lang in
  all nine files.
- `WaterskinItem.MAX_CAPACITY = 64`, capacity from config, sprite by thirds, `furnaceTicks`,
  `keepServings`. `CookingRecipeMixin` (keeps servings through a cooking recipe) and `FurnaceMixin`
  (cook time per serving) are added and registered, ready for step 8's recipes.
- Water bottles: `Vanilla.setMaxStackSize` from `WaterPurity.setQuality` and on loot;
  1.20.1 `ItemStackMixin.getMaxStackSize` through `WaterPurity.maxStackSize`. Bowl `stacksTo` from config.
- Hanging pots moved to `HangingPotBlockEntity` (servings, boiled steps; migrates old `level`), the
  blockstate keeps `level` as a 0..3 fill and `purity`; `HangingPotBlock.pour/draw/setWater/servings/room`;
  interactions, Supplementaries faucet and models updated. Village houses get 1-4 empty bowls.

Next, in order:

1. Rewrite `HangingPotGameTest` for the block entity API (it is the only test that no longer
   compiles), add pot migration and capacity tests.
2. Brewing stand: test manual, shift-click and hopper insertion of a stack of water bottles on 26.3
   and 1.20.1; add a mixin only if vanilla lets more than one in.
3. Tests for each capacity/stack setting at 1, default, 64; the over-capacity vessel; 1.20.1 stack
   size; distiller tank under 3 refusing a bucket.
4. Clay bowl recipe yields 3 (both recipe providers); then step 8: one smelting recipe per vessel and
   grade with no servings in the ingredient, `item_enabled`-style `config_enabled` condition for
   `enableFurnaceBoiling` per loader, bottle/bowl 160/80 ticks and bucket 480/240, `boil_water`
   criteria, pot recipes (5 ingots, 2 sticks, 1 chain), distiller boiler centre iron; then
   `runDatagen` on every Fabric node and `checkDatagen`.
5. Benchmark `standard` x3 on the five default nodes against `rework-step0`. A first run after step 6
   failed only because `full_bar_guard` used Clean water, now fixed; a quick 26.3.x run was clean
   (idle tick 1 B from the baseline drain, as expected).
6. Step 9 docs (list in the scratch notes: Cold Sweat AGENTS files, purity AGENTS, main AGENTS pots
   and HealthRegen, benchmark and gametest AGENTS, WATER-REFERENCE, WATER-SICKNESS, DISTILLATION-PLAN,
   CHANGELOG, player docs) and step 10.
