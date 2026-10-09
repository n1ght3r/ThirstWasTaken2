# Create Pipes n Physics integration plan

What ThirstWasTaken2 does with [Create Pipes n Physics](https://modrinth.com/mod/create-pipes-n-physics)
(mod id `pipesnphysics`, package `de.devin.pipesnphysics`), a Create addon that replaces the transport
of Create's pipes and pumps with its own engine: pressure and gravity, pumps that add head instead of
range, siphons, viscosity, a centrifuge. It adds no water of its own. What it changes is how water
already graded by this mod moves between Create's blocks. This file sets the order of work, what each
step needs and how each one is checked. Every step is done; how the result works is in
[src/main/create/AGENTS.md](../../../../src/main/create/AGENTS.md).

Written on 2026-10-09 from:

- the repository [StaticFX/create-pipes-n-physics](https://github.com/StaticFX/create-pipes-n-physics),
  `main` at `e85f454` (2026-09-10), which is `3.2.1`;
- the Modrinth project `create-pipes-n-physics`, newest release `3.2.1` (`kH90vhqN`, 2026-09-10);
- three runs of [tools/agent/integrations/pipesnphysics-water.jsonl](../../../../tools/agent/integrations/pipesnphysics-water.jsonl)
  on `1.21.1-neoforge` on the same day, with Create alone and with Create and Pipes n Physics.

## Which build for which node

**Only `1.21.1-neoforge`.** Every release is NeoForge 1.21.1, built against Create 6.0.10, the same
Create the node compiles the Sand Filter against. Create 6 has no other NeoForge node, so neither does
this.

| Node | Build | Modrinth id | Requires on `runClient` |
|---|---|---|---|
| `1.21.1-neoforge` | `3.2.1` | `kH90vhqN` | Create (`deps.create`). Sable and Sable Companion are optional; see the crash below |
| every other node | none | — | — |

**It crashes without Sable.** `3.2.1` throws `NoClassDefFoundError: SableCompanion` from
`TiltedTankFluid.resolve` on the first windowed Fluid Tank a Flywheel client renders, when Sable
Companion is absent. The call is not behind its own gate. This is not ours to fix, but every test run
hits it: set `fluidTiltEnabled = false` in `run/1.21.1-neoforge/config/pipesnphysics-client.toml`.
Reported upstream as [#89](https://github.com/StaticFX/create-pipes-n-physics/issues/89).

## What it changes for water

Its engine copies the `FluidStack` it moves (`copyWithAmount`) and compares with
`isSameFluidSameComponents`. Graded water therefore keeps its components, and two grades never merge.
That is the behaviour this mod wants, and Create alone already behaves this way. Two places break,
because the engine compares stacks it builds itself against stacks this mod has stamped:

| Path | Create alone | With Pipes n Physics 3.2.1 | Why |
|---|---|---|---|
| Tank → pump → tank, graded water | grade kept | **grade kept** (1930 of 2000 mB arrived, purity 1) | components copied |
| Murky water pumped towards a tank of Pure | refused | **refused**, Pure tank unchanged | `isSameFluidSameComponents` |
| Pump drawing world water through an open pipe end (sea, river, lake, cauldron) | `water_salty` / graded water arrives | **nothing moves**. The pool stays, the tank stays empty. Lava, the control, flows (940 mB) | see [the intake mismatch](#the-intake-mismatch) |
| Water above a Sand Filter, filter output piped down | needs a pump above; output drains with a pump below | input fills, filter works, **output never leaves**, even with a pump below and nothing above | see [the Sand Filter as one body](#the-sand-filter-as-one-body) |
| Same, with the filter tagged `pipesnphysics:separate_ports` (data pack) | — | **works**: 2000 mB Murky came out 2000 mB Clean by gravity alone, and 3000 mB Clean through the pump | |
| Spout, Item Drain, Hose Pulley | grade kept | Spout and Item Drain are untouched by the addon. Hose Pulley: **grade kept** (2468 mB of `water_salty` from a sea pool, step 4) | the addon probes and drains the same Create handler |

### The intake mismatch

To plan a tick, the engine reads what an open pipe end can drink without touching the world:
`BoundaryColumn.drinkableSource` returns `new FluidStack(fluidState.getType(), 1000)`, or
`VanillaFluidTargets.drainBlock(..., simulate)` for a cauldron. That stack carries no component. It
then drains through Create's own `OpenEndedPipe.removeFluidFromSpace`, where
[OpenEndedPipeMixin](../../../../src/main/create/java/com/thirstwastaken2/create/mixin/OpenEndedPipeMixin.java)
stamps every drawn water with its grade, as it must. `BoundaryColumn.drainMatching` compares the
two with `isSameFluidSameComponents`, finds a stamped stack where it asked for a plain one, and moves
nothing. Every water source is affected, since every one gets a stamp. Lava and honey carry none,
which is why they still flow.

On a Sable sub-level the addon cancels Create's method and drains itself (`drainSourceAt`, also an
unstamped `new FluidStack`). Our `@ModifyReturnValue` never runs there, so the probe and the drain agree
and water flows, unstamped: it counts as `defaultQuality`. That is a gap, not a failure, and it needs
Sable to see.

### The Sand Filter as one body

The engine joins every pipe touching a handler into one network through it, as two pipes on one tank
really are, and asks the side-agnostic (`null`) capability first. The filter's `null` side is its
output tank. With Murky water upstream, the network's fluid is Murky and the engine asks the output
tank for Murky water, which holds only Clean. With nothing upstream the same pump drains the output
normally. The addon's own answer is the `pipesnphysics:separate_ports` block tag (in code,
`FluidHandlerApi.declareSeparatePorts`), and a data pack with that tag fixed both rows.

## Status

| Step | State |
|---|---|
| 1. The mod on the `runClient` classpath | done |
| 2. The Sand Filter's ports: a tag | done, checked 2026-10-09 |
| 3. The intake mismatch: a mixin | done, checked 2026-10-09; built differently from the plan, see below |
| 4. Hose Pulley and cauldron intake | checked 2026-10-09: both keep their grade |
| 5. Decision: the centrifuge | decided: not planned |
| 6. Upstream | posted 2026-10-09: [#89](https://github.com/StaticFX/create-pipes-n-physics/issues/89) (Sable crash), [#90](https://github.com/StaticFX/create-pipes-n-physics/issues/90) (intake probe) |
| 7. Docs | done |

**Checked on 2026-10-09**, with `pipesnphysics-water.jsonl` on `1.21.1-neoforge` (Create 6.0.10, Pipes n
Physics 3.2.1, no hand-made data pack), every row in one run:

| Row | Result |
|---|---|
| sea, pump through an open end | 928 mB of `water_salty`, the pool drunk (was: nothing) |
| lava, the control | 940 mB |
| tank to tank, Murky | 1930 mB of purity 1 |
| gravity through the Sand Filter | 2000 mB of purity 2, the filter empty |
| pumped through the Sand Filter | 3000 mB of purity 2 |
| Murky towards Pure | refused, 500 mB of purity 3 |
| cauldron `purity=2`, pump through an open end | 965 mB of purity 1, the cauldron emptied |
| sea pool on an open top end, no pump | 1000 mB of `water_salty`, the pool drunk |
| Hose Pulley over a sea pool, pump above | 2468 mB of `water_salty` |

## 1. The mod on the `runClient` classpath

**Done.** In `build.neoforge.gradle.kts`, beside Create's line and commented out like it (Create is off in
`runClient` by default):

```kotlin
// runClientMod(listOf("pipesnphysics", "create-pipes-n-physics"), "maven.modrinth:create-pipes-n-physics:kH90vhqN") { isTransitive = false }
```

plus `optionalRunMods.include(...)` for both names, so `-PwithoutOptional` keeps them as known names.
Add the mod to `MODRINTH_DEPS` in `.github/scripts/update_mc_deps.py` only if a deps key comes with it.
None is needed: neither step 2 nor step 3 compiles against the addon. Pin the version id in the build
script and bump it by hand.

**Not a new integration row.** Both fixes live in `src/main/create`: they only matter where the Sand
Filter and the stamping mixins exist, which is exactly `deps.create`. The addon is a second gate inside
that directory, not a directory of its own.

## 2. The Sand Filter's ports: a tag

**Done.** `src/main/create/resources/data/pipesnphysics/tags/block/separate_ports.json`:

```json
{
  "neoforge:conditions": [{ "type": "neoforge:mod_loaded", "modid": "pipesnphysics" }],
  "replace": false,
  "values": [{ "id": "thirstwastaken2:sand_filter", "required": false }]
}
```

Data, not code, so nothing to keep off the load path. `checkDataConditions` wants the condition. A tag
rather than `FluidHandlerApi.declareSeparatePorts`, since the call would need the addon's API on the
classpath and an entrypoint gate for one line.

**Check:** rows "gravity" and "pumped" of the script, without the hand-made data pack. Both passed.

## 3. The intake mismatch: a mixin

**Done, differently in three places.** What was built is `BoundaryColumnMixin`, `PipesPresence` and
`IntakeSamples` in `src/main/create`:

- **It stamps the first probe in `intakeFluid`, not every return of `drinkableSource`.** `intakeFluid`
  also probes the world position of a Sable sub-level's mouth and, through `SableCompat`, overlapping
  contraptions, and both of those drain through the addon's own `drainSourceAt`, unstamped. Stamping
  them would have turned the Sable gap below into nothing moving at all. The probe is also left plain
  when `worldOutputPos` says the mouth itself is on a sub-level, for the same reason.
- **In `thirstwastaken2.create.mixins.json`, not a config of its own.** The integration table gives a
  directory one mixin config; `CreateMixinPlugin` applies this one mixin only when `PipesPresence`
  passes, which is the same effect without a build-logic field only this would use.
- **The gate reads the class file.** The mixin `@Shadow`s `worldOutputPos`, and a shadow that finds
  nothing fails the class however `require` is set, so `PipesPresence` checks that `BoundaryColumn`
  in the addon's jar declares `drinkableSource`, `worldOutputPos` and `intakeFluid` with 3.2.1's
  descriptors, as `ColdSweatPresence` does for Cold Sweat. The injector is still `require = 0`.

`IntakeSamples` is the cache below: one sample per position and level for `SampledWater.RESAMPLE_TICKS`,
pruned once per that many ticks, the cauldron never cached.

The plan as written:

The fix is to make the probe carry the same stamp the drain will. A `@ModifyReturnValue` on
`BoundaryColumn.drinkableSource(Level, BlockPos, boolean, boolean)` that stamps a returned water stack
with the quality of the water at `pos`, through `WaterFluids.stampIfKnown`. It must match exactly what
`OpenEndedPipeMixin` stamps at the same block, so both use the same sampling: `WaterPurity.storedQuality`
for a cauldron, `WaterPurity.sampleAt` for world water.

- **Off the load path without a compile dependency.** The target names only Minecraft types, so the
  mixin can be `@Pseudo` with `targets = "de.devin.pipesnphysics.engine.boundary.BoundaryColumn"`,
  and the module never compiles against the addon. It goes in a mixin config of its own,
  `thirstwastaken2.create.pipesnphysics.mixins.json`, whose plugin applies it only when
  `LoadingModList` has `pipesnphysics` (the same probe `CreatePresence` makes for `create`).
- **`require = 0`.** The addon says its engine classes move between releases (`EndpointApi`'s Javadoc).
  If a release moves `drinkableSource`, the mixin should fail quietly and intake goes back to the
  broken state, not crash the game at class load. The agent script catches it.
- **The cache.** `drinkableSource` runs for every open pipe end on every engine tick. `SampledWater`
  holds one position and the open ends of a network are many, so the mixin needs a small cache keyed by
  position with the same `RESAMPLE_TICKS` (100), cleared of entries older than that. A cauldron is a
  blockstate read and is never cached, as in `SampledWater`.
- **The two caches may disagree** for one tick when the water around a source changes between the
  probe and the drain, the probe sampled at tick N and the pipe's own `SampledWater` at tick N−50.
  Then that tick moves nothing and the next one does. Acceptable. The cleaner alternative is step 6.

**Check:** row "sea" of the script, with `seaPool` failing (the pool was drunk) and `seaCheck` passing.
Add a row for a cauldron with `purity=2` drawn through an open end, and one for a vertical mouth over a
river with no pump (the addon's siphon), since both go through `drinkableSource`.

## 4. Hose Pulley and cauldron intake

**Checked: both keep their grade** (rows "cauldron" and "pulley" of the script). The addon probes and
drains a pulley through the same Create handler, which `FluidDrainingBehaviourMixin` stamps both times,
so the two agree. The motor problem below was the rig, not the motor: a Hose Pulley takes its pipe on
the side counter-clockwise of its `facing` (west for `facing=north`, turning on the x axis), never on
top. A creative motor at `ScrollValue:-32` on its east side lowers the hose.

The plan as written:

The addon mixes into `HosePulleyBlockEntity` and probes `FluidDrainingBehaviour` through an accessor.
Our `FluidDrainingBehaviourMixin` stamps `getDrainableFluid`. If the addon compares its probe against
the stamped value, the pulley breaks the same way the open end does. Add a row: a Hose Pulley over a
sea-water pool, a pump, a tank. The pulley needs a motor turning it the lowering way, which a creative
motor placed by command did not do for `create-water.jsonl`. Try `rotation_speed_controller` or a
negative speed.

## 5. Decision: the centrifuge

**Decided: not planned**, as proposed.

The addon splits a fluid into component fluids by recipe (`CentrifugeApi`, or data packs). A recipe
from `water` with `water_salty` to fresh water would be a second way to desalinate besides the Distiller.
[SALT-WATER-REFUSALS.md](../../mechanics/SALT-WATER-REFUSALS.md) keeps sea water salty everywhere else,
so **proposed: not planned.** A pack maker can add the recipe themselves. Note that a recipe's input is
matched with `isSameFluidSameComponents`, so a graded recipe would need one recipe per grade.

## 6. Upstream

Two issues on [StaticFX/create-pipes-n-physics](https://github.com/StaticFX/create-pipes-n-physics/issues),
written for the author to act on, with the steps above as the reproduction:

1. The `SableCompanion` crash without Sable (`TiltedTankFluid.resolve`, `3.2.1`).
2. The intake probe builds a plain `FluidStack`, which disagrees with what `removeFluidFromSpace`
   drains when another mod adds components to it. Ask that the probe come from Create's own
   `removeFluidFromSpace(true)` (simulate), or for an API hook to decorate it. Either removes step 3's
   mixin into an internal class.

Posted on 2026-10-09 as [#89](https://github.com/StaticFX/create-pipes-n-physics/issues/89) and [#90](https://github.com/StaticFX/create-pipes-n-physics/issues/90). When [#90](https://github.com/StaticFX/create-pipes-n-physics/issues/90) is fixed, `BoundaryColumnMixin`,
`PipesPresence` and `IntakeSamples` can go.

## 7. Docs

**Done.**

- [src/main/create/AGENTS.md](../../../../src/main/create/AGENTS.md): a section on the addon (the tag, the
  mixin, its gate and `require = 0`), and the script in its testing section.
- [tools/agent/AGENTS.md](../../../../tools/agent/AGENTS.md): the script in the `integrations/` row.
- `CHANGELOG.md`: one line. Store pages: not listed, per the store-page rule for new integrations.
- Root `AGENTS.md`: the optional integrations table names this under Create's row, not a row of its own.

## Not planned

- **Pumps and turbines** (`PumpApi`, `TurbineApi`): this mod has neither.
- **`FluidHandlerApi` roles for the Sand Filter** beyond separate ports: the default (a reservoir per
  port) is what the runs above showed working.
- **The Sable sub-level gap** of [the intake mismatch](#the-intake-mismatch): water arrives at
  `defaultQuality`. Needs Sable to test, and step 6's upstream fix would close it too.
