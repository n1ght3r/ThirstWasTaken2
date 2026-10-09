# Create Pipes n Physics: issue drafts

Two issues for [StaticFX/create-pipes-n-physics](https://github.com/StaticFX/create-pipes-n-physics/issues),
step 6 of [PIPES-N-PHYSICS-INTEGRATION.md](PIPES-N-PHYSICS-INTEGRATION.md). Not posted yet: each goes out
in this project's maintainer's name, so they post it, or say to. Delete this file once both are up,
and link them from the plan.

---

## 1. `NoClassDefFoundError: SableCompanion` without Sable

**Version:** 3.2.1 (NeoForge 1.21.1), Create 6.0.10, no Sable or Sable Companion installed.

**What happens:** the client crashes with `NoClassDefFoundError` for `SableCompanion`, thrown from
`TiltedTankFluid.resolve`, the first time a Fluid Tank with a window is rendered by Flywheel.

**Steps:**

1. Install Create 6.0.10 and Create Pipes n Physics 3.2.1, nothing else.
2. Place a Fluid Tank and look at it.

**Expected:** the tank renders; Sable is listed as optional.

**Workaround:** `fluidTiltEnabled = false` in `config/pipesnphysics-client.toml`.

It looks like `TiltedTankFluid.resolve` reaches Sable Companion without the `SableCompat.isCompanionLoaded()`
check the rest of the mod uses.

---

## 2. Open-end intake moves nothing when another mod adds components to drained water

**Version:** 3.2.1 (NeoForge 1.21.1), Create 6.0.10.

**Context:** our mod (Thirst Was Taken 2) grades water. When Create's `OpenEndedPipe.removeFluidFromSpace`
draws water from the world or a cauldron, a mixin of ours adds a data component to the returned
`FluidStack` (its grade), so that the water keeps it in tanks and pipes.

**What happens:** with Pipes n Physics, a pump drawing water through an open pipe end moves nothing.
Lava and honey still flow. `BoundaryColumn.drinkableSource` builds the probe itself
(`new FluidStack(fluidState.getType(), 1000)`, or `VanillaFluidTargets.drainBlock(..., true)`), and
`drainMatching` later compares it with `isSameFluidSameComponents` against what `removeFluidFromSpace`
actually drains, which carries the component. The two never match.

**Steps:** a one-block water pool, a pipe end facing it, a Mechanical Pump, a Fluid Tank, with any mod
that adds a component to fluid drained by `OpenEndedPipe`.

**Asks, either of:**

- build the probe from Create's own `OpenEndedPipe.removeFluidFromSpace(true)` (simulate), so it is the
  same stack the drain will return, components included; or
- an API hook to decorate the probe, beside `EndpointApi`.

Either lets us drop a `@Pseudo` mixin into `BoundaryColumn.intakeFluid`, which we added in the meantime
and which we know may break when the engine moves. The Sable sub-level path
(`OpenEndedPipeMixin.drainSourceAt`) builds its own stack the same way, so it would want the same
treatment.
