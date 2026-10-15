# Mekanism integration plan

What ThirstWasTaken2 does with [Mekanism](https://modrinth.com/mod/mekanism)
([source](https://github.com/mekanism/Mekanism)). How the finished parts work is in
[src/main/mekanism/AGENTS.md](../../../../src/main/mekanism/AGENTS.md) and
[src/main/mekanismforge/AGENTS.md](../../../../src/main/mekanismforge/AGENTS.md); this file is the record
of what was found and decided.

Written on 2026-10-15 from Mekanism `1.21.1-10.7.19.85` (tag `v1.21.1-10.7.19.85`) and
`1.20.1-10.4.16.80` (tag `v1.20.1-10.4.16.80`).

## Nodes

Mekanism ships for NeoForge 1.21.1 and for Forge (and the old NeoForge fork) on 1.20.1, nothing newer.
So `1.21.1-neoforge` and `1.20.1-forge` set `deps.mekanism`; no Fabric node can.

## What was found

| Part | Before | Now |
|---|---|---|
| Electric Pump | Built a plain `FluidStack` for every source, which read as `defaultQuality` (Clean): a pump in the sea made drinkable water | Stamped with the sampled grade of the block |
| Rotary Condensentrator, vapour to water | Plain water out, so sea water boiled and condensed came back Clean | **Pure**, distilled |
| Rotary Condensentrator, water to vapour | Takes any water by tag, sea water included | Unchanged; the vapour has no grade |
| Mechanical pipes, Fluid Tank, Dynamic Tank, portable tank | Compare with `isSameFluidSameComponents` (1.21.1) or `isFluidEqual` (1.20.1) | Nothing to do: grades travel and never merge |
| Fluidic Plenisher | Places a water block | Nothing to do: water in the world is sampled again |
| Thermal Evaporation, Electrolytic Separator, Chemical Washer | Consume water | Nothing to do |
| Heavy water (pump with a Filter upgrade) | Not in `minecraft:water` | Left alone |

## Decisions

- **Condensed water is Pure**, chosen over keeping the input's grade (the vapour is a chemical stack
  with no components to carry it) and over leaving it at the default. It costs power and a machine, and
  desalinating sea water is what a condenser is for.
- **Both nodes**, chosen over 1.21.1 first.
- **A pump keeps grades apart.** Mekanism's own check already refuses a source whose stack differs from
  the last one taken, so a pump holding one grade leaves sources of another in the world. Not changed:
  mixing grades would let one Pure source upgrade a lake.

## Checked in game

On 2026-10-15, `1.21.1-neoforge` with Mekanism 10.7.19, by hand through the agent queue and then
[mekanism-water.jsonl](../../../../tools/agent/integrations/mekanism-water.jsonl):

- a pump on a 3×3 sea-water pool in an ocean biome filled 8000 mB of `water_salty` water;
- a Basic Mechanical Pipe on top of it carried that into a Basic Fluid Tank, still `water_salty`;
- a pump on a still pool in plains filled `water_purity: 1` (Murky), not salty;
- a Rotary Condensentrator given 5000 mB of water vapour by NBT condensed `water_purity: 3` (Pure).

The same day, `1.20.1-forge` with Mekanism 10.4.16 through
[mekanism-water-1.20.1.jsonl](../../../../tools/agent/integrations/mekanism-water-1.20.1.jsonl): the
tank behind the sea pump held 12000 mB tagged `salty`, the plains pump `purity: 1`, and the
Condensentrator `purity: 3`. Every check passed.

In the scripts the pipe is there from the start, so it empties the pump: the sea water is read in the
Fluid Tank, not in the pump.

## Not done

- **Filling a bottle or a waterskin** from a Mekanism tank goes through the item's fluid capability,
  which `WaterContainerFluids` stamps; not checked in game with Mekanism.
- **The pump's own bucket slot** fills a bucket from its tank through the same capability; not checked.
