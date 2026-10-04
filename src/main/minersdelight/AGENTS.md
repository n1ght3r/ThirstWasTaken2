# src/main/minersdelight — Miner's Delight's copper cup keeps water's grade

[Miner's Delight](https://modrinth.com/mod/miners-delight) (mod id `minersdelight` on 1.21.1,
`miners_delight` on 1.20.1, package `com.sammy.minersdelight` on both) adds a copper cup that is a small
bucket: it scoops a source block, draws from a full cauldron, and the water cup it becomes pours into a
cauldron or places water. Nothing it did carried our grade, so a cup scooped from the sea poured into a
cauldron as Clean water. The plan and its decisions are in
[docs/dev/integration/cooking/MINERS-DELIGHT-INTEGRATION.md](../../../docs/dev/integration/cooking/MINERS-DELIGHT-INTEGRATION.md).

**Built on `1.21.1-neoforge` and `1.20.1-forge`**, the mod's two builds for a version this mod supports
(1.4.5 and 1.20.1-1.4.5-backport). Most of it is not here:

| What | Where |
|---|---|
| The water cup is a water container, graded like the water bucket, with no thirst value | `WaterPurity.resolve`, by registry id under both namespaces |
| An empty copper cup draws a graded water cup from a full cauldron | `WaterPurity.drawsFromCauldron`, asked by `WaterInteractions.transferCauldronPurity` |
| A water cup poured into a cauldron stores its grade | unchanged core: it is a water container |
| A copper cup scooping a source is sampled there | **this directory**, `CopperCupItemMixin` |
| A Spout filling a copper cup by Miner's Delight's `create:filling` recipe stamps the tank's grade | `FillingBySpoutMixin` in `src/main/create` and `src/main/createforge` |
| An Item Drain emptying a water cup by its `create:emptying` recipe keeps its grade | unchanged `GenericItemEmptyingMixin`: it asks `isWaterContainer` |
| A water cup on a full cauldron, which Miner's Delight does not pour, leaves the cauldron's grade alone | `WaterInteractions.transferCauldronPurity`, which asks for a cauldron interaction first |
| The milk cup and the soups | ids in `ThirstConfig`, `minersDelightDrinks` and `minersDelightFoods` |

```
minersdelight/java/com/thirstwastaken2/minersdelight/
  MinersDelightPresence          the gate: the copper cup's class file, read for a method, never loaded
  MinersDelightMixinPlugin       applies the mixin only where CopperCupItem still declares use (m_7203_)
  mixin/CopperCupItemMixin       use: capture the ray's water, stamp the filled cup
minersdelight/resources/
  thirstwastaken2.minersdelight.mixins.json
```

## How it works

- **The scoop.** `CopperCupItem.use` casts `getPlayerPOVHitResult`, calls `pickupBlock` and trades the
  plain bucket it gets for a water cup, handed back through `ItemUtils.createFilledResult`. That is
  core's `BucketItemMixin` shape, and the mixin reuses its `FillCapture`: clear at the head, capture on
  the ray, stamp the filled stack. `pickupBlock` took no entity on 1.20.1 and the Player on 1.21.1, so
  the mixin does not slice from it; it stamps any `createFilledResult` whose stack is a water container,
  which the emptying branch's empty cup is not. One source for both nodes, no fork.
- **Compiled against the mod**, so `@Mixin` names `CopperCupItem` and the Forge refmap maps `use`,
  `getPlayerPOVHitResult` and `createFilledResult` to `m_7203_`, `m_41435_` and `m_41813_`. The mod is
  `compileOnly` on NeoForge and `modCompileOnly` on Forge, not transitive.
- **Cauldrons** need nothing here. Miner's Delight puts the copper cup in vanilla's `WATER` interaction
  map and the water cup in `EMPTY`, the bucket's shape, so core's deferred transfer sees both once
  `WaterPurity` knows the two ids.

- **On a full cauldron** the water cup has no interaction (only `EMPTY` holds it), so Miner's Delight
  places its water in the world beside the cauldron and hands back an empty cup. Core used to store the
  cup's grade in the cauldron anyway, since the hand changed; it now asks for the interaction first.
- **Create, 1.20.1 only.** The 1.21.1 build ships its two Create recipes under `data/minersdelight/
  recipes/`, the pre-1.21 folder name, so 1.21.1 never loads them: a Spout or Drain there does nothing
  to a cup. That is the mod's bug. `FillingBySpoutMixin` is built on both Create nodes all the same; it
  reaches any filling recipe whose result is a water container.

## How it stays optional

1. **Build.** Only where `deps.miners_delight` is set do `build.neoforge.gradle.kts` and
   `build.forge.gradle.kts` add this directory and append the mixin config to the built manifest, with
   both mod ids as optional dependencies, from
   [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   `-PwithoutOptional=minersdelight` (or `miners-delight`) leaves the mod out of `runClient`.
2. **Mixin plugin.** `shouldApplyMixin` asks `MinersDelightPresence`, which reads the target's class file
   as a resource and never loads it. A class or method gone upstream is logged once and the mixin
   skipped. The core ids match nothing where the mod is absent.

## Checking it

- `checkOptionalSeam` finds the plugin and the gate loaded without the mod, and passes.
- `runGametest` passes unchanged: the mod is never on its classpath.
  `minersDelightValuesAreMergedIntoAnOlderConfig` checks the config values under both namespaces.
- What it does is checked in a real client with
  [tools/agent/integrations/miners-delight.jsonl](../../../tools/agent/integrations/miners-delight.jsonl)
  and its `-1.20.1` copy, whose headers say how to run and verify them, and Create's Spout and Item
  Drain with [miners-delight-create-1.20.1.jsonl](../../../tools/agent/integrations/miners-delight-create-1.20.1.jsonl),
  which needs Create's `runClientMod` line in `build.forge.gradle.kts` uncommented.
