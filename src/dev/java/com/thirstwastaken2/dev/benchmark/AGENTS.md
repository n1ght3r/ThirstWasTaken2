# /thirst benchmark

What the mod costs a server, in time and in memory, with nobody joining. Players are simulated
entirely on the server, and the results land in a JSON report plus a console summary.

Every node runs it, the `-neoforge` ones included. The simulated players are the loader's own fake
player — Fabric API's `FakePlayer` or NeoForge's — and that is the whole of the difference: everything
in this package is loader independent, and `BenchmarkPlayer` has one copy per loader, in
`src/dev/fabric` and `src/dev/neoforge`, under the same name, package and signatures.
[src/dev/java/AGENTS.md](../../../../AGENTS.md) is the source set this belongs to, and holds the rules
every tool here follows and the harness this one shares with the agent client.

A report is worth comparing with another from the same node and not across loaders. Fabric and
NeoForge keep a player's thirst in machinery of their own — an attachment API each, with sync rules of
their own — so the same mod code costs different amounts on the two, especially the allocation
figures. Both are real; neither is the other's baseline. `environment.loader` says which one wrote a
report, and the summary's first line begins with it.

## Running it unattended (the way an agent should)

```bash
./gradlew ":26.2.x:runBenchmark"
```

Any node's name works there, `26.2.x-neoforge` as much as `26.2.x`.

Run it in the background and wait for the task to exit. It starts the dedicated server in
`run/<node>/`, runs `/thirst benchmark` from the console as soon as the server is up, writes the report
and stops the server. Progress is printed as `[ThirstBenchmark] ...` lines, and the last one is

    [ThirstBenchmark] DONE status=ok in 41.3 s report=<absolute path>

The Gradle task exits 0 even when the benchmark itself failed, so always read `status` in the report:
`ok`, `cancelled` or `failed`, with `message` explaining the last two. The newest report is also copied to
`run/<node>/benchmark/latest.json`.

`runBenchmark` shares `runServer`'s run directory and accepted EULA, but **not its world**. It opens
`thirst-benchmark` beside it, with `--world`, generated from the fixed `thirst.benchmark.seed` in
`gradle.properties` that the `benchmarkWorldSeed` task writes into the node's `server.properties` when
that file has no seed of its own. So a run never depends on what somebody built in the dev world, a
crash mid-run cannot leave the dev world altered, and the two nodes of one Minecraft version -- the
Fabric one and the NeoForge one -- measure the same terrain. Worldgen differs between Minecraft
versions, so the same seed still gives different terrain across versions; `environment.levelSeed` and
`environment.levelDirectory` say what a run actually measured, and `environment.cpu` says where.

`-Pthirst.benchmark.seed=<seed>` measures somewhere else on purpose, and only takes hold for a world
that does not exist yet: delete `run/<node>/thirst-benchmark/` first.

The benchmark and `runServer` still cannot run at the same time, and neither can two nodes' benchmarks
when something else holds port 25565.

`-Pbenchmark=<arguments>` picks what runs, exactly as typed after `/thirst benchmark`:

| Arguments | Player counts | Warm-up + measured ticks per count | Ops per interaction |
|---|---|---|---|
| `quick` | 1, 10, 50 | 100 + 200 | 2 000 |
| `standard` (default) | 1, 10, 50, 100, 200 | 200 + 600 | 10 000 |
| `stress` | 1, 100, 250, 500, 1000 | 200 + 600 | 20 000 |
| `"players <count> [ticks]"` | `count` (1 to 2000) | `ticks`/3, at least 100, + `ticks` (default 600) | 5 000 |

A typo in the arguments is logged as `DONE status=failed` and the server stops anyway.

Ticks in a profile are simulated ticks, not wall-clock ones. Each server tick the benchmark runs as many
simulated ticks as fit in its 35 ms budget, so 20 000 ticks for 200 players take a few seconds rather than
17 minutes. Intervals inside the simulation, such as a drink every 600 ticks, count simulated ticks too.

On a 22-thread desktop the whole `runBenchmark` task takes about 25-35 seconds for `quick` or
`standard`, most of it Gradle and server startup; the benchmark itself runs for 3-6 seconds. `stress`
and large `players` counts take longer mainly because creating the simulated players is slow.

## Running it by hand

Start `./gradlew ":26.2.x:runServer"` and type into its console:

- `thirst benchmark [quick|standard|stress|players <count> [ticks]]`
- `thirst benchmark status`
- `thirst benchmark cancel`, which still restores the world and writes a partial report

In game it needs permission level 4. The server stays responsive while it runs: the benchmark does at most
35 ms of work per server tick and carries on over the following ticks.

To drive that console from a script rather than a keyboard, write lines to the server's standard input:
Loom forwards it, and `runServer` accepts commands on it. Wait for the `Done (` line before the first
command. When the writer is .NET, for example `System.Diagnostics.Process` from PowerShell, send an empty
line first: .NET puts a UTF-8 byte order mark in front of the first write, which turns that line into
`Unknown or incomplete command`. `runBenchmark` needs none of this and is the better choice unless the
point is to test the command itself.

## Profiling a run

A report says what something cost. It does not say where the cost is, and the benchmark is the wrong
place to grow an answer to that: the JDK already has one.

```bash
./gradlew ":26.2.x-neoforge:runBenchmark" -Pprofile
```

`-Pprofile` starts JFR with `settings=profile`, a stack depth of 1024 and `DebugNonSafepoints`, and
writes `run/<node>/benchmark/latest.jfr` when the server stops. Open it with JDK Mission Control, or
read it without one:

```bash
jfr summary run/<node>/benchmark/latest.jfr
jfr print --events jdk.ObjectAllocationSample --stack-depth 24 run/<node>/benchmark/latest.jfr
```

`jdk.ObjectAllocationSample` is the event worth reading first, because allocation is the figure the
benchmark measures most reliably. Filtering the samples to the ones whose stack passes through
`dev.benchmark` keeps the run's own work and drops class loading and server startup.

A recorded run is **not a measurement**: sampling makes it slower and skews every number in its report.
The report says so in `environment.profiling`, which is true under `-Pprofile` and under any profiler
or debugger attached by hand, and `aggregate.py` refuses a set with one in it.

## What it measures

### Tick scenarios

For each player count, every simulated tick does, for every player, what a real session does:

| Section | Work | Mod code exercised |
|---|---|---|
| `exhaustion` | Vanilla's exhaustion charges: 6 in 10 players sprint, 2 swim, 2 idle; sprint-jumps, attacks, mining, and the Hunger effect on 1 in 10 | `PlayerMixin` → `ThirstManager.mirrorExhaustion` |
| `sprint_gate` | One `Player#canSprint` per player | `PlayerMixin` sprint gate |
| `interactions` | Once every 30 s per player, staggered: sample the fixture water into a bowl, and half a cycle later drink a bowl | `WaterPurity.sampleAt`, `ItemStackMixin` → `ThirstManager.drinkItem` |
| `food_tick` | Vanilla `FoodData#tick`, with health kept below full so regeneration keeps firing | `FoodDataMixin`, `HealthRegen` |
| `thirst_tick` | `ThirstManager.tickPlayer` | buffered exhaustion, modifier cache, Hunger refund, consumption |
| `sync_encode` | `ThirstData.write` for every player whose attachment changed | what a real client's sync packet carries |

Half the players wear diamond armour with Protection IV, Unbreaking III and Mending, which is what makes
the exhaustion modifier expensive to compute. 1 in 20 has Nausea and 1 in 20 Fire Resistance. Upkeep that
only keeps the simulation going runs outside every timed section: topping thirst up before it runs dry
and restoring health and food.

Per count, the report gives milliseconds per tick (mean, p50, p95, p99, max), the share of a 50 ms tick,
microseconds per player, bytes allocated per tick and per player, the time share of each section, sync
packets per player per second, and garbage collections during the measurement.

### Interactions

One player standing over the water fixture repeats each operation. Operations that change the world or
the player are reset between runs outside the timed window. Operations that change nothing are timed in
batches of 64, because one alone is quicker than the clock resolves; their percentiles are over batch
means. Each operation checks once that it actually did something and fails the run otherwise, so spawn
protection refusing a fill cannot pass as an impressively fast result.

A drink carries vanilla's advancement criteria with it, and that is most of what the drink operations
allocate rather than anything the mod does: on 1.21.11-neoforge, 63% of everything the benchmark
allocates on the server thread is advancement machinery, and `DrinkItem.drinkEffects` alone is a third
of the drink operations' allocation. It is fair -- a real player fires those criteria too -- but it
means a drink operation's figures move when vanilla changes, not only when the mod does. One node is
the exception: NeoForge 21.1 hands a fake player a no-op `PlayerAdvancements`, so on 1.21.1-neoforge
the criteria do nothing and `drink_water_bottle` allocates 338 B where every other node is between
1800 and 2700. That is the loader, not the mod, and it is the second reason a figure compares within a
node and not across nodes.

`sample_water`, `fill_bottle`, `fill_bucket`, `fill_bowl`, `fill_waterskin`, `drink_water_bottle`,
`drink_waterskin`, `drink_by_hand`, `cauldron_pour`, `full_bar_guard`, `tooltip_water_bottle`,
`tooltip_waterskin`, `tooltip_food`, `thirst_lookup`, `thirst_lookup_potion`, `sample_water_covered`, `heal_food`, `water_quality_read`, `waterskin_mix`,
`exhaustion_mirror`, `thirst_tick_idle`. The report's `description` field says what each one does.

### With Create Fly

```bash
./gradlew ":26.2.x:runBenchmark" -Pcreate
```

Fabric nodes only, because Create Fly is: the NeoForge nodes build no Sand Filter, `-Pcreate` does
nothing there and `environment.createFly` is false.

`-Pcreate` puts Create Fly on the benchmark server's classpath, on the nodes that build the Sand Filter
(`deps.create_fly`). The report's `environment.createFly` says whether it was loaded, and six operations
join the list:

| Operation | What it times |
|---|---|
| `createfly_filter_active` | one Sand Filter tick moving dirty water into an empty output |
| `createfly_filter_blocked` | one tick whose output holds water of another grade, so nothing moves |
| `createfly_filter_idle` | one tick with an empty input |
| `createfly_fill_bottle` | `GenericItemFilling.fillItem` filling a glass bottle, with the purity stamp |
| `createfly_empty_bottle` | `GenericItemEmptying.emptyItem`, simulated, reading a graded bottle |
| `createfly_open_pipe_draw` | `OpenEndedPipe#removeFluidFromSpace`, simulated, over the water fixture |

The operations live in `src/dev/createfly/java` and are reached by name from `InteractionScenario`, so
nodes without Create Fly compile nothing of them. The filter is a real block entity attached to the
level but not placed, so the server never ticks it behind the benchmark's back. Everything else in the
report still runs, so comparing a run with and without `-Pcreate` shows what installing Create Fly
changes for the mod's own work.

### Memory

- `thirstDataBytes`, `exhaustionTrackerBytes`: the shallow size of each object, from allocating ten
  thousand of them.
- `firstTouchBytesPerPlayer`: allocated the first time a player drains, attachment creation included.
- `steadyTickBytesPerPlayer`: allocated per player per tick by ordinary walking — an exhaustion charge
  and the thirst tick it feeds — averaged over 180 ticks, after a warm-up that is thrown away. It has
  to be many ticks rather than one. The tick only builds a `ThirstData` when exhaustion crosses a sync
  step of 0.25, and one walking charge moves it by 0.028, so a single charge from a standing start
  always takes the cheap carry branch. Measuring one charge read 0.5 B/player on every version, while
  the same work under `tickScenarios[].sections.thirst_tick` came to 105 B/player on 1.21.1; the two
  agree now.
- `thirstCacheEntries`, `purityInfoEntries`: the sizes of `ThirstApi.CACHE` and `WaterPurity.INFO`.
- `heapUsedAfterGcBeforeMiB`, `heapUsedAfterGcAfterMiB`, `heapRetainedKiB`: whole-heap use after a full
  collection, taken once the benchmark area has loaded and again after the run has let go of its players,
  with the area still loaded so chunk memory cancels out. The first run on a freshly started server also
  counts what stays for the server's lifetime: the thirst cache and vanilla's per-UUID stats and
  advancements for the simulated players. A second run in the same session should come out near zero;
  a value that keeps growing from run to run is a leak. Other threads allocate too, so a few MiB either
  way is noise.

Allocation figures count only the server thread and are exact regardless of garbage collection. If the
JVM cannot count them, `environment.allocationTracking` is false and every byte figure is zero.

## Comparing two versions of the code

One report is a measurement; it takes a set of them to answer whether anything changed.
[`tools/benchmark`](../../../../../../../tools/benchmark) is that loop:

```bash
python tools/benchmark/bench.py --repeats 3
python tools/benchmark/aggregate.py run/benchmark-sets/<before> --compare run/benchmark-sets/<after>
```

`bench.py` compiles every node first, so no build ever runs between two timed runs, then repeats in the
outer loop and iterates nodes in the inner one, so a machine that warms up or gets busier over the hour
biases every node the same way rather than only the ones at the end. It keeps every report.

[docs/dev/benchmark/BENCHMARK-BASELINE.md](../../../../../../../docs/dev/benchmark/BENCHMARK-BASELINE.md) is what every node
measured on 2026-09-16, three runs each, and the machine it was measured on. That is the set to compare
a new one against; take a fresh baseline on a different machine rather than comparing across two.

`aggregate.py` reduces a set to a median and a **spread**, `(max - min) / median`, and `--compare` puts
two sets side by side and calls a change `noise` when it is smaller than the spread of the runs behind
it -- the rule below, made mechanical. It refuses a set with a failed run in it, and refuses to compare
two sets whose `environment.cpu` or `environment.levelSeed` disagree.

Use the same node — the same Minecraft version and the same loader — the same profile and the same
machine, with nothing else heavy running.
Run each side three times, and know what each kind of figure is worth. Three `standard` runs of every
node on one desktop, all eight of them measured back to back, put the spread at:

| Figure | Spread over three runs | What it can answer |
|---|---|---|
| `interactions[].bytesPerOp` | median 3%, worst 79% | a few percent is already evidence |
| `tickScenarios[].allocatedBytesPerPlayerPerTick` | 0 to 15% on six nodes of eight | the same |
| `memory.steadyTickBytesPerPlayer`, `firstTouchBytesPerPlayer` | 0 to 16% | the same |
| `tickScenarios[].msPerTick.mean`, `microsPerPlayerPerTick` | 14 to 73% | only a change of that size or more |
| `interactions[].microsPerOp.mean` | median 45%, worst 222% | almost nothing on its own |

The time figures are that unsteady because of how small they are: the mod's whole per-tick cost at 200
players is tens of microseconds, so a scheduling hiccup or one garbage collection moves the mean by
half. Allocation is counted rather than timed, and is steady to a few percent. **So chase regressions
in the byte figures, and treat a time figure as an order of magnitude** unless it moved by more than
its own spread. `aggregate.py --compare` does exactly that comparison and labels the rest `noise`.

The fields that matter:

- `tickScenarios[].msPerTick.mean` and `.p99`, `microsPerPlayerPerTick`
- `tickScenarios[].sections.*.sharePercent` and `microsPerTick`, to see which part moved
- `tickScenarios[].allocatedBytesPerPlayerPerTick`, `syncPacketsPerPlayerPerSecond`
- `interactions[].microsPerOp.mean`, `interactions[].bytesPerOp`
- `memory.*`

## What it does not measure

- Client work: HUD drawing, tooltip rendering (building the tooltip lines is measured), the config screen.
- The wire. Changed attachments are counted and their `ThirstData` encoded, but payload wrapping,
  compression and Netty are not.
- What the loader itself does around a sync, which is not the same on both and not the same on every
  NeoForge. A simulated player has no client, and each loader works that out somewhere else: NeoForge
  from 26.1 builds the sync payload first and drops it when it finds no channel, which a recording puts
  at about a sixth of everything the benchmark allocates on the server thread, against under a
  hundredth for Fabric's; before 26.1 the mod's own predicate turns a fake player away first, so the
  payload is never built and those two nodes report less sync cost than a real player would cause. So
  `allocatedBytesPerPlayerPerTick` is comparable between two runs of one node and not between nodes.
- Vanilla's own per-player cost, such as movement, chunk sending and entity tracking. Percentages are of a
  50 ms budget, not of a real server's tick.
- Other dimensions, so the Nether branch of the exhaustion modifier.
- Interleaving. Sections run one kind of work across all players, which is slightly friendlier to the CPU
  cache than the game's per-player order.

## What it does to the server and the dev world

- A dedicated server with nobody online stops ticking after `pause-when-empty-seconds` (60 by default), and
  a paused server fires no tick events. While a run is active `BenchmarkRunner` calls
  [`ServerAwake`](../../../../AGENTS.md#the-shared-harness) every tick, and once more when the command
  starts a run, which
  also wakes a server that has already paused. Without that, a run typed into the console more than a
  minute after startup never advances, and a long run stalls a minute in. The pause behaves normally
  again once the run ends.

- Works in `thirst-benchmark`, a world of its own, so nothing here touches the dev world `runServer`
  opens. Force-loads 5x5 chunks around chunk (0, 0), or around the nearest of a grid of candidates 32 chunks
  apart that is outside spawn protection and not an ocean or a beach (sea water has no grade to stamp
  and never hydrates, so the fill and drink interactions would fail there), and builds a small water fixture a few blocks below the build limit. Both are
  undone when the run ends: completed, cancelled, failed, or cut short by the server stopping. Only a
  crash mid-run can leave them behind.
- Simulated players are `BenchmarkPlayer`s, the loader's own fake player built fresh per run: not in the
  player list, not in the level, invulnerable, and every packet to them is dropped. Nothing is saved for
  them. The mod turns a fake player away from its NeoForge sync predicate before 26.1, where asking
  whether such a connection carries the mod's channel throws rather than answering; from 26.1 on
  NeoForge answers it itself.
- Vanilla creates a stats object and full advancement progress for every player it constructs, keyed by
  UUID, and only forgets them when a player disconnects. That progress costs hundreds of kilobytes a
  player, so `BenchmarkWorld.releasePlayers` removes the simulated players' entries from `PlayerList` by
  reflection when the run ends, unregistering the advancement listeners the way `PlayerList#remove`
  does. NeoForge hands a fake player a throwaway advancements object on some versions and caches a real
  one per UUID on others, so there the cleanup drops whatever did end up in those maps, and nothing when
  nothing did. Any player the benchmark creates must go through `BenchmarkWorld.player` or `extraPlayer`, or it
  is not forgotten. The UUIDs are derived from the player index, so even a missed cleanup is reused by the
  next run rather than piling up.

## Rules

These are the benchmark's; the source set's own are in
[src/dev/java/AGENTS.md](../../../../AGENTS.md) and the agent client's are in
[../agent/AGENTS.md](../agent/AGENTS.md).

- A scenario that stops exercising the mod must fail, never report fast numbers. Interaction operations
  check their outcome for this reason.
- Every block the benchmark places goes through `BenchmarkWorld.place`, so `close` can restore it.
- Stages do bounded work per call and are resumed on the next tick. Never loop until done inside a stage:
  the server watchdog kills a tick that runs for 60 seconds.
