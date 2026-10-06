# Benchmark baseline

What `/thirst benchmark` measured on every node on 2026-10-06, after the purification rework, as the
mark later runs are read against. It replaces the earlier baselines (1.0.6 on 2026-09-16, the 1.20.1
nodes on 1.4.1 on 2026-09-28), which git history keeps. Each figure is the **median of three runs**,
with the spread of those runs in brackets: `(max - min) / median`. A later run that differs by less than
the spread has shown nothing.

Numbers are worth only what the machine they came from is worth, so this file records it. Take a
new baseline on a different machine rather than comparing across two.

| | |
|---|---|
| Machine | Intel64 Family 6 Model 170 Stepping 4, GenuineIntel, 22 logical processors, Windows 11 10.0 (amd64) |
| JVM heap | 3970.0 MiB max, allocation counting on |
| Java | 17 on the 1.20.1 nodes, 21 on the 1.21.x nodes, 25 on the 26.x ones, as each Minecraft version requires |
| World | `thirst-benchmark`, seed `8213470596118`, the same terrain on every node |
| Profile | `standard`: 1, 10, 50, 100 and 200 players, 200 warm-up + 600 measured ticks each, 10 000 ops per interaction |
| Mod | 1.6.2 with the purification rework, uncommitted; set `run/benchmark-sets/rework-final` |

```bash
python tools/benchmark/bench.py --repeats 3
python tools/benchmark/aggregate.py <this set> --compare run/benchmark-sets/<new>
```

## What the rework changed

Read against the set taken just before it (`run/benchmark-sets/rework-step0`, the five default nodes):

- **The steady tick allocates about 1.2 B more per player** on 1.20.1, 1.21.1-neoforge and 26.3.x.
  That is the baseline drain, a point of thirst a minute, which writes the player's thirst about every
  75 ticks even when they stand still. Expected, and not a regression; `thirst_tick_idle` shows it.
- First touch and the size of `ThirstData` and `ExhaustionTracker` are unchanged.
- Time per tick is better or inside its spread everywhere.
- `thirst_lookup` still allocates nothing, plain water included; tooltips and drinks allocate less.

**Healing changed after these tables were taken.** Food now heals from half the thirst bar and quenched
heals on its own again (see `HealthRegen`). On 26.3.x, `run/benchmark-sets/rework-heal` and
`rework-heal-2` read 0.045 ms/tick at 200 players instead of 0.021, steady and outside the spread. The
tick scenario keeps its players at 16 health with thirst 8 to 20, so where the old gate refunded nearly
every heal, vanilla now heals most of them: the extra time is vanilla's `heal`, not the mod's. The mod's
own figures did not move: `thirst_tick_idle` 0.58 B, `heal_food` 16 B, steady tick 9.88 B. Before the
rework, with the same kind of heal, it read 0.055. Take the next full baseline with this healing in.

## Per server tick, 200 players

A tick has 50 ms. `us/player` is per player per tick.

| Node | ms/tick | p99 ms | us/player | B/player/tick |
|---|---|---|---|---|
| 1.20.1 | 0.035 (40%) | 0.149 (46%) | 0.177 (40%) | 11.97 (3%) |
| 1.21.1 | 0.023 (78%) | 0.1 (9%) | 0.117 (75%) | 117.9 (0%) |
| 1.21.11 | 0.024 (17%) | 0.124 (46%) | 0.121 (17%) | 52.31 (15%) |
| 26.1.x | 0.021 (38%) | 0.076 (83%) | 0.106 (38%) | 17.24 (0%) |
| 26.2.x | 0.021 (10%) | 0.082 (21%) | 0.106 (11%) | 17.24 (0%) |
| 26.3.x | 0.021 (14%) | 0.091 (47%) | 0.106 (14%) | 17.71 (0%) |
| 1.20.1-forge | 0.082 (66%) | 0.293 (51%) | 0.41 (66%) | 52.72 (48%) |
| 1.21.1-neoforge | 0.044 (39%) | 0.135 (68%) | 0.221 (38%) | 10.08 (10%) |
| 1.21.11-neoforge | 0.037 (27%) | 0.231 (32%) | 0.184 (27%) | 12.83 (2%) |
| 26.1.x-neoforge | 0.045 (38%) | 0.189 (43%) | 0.223 (38%) | 53.95 (0%) |
| 26.2.x-neoforge | 0.038 (24%) | 0.158 (35%) | 0.188 (23%) | 51.52 (11%) |
| 26.3.x-neoforge | 0.038 (29%) | 0.147 (29%) | 0.188 (29%) | 45.52 (0%) |

## Memory

| Node | first touch B/player | steady tick B/player | ThirstData B | ExhaustionTracker B |
|---|---|---|---|---|
| 1.20.1 | 504.8 (0%) | 7.839 (0%) | 32 | 40 |
| 1.21.1 | 1650 (0%) | 108 (15%) | 32.05 | 40 |
| 1.21.11 | 1246 (2%) | 50.77 (10%) | 32 | 40 |
| 26.1.x | 870.2 (0%) | 12.94 (24%) | 32.05 | 40 |
| 26.2.x | 870.5 (5%) | 9.878 (0%) | 32.05 | 40 |
| 26.3.x | 870.2 (0%) | 9.878 (31%) | 32.05 | 40 |
| 1.20.1-forge | 352.8 (0%) | 20.77 (0%) | 32 | 40 |
| 1.21.1-neoforge | 362.5 (0%) | 7.839 (0%) | 32 | 40 |
| 1.21.11-neoforge | 437 (16%) | 7.833 (0%) | 32.05 | 40 |
| 26.1.x-neoforge | 694.5 (0%) | 51.79 (0%) | 32.05 | 40 |
| 26.2.x-neoforge | 663.8 (7%) | 47.71 (13%) | 32 | 40 |
| 26.3.x-neoforge | 638.2 (0%) | 44.63 (0%) | 32.05 | 40 |

## Bytes per interaction

The steadiest thing the benchmark measures. This is the table a change to the mod's own code shows up in first.

Fabric:

| Operation | 1.20.1 | 1.21.1 | 1.21.11 | 26.1.x | 26.2.x | 26.3.x |
|---|---|---|---|---|---|---|
| `cauldron_pour` | 912 (3%) | 979.4 (1%) | 969.2 (2%) | 1034 (4%) | 957 (2%) | 952 (2%) |
| `drink_by_hand` | 2428 (3%) | 2599 (0%) | 1529 (4%) | 1171 (5%) | 1706 (3%) | 2268 (2%) |
| `drink_water_bottle` | 1259 (1%) | 1760 (0%) | 2664 (2%) | 2178 (2%) | 2215 (3%) | 2613 (1%) |
| `drink_waterskin` | 992.2 (2%) | 1791 (1%) | 2792 (4%) | 2336 (0%) | 2383 (0%) | 2736 (0%) |
| `exhaustion_mirror` | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) |
| `fill_bottle` | 4162 (4%) | 3356 (6%) | 1948 (17%) | 1862 (3%) | 1930 (2%) | 1804 (3%) |
| `fill_bowl` | 1616 (1%) | 1076 (7%) | 1086 (5%) | 993.4 (5%) | 991 (2%) | 953.2 (2%) |
| `fill_bucket` | 5265 (4%) | 4487 (3%) | 3263 (6%) | 2962 (1%) | 3050 (2%) | 3292 (3%) |
| `fill_waterskin` | 864 (10%) | 912 (9%) | 882.9 (3%) | 784 (3%) | 784 (0%) | 696 (5%) |
| `full_bar_guard` | 48 (0%) | 48 (0%) | 24 (0%) | 24 (12%) | 24 (0%) | 24 (0%) |
| `heal_food` | 16 (0%) | 16 (0%) | 16 (0%) | 16 (0%) | 16 (0%) | 16 (0%) |
| `sample_water` | 9.697 (173%) | 24 (0%) | 24.01 (0%) | 24 (0%) | 24 (0%) | 24 (0%) |
| `sample_water_covered` | 12.31 (43%) | 24.01 (0%) | 24 (0%) | 24 (0%) | 24.01 (0%) | 24.01 (0%) |
| `thirst_lookup` | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) |
| `thirst_lookup_potion` | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) |
| `thirst_tick_idle` | 0.454 (0%) | 6.815 (15%) | 3.505 (6%) | 0.779 (25%) | 0.584 (0%) | 0.584 (33%) |
| `tooltip_food` | 144 (0%) | 144 (0%) | 144 (0%) | 144 (0%) | 144 (0%) | 144 (0%) |
| `tooltip_water_bottle` | 216 (0%) | 216 (0%) | 216 (0%) | 216 (0%) | 216 (0%) | 216 (0%) |
| `tooltip_waterskin` | 384 (0%) | 384 (0%) | 384 (0%) | 384 (0%) | 384 (0%) | 384 (0%) |
| `water_quality_read` | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) |
| `waterskin_mix` | 0 (0%) | 80 (0%) | 176 (0%) | 176 (0%) | 176 (0%) | 112 (0%) |

NeoForge, and Forge on 1.20.1:

| Operation | 1.20.1-forge | 1.21.1-nf | 1.21.11-nf | 26.1.x-nf | 26.2.x-nf | 26.3.x-nf |
|---|---|---|---|---|---|---|
| `cauldron_pour` | 1339 (19%) | 1166 (5%) | 1196 (8%) | 1352 (5%) | 1328 (5%) | 1095 (3%) |
| `drink_by_hand` | 2906 (7%) | 1813 (0%) | 2100 (2%) | 1627 (3%) | 1219 (7%) | 1336 (6%) |
| `drink_water_bottle` | 1255 (1%) | 381 (0%) | 2404 (2%) | 2596 (4%) | 2078 (2%) | 2132 (2%) |
| `drink_waterskin` | 1016 (1%) | 493.1 (6%) | 2567 (4%) | 2759 (0%) | 2225 (2%) | 2297 (1%) |
| `exhaustion_mirror` | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) |
| `fill_bottle` | 4801 (14%) | 3583 (10%) | 2237 (16%) | 2063 (6%) | 2085 (4%) | 1905 (5%) |
| `fill_bowl` | 1784 (1%) | 1128 (9%) | 1222 (11%) | 1103 (8%) | 1153 (8%) | 1118 (7%) |
| `fill_bucket` | 5450 (1%) | 3703 (1%) | 3174 (10%) | 2987 (6%) | 2461 (5%) | 2345 (5%) |
| `fill_waterskin` | 992 (4%) | 992 (7%) | 953 (8%) | 909.8 (4%) | 936 (9%) | 816 (7%) |
| `full_bar_guard` | 64 (0%) | 48 (0%) | 24 (0%) | 24 (0%) | 24 (0%) | 24.01 (0%) |
| `heal_food` | 88 (273%) | 48 (0%) | 48 (0%) | 48 (0%) | 48 (0%) | 48 (0%) |
| `sample_water` | 24.01 (0%) | 24.01 (0%) | 2.428 (275%) | 4.163 (46%) | 3.807 (73%) | 4.651 (22%) |
| `sample_water_covered` | 24.01 (0%) | 24 (0%) | 14.14 (53%) | 12.86 (28%) | 10.48 (40%) | 8.303 (89%) |
| `thirst_lookup` | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) |
| `thirst_lookup_potion` | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) |
| `thirst_tick_idle` | 16.26 (0%) | 0.454 (0%) | 0.454 (0%) | 3.245 (0%) | 2.986 (13%) | 2.791 (0%) |
| `tooltip_food` | 144 (0%) | 144 (0%) | 144 (0%) | 144 (0%) | 144 (0%) | 144 (0%) |
| `tooltip_water_bottle` | 216 (0%) | 216 (0%) | 216 (0%) | 216 (0%) | 216 (0%) | 216 (0%) |
| `tooltip_waterskin` | 384 (0%) | 384 (0%) | 384 (0%) | 384.1 (0%) | 384.1 (0%) | 384.1 (0%) |
| `water_quality_read` | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) |
| `waterskin_mix` | 0 (0%) | 80 (0%) | 176 (0%) | 176 (0%) | 176 (0%) | 112 (0%) |

## Microseconds per interaction

Kept for order of magnitude only: a difference under about half is not a difference. See
[the benchmark's own notes](../../../src/dev/java/com/thirstwastaken2/dev/benchmark/AGENTS.md#comparing-two-versions-of-the-code).

Fabric:

| Operation | 1.20.1 | 1.21.1 | 1.21.11 | 26.1.x | 26.2.x | 26.3.x |
|---|---|---|---|---|---|---|
| `cauldron_pour` | 2.07 (20%) | 2.52 (20%) | 2.80 (8%) | 4.66 (47%) | 2.13 (62%) | 2.01 (33%) |
| `drink_by_hand` | 3.21 (9%) | 3.23 (12%) | 3.80 (2%) | 3.67 (19%) | 3.76 (2%) | 3.46 (33%) |
| `drink_water_bottle` | 2.36 (39%) | 2.09 (15%) | 5.28 (20%) | 5.19 (25%) | 5.84 (6%) | 5.94 (31%) |
| `drink_waterskin` | 2.89 (22%) | 2.67 (11%) | 5.09 (13%) | 3.45 (23%) | 4.21 (13%) | 3.69 (14%) |
| `exhaustion_mirror` | 0.03 (36%) | 0.02 (35%) | 0.01 (21%) | 0.03 (112%) | 0.01 (14%) | 0.02 (100%) |
| `fill_bottle` | 7.83 (27%) | 9.00 (47%) | 6.78 (40%) | 7.73 (18%) | 9.06 (17%) | 8.83 (22%) |
| `fill_bowl` | 3.96 (38%) | 3.83 (49%) | 3.60 (16%) | 2.97 (27%) | 3.61 (45%) | 3.41 (26%) |
| `fill_bucket` | 13.29 (10%) | 15.38 (35%) | 13.82 (21%) | 13.27 (32%) | 12.77 (23%) | 12.88 (41%) |
| `fill_waterskin` | 4.05 (47%) | 3.84 (33%) | 3.38 (3%) | 4.76 (18%) | 4.22 (11%) | 3.73 (7%) |
| `full_bar_guard` | 0.27 (18%) | 0.10 (16%) | 0.10 (17%) | 0.13 (95%) | 0.14 (77%) | 0.14 (67%) |
| `heal_food` | 0.67 (19%) | 0.27 (15%) | 0.24 (16%) | 0.33 (50%) | 0.39 (62%) | 0.36 (10%) |
| `sample_water` | 1.47 (55%) | 1.03 (6%) | 1.27 (9%) | 1.16 (7%) | 1.28 (16%) | 1.05 (24%) |
| `sample_water_covered` | 2.33 (32%) | 1.71 (4%) | 1.95 (13%) | 1.81 (3%) | 1.93 (26%) | 1.60 (23%) |
| `thirst_lookup` | 0.05 (41%) | 0.07 (29%) | 0.08 (22%) | 0.11 (21%) | 0.10 (114%) | 0.06 (209%) |
| `thirst_lookup_potion` | 0.14 (24%) | 0.13 (38%) | 0.15 (22%) | 0.19 (25%) | 0.18 (45%) | 0.16 (53%) |
| `thirst_tick_idle` | 0.05 (75%) | 0.06 (78%) | 0.06 (22%) | 0.04 (83%) | 0.13 (43%) | 0.11 (92%) |
| `tooltip_food` | 0.14 (23%) | 0.40 (19%) | 0.37 (19%) | 0.51 (11%) | 0.60 (25%) | 0.43 (47%) |
| `tooltip_water_bottle` | 0.49 (20%) | 0.35 (55%) | 0.41 (18%) | 0.36 (32%) | 0.32 (43%) | 0.40 (37%) |
| `tooltip_waterskin` | 0.63 (15%) | 0.27 (58%) | 0.28 (10%) | 0.43 (7%) | 0.38 (67%) | 0.48 (59%) |
| `water_quality_read` | 0.10 (18%) | 0.05 (31%) | 0.05 (28%) | 0.08 (14%) | 0.06 (24%) | 0.07 (23%) |
| `waterskin_mix` | 0.18 (18%) | 0.28 (30%) | 0.26 (37%) | 0.11 (88%) | 0.16 (53%) | 0.13 (16%) |

NeoForge, and Forge on 1.20.1:

| Operation | 1.20.1-forge | 1.21.1-nf | 1.21.11-nf | 26.1.x-nf | 26.2.x-nf | 26.3.x-nf |
|---|---|---|---|---|---|---|
| `cauldron_pour` | 5.42 (46%) | 3.74 (29%) | 5.62 (38%) | 7.21 (20%) | 5.49 (36%) | 3.54 (14%) |
| `drink_by_hand` | 5.55 (38%) | 4.64 (25%) | 4.37 (32%) | 3.90 (19%) | 3.65 (50%) | 4.30 (23%) |
| `drink_water_bottle` | 5.17 (25%) | 2.71 (14%) | 8.01 (14%) | 6.85 (45%) | 5.30 (16%) | 6.03 (26%) |
| `drink_waterskin` | 5.47 (35%) | 3.23 (53%) | 6.47 (34%) | 4.61 (6%) | 3.50 (85%) | 4.67 (14%) |
| `exhaustion_mirror` | 0.03 (50%) | 0.02 (100%) | 0.02 (110%) | 0.01 (355%) | 0.01 (47%) | 0.02 (59%) |
| `fill_bottle` | 15.03 (26%) | 12.49 (45%) | 13.92 (66%) | 8.17 (5%) | 8.28 (13%) | 9.23 (7%) |
| `fill_bowl` | 6.51 (33%) | 5.35 (57%) | 7.05 (38%) | 5.83 (10%) | 4.57 (50%) | 5.14 (14%) |
| `fill_bucket` | 23.15 (9%) | 17.17 (14%) | 18.65 (18%) | 16.57 (9%) | 14.13 (1%) | 13.56 (13%) |
| `fill_waterskin` | 6.27 (50%) | 4.86 (41%) | 4.92 (53%) | 4.33 (29%) | 5.12 (34%) | 3.94 (17%) |
| `full_bar_guard` | 0.35 (44%) | 0.19 (34%) | 0.17 (20%) | 0.24 (67%) | 0.18 (16%) | 0.21 (44%) |
| `heal_food` | 1.17 (40%) | 0.98 (75%) | 0.49 (20%) | 0.50 (55%) | 0.63 (37%) | 0.39 (63%) |
| `sample_water` | 1.91 (84%) | 1.70 (11%) | 1.56 (23%) | 2.06 (39%) | 1.90 (6%) | 2.01 (10%) |
| `sample_water_covered` | 3.51 (43%) | 2.18 (46%) | 3.50 (22%) | 4.18 (16%) | 4.30 (43%) | 3.17 (25%) |
| `thirst_lookup` | 0.08 (26%) | 0.07 (103%) | 0.12 (32%) | 0.12 (13%) | 0.08 (34%) | 0.06 (91%) |
| `thirst_lookup_potion` | 0.21 (30%) | 0.21 (50%) | 0.15 (39%) | 0.20 (72%) | 0.27 (107%) | 0.14 (17%) |
| `thirst_tick_idle` | 0.14 (8%) | 0.08 (102%) | 0.15 (66%) | 0.07 (87%) | 0.06 (155%) | 0.06 (75%) |
| `tooltip_food` | 0.29 (17%) | 0.53 (28%) | 0.54 (37%) | 0.51 (53%) | 0.41 (6%) | 0.51 (31%) |
| `tooltip_water_bottle` | 0.83 (30%) | 0.55 (33%) | 0.30 (129%) | 0.46 (24%) | 0.41 (106%) | 0.46 (30%) |
| `tooltip_waterskin` | 0.86 (26%) | 0.44 (21%) | 0.48 (30%) | 0.51 (17%) | 0.57 (30%) | 0.59 (27%) |
| `water_quality_read` | 0.08 (112%) | 0.06 (16%) | 0.07 (11%) | 0.06 (57%) | 0.07 (13%) | 0.05 (39%) |
| `waterskin_mix` | 0.26 (64%) | 0.19 (46%) | 0.17 (148%) | 0.11 (33%) | 0.18 (54%) | 0.19 (62%) |

The time columns carry wide spreads, so they are an order of magnitude and nothing finer.
`B/player/tick` and the bytes per interaction are the figures to watch for a regression.
