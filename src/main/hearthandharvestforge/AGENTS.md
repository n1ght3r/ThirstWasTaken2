# src/main/hearthandharvestforge — water grades in Hearth and Harvest on Forge 1.20.1

Hearth and Harvest's 1.20.1 build is Forge only. From **1.4.1** it is the 1.21.1 mod ported back: the
same Sink, Jugs, Trough, Sprinkler, Keg and Stomping Basin, through the same `HHFluidHandling`,
with Forge's fluid API in place of NeoForge's. So this directory carries the
[`hearthandharvest`](../hearthandharvest/AGENTS.md) mixins over, one for one, and that file says what each
one fixes and why. Before 1.4.1 it was the early mod, **1.0.12c**, which only needed the Jug and the salt
recipe; those two mixins stay for it. The plan is
[docs/dev/integration/cooking/HEARTH-AND-HARVEST-INTEGRATION.md](../../../docs/dev/integration/cooking/HEARTH-AND-HARVEST-INTEGRATION.md).

**Built on `1.20.1-forge` only.** What it does, as on 1.21.1:

- **a tank keeps the grade** poured into it, and refuses a second grade;
- **the Sink gives Murky water** and refuses sea water; **a Jug scooping a source samples it**;
- **a wet sponge stomped gives Murky water**; **rain in a Trough or Sprinkler is rainwater**, a source
  beside a Sprinkler is sampled;
- **the Keg** keeps the grade through its container slot, and ferments nothing but brine from sea water;
- **only sea water boils down to salt** in Farmer's Delight's Cooking Pot.

```
hearthandharvestforge/java/com/thirstwastaken2/hearthandharvestforge/
  HearthAndHarvestForgeMixinPlugin   applies each mixin only while its target declares the method it needs
  HearthWater                        STANDING (Murky), stamped(), settle(): the Forge copy
  StampingTank                       a tank seen through a pour, water stamped first: the Forge copy
  GradedItemFluidHandler             1.0.12c's Jug: a container's fluid handler with the grade carried across
  mixin/FluidHandlingMixin           1.4.1: HHFluidHandling.testEmptying/testFilling, pour and draw with the grade
  mixin/SinkBlockMixin               1.4.1: BasinBlock.hhUseItemOn and tick
  mixin/SprinklerBlockMixin          1.4.1: SprinklerBlock.hhUseItemOn, FluidUtil replaced by useOnTank
  mixin/JugBlockItemMixin            1.4.1: JugBlockItem.use, the scooped water sampled
  mixin/TroughBlockEntityMixin       1.4.1: TroughBlockEntity.serverTick, rain stamped
  mixin/SprinklerBlockEntityMixin    1.4.1: SprinklerBlockEntity.refill, rain stamped, a source sampled
  mixin/KegBlockEntityMixin          1.4.1: KegBlockEntity.transfer, pour and draw with the grade; findRecipe, sea water ferments only brine
  mixin/StompingBasinRecipeMixin     1.4.1: StompingBasinRecipe.getResultFluid, Murky
  mixin/JugBlockMixin                1.0.12c: JugBlock's lambda$use$0, the held container's handler wrapped
  mixin/SaltFromSeaWaterMixin        both: CookingPotRecipe.matches, the salt recipe takes sea water only
hearthandharvestforge/resources/
  thirstwastaken2.hearthandharvestforge.mixins.json
```

## Where it differs from 1.21.1

- **`hhUseItemOn`.** On 1.21.1 the Sink and the Sprinkler are hooked in vanilla's `useItemOn`. 1.20.1 has
  no such method, so the mod's own `use` calls its own `hhUseItemOn` first and falls back to vanilla when
  that answers `null`. The mixins hook `hhUseItemOn`, and `useOnTank` answers `null`, not a pass, when
  nothing happened.
- **The placed Jug** goes through `HHFluidHandling.useOnTank` on 1.4.1, so `FluidHandlingMixin` covers it;
  `JugBlockMixin` is 1.0.12c's alone and is skipped without a warning on 1.4.1.
- **Names under SRG.** The Forge jar is in SRG names. Hearth and Harvest's own methods, lambdas and
  Forge's are not renamed, so those mixins are `remap = false`. Two targets are Minecraft's (the Sink's
  `tick`, the jug item's `use`) and go through the refmap; the plugin checks for each class one method of
  the mod's own instead, since a Minecraft method is an SRG name in the jar.
- **The salt recipe.** On 1.21.1 a data file replaces it; on Forge 47 the mod's own file wins over a
  replacement, ordering or not, so it stays a mixin. 1.0.12c's `minecraft:salt_from_bottle` took any
  potion: a fresh water bottle now fails it. 1.4.1's `hearthandharvest:cooking/salt_from_bottle` takes a
  water bottle by exact NBT (`forge:nbt`), which a plain bottle matched and our stamped sea water did not:
  a fresh one fails it now, and a sea water bottle alone among the ingredients matches. `matches` is
  Farmer's Delight's own method, so `remap = false`.
- **Compiled against the mod.** `build.forge.gradle.kts` has Hearth and Harvest and Farmer's Delight (the
  Keg's supertype) as `modCompileOnly`, remapped to Mojang names for the compile.

## How it stays optional

1. **Build.** Only where `deps.hearth_and_harvest` is set does `build.forge.gradle.kts` add this
   directory and name the mixin config in the jar manifest, from
   [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
2. **Mixin plugin.** A resource lookup for a class of Hearth and Harvest, and a read of the target's class
   file for the method, neither of which loads a class. A 1.4.1 mixin on 1.0.12c finds no class and is
   skipped, and the other way round.
3. **Nothing else names the mod.** `HearthWater`, `StampingTank` and `GradedItemFluidHandler` name only
   Forge and core types and are loaded only from the mixins.

## Checking it

- `checkOptionalSeam`, `checkLoaderSeam`, `checkVersionSeam` and `checkDataConditions` pass on
  `1.20.1-forge`, and `tools/agent/smoke/boot.jsonl` with `-PwithoutOptional=hearth-and-harvest,cold-sweat`
  comes up.
- [tools/agent/integrations/hearth-and-harvest-1.20.1.jsonl](../../../tools/agent/integrations/hearth-and-harvest-1.20.1.jsonl),
  against 1.4.1: every id, the Sink, Trough, Sprinkler, placed Jug, the jug item on the sea, the Stomping
  Basin, the Keg, the Cooking Pot, and a juice and a glass of wine.
