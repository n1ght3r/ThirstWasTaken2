# Immersive Engineering integration plan

What ThirstWasTaken2 does with [Immersive Engineering](https://modrinth.com/mod/immersiveengineering)
([source](https://github.com/BluSunrize/ImmersiveEngineering)). How the finished parts work is in
[src/main/immersiveengineering/AGENTS.md](../../../../src/main/immersiveengineering/AGENTS.md) and
[src/main/immersiveengineeringforge/AGENTS.md](../../../../src/main/immersiveengineeringforge/AGENTS.md);
this file is the record of what was found and decided.

Written on 2026-10-15 from IE `12.4.2-194` (tag `12.4.2-194`, NeoForge 1.21.1) and `10.2.0-183` (tag
`10.2.0-183`, Forge 1.20.1).

## Nodes

IE ships for NeoForge 1.21.1, NeoForge 1.20.4 (no node) and Forge 1.20.1, nothing newer. So
`1.21.1-neoforge` and `1.20.1-forge` set `deps.immersive_engineering`; no Fabric node can.

## What was found

| Part | Before | Now |
|---|---|---|
| Fluid Pump, infinite pool (`gatherInfiniteFluidFromWorld`) | A plain `FluidStack` for every bucket, read as `defaultQuality` (Clean): a pump in the sea made drinkable water | Stamped with the sampled grade of the block beside it |
| Fluid Pump, block by block (`Utils.drainFluidBlock` in `tickServer`) | The same | Stamped, sampled before the block is picked up |
| Bottling Machine, glass bottle by recipe | IE's potion recipes take water by tag and hand out a plain water bottle: sea water made Clean bottles | The bottle gets the grade of the water spent |
| Bottling Machine, a container with a fluid capability | Filled through the item's capability | Nothing to do: `WaterContainerFluids` stamps it |
| Jerrycan, Chemical Thrower, Drill, Buzzsaw (`IEItemFluidHandler.fill`), 1.21.1 | Tops up with `isSameFluid`, ignoring components: Murky into Pure stayed Pure | Tops up only with the same grade |
| The same on 1.20.1 | `isFluidEqual`, which reads the tag | Nothing to do |
| Fluid Pipe, Wooden and Metal Barrel, Fluid Tank, the pump's tank | NeoForge or Forge `FluidTank`, which compares components or the tag; pipes copy stacks with `copyWithAmount` | Nothing to do: grades travel and never merge |
| A bucket clicked on a barrel, the Fluid Tank or the Refinery (`FluidUtils.interactWithFluidHandler`), or put in a machine's container slots (`fillFluidContainer`, `tryEmptyContainer`), 1.21.1 | NeoForge's bucket wrapper gives and takes plain water: a bucket drawn from Murky water came out Clean, and a Murky bucket went in plain ([#50](https://github.com/n1ght3r/ThirstWasTaken2/issues/50)) | Inside those three, and only there, the container's handler is core's `WaterQualityFluidHandler`, as in Extra Delight |
| The same on 1.20.1 | Forge's bucket wrapper drops the tag the same way, and its `drain(FluidStack)` refuses tagged water | The same, through a Forge copy of `WaterQualityFluidHandler` in `src/main/forge-fluidhandler` |
| Cloche, Mixer, concrete and mud by bottling | Take water by tag | Nothing to do: any grade works |

**Which pump path runs.** NeoForge and Forge ask the gamerule in `canConvertToSource`, not the number
of neighbouring sources. With `waterSourceConversion` on (the default), every water block counts as one
that refills, the pump's area scan skips water, and only the infinite path runs, on a source with two
or more horizontal source neighbours. With it off, the pump takes water block by block. Both are
stamped.

## Decisions

- **Both nodes**, as for Mekanism.
- **A pump keeps grades apart.** Its tank refuses a bucket of another grade, so a pump holding Murky
  water waits on a Clean block until its outputs have emptied it. Not changed: mixing would let one
  grade upgrade another.
- **The Jerrycan compares grades, not components**, so unstamped water tops up a can of the default
  grade, and only water is changed: another fluid compares as IE wrote it.
- **The bottle's grade is kept from the drain**, not read from the tank at the end of the process,
  since the tank may have been refilled with another grade by then. Not saved with the process: a
  process reloaded after its drain reads the tank.

## Checked in game

On 2026-10-15, `1.21.1-neoforge` with IE 12.4.2, through
[immersive-engineering.jsonl](../../../../tools/agent/integrations/immersive-engineering.jsonl):

- a pump on a 3×3 sea-water pool in an ocean biome filled 4000 mB of `water_salty` water;
- with `waterSourceConversion` off, a pump on one sea-water source took 1000 mB of `water_salty`
  water, and one in plains 1000 mB of `water_purity: 1` (Murky);
- a Jerrycan of 1000 mB of Pure water used on a Metal Barrel of Murky water stayed at 1000 mB, and used
  on a barrel of Pure water topped up to 5000 mB;
- a Bottling Machine formed with a hammer, given 4000 mB of `water_salty` water by NBT, turned a glass
  bottle into a water bottle with `water_salty`.

On 2026-10-27, the same node and script, for [#50](https://github.com/n1ght3r/ThirstWasTaken2/issues/50):
an empty bucket on the Murky Metal Barrel came back a water bucket with `water_purity: 1`, and poured
into an empty Wooden Barrel left 1000 mB of `water_purity: 1` there. The same day `1.20.1-forge`, through
[immersive-engineering-1.20.1.jsonl](../../../../tools/agent/integrations/immersive-engineering-1.20.1.jsonl),
gave a water bucket tagged `purity: 1` and a Wooden Barrel of 1000 mB tagged `purity: 1`, and every
older check there still passed.

The same day, `1.20.1-forge` with IE 10.2.0 through
[immersive-engineering-1.20.1.jsonl](../../../../tools/agent/integrations/immersive-engineering-1.20.1.jsonl):
the three pumps the same, in the `thirstwastaken2` tag, and the Bottling Machine's bottle tagged
`salty`. Every check passed. On 1.20.1 the pump runs only on a redstone signal; it has no
`redstoneInverted`.

## Not done

- **A Bottling Machine fed by pipe** from a pump in the sea, rather than by NBT: the same tank, not
  checked in game.
