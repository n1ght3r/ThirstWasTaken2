# Java API

For mods that need more than a [data pack](/docs/developers/data-packs): reading and changing a
player's thirst, reacting to drinking, and working with water purity. It is the same on Fabric and
NeoForge and on every supported Minecraft version.

Everything public lives in the package `com.thirstwastaken2.api`, in two classes:

- `ThirstApi`: item values, player thirst and water purity.
- `ThirstEvents`: callbacks for drinking and exhaustion.

Nothing outside that package is API. It can change in any release.

::: warning Version
The methods on this page need 1.1.0 or later. Older releases only have
`ThirstApi.thirstValues` and `ThirstApi.restoresThirst`.
:::

## Adding the dependency

ThirstWasTaken2 is published through [Modrinth Maven](https://support.modrinth.com/en/articles/8801191-modrinth-maven).
Depend on it at compile time only, so players are not forced to install it.

```kotlin
repositories {
    exclusiveContent {
        forRepository { maven("https://api.modrinth.com/maven") { name = "Modrinth" } }
        filter { includeGroup("maven.modrinth") }
    }
}

dependencies {
    // Fabric (Loom)
    modCompileOnly("maven.modrinth:thirst-was-taken-2:<version>+<minecraft>")
    // NeoForge (ModDevGradle)
    compileOnly("maven.modrinth:thirst-was-taken-2:<version>+<minecraft>-neoforge")
}
```

The version is the Modrinth version number, for example `1.1.0+26.3` on Fabric and
`1.1.0+26.3-neoforge` on NeoForge. Each loader and Minecraft version has its own upload, listed on
the [versions page](https://modrinth.com/mod/thirst-was-taken-2/versions).

To try it in a development client, add the same coordinate to the run classpath: `modLocalRuntime` on
Loom, `runtimeOnly` on ModDevGradle.

Modrinth Maven serves the mod jar only, without sources. The documentation of every method is in the
[source on GitHub](https://github.com/whoisnighter/ThirstWasTaken2/tree/main/src/main/java/com/thirstwastaken2/api).

## Keeping it optional

Check that ThirstWasTaken2 is loaded before touching the API, and keep every call in a class of its
own that is only loaded after the check. Java checks a whole class before running any of it, so an
`if` around the call inside a class that names `ThirstApi` still crashes a game without the mod.

```java
// Anywhere in your mod. This class never names ThirstApi.
if (FabricLoader.getInstance().isModLoaded("thirstwastaken2")) { // NeoForge: ModList.get().isLoaded(...)
    ThirstCompat.init();
}
```

```java
// ThirstCompat.java, only loaded after the check above.
final class ThirstCompat {
    static void init() {
        ThirstEvents.DRINK.register((player, stack, drink) -> {
            // Anything drunk in the rain restores one extra point.
            if (player.level().isRainingAt(player.blockPosition())) {
                drink.setThirst(drink.thirst() + 1);
            }
        });
    }
}
```

## Player thirst

| Method | Returns or does |
|---|---|
| `maxThirst()` | The most thirst and quenched a player can have, 20. Use it instead of the number. |
| `thirst(player)` | Current thirst, 0 to 20. |
| `quenched(player)` | Current [quenched](/docs/features/thirst-and-quenched), 0 up to thirst. |
| `isEnabled(player)` | Whether thirst applies. False in creative and spectator, and when [`/thirst enable`](/docs/commands#thirst-enable) turned it off. |
| `drink(player, thirst, quenched)` | Restores thirst the way a drink does. Negative amounts count as 0. |
| `addExhaustion(player, amount)` | Charges exhaustion now, scaled by climate and armour like any other. 4 exhaustion costs one point. |

`drink` and `addExhaustion` work on the server. On the client they do nothing. Reading works on both
sides, but a client only knows its own player's thirst.

```java
// A HUD element of your own.
static float thirstFraction(Player player) {
    if (!ThirstApi.isEnabled(player)) return 1.0F;
    return ThirstApi.thirst(player) / (float) ThirstApi.maxThirst();
}
```

## Item values

| Method | Returns |
|---|---|
| `thirstValues(stack)`, `thirstValues(item)` | `{thirst, quenched}` the item restores, or `null`. Read the array, never change it. |
| `restoresThirst(stack)` | Whether the item restores anything. |

Values are resolved in the [order the data pack page describes](/docs/developers/data-packs#which-value-wins).
To set a value, use a data pack file rather than code.

## Water purity

Water has four [grades](/docs/features/water-purity), from 0 (Dirty) to 3 (Pure). Sea water is not a
grade: it never hydrates and cannot be purified.

| Method | Returns |
|---|---|
| `isWaterContainer(stack)` | Whether the item holds water: a bottle, bucket, filled waterskin or water bowl. Sea water counts. |
| `purity(stack)` | The grade, 0 to 3, or `NOT_WATER` (-1) for anything that is not fresh water. |
| `isSalt(stack)` | Whether the item holds sea water. |
| `waterBottle(purity)` | A water bottle of that grade, kept between 0 and 3. |
| `minPurity()`, `maxPurity()` | 0 and 3. |

```java
// A canteen that hands out purified water.
ItemStack water = ThirstApi.waterBottle(ThirstApi.maxPurity());
```

## Events

Register listeners once, while your mod starts. Both events fire on the server only. A listener that
throws an exception is logged once and skipped, and the other listeners still run.

### DRINK

Fires when an item is drunk or eaten and is about to restore thirst. It covers every way the mod
drinks an item, including from a Sophisticated Backpacks upgrade or a Supplementaries jar. Drinking
water by hand from a lake fires it with an empty stack. Sea water restores nothing, so it does not fire.

The listener gets the player, the stack as it was before being used up, and the amounts, which it can
change:

| Method on the amounts | Does |
|---|---|
| `thirst()`, `quenched()` | What the drink will restore. |
| `setThirst(value)`, `setQuenched(value)` | Changes it, kept between 0 and 20. |
| `cancel()` | Restores nothing. The item's other effects, such as a potion's, still apply. |

Each listener sees what the ones before it left.

```java
ThirstEvents.DRINK.register((player, stack, drink) -> {
    if (stack.is(MyItems.SALTY_SNACK)) drink.cancel();
});
```

`ThirstApi.drink` does not fire this event.

### EXHAUSTION

Fires once per player per tick, on a tick where the player built up exhaustion from running, jumping,
fighting or an effect. The listener gets the raw amount, before climate and armour are applied, and
returns the amount to charge instead. The amount can be negative for a moment while the Hunger effect
is active.

```java
// Half the drain underwater, for a player wearing a rebreather.
ThirstEvents.EXHAUSTION.register((player, amount) ->
        player.isUnderWater() && MyItems.hasRebreather(player) ? amount * 0.5F : amount);
```

## Compatibility promise

- A public method in `com.thirstwastaken2.api` is only removed or changed after it has been marked
  deprecated for at least one minor release.
- `ThirstApi.API_VERSION` goes up with every addition. It is read at run time, so it tells the
  version installed, not the one compiled against. It is 2 for everything on this page.
- The API only uses Minecraft and Java types plus its own, and looks the same on every supported
  Minecraft version and loader.
- The [data pack format](/docs/developers/data-packs) keeps its keys. A new key will be optional.

Missing something, such as an event for when thirst changes? Ask in an
[issue](https://github.com/whoisnighter/ThirstWasTaken2/issues).
