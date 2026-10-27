# src/main/immersiveengineeringforge — water through Immersive Engineering on Forge 1.20.1

The same Fluid Pump, Bottling Machine and bucket mixins as [src/main/immersiveengineering](../immersiveengineering/AGENTS.md),
for Immersive Engineering 10.2.0 on Forge 47, the `1.20.1-forge` node. Read that guide first; this one
records only what differs.

- **Its own directory**, since both mixins name the loader's `FluidStack`. Same deps key,
  `deps.immersive_engineering`, and [its own row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
- **Water quality is an NBT tag** on the fluid stack, through `com.thirstwastaken2.forge.WaterFluids`
  and `SampledWater`.
- **Buckets** go through `com.thirstwastaken2.forge.WaterQualityFluidHandler`, the Forge copy of the
  NeoForge one. Forge's bucket wrapper compares the tag in `drain(FluidStack)`, so it is handed plain
  water and the grade is put back on what comes out.
- **No Jerrycan mixin.** `IEItemFluidHandler.fill` compares with `isFluidEqual`, which on Forge reads
  the tag, so grades already stay apart there.
- **IE's jar is in SRG names**, so it is a `modCompileOnly` and remapped. The mixins are
  `remap = false`: they name only IE's and Forge's own methods, and the vanilla calls in their bodies are
  remapped with the rest of the jar.

```
immersiveengineeringforge/java/com/thirstwastaken2/immersiveengineeringforge/
  ImmersiveEngineeringForgePresence, ImmersiveEngineeringForgeMixinPlugin, ImmersiveEngineeringForgeWater,
  mixin/FluidPumpMixin, mixin/BottlingProcessMixin, mixin/FluidUtilsMixin, mixin/ForgeFluidUtilMixin
immersiveengineeringforge/resources/
  thirstwastaken2.immersiveengineeringforge.mixins.json
```

## Checking it

[tools/agent/integrations/immersive-engineering-1.20.1.jsonl](../../../tools/agent/integrations/immersive-engineering-1.20.1.jsonl)
makes the same pump, Bottling Machine and bucket checks, in Forge's NBT. Uncomment IE's `runClientMod` line in
`build.forge.gradle.kts` first.
