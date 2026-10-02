# src/main/herbalbrews — the Tea Kettle and the Jug in Let's Do: HerbalBrews

[Let's Do: HerbalBrews](https://modrinth.com/mod/lets-do-herbalbrews) (mod id `herbalbrews`, package
`net.satisfy.herbalbrews`) brews teas and coffee in a Tea Kettle and serves three cups at once from a
placed Jug. The plan and its decisions are
[docs/dev/integration/lets-do/HERBALBREWS-INTEGRATION.md](../../../docs/dev/integration/lets-do/HERBALBREWS-INTEGRATION.md).

**Built on `1.21.1` and `1.21.1-neoforge`**, the mod's only builds for a version this mod supports
(1.1.4). Its 1.20.1 line is no longer updated and is not built against. What it does:

- **Tea Kettle**: a salty bucket or bottle in the water slot fills nothing and stays there, so sea water
  brews no tea. Fresh water of any grade fills it as before: the kettle boils.
- **Jug**: drinking a placed jug drinks each tea or coffee in it as the item it was, so three cups from
  a jug restore what three cups from the hand do.

The drink values are not here. They are ids in `ThirstConfig` (`herbalBrewsDrinks`), common code that
names no class of the mod.

```
herbalbrews/java/com/thirstwastaken2/herbalbrews/
  HerbalBrewsPresence      the gate: each class looked up as a resource, the mod's own methods read off the class file
  HerbalBrewsMixinPlugin   applies each mixin only when the methods it injects into or calls are there
  mixin/TeaKettleMixin     TeaKettleBlockEntity.tick: ItemStack.is(TagKey) false for a salty stack
  mixin/JugMixin           JugBlock.useItemOn: ThirstManager.drinkItem for each cup before clearDrinks
herbalbrews/resources/
  thirstwastaken2.herbalbrews.mixins.json
```

## How it works

- **The kettle's level keeps no grade.** `tick` moves the water slot into an int once the stack is in
  one of two item tags, matched by item alone. `TeaKettleMixin` wraps every `ItemStack.is(TagKey)` in
  `tick`; the heat item check is one of them, and no heat item is salty. A refused stack is left in the
  slot, like a salty bucket in Farm & Charm's Cooking Pot.
- **The jug's cups are drunk as items.** `JugMixin` injects before `JugBlockEntity.clearDrinks` and hands
  each stack from `getDrinks()` to `ThirstManager.drinkItem`, the call a drink finished by hand goes
  through, so `ThirstEvents.DRINK` fires for each and the config decides the values.
- **Bottles and canteens are not accepted** by the kettle: that is the tag HerbalBrews chose.

## Names on Fabric

The Fabric jar is in intermediary, so HerbalBrews is a `modCompileOnly`. `useItemOn` and
`ItemStack.is` are Minecraft's and remapped; `tick` and `clearDrinks` are the mod's own and are not
(`remap = false`). The plugin checks only the mod's own names.

## How it stays optional

1. **Build.** Only where `deps.herbalbrews` is set do the loader scripts add this directory and append
   the mixin config to the built manifest (on NeoForge with `herbalbrews` as an optional dependency),
   from [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt).
   HerbalBrews and Architectury API are on `runClient` only. `-PwithoutOptional=herbalbrews` leaves
   HerbalBrews out; Architectury stays while another Let's Do mod needs it.
2. **Runtime gate.** `HerbalBrewsPresence` answers with resource lookups and class file reads, never
   loading a class, and names no class of the mod, no Minecraft class and no loader.

## Checking it

- `checkOptionalSeam` finds the plugin and the gate loaded without the mod, and passes.
- `runGametest` passes unchanged: the mod is never on its classpath.
  `herbalBrewsAndBeachpartyDrinksAreMergedIntoAnOlderConfig` checks the config values.
- `./gradlew ":<node>:runClient" -Pagent=tools/agent/smoke/boot.jsonl -PwithoutOptional=herbalbrews`
  comes up and stays up.
- What it does is checked in a real client with
  [tools/agent/integrations/herbalbrews.jsonl](../../../tools/agent/integrations/herbalbrews.jsonl).
