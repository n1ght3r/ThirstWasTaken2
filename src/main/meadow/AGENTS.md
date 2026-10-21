# src/main/meadow — water in Let's Do: Meadow

[Let's Do: Meadow](https://modrinth.com/mod/lets-do-meadow) (mod id `meadow`, package
`net.satisfy.meadow`) has a wooden bucket, a wooden cauldron and a Cooking Pot (`cookpot`) that holds
water. None of them carried our grade. The plan and its decisions are
[docs/dev/integration/lets-do/MEADOW-INTEGRATION.md](../../../docs/dev/integration/lets-do/MEADOW-INTEGRATION.md).

**Built on `1.21.1` and `1.21.1-neoforge`**, the mod's only builds for a version this mod supports
(1.4.10). Its 1.20.1 line is no longer updated and is not built against. What it does:

| What | Where |
|---|---|
| The wooden water bucket is a water container graded like the water bucket, with no thirst value; the wooden bucket draws one from a full vanilla cauldron | `WaterPurity.resolve`, by registry id |
| A wooden water bucket poured into a vanilla cauldron, or drawn from one, keeps its grade | unchanged core: Meadow puts both buckets in vanilla's interaction maps |
| The hanging pot and the distiller take and fill wooden buckets | unchanged core: they ask `WaterPurity.vessel` |
| A wooden bucket scooping a source is sampled there | **this directory**, `WoodenBucketMixin` |
| The wooden cauldron is standing water: sea water is refused, and what is drawn from it is Murky | **this directory**, `AbstractCauldronBlockMixin` and `ItemUtilsMixin` |
| The Cooking Pot refuses sea water | **this directory**, `CookingCauldronBlockEntityMixin` |
| The seven wooden milk buckets | ids in `config/itemvalues/LetsDoItemValues`, `meadowDrinks` |

```
meadow/java/com/thirstwastaken2/meadow/
  MeadowMixinPlugin                     applies each mixin only where Meadow is installed, by resource lookup
  WoodenCauldron                        Murky for standing water; tells the wooden cauldrons apart by their interaction maps
  mixin/WoodenBucketMixin               WoodenBucket.use: capture the ray's water, stamp the one new ItemStack, the filled bucket
  mixin/AbstractCauldronBlockMixin      useItemOn: refuse sea water on a wooden cauldron; mark a use of the wooden water one
  mixin/ItemUtilsMixin                  createFilledResult: stamp Murky while the wooden water cauldron is used
  mixin/CookingCauldronBlockEntityMixin interact: refuse a salty stack
meadow/resources/
  thirstwastaken2.meadow.mixins.json
```

## How it works

- **The scoop.** `WoodenBucket.use` is not `BucketItem.use`, but the same shape: it casts
  `getPlayerPOVHitResult`, calls `pickupBlock`, and builds its filled bucket with `new ItemStack`, the one
  `new ItemStack` in the method (the emptying branch builds its empty bucket in `getEmptySuccessItem`).
  The mixin reuses core's `FillCapture`: clear at the head, capture on the ray, stamp the new stack.
- **The wooden cauldron.** The wooden water cauldron is a plain `LayeredCauldronBlock` built in Meadow's
  registry, so on 1.21.1 it never gets the `purity` property, and core's `WaterInteractions` only knows
  vanilla's two cauldrons. Like Farm & Charm's Water Trough, it is therefore standing water: nothing
  remembered, everything drawn out `WoodenCauldron.STANDING` (Murky, the trough's grade). Meadow's three
  wooden cauldrons are told apart from every other cauldron by their interaction maps,
  `WoodenCauldronBehavior.EMPTY`, `WATER` and `POWDER_SNOW`, read through the shadowed `interactions`
  field. That needs no registry lookup and no string.
- **Drawing.** Meadow's draws run in lambdas of `WoodenCauldronBehavior` and hand their result through
  `ItemUtils.createFilledResult`. `AbstractCauldronBlockMixin` sets a thread-local flag on the server
  for the length of `useItemOn` on the wooden water cauldron; `ItemUtilsMixin` stamps the filled stack
  only while it is set, and only if it is a water container, so a pour's empty bucket is left alone.
- **Sea water.** `WaterPurity.isSalty` is true only on a water container holding sea water, so the two
  refusals touch nothing else the cauldron or the pot is used with. The click is spent
  (`ItemInteractionResult.CONSUME`) and the water stays in the hand, as Bountiful Fares' vessel.
- **Meadow's own bug.** Its water bottle interaction on the wooden cauldron checks `!= null ||` where it
  means `== null ||`, so a water bottle never pours in. Nothing here depends on it.

## Names on Fabric

The Fabric jar is in intermediary, so Meadow is a `modCompileOnly`. `useItemOn`, `use`,
`getPlayerPOVHitResult`, `createFilledResult` and the `interactions` field are Minecraft's and remapped
by Loom. `interact` is Meadow's own, so its mixin says `remap = false`. Foundation, nested in Meadow's
jar, is not compiled against: nothing here names it.

## How it stays optional

1. **Build.** Only where `deps.meadow` is set do the loader scripts add this directory and append the
   mixin config to the built manifest (on NeoForge with `meadow` as an optional dependency), from
   [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   Meadow and Architectury API are on `runClient` only, Meadow commented out by default for its world
   generation. `-PwithoutOptional=meadow` leaves it out.
2. **Mixin plugin.** `MeadowMixinPlugin` answers with a resource lookup, never loading a class. The two
   mixins on Minecraft's classes, `AbstractCauldronBlock` and `ItemUtils`, ask for Meadow's
   `WoodenCauldronBehavior` instead of their own target, so they are not applied without Meadow. The
   core ids match nothing where the mod is absent.

## Checking it

- `checkOptionalSeam` finds the plugin loaded without the mod, and passes.
- `runGametest` passes unchanged: the mod is never on its classpath.
  `meadowDrinksAreMergedIntoAnOlderConfig` checks the config values.
- What it does is checked in a real client with
  [tools/agent/integrations/meadow.jsonl](../../../tools/agent/integrations/meadow.jsonl), whose header
  says how to run and verify it.
