# src/main/createforge — the Sand Filter on Create for Forge 1.20.1

The Sand Filter of [src/main/create](../create/AGENTS.md) on MinecraftForge 47, for Create 6's 1.20.1
build. Read that guide and the Create Fly one it points to first: the behaviour, the four mixins and the
caching rules are the same. This file only records what differs on Forge 1.20.1.

This directory is **only compiled by nodes that set `deps.create` on Forge**: its row in
[the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt)
shares the deps key with the NeoForge one and lists only `Loader.FORGE`. Today that is `1.20.1-forge`,
with Create 6.0.8. It is a directory of its own rather than a fork of `src/main/create`, because every
class there names NeoForge's fluid and capability API.

```
java/com/thirstwastaken2/createforge/
  CreatePresence          the gate: FML's mod file for `create`, and a Create 6 marker class inside it
  CreateEntrypoint        an @IntegrationEntrypoint Runnable; Forge 47 takes one @Mod class per mod id
  CreateMixinPlugin       applies the mixins below only when the gate passes
  SandFilter              block, item, block entity type, creative tab entry
  SandFilterBlock         1.20.1's Block signatures; otherwise the NeoForge block
  SandFilterBlockEntity   the NeoForge block entity on Forge's FluidStack, with its own getCapability
  mixin/                  the four NeoForge mixins, on Forge's FluidStack
resources/
  thirstwastaken2.createforge.mixins.json
  assets/…                copies of the Create Fly blockstate, model and texture
  data/…                  1.20.1's plural folders: recipe and unlock (Forge conditions), loot table, two tags
```

Water quality on Forge's `FluidStack` is `forge/WaterFluids`, and the per-pump sample cache is
`forge/SampledWater`, both in `src/main/forge`, as NeoForge keeps its own in `src/main/neoforge`.

## What differs from the NeoForge filter

- **Entrypoint.** `ThirstWasTaken2Forge` runs every `@IntegrationEntrypoint` in its scan data while it
  is constructed, so `CreateEntrypoint` reaches the mod bus through `FMLJavaModLoadingContext`.
- **Capability.** Forge 47 asks the block entity, not a registry: `getCapability` hands out the input
  tank's `LazyOptional` from above and the output's from below or for a hand, and nothing on the sides.
  The tank behaviours invalidate their own on unload.
- **Fluid stacks.** `isFluidEqual` compares fluid and tag, `new FluidStack(stack, amount)` copies with
  an amount; the rest is the same.
- **The recipe** takes `#forge:sand`, since Forge 47 fills no `c:sands`, and carries `forge:mod_loaded`
  in `conditions`, as its unlock does.
- **The loot table has no condition.** Forge 47 reads `conditions` on recipes and advancements but not on
  loot tables, and a table naming an item that is not registered fails to parse on every server without
  Create. So it drops a `tag` entry, `thirstwastaken2:sand_filter`, an item tag holding the block's item
  as `required: false`: it parses everywhere and drops the item only where Create registered it.

## Compiling against Create

Create's Modrinth jar and the libraries it bundles (Ponder with Catnip, Flywheel, Registrate) are in SRG
names, so all of them are `modCompileOnly` and remapped. A remapping configuration takes only modules,
not files copied out of the jar as on NeoForge, so `build.forge.gradle.kts` reads the libraries' Maven
coordinates from Create's `META-INF/jarjar/metadata.json` and resolves them from maven.createmod.net and
maven.tterrag.com. The mixins are `remap = false`: they name Create's own members, and the Minecraft
classes in their signatures keep their names under SRG.

## Testing

The gametests run without Create and prove the node still loads without it, and a server without it
logs no loot table error. With Create,
[tools/agent/integrations/create-water-1.20.1.jsonl](../../../tools/agent/integrations/create-water-1.20.1.jsonl)
runs the NeoForge script's checks in NBT, plus breaking the filter. Run on 2026-09-28: Murky came out
Clean, a pump from a sea-water pool filled a Fluid Tank with `salty:1b` water, a Spout filled a
waterskin with three servings of grade 2 and spent 750 mB, an Item Drain kept a dirty bowl's grade 0 in
its tank, and the broken filter dropped itself. A production Forge 47.4.10 server with the shipped jar
and Create 6.0.8 logged "Create found, registered the Sand Filter" with no mixin or data errors.

Not yet checked in game: the Hose Pulley, as on NeoForge.
