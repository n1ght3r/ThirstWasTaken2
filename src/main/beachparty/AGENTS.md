# src/main/beachparty — placed cocktails in Let's Do: Beachparty

[Let's Do: Beachparty](https://modrinth.com/mod/lets-do-beachparty) (mod id `beachparty`, package
`net.satisfy.beachparty`) mixes cocktails at a Palm Bar. Drunk from the hand a cocktail is an item like
any other; placed, one glass is sipped three times. The plan and its decision are
[docs/dev/integration/lets-do/BEACHPARTY-INTEGRATION.md](../../../docs/dev/integration/lets-do/BEACHPARTY-INTEGRATION.md).

**Built on `1.21.1` and `1.21.1-neoforge`**, the mod's only builds for a version this mod supports
(2.1.5). Its 1.20.1 line is no longer updated and is not built against. What it does:

- **Placed cocktail**: each of the three sips restores a third of what the glass restores drunk from
  the hand, rounded, at least one point of thirst.

The cocktail and coconut values are not here. They are ids in `ThirstConfig` (`beachpartyDrinks`,
`beachpartyFoods`), common code that names no class of the mod. Beachparty has no water container to
grade: its Mini Fridge freezes water into ice, which keeps no grade and melts into water sampled where
it lies.

```
beachparty/java/com/thirstwastaken2/beachparty/
  BeachpartyMixinPlugin     applies the mixin only where CocktailBlock is on the classpath, by resource lookup
  mixin/CocktailBlockMixin  CocktailBlock.useWithoutItem: a third of the glass's values before the stage goes down
beachparty/resources/
  thirstwastaken2.beachparty.mixins.json
```

## How it works

- **A sip is found by its `setBlock`.** `useWithoutItem` lowers the stage with the method's one
  `Level.setBlock`, on the server, and breaks the empty glass with `destroyBlock`, so injecting before
  `setBlock` is exactly the three sips.
- **The glass decides.** The values are `ThirstApi.thirstValues` of the block's item, the same config
  entry the hand reads, so a changed or blacklisted cocktail changes its sips too. They go through
  `ThirstApi.drink`, which is a drink's cap without `ThirstEvents.DRINK`: a sip has no item to report.

## Names on Fabric

The Fabric jar is in intermediary, so Beachparty is a `modCompileOnly`. `useWithoutItem` and
`Level.setBlock` are Minecraft's and remapped; nothing of the mod's own is named, so the plugin checks
only that the class is there.

## How it stays optional

1. **Build.** Only where `deps.beachparty` is set do the loader scripts add this directory and append
   the mixin config to the built manifest (on NeoForge with `beachparty` as an optional dependency),
   from [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   Beachparty, Architectury API and Trinkets (Fabric) or Curios (NeoForge) are on `runClient` only.
   Beachparty's manifests ask for neither, but it fails to load without them: its Fabric client
   entrypoint names Trinkets' renderer, and its NeoForge items implement Curios' `ICurioItem`.
   `-PwithoutOptional=beachparty` leaves Beachparty out, and Trinkets or Curios with it.
2. **Runtime gate.** `BeachpartyMixinPlugin` answers with a resource lookup, never loading a class.

## Checking it

- `checkOptionalSeam` finds the plugin loaded without the mod, and passes.
- `runGametest` passes unchanged. `herbalBrewsAndBeachpartyDrinksAreMergedIntoAnOlderConfig` checks the
  config values.
- `./gradlew ":<node>:runClient" -Pagent=tools/agent/smoke/boot.jsonl -PwithoutOptional=beachparty`
  comes up and stays up.
- What it does is checked in a real client with
  [tools/agent/integrations/beachparty.jsonl](../../../tools/agent/integrations/beachparty.jsonl).
