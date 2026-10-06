# Data Packs

Any item can be given a thirst value by a data pack, with no code. A mod can ship the same file in its
own resources, so its drinks work with ThirstWasTaken2 without depending on it.

## The file

Put a JSON file anywhere under `data/<namespace>/thirstwastaken2/drinks/`. The file name is free.

```json
{
  "values": {
    "mymod:lemonade": { "thirst": 6, "quenched": 8 },
    "mymod:iced_tea": { "thirst": 8, "quenched": 12 },
    "mymod:ice_cube": { "thirst": 1 },
    "mymod:salted_crackers": { "thirst": 0 }
  }
}
```

| Key | Meaning |
|---|---|
| `thirst` | Points of thirst restored, out of 20. One droplet on the bar is two points. Required. |
| `quenched` | Points of [quenched](/docs/features/thirst-and-quenched), the hidden buffer spent before thirst drops. Optional, 0 by default. |

- Both are kept between 0 and 20. Quenched never ends up higher than thirst.
- For scale, see the [values the mod ships](/docs/features/drinking#what-is-worth-drinking).
- Drinks and foods use the same folder. The value is restored when the item is finished, whether it is
  drunk or eaten.

## Which value wins

When several sources name an item, the first one in this list is used:

1. `itemBlacklist` in the config. A listed item restores nothing.
2. [`drinks` and `foods`](/docs/configuration#drinks-and-foods) in the config.
3. Data pack files.
4. The `c:drinks` item tag, which gives every tagged item the same value.
5. [Keyword matching](/docs/configuration#enablekeywordmatching), when it is turned on.

A server owner can always override what a mod or data pack sets.

::: tip
The config is read separately by each player's game. An item listed in a player's own config shows
that value in their tooltips, even though the server decides what drinking restores.
:::

## Removing a value

An entry of `"thirst": 0` (with no quenched) makes the item restore nothing, even if it is tagged
`c:drinks` or matches a keyword. Only the config beats it.

## Mistakes and missing mods

- An item that does not exist is skipped with a warning in the server log. The rest of the file still
  loads, so one file can name items from mods that may not be installed.
- A file that is not valid JSON, or has no `values`, is skipped whole with a warning.
- A pack higher in the pack list replaces a file with the same path, the same way it replaces a
  recipe.
- If two different files name the same item, the file whose name sorts last wins, and the log says so.

## Reloading

Files are read when the server starts and on `/reload`. Every connected player is sent the new values
straight away, so tooltips and drinking stay in step. The log confirms each load:

```
Loaded thirst values for 12 items from data packs
```

## The `c:drinks` tag

For a flat value instead of an exact one, tag the item `c:drinks`. It then restores
[`drinkTagValue`](/docs/configuration#drinktagvalue), 6 thirst and 4 quenched by default. Items also
tagged `c:drinks/magic` or `c:drinks/ominous`, such as potions, are left out.
