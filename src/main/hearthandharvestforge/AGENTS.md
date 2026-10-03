# src/main/hearthandharvestforge — Hearth and Harvest's salt recipe and Jug on Forge 1.20.1

Hearth and Harvest's 1.20.1 build (1.0.12c, Forge only) is the early mod: a Cask whose mead takes a
honey bottle, a Jug filled through item capabilities, a wine rack, and fewer items. Two things need code.
`minecraft:salt_from_bottle` boils **any potion** into two salt in Farmer's Delight's Cooking Pot, so a
fresh water bottle matched it as well as our purifying recipe, and in game a Pure bottle came out as salt.
And the placed Jug moved water through Forge's bucket wrapper, which pours and draws plain water, so a
Dirty bucket poured in came back out Clean. The NeoForge side is [`hearthandharvest`](../hearthandharvest/AGENTS.md);
the plan is
[docs/dev/integration/cooking/HEARTH-AND-HARVEST-INTEGRATION.md](../../../docs/dev/integration/cooking/HEARTH-AND-HARVEST-INTEGRATION.md).

**Built on `1.20.1-forge` only.** What it does:

- **only sea water boils down to salt**. A water container in the pot that is not salty fails that
  recipe, so a fresh bottle goes to our purifying recipe alone and a Pure one stays where it is. A potion
  that is not water still makes salt, as Hearth and Harvest meant;
- **a Jug keeps the grade** of a bucket poured in, Clean for an unstamped one, and refuses a bucket of
  another grade while it holds water, as the 1.21.1 tanks do. Our own waterskin, canteen, flask and bowls
  carry their grade on their handler already and are left alone.

```
hearthandharvestforge/java/com/thirstwastaken2/hearthandharvestforge/
  HearthAndHarvestForgeMixinPlugin   applies each mixin only with Hearth and Harvest present and its target method still there
  GradedItemFluidHandler             a container's fluid handler with the grade carried across: drained water stamped, filled container stamped
  mixin/SaltFromSeaWaterMixin        CookingPotRecipe.matches(RecipeWrapper, Level): the salt recipe takes sea water only
  mixin/JugBlockMixin                JugBlock's lambda$use$0: the held container's handler wrapped in GradedItemFluidHandler
hearthandharvestforge/resources/
  thirstwastaken2.hearthandharvestforge.mixins.json
```

## Why a mixin and not the data file 1.21.1 uses

On 1.21.1 a replacement file at the same id, with this mod ordered after Hearth and Harvest, wins. On
Forge 47 it did not: the agent check found Hearth and Harvest's own file loaded whatever the ordering,
so the replacement was dropped. The mixin works whichever file loads.

`matches(RecipeWrapper, Level)` is Farmer's Delight's own method, which the vanilla bridge `m_5818_`
calls, so it keeps its name in a production jar and needs no refmap entry. The recipe id is read through
vanilla's `getId`, which the reobfuscation of this mod's own classes renames. Named by string, since
nothing compiles against Farmer's Delight.

## The Jug

`JugBlock.use` looks up the held stack's `FLUID_HANDLER_ITEM` capability and does the work in a lambda,
`lambda$use$0(JugBlockEntity, Level, BlockPos, Player, InteractionHand, IFluidHandlerItem)`: drain the
container into the jug, or fill it from the jug, then hand the player `getContainer()`. The mixin swaps
the handler argument at the lambda's head for `GradedItemFluidHandler`, in the shape of Sophisticated's
`WaterQualityFluidHandler` on NeoForge: water drained from it carries the container's grade, so the jug's
`FluidTank`, which compares fluid stacks with their NBT, keeps it and refuses another grade; water filled
into it goes down plain, and the container it hands back is stamped. A lambda's name is fixed when the mod
is compiled and never remapped, so it is the same in a production jar; the plugin checks it still exists.
A water bottle has no fluid capability on Forge, so bottles do nothing with this jug, as before.

## How it stays optional

1. **Build.** Only where `deps.hearth_and_harvest` is set does `build.forge.gradle.kts` add this
   directory and name the mixin config in the jar manifest, from
   [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
2. **Mixin plugin.** A resource lookup for a class of Hearth and Harvest, and a read of the target's class
   file for the method, neither of which loads a class.

## Checking it

- `checkOptionalSeam`, `checkLoaderSeam` and `checkDataConditions` pass on `1.20.1-forge`, and
  `tools/agent/smoke/boot.jsonl` with `-PwithoutOptional=hearth-and-harvest,cold-sweat` comes up.
- [tools/agent/integrations/hearth-and-harvest-1.20.1.jsonl](../../../tools/agent/integrations/hearth-and-harvest-1.20.1.jsonl)
  passed on 2026-10-03: every id exists, a Murky bottle boils Pure, a sea water bottle into two salt, a
  Pure bottle stays, a Jug takes a Dirty bucket and gives it back Dirty, refuses a Clean one meanwhile and
  gives sea water back salty, and a juice restores 8.
