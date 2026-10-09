# src/main/create — the Sand Filter on Create for NeoForge

The same Sand Filter as [src/main/createfly](../createfly/AGENTS.md), built on
[Create](https://modrinth.com/mod/create) itself for NeoForge. Water pumped in at the top comes out
of the bottom one grade cleaner, and water keeps its grade through Create's pipes, pumps, Spouts and
drains. Read the Create Fly guide first: the behaviour, the one-component rule for water quality and
the four mixins are the same, and this file only records what differs.

This directory is **only compiled by nodes that set `deps.create`** in
`stonecutter.properties.toml`. Today that is `1.21.1-neoforge`, with Create 6.0.10. Create 6 ships
for NeoForge on 1.21.1 only, so no other NeoForge node can set it. Its 1.20.1 build is for Forge 47, and
the same filter for it lives in [src/main/createforge](../createforge/AGENTS.md).

```
java/com/thirstwastaken2/create/
  CreatePresence          the gate: FML's mod file for `create`, and a marker class inside it
  CreateEntrypoint        a second @Mod class for the mod id; FML constructs it next to the loader's own
  CreateMixinPlugin       applies the mixins below only when the gate passes, BoundaryColumnMixin only
                          when PipesPresence passes too
  PipesPresence           the gate for Create Pipes n Physics: its mod file, and the engine methods read
                          off its class file
  IntakeSamples           the quality at each open pipe end that addon probes, by position
  SandFilter              block, item, block entity type, fluid capability, creative tab entry
  SandFilterBlock         IBE and IWrenchable; the comparator reads the output tank
  SandFilterBlockEntity   two one-bucket tanks, the transfer, and the goggle tooltip
  mixin/                  quality through Create's item and world fluid transfers
resources/
  thirstwastaken2.create.mixins.json
  assets/…                copies of the Create Fly blockstate, model and texture
  data/…                  recipe, recipe unlock, loot table (NeoForge conditions), pickaxe tag, and
                          pipesnphysics:separate_ports
```

## How it stays optional

1. **Build.** Only when `deps.create` is set, `build.neoforge.gradle.kts` adds these directories and
   appends the mixin config and an optional `create` dependency to the built `neoforge.mods.toml`, as
   [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt) says. The source manifest, shared by every NeoForge node, never names them.
2. **Runtime gate.** `CreateEntrypoint` touches `SandFilter` only after `CreatePresence.isPresent()`.
   The gate reads `LoadingModList`, which is complete before any mixin config is read, and looks the
   marker class up inside Create's own jar, so it loads no Create or Minecraft class.
3. **Data.** The recipe, its advancement and the loot table carry `neoforge:mod_loaded` conditions.

**Nothing outside this directory may reference a class in it.**

Water quality on NeoForge's `FluidStack` is `WaterFluids`, and the per-pump sample cache is
`SampledWater`, both in `src/main/neoforge` and shared with the Sophisticated Core integration; the
one grade the Sand Filter adds per pass stays in `SandFilterBlockEntity`.

## Compiling against Create

Create's Modrinth jar is `compileOnly`, and so are the three libraries it bundles in `META-INF/jarjar`
(Ponder with Catnip, Flywheel, Registrate), which the `createLibraries` task copies out: the filter
extends classes whose supertypes live there. `runClient` gets the real jar through `clientRunMods`,
and FML reads the bundled libraries out of it the same way it does in a player's game. ModDevGradle
does not remap, so none of the Create Fly workarounds apply.

## Differences from Create Fly

- **Millibuckets, not droplets.** A bucket is 1000 and the filter moves 10 a tick.
- **Pipes find the tanks through `Capabilities.FluidHandler.BLOCK`**, registered by side:
  `UP` is the input, `DOWN` and `null` (a player's hand) the output, the sides have none. A pipe
  beside the filter therefore does not bend towards it at all.
- **Only water enters** through the input tank's `FluidTank` validator rather than a custom handler.
- **The transfer mutates the tanks' own stacks** and hands each tank its stack back through
  `SmartFluidTank.setFluid`, which is what fires Create's update callback, so a busy filter still
  allocates nothing per tick. The caching rules are the Create Fly ones.
- **Goggles** read `IHaveGoggleInformation` straight off the block entity, as every Create block
  entity on NeoForge does, so there is no client-only half.

## Create Pipes n Physics

[Create Pipes n Physics](https://modrinth.com/mod/create-pipes-n-physics) (`pipesnphysics`, NeoForge
1.21.1 only) replaces the transport of Create's pipes and pumps with its own engine. It copies stacks
with their components and compares them with `isSameFluidSameComponents`, so grades already travel
and never merge. Two things needed fixing, both here, since they only matter where the Sand Filter and
the mixins above exist. It has no deps key and nothing compiles against it. The plan, and what was
found in game, are in
[PIPES-N-PHYSICS-INTEGRATION.md](../../../docs/dev/integration/storage/PIPES-N-PHYSICS-INTEGRATION.md).

- **The Sand Filter is two ports.** The engine joins every pipe touching a handler into one network
  and asks the `null` side, the output tank, for the upstream fluid, so the filter's output never left.
  `data/pipesnphysics/tags/block/separate_ports.json` puts the filter in the addon's own tag, behind a
  `neoforge:mod_loaded` condition.
- **The intake probe is stamped.** The engine plans from a plain `FluidStack` it builds for an open
  pipe end (`BoundaryColumn.drinkableSource`), then drains through Create's `removeFluidFromSpace`,
  which `OpenEndedPipeMixin` stamps, and moves nothing when the two differ. `BoundaryColumnMixin`
  stamps the first probe in `intakeFluid`, the pipe's own mouth, with `IntakeSamples`, which samples
  the way `SampledWater` does. It leaves the probe plain when the mouth is on a Sable sub-level
  (`worldOutputPos` differs from the mouth): the addon drains that one itself, unstamped, and our
  `@ModifyReturnValue` never runs on its cancelled path.

`BoundaryColumnMixin` is `@Pseudo` with a string target, in this directory's mixin config, and its
injector is `require = 0`. The addon says its engine classes move between releases, and a `@Shadow`
that finds nothing fails the class, so `PipesPresence` reads `BoundaryColumn`'s class file out of the
addon's jar and the plugin applies the mixin only when `drinkableSource`, `worldOutputPos` and
`intakeFluid` are there with 3.2.1's descriptors. Otherwise it logs a warning and intake is back to
moving nothing, with no crash.

Its `runClientMod` line sits under Create's in `build.neoforge.gradle.kts`, commented out like it,
pinned by Modrinth version id and bumped by hand. 3.2.1 crashes a client without Sable on the first
windowed Fluid Tank: set `fluidTiltEnabled = false` in `run/1.21.1-neoforge/config/pipesnphysics-client.toml`.

## Testing

The gametests run without Create and prove the node still loads without it. With Create, drive
`./gradlew ":1.21.1-neoforge:runClient" -Pdriven -Pquickplay=<world>` through the agent. Use
`client.command` for `/data get block`: the agent's `server.command` runs off the server thread,
where `Level.getBlockEntity` answers null for every block. Checked on 2026-09-18:

- NBT into the input tank (`InputTanks:[{TankContent:{Fluid:{id:"minecraft:water",amount:1000,...}}}]`)
  filters 10 mB a tick, Murky to Clean;
- a pump from a sea water source, through pipes into the top of the filter, and a second pump from
  the bottom into a Fluid Tank: sea water arrives unchanged;
- the same from a water cauldron with `purity=2` (Murky): the tank holds Clean water and the cauldron
  is drained;
- a Spout over a Depot fills a glass bottle with the tank's grade and `water_salty: false`;
- a graded bottle dropped on an Item Drain keeps its grade in the drain's tank;
- a stack of three terracotta bowls on a Depot under a Spout all come out Clean water bowls. Create
  asks the whole stack's capability, which refuses a stack of more than one, so
  `GenericItemFillingMixin` asks about one. Checked on 2026-09-23, two filters in series, Dirty to
  Murky to Clean.

[tools/agent/integrations/create-water.jsonl](../../../tools/agent/integrations/create-water.jsonl) repeats two of these unattended,
the Sand Filter by NBT and a Mechanical Pump drawing from a sea-water pool through an open pipe end
into a Fluid Tank. Run on 2026-09-19 after `WaterFluids` and `SampledWater` moved to
`src/main/neoforge`: Murky came out Clean, and the tank held `water_salty` water. Once the waterskin
and the bowls had a fluid capability, the same script had a Spout fill a waterskin on a Depot (three
servings of the tank's grade, and 750 mB gone from the Spout's 1000, so one fill and not two) and an
Item Drain empty a dirty water bowl (250 mB of `water_purity: 0`). Create reaches them through the
capability; the item mixins stamp the result again, to the same grade.

[tools/agent/integrations/pipesnphysics-water.jsonl](../../../tools/agent/integrations/pipesnphysics-water.jsonl)
does the same with Create Pipes n Physics, nine rows; its header says what each should read. Run on
2026-10-09, every row passed, the Hose Pulley among them. A Hose Pulley takes its pipe on the side
counter-clockwise of its `facing`, not on top, and a creative motor at `ScrollValue:-32` on the other
side lowers the hose. With Create alone the pulley is still not checked.
