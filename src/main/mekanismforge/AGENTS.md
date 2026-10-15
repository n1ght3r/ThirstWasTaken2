# src/main/mekanismforge — water through Mekanism on Forge 1.20.1

The same Electric Pump and Rotary Condensentrator mixins as [src/main/mekanism](../mekanism/AGENTS.md),
for Mekanism 10.4.16 on Forge 47, the `1.20.1-forge` node. Read that guide first; this one records only
what differs.

- **Its own directory**, since both mixins name the loader's `FluidStack`. Same deps key,
  `deps.mekanism`, and [its own row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
- **Water quality is an NBT tag** on the fluid stack, through `com.thirstwastaken2.forge.WaterFluids`
  and `SampledWater`. Mekanism compares fluid stacks with `isFluidEqual`, which reads the tag, so grades
  still travel and never merge.
- **The rotary recipe is `mekanism.api.recipes.RotaryRecipe`** itself: 10.4 has no `BasicRotaryRecipe`.
- **Mekanism's jar is in SRG names**, so it is a `modCompileOnly` and remapped. The mixins are
  `remap = false`: they name only Mekanism's own methods, and the vanilla calls in their bodies are
  remapped with the rest of the jar.

```
mekanismforge/java/com/thirstwastaken2/mekanismforge/
  MekanismForgePresence, MekanismForgeMixinPlugin, mixin/ElectricPumpMixin, mixin/RotaryRecipeMixin
mekanismforge/resources/
  thirstwastaken2.mekanismforge.mixins.json
```

## Checking it

[tools/agent/integrations/mekanism-water-1.20.1.jsonl](../../../tools/agent/integrations/mekanism-water-1.20.1.jsonl)
makes the same checks, in Forge's NBT. Uncomment Mekanism's `runClientMod` line in
`build.forge.gradle.kts` first.
