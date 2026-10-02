# src/main/culturaldelights — sea water in Cultural Delights' vat

[Cultural Delights](https://modrinth.com/mod/cultural-delights) (mod id `culturaldelights`, package
`com.baisylia.culturaldelights`) brews drinks in a vat since 0.18. Its water recipes take a water
bucket as an item, matched by the tag `c:buckets/water`, and a bucket of sea water is still a
`minecraft:water_bucket`. This directory stops the vat brewing from sea water. The plan and what is
still to do are in
[docs/dev/integration/cooking/CULTURAL-DELIGHTS-INTEGRATION.md](../../../docs/dev/integration/cooking/CULTURAL-DELIGHTS-INTEGRATION.md).

**Built on `1.21.1-neoforge` only.** The vat is 0.18, which is NeoForge only; the Fabric port stopped at
0.17, has no vat, and is not built against. What it does:

- **nothing brews while the vat holds sea water**: the bucket can sit in its slot, by hand or by hopper,
  and a vat already brewing stops;
- **fresh water of any grade brews** into ordinary drinks with no grade, as Brewin' and Chewin's keg
  does: the drink is its own item.

The drink and food values are not here. They are ids in `ThirstConfig`, common code that names no class
of the mod, so they reach every node, the Fabric port's foods included.

## Which build

| Node | Build | Modrinth id | Also on `runClient` |
|---|---|---|---|
| `1.21.1-neoforge` | `0.18.1-1.21.1` | `jL5hP1qm` | Cook's Collection `0.6.1-1.21.1` (`z1YUDQXH`), which 0.18 requires; Farmer's Delight is already there |

The 0.18 source is not published (the `Cultural-Delights-1.21` repository stops at 0.17.8), so the one
target below was read from the jar.

```
culturaldelights/java/com/thirstwastaken2/culturaldelights/
  CulturalDelightsPresence     the gate: the mod file has the vat's class, and each target method
                               is read off its class file
  CulturalDelightsMixinPlugin  applies each mixin only where the gate allows it
  VatWater                     whether any slot holds a salty stack; names nothing of the mod's
  mixin/VatSeaWaterMixin       hasRecipe: false while the vat holds sea water
culturaldelights/resources/
  thirstwastaken2.culturaldelights.mixins.json
```

## How it works

`VatBlockEntity.tick` asks the private static `hasRecipe(VatBlockEntity)` every tick; on true the
progress goes up, on false it is reset. `VatSeaWaterMixin` answers false at its head while any slot
holds a stack with `water_salty: true`. The vat is a vanilla `Container` (it is a `WorldlyContainer`),
so `VatWater` reads it as one and names no class of the mod.

The mod also turns sea water into milk in Farmer's Delight's Cooking Pot (`milk_from_beans`) and into
`corn_dough` at a crafting table. Both are left alone: the pot boils, and the dough is not drunk.

## How it stays optional

1. **Build.** Only where `deps.cultural_delights` is set does `build.neoforge.gradle.kts` add this
   directory and append the mixin config to the built manifest with `culturaldelights` as an optional
   dependency, from [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   The mod is `compileOnly`, and on `runClient` only, with Cook's Collection, so `runServer` and the
   gametests run without it. `-PwithoutOptional=culturaldelights` (or `cultural-delights`) leaves both
   out of `runClient`.
2. **Runtime gate.** `CulturalDelightsPresence` reads FML's list of mod files and looks for the vat's
   class in the mod's own jar, so a 0.17 under the same id turns it off with one log line. It names no
   class of the mod's and no Minecraft class.
3. **Mixin plugin.** `shouldApplyMixin` asks the gate, then whether `hasRecipe` is still declared.

## Checking it

- `checkOptionalSeam` finds the plugin and the gate loaded without the mod, and passes.
- `runGametest` passes unchanged: the mod is never on its classpath.
  `culturalDelightsDrinksAreMergedIntoAnOlderConfig` checks the config values.
- `./gradlew ":1.21.1-neoforge:runClient" -Pagent=tools/agent/smoke/boot.jsonl -PwithoutOptional=culturaldelights`
  comes up and stays up.
- What it does is checked in a real client with
  [tools/agent/integrations/cultural-delights.jsonl](../../../tools/agent/integrations/cultural-delights.jsonl),
  whose header says how to run and verify it.
