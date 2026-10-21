# src/main/bountifulfares — Bountiful Fares' cup keeps water's grade

[Bountiful Fares](https://modrinth.com/mod/bountiful-fares) (mod id `bountifulfares`, package
`net.hecco.bountifulfares` on both loaders) adds a ceramic cup that is a glass bottle: it scoops a water
source, draws from and pours into a cauldron, and the water cup it becomes is drunk like a water bottle.
Its teas are crafted from a water cup, and its Fermentation Vessel takes any water container to ferment
ciders, wines and mead. Nothing it did carried our grade, so a cup scooped from the sea poured into a
cauldron as Clean water. The plan and its decisions are in
[docs/dev/integration/cooking/BOUNTIFUL-FARES-INTEGRATION.md](../../../docs/dev/integration/cooking/BOUNTIFUL-FARES-INTEGRATION.md).

**Built on `1.21.1` and `1.21.1-neoforge`**, the only line the mod still updates (3.0.13). Its 1.20.x
builds stopped in 2024 and it has none newer. Most of it is not here:

| What | Where |
|---|---|
| The water cup is plain water, one serving, graded like the bottle, swapping with the empty cup (`jar`) | `WaterPurity.resolve`, by registry id |
| An empty cup draws a graded water cup from a full cauldron | `WaterPurity.drawsFromCauldron`, asked by `WaterInteractions.transferCauldronPurity` |
| A water cup poured into a cauldron, a hanging pot or a distiller stores its grade | unchanged core: it is a water container and a `WaterPurity.Vessel` |
| A cup scooping a source is sampled there | **this directory**, `CupItemMixin` |
| The Fermentation Vessel refuses sea water | **this directory**, `FermentationVesselBlockMixin` |
| Sea water crafts no tea, nor the Sponge Cake | **this directory**, `ShapedRecipeMixin`, `ShapelessRecipeMixin` |
| The water cup's value, the teas, ciders, wines, stews, compotes and fruit | ids in `config/itemvalues/OtherModItemValues`, `bountifulFaresDrinks` and `bountifulFaresFoods` |

```
bountifulfares/java/com/thirstwastaken2/bountifulfares/
  BountifulFaresMixinPlugin      the gate: the mod's class files, read for a method, never loaded
  BountifulFaresRecipes          what the two recipe mixins share
  mixin/CupItemMixin             use: capture the ray's water, stamp the water cup turnCupIntoItem gets
  mixin/FermentationVesselBlockMixin   useItemOn: an empty vessel spends the click on sea water
  mixin/ShapedRecipeMixin, ShapelessRecipeMixin   matches: false for the mod's recipes on sea water
bountifulfares/resources/
  thirstwastaken2.bountifulfares.mixins.json
```

## How it works

- **The scoop.** `CupItem.use` casts `getPlayerPOVHitResult(SOURCE_ONLY)` and, on any water, hands a
  new water cup to `turnCupIntoItem`, which goes through `ItemUtils.createFilledResult`. That is core's
  `BottleItemMixin` shape, and the mixin reuses its `FillCapture`: clear at the head, capture on the ray,
  stamp the third argument of `turnCupIntoItem`, the only thing `use` fills.
- **Cauldrons** need nothing here. The mod puts the empty cup in vanilla's `WATER` map and the water
  cup in `EMPTY` and `WATER`, one level each, the bottle's shape, so core's deferred transfer sees both once
  `WaterPurity` knows the two ids.
- **The vessel** holds its water as a blockstate (`fermentation_stage=water`), not a stack, so there is
  no grade to keep. Sea water is refused at the one place it gets in, an `EMPTY` vessel; a fermented drink
  is safe whatever fresh water went in, as in Brewin' and Chewin's keg.
- **Crafting** is matched by result, as Croptopia's is: any shaped or shapeless recipe whose result is in
  the `bountifulfares` namespace refuses a grid with a salty stack. Fresh water of any grade still matches.
- **The dispenser.** The mod's dispenser fills a cup from the water in front of it, unstamped. Core does
  not stamp a bottle a dispenser fills either, so the two stay alike.
- **Compiled against the mod**, `modCompileOnly` on Fabric and `compileOnly` on NeoForge, not transitive.
  The NexusLib it requires is on the `runClient` classpath only, beside it.

## How it stays optional

1. **Build.** Only where `deps.bountiful_fares` is set do `build.gradle.kts` and
   `build.neoforge.gradle.kts` add this directory and name the mixin config in the built manifest, with
   `bountifulfares` as an optional dependency on NeoForge, from
   [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   `-PwithoutOptional=bountifulfares` (or `bountiful-fares`) leaves the mod out of `runClient`, where it
   is off by default: its trees and crops add world generation. Uncomment its two `runClientMod` lines in
   the node's script to work on it.
2. **Mixin plugin.** `onLoad` looks for the mod's common class as a resource; without it, no mixin
   applies, the two vanilla recipe mixins included. A mixin on the mod's own classes also needs its target
   class, and `CupItemMixin` the mod's own `turnCupIntoItem`, read off the class file. Minecraft overrides
   such as `use` are not probed: they have another name in a Fabric player's game, and do not move within
   one Minecraft version. A missing target is logged once and its mixin skipped. The core ids match
   nothing where the mod is absent.

## Checking it

- `checkOptionalSeam` finds the plugin and the helper loaded without the mod, and passes.
- `runGametest` passes unchanged: the mod is never on its classpath.
  `bountifulFaresValuesAreMergedIntoAnOlderConfig` checks the config values.
- What it does is checked in a real client with
  [tools/agent/integrations/bountiful-fares.jsonl](../../../tools/agent/integrations/bountiful-fares.jsonl),
  whose header says how to run and verify it, on both nodes.
