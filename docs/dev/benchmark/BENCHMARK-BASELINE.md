# Benchmark baseline

What `/thirst benchmark` measured on every node on 2026-09-16, as the mark later runs are read
against. The two 26.3 nodes came later and have no baseline yet; take one on the machine below before
reading a 26.3 run against anything. The two 1.20.1 nodes came later too, and have
[a baseline of their own](#the-1201-nodes), taken on mod 1.4.1. Each figure is the **median of three runs**, with the spread of those runs in brackets:
`(max - min) / median`. A later run that differs by less than the spread has shown nothing.

Numbers are worth only what the machine they came from is worth, so this file records it. Take a
new baseline on a different machine rather than comparing across two.

| | |
|---|---|
| Machine | Intel64 Family 6 Model 170 Stepping 4, GenuineIntel, 22 logical processors, Windows 11 10.0 (amd64) |
| JVM heap | 3970.0 MiB max, allocation counting on |
| Java | 21 on the 1.21.x nodes, 25 on the 26.x ones, as each Minecraft version requires |
| World | `thirst-benchmark`, seed `8213470596118`, the same terrain on every node |
| Profile | `standard`: 1, 10, 50, 100 and 200 players, 200 warm-up + 600 measured ticks each, 10 000 ops per interaction |
| Mod | 1.0.6 |

```bash
python tools/benchmark/bench.py --repeats 3
python tools/benchmark/aggregate.py <this set> --compare run/benchmark-sets/<new>
```

## Per server tick, 200 players

A tick has 50 ms. `us/player` is per player per tick.

| Node | ms/tick | p99 ms | us/player | B/player/tick |
|---|---|---|---|---|
| 1.21.1 | 0.049 (73%) | 0.196 (45%) | 0.245 (73%) | 90.52 (4%) |
| 1.21.11 | 0.06 (38%) | 0.224 (48%) | 0.299 (37%) | 42.52 (0%) |
| 26.1.x | 0.049 (49%) | 0.188 (34%) | 0.245 (48%) | 12.43 (0%) |
| 26.2.x | 0.051 (14%) | 0.23 (23%) | 0.253 (14%) | 12.43 (0%) |
| 1.21.1-neoforge | 0.047 (55%) | 0.182 (36%) | 0.234 (56%) | 7.903 (72%) |
| 1.21.11-neoforge | 0.04 (23%) | 0.2 (30%) | 0.2 (22%) | 11.12 (1%) |
| 26.1.x-neoforge | 0.057 (25%) | 0.283 (49%) | 0.285 (24%) | 43.44 (15%) |
| 26.2.x-neoforge | 0.06 (45%) | 0.245 (42%) | 0.301 (45%) | 43.8 (14%) |

The time columns carry spreads of 14 to 73%, so they are an order of magnitude and nothing
finer. `B/player/tick` is the column to watch for a regression.

## Memory

| Node | first touch B/player | steady tick B/player | ThirstData B | ExhaustionTracker B |
|---|---|---|---|---|
| 1.21.1 | 1738 (2%) | 98.63 (4%) | 32.05 | 40 |
| 1.21.11 | 1231 (3%) | 42.06 (0%) | 32.05 | 40 |
| 26.1.x | 870.2 (5%) | 8.278 (0%) | 32.05 | 40 |
| 26.2.x | 870.5 (0%) | 8.278 (0%) | 32.05 | 40 |
| 1.21.1-neoforge | 362.5 (7%) | 6.594 (38%) | 32 | 40 |
| 1.21.11-neoforge | 325 (12%) | 6.589 (0%) | 32.05 | 40 |
| 26.1.x-neoforge | 694.5 (4%) | 42.9 (10%) | 32.05 | 40 |
| 26.2.x-neoforge | 758.8 (16%) | 44.59 (9%) | 32 | 40 |

## Bytes per interaction

The steadiest thing the benchmark measures: a median spread of 3% across every node and
operation. This is the table a change to the mod's own code shows up in first.

| Operation | 1.21.1 | 1.21.11 | 26.1.x | 26.2.x | 1.21.1-nf | 1.21.11-nf | 26.1.x-nf | 26.2.x-nf |
|---|---|---|---|---|---|---|---|---|
| `cauldron_pour` | 815.8 (5%) | 823.2 (9%) | 798.9 (3%) | 780.9 (3%) | 1019 (8%) | 885 (22%) | 927.4 (21%) | 842.2 (13%) |
| `drink_by_hand` | 2844 (4%) | 2436 (6%) | 1931 (6%) | 1930 (5%) | 1942 (3%) | 2258 (4%) | 2586 (4%) | 1657 (10%) |
| `drink_water_bottle` | 1846 (10%) | 2681 (1%) | 2255 (6%) | 2269 (6%) | 338.4 (9%) | 2369 (2%) | 2692 (12%) | 2246 (5%) |
| `drink_waterskin` | 1846 (2%) | 2845 (1%) | 2401 (1%) | 2347 (4%) | 448.6 (4%) | 2566 (2%) | 2813 (4%) | 2353 (2%) |
| `exhaustion_mirror` | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) |
| `fill_bottle` | 3222 (5%) | 1914 (16%) | 2071 (12%) | 2033 (19%) | 3425 (11%) | 2325 (8%) | 2403 (79%) | 2236 (19%) |
| `fill_bowl` | 1120 (3%) | 1111 (8%) | 1042 (6%) | 1056 (5%) | 1248 (11%) | 1168 (11%) | 1210 (12%) | 1173 (11%) |
| `fill_bucket` | 4286 (3%) | 3321 (3%) | 3013 (4%) | 3127 (3%) | 3765 (1%) | 3222 (5%) | 3099 (15%) | 2608 (6%) |
| `fill_waterskin` | 984 (3%) | 936 (3%) | 816 (14%) | 896 (5%) | 1096 (9%) | 1024 (6%) | 997.2 (7%) | 984 (9%) |
| `full_bar_guard` | 48 (0%) | 24 (0%) | 24 (0%) | 24 (0%) | 48 (0%) | 24 (0%) | 24.01 (0%) | 24.01 (0%) |
| `sample_water` | 40.01 (0%) | 40.01 (0%) | 40 (0%) | 40 (0%) | 40.01 (39%) | 21.08 (12%) | 18.25 (66%) | 16 (8%) |
| `thirst_lookup` | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) | 0 (0%) |
| `thirst_tick_idle` | 116 (4%) | 49 (0%) | 9 (0%) | 9 (0%) | 7 (43%) | 7 (0%) | 50 (10%) | 52 (10%) |
| `tooltip_food` | 144 (0%) | 144 (0%) | 144 (0%) | 144 (0%) | 144 (0%) | 144 (0%) | 144 (0%) | 144 (0%) |
| `tooltip_water_bottle` | 232 (0%) | 232 (0%) | 232 (0%) | 232 (0%) | 232 (0%) | 232 (0%) | 232 (0%) | 232 (0%) |
| `tooltip_waterskin` | 400 (0%) | 400 (0%) | 400 (0%) | 400 (0%) | 400 (0%) | 400.1 (0%) | 400.1 (0%) | 400.1 (0%) |
| `water_quality_read` | 16 (0%) | 16 (0%) | 16 (0%) | 16 (0%) | 16 (0%) | 16 (0%) | 16 (0%) | 16 (0%) |
| `waterskin_mix` | 112 (0%) | 208 (0%) | 208 (8%) | 208 (8%) | 96 (17%) | 192 (0%) | 192 (8%) | 192 (0%) |

## Microseconds per interaction

Kept for order of magnitude only: the median spread here is 45% and the worst 222%, so a
difference under about half is not a difference. See
[the benchmark's own notes](../../../src/dev/java/com/thirstwastaken2/dev/benchmark/AGENTS.md#comparing-two-versions-of-the-code).

| Operation | 1.21.1 | 1.21.11 | 26.1.x | 26.2.x | 1.21.1-nf | 1.21.11-nf | 26.1.x-nf | 26.2.x-nf |
|---|---|---|---|---|---|---|---|---|
| `cauldron_pour` | 2.25 (13%) | 2.66 (44%) | 2.90 (36%) | 3.34 (41%) | 5.05 (53%) | 4.63 (35%) | 3.76 (80%) | 3.40 (10%) |
| `drink_by_hand` | 5.47 (33%) | 5.59 (31%) | 4.79 (5%) | 5.19 (24%) | 4.83 (28%) | 6.42 (40%) | 6.71 (48%) | 3.56 (20%) |
| `drink_water_bottle` | 4.91 (72%) | 9.12 (38%) | 5.94 (52%) | 8.40 (56%) | 2.37 (49%) | 9.63 (26%) | 6.73 (130%) | 5.93 (70%) |
| `drink_waterskin` | 4.21 (191%) | 5.10 (64%) | 5.47 (53%) | 6.05 (78%) | 3.19 (80%) | 7.42 (24%) | 4.70 (93%) | 4.52 (24%) |
| `exhaustion_mirror` | 0.04 (62%) | 0.02 (67%) | 0.01 (60%) | 0.01 (54%) | 0.03 (188%) | 0.02 (222%) | 0.01 (89%) | 0.02 (67%) |
| `fill_bottle` | 10.74 (53%) | 8.75 (11%) | 11.63 (14%) | 13.07 (77%) | 14.20 (33%) | 19.88 (30%) | 20.86 (60%) | 12.37 (95%) |
| `fill_bowl` | 4.75 (30%) | 4.78 (12%) | 5.59 (56%) | 5.69 (24%) | 6.47 (27%) | 7.41 (23%) | 5.71 (45%) | 4.19 (68%) |
| `fill_bucket` | 17.07 (18%) | 18.14 (16%) | 17.40 (16%) | 19.28 (33%) | 18.96 (20%) | 21.83 (34%) | 16.14 (91%) | 19.57 (30%) |
| `fill_waterskin` | 3.89 (44%) | 5.07 (47%) | 5.04 (43%) | 3.54 (66%) | 6.09 (36%) | 5.67 (38%) | 4.07 (63%) | 4.44 (46%) |
| `full_bar_guard` | 0.16 (6%) | 0.16 (67%) | 0.30 (69%) | 0.22 (52%) | 0.16 (4%) | 0.16 (57%) | 0.10 (63%) | 0.16 (44%) |
| `sample_water` | 1.53 (31%) | 1.60 (31%) | 1.59 (14%) | 2.04 (33%) | 2.12 (22%) | 1.69 (25%) | 1.85 (192%) | 2.05 (52%) |
| `thirst_lookup` | 0.09 (67%) | 0.05 (66%) | 0.04 (173%) | 0.05 (102%) | 0.06 (52%) | 0.09 (61%) | 0.06 (46%) | 0.08 (11%) |
| `thirst_tick_idle` | 0.05 (73%) | 0.04 (58%) | 0.03 (13%) | 0.07 (61%) | 0.05 (16%) | 0.08 (38%) | 0.03 (104%) | 0.06 (7%) |
| `tooltip_food` | 0.35 (47%) | 0.31 (17%) | 0.50 (59%) | 0.51 (38%) | 0.29 (59%) | 0.21 (88%) | 0.42 (22%) | 0.47 (15%) |
| `tooltip_water_bottle` | 0.45 (65%) | 0.43 (72%) | 0.25 (111%) | 0.38 (52%) | 0.23 (66%) | 0.52 (39%) | 0.28 (15%) | 0.28 (75%) |
| `tooltip_waterskin` | 0.39 (46%) | 0.48 (38%) | 0.55 (40%) | 0.56 (60%) | 0.37 (72%) | 0.56 (7%) | 0.38 (25%) | 0.55 (37%) |
| `water_quality_read` | 0.07 (14%) | 0.06 (31%) | 0.04 (91%) | 0.05 (30%) | 0.08 (68%) | 0.08 (20%) | 0.05 (29%) | 0.06 (29%) |
| `waterskin_mix` | 0.29 (44%) | 0.33 (85%) | 0.29 (134%) | 0.47 (65%) | 0.22 (35%) | 0.15 (81%) | 0.19 (185%) | 0.22 (14%) |

## The 1.20.1 nodes

Taken on 2026-09-28 on the same machine, on mod 1.4.1 with Java 17, the same world and profile, into
`run/benchmark-sets/2026-09-28-1.20.1`. The mod had changed a great deal since 1.0.6, so 1.21.1 was
measured again the same day; read the 1.20.1 nodes against that column, not against the tables above.

| Figure | 1.20.1 | 1.20.1-forge | 1.21.1 (same day) |
|---|---|---|---|
| ms/tick, 200 players | 0.063 (27%) | 0.06 (140%) | 0.075 (16%) |
| p99 ms, 200 players | 0.246 (60%) | 0.245 (87%) | 0.25 (21%) |
| B/player/tick, 200 players | 12.7 (20%) | 53.58 (5%) | 105.4 (15%) |
| first touch B/player | 504.8 (3%) | 351.8 (8%) | 1650 (2%) |
| steady tick B/player | 6.594 (0%) | 20.06 (0%) | 93.57 (14%) |

Bytes per interaction, where the two differ from each other or from the later nodes:

| Operation | 1.20.1 | 1.20.1-forge |
|---|---|---|
| `cauldron_pour` | 778.2 (4%) | 996.9 (9%) |
| `drink_by_hand` | 2330 (4%) | 2798 (3%) |
| `drink_water_bottle` | 1178 (3%) | 1250 (8%) |
| `drink_waterskin` | 897.7 (0%) | 908.5 (4%) |
| `fill_bottle` | 5344 (29%) | 4776 (17%) |
| `fill_bowl` | 1601 (4%) | 1768 (2%) |
| `fill_bucket` | 5300 (3%) | 5370 (1%) |
| `fill_waterskin` | 864 (15%) | 982.2 (2%) |
| `full_bar_guard` | 48 (0%) | 64 (0%) |
| `thirst_tick_idle` | 7 (85%) | 20 (16%) |
| `tooltip_water_bottle` | 248 (0%) | 248 (0%) |
| `tooltip_waterskin` | 416 (0%) | 416 (0%) |
| `waterskin_mix` | 16 (0%) | 16 (100%) |

Filling a bottle or bucket costs the most on 1.20.1, where water is written into the item's tag rather
than a component. The tooltips are 16 bytes above the later nodes. The idle tick is a few bytes, as on
the NeoForge nodes. `python tools/benchmark/aggregate.py run/benchmark-sets/2026-09-28-1.20.1 --ops`
prints the rest.
