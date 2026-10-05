# Distillation: the Copper Distiller at work

**Status: done** (2026-10-05). Written 2026-10-04; decisions settled 2026-10-05. The block, its four parts and their
recipes are built (`block/DistillerBlock`, commit "feat: add the copper distiller block…"); this plan is
what makes it distil. Done so far (2026-10-05): step 1, the version seams; step 2, the machine, which
distils; steps 3 and 4, its GUI; step 5, building it in the world; step 6, the fire in the model;
step 7, salt; step 8, the cooling tub; step 9, the config; the tests (10); the docs (11); and Jade.
Left: screenshots for the docs site, and the hand checks in a client. It answered the roadmap's "Sea water is a dead end" (the roadmap is done and gone), and fits the
[purification rework](PURIFICATION-REWORK.md): the distiller is a dedicated vessel, so it makes Pure.

## How a real still works, and what the model already shows

1. **Boiler.** Water is heated over a fire until it boils. Only water leaves as steam: salt, sediment,
   metals and most microbes stay behind in the boiler (microbes are killed by the boil as well).
2. **Swan neck.** The steam rises out of the helmet and runs along the pipe to the condenser.
3. **Condenser (worm tub).** The pipe coils down through a tub of cold water. The steam condenses
   *inside the coil*; the cold water around it is only there to carry the heat away. It never touches
   the distillate, and in a real still it warms up and has to be topped up or kept flowing.
4. **Tap.** The distilled water runs out of the end of the coil, through the tap, into the basin.
5. **What is left in the boiler.** With sea water, brine that dries to salt; with fresh water, a scum
   of whatever was in it.

So **the water in the cooling tub is not drinking water**, and the clean water comes only out of the
tap. The model already says this: the tub is drawn with plain coolant water (grade 1's sprite), the
basin under the tap with Pure water.

**Does distilling dirty fresh water make sense?** Yes; it is the most thorough purification there is.
Boiling kills what lives in the water and distilling leaves behind everything that does not boil off:
salt, mud, metals. The one thing it does not remove is a liquid that boils lower than water (some
solvents and fuels), which real distillers handle by throwing the first runnings away. Nothing in the
game's water models that, so a distiller should take any water and give Pure.

## Decisions

| | Decision | Why |
|---|---|---|
| Interface | **A GUI is required.** Right-clicking either half with an empty hand, or anything that is not water or an empty container, opens it | the user's call; a machine with fuel, two tanks and up to four slots needs one |
| Shortcuts | **Right-click with a water container pours it into the boiler; with an empty container draws Pure water from the basin**, as the hanging pot does | the GUI stays for fuel, salt and watching it; the shortcuts keep the pot's feel |
| Output | **Always Pure** (grade 3), whatever went in | a dedicated vessel, per the purification rework; and it is what distilling does |
| Input | **Sea water and every grade of fresh water** | realistic (above). The hanging pot keeps its place: faster (4 s against 8 s) and far cheaper for fresh water; the distiller is the only way to use sea water, and runs on fuel and hoppers |
| Salt | **Only when another mod already has salt**, and then that mod's own salt item | the user's call: the mod adds no salt item of its own |
| Heat | **Furnace fuel in the brick firebox**, not a campfire below | the firebox is part of the machine; burning time comes from vanilla's fuel values |
| Clean water | **Collected from the basin**, by filling an empty container in the output slot or with the shortcut | the tap is where it comes out (above) |
| Cooling tub | **Filled once** with any water, sea water included; without it nothing condenses and the machine stops, saying why in the GUI. The model shows the tub empty until then | explains the tub in one sentence, costs no drinking water, then is forgotten. Choice B below |
| Numbers | **8 s a serving, 9-serving tanks (three buckets), one salt per 3 salty servings** (a bucket of sea water), serving time and tank size in the config | coal (80 s) runs ten servings; a fill lasts minutes; salt stays a by-product, not cheap |
| Building it | **Placed part by part in the world, or crafted whole.** Firebox, boiler and cooling tub are blocks of their own; the copper pipe joins them. See [Building it in the world](#building-it-in-the-world) | the user's call |

## The GUI

Laid out like vanilla's furnace and brewing stand, so it reads at a glance:

```
 [water in]              [boiler tank]  →→→  [basin tank]      [empty in] → [filled out]
 [fuel]   (flame)                      progress                              [salt out]*
```

- **Water in**: any water container the mod knows, graded or salty: water bucket, water bottle, the
  terracotta water bowl, waterskin, copper canteen, iron flask. Its servings pour into the boiler tank
  and the empty container is left in the slot (a bucket becomes a bucket, a bottle a glass bottle).
- **Fuel**: anything a furnace burns. A flame shows the burn left, as the furnace's does.
- **Boiler tank**: a gauge of servings waiting, tinted by what they are (salty or a fresh grade).
- **Progress arrow**: one serving at a time, boiler to basin.
- **Basin tank**: servings of Pure water waiting.
- **Empty in / filled out**: an empty bottle, bucket, terracotta bowl or carried container in, filled
  from the basin, out. The fill keeps each container's own size: a bucket takes three servings.
- **Salt out** (*): only shown when a salt item exists (see [Salt](#salt)).

The halves share one machine: the state lives in a block entity on the **boiler half**, and the tub half
opens the same menu.

## How it runs

- **Tanks.** Boiler tank and basin tank hold 9 servings each (three buckets). The boiler tank keeps one
  quality: salt, or the worst fresh grade poured in (the same mixing rule as the hanging pot and the
  cauldron). The basin is always Pure.
- **A serving takes 8 s** while the fire burns. The copper hanging pot takes 4 s a serving, but only
  for fresh water up to Pure; the distiller handles salt water and runs unattended from fuel, so it may
  be slower. To tune.
- **Fuel.** Burns as in a furnace: coal lasts 80 s, ten servings at 8 s. It only burns while there is
  water to boil and room in the basin, so it is not wasted.
- **Salt.** Every 3 salty servings (one bucket of sea water) make one salt item, when a salt item
  exists. Fresh water leaves nothing.
- **The fire in the model** follows the machine: a `lit` blockstate property on the boiler half picks a
  firebox drawn burning or cold (the generator writes both), and the light level and chimney smoke follow
  it. As built: `lit` follows the fuel, burning while a fire is alight (held as well as burning down),
  so a new or idle distiller is cold. The unpiped boiler and the lone firebox stay cold whatever.
- **Hoppers.** The block entity is a `WorldlyContainer`: from above into **water in**, from the sides into
  **fuel**, and from below out of **filled out** and **salt out**, as a furnace is automated.

### The cooling tub: the three choices weighed (B chosen)

| | Rule | For | Against |
|---|---|---|---|
| A | **Cosmetic.** The tub is always full | simplest; nothing to explain | the tub does nothing |
| B | **Fill once.** The tub must be filled once with any water (sea water included) or no steam condenses | realistic; uses the coolant idea; one bucket, then forgotten | one more thing to get wrong |
| C | **Coolant runs down.** Each serving warms the tub and it has to be topped up | most realistic | chores, and a fourth gauge in the GUI |

**Chosen: B.** It explains why the tub is there in one sentence, takes sea water (so it never costs
drinking water), and asks nothing more once done. A tub is filled by right-clicking it with any water
container, standing alone or in the machine; the coolant is a `cooled` blockstate property, so the tub's
model shows it, and it stays when the machine is assembled or broken into its parts.

## Salt

The mod adds no salt. A salt item is looked up through a tag of the mod's own,
`thirstwastaken2:distiller_salt`, filled with optional entries only:

| Entry | Who fills it, from the jars this repo builds against |
|---|---|
| `#c:dusts/salt` (optional) | Expanded Delight (`expandeddelight:salt`), Spelunkery (`spelunkery:salt`), Cultural Delights (Cook's Collection's salt) |
| `#c:salt`, `#c:salts` (optional) | Croptopia (`croptopia:salt`), Spelunkery |
| `hearthandharvest:salt` (optional) | Hearth and Harvest, which has no salt tag of its own |

- With the tag empty, there is **no salt slot** and the machine makes no salt.
- With more than one salt item, the **first in the tag** is used. A config option,
  `distillerSaltItem` (an item id, empty for automatic), can pin one in a modpack.
- The tag is written by datagen with `required: false` on every entry, so a pack without those mods
  loads it cleanly (`checkDataConditions` stays happy: it names no mod id outside a tag reference).
- 1.20.1's conventional tag names differ: the Fabric mods there use `c:` as now, Forge mods `forge:`.
  The 1.20.1 node's tag adds `#forge:dusts/salt` and `#forge:salt` (none of the jars here fills them,
  but Forge packs do). Hearth and Harvest for 1.20.1 tags its salt nowhere, hence its own entry; its
  1.21.1 build puts it in `c:dusts/salt`.
- As built: `block/DistillerSalt` resolves the salt once and keeps it until the tags reload or the
  config changes. A pin that names no item falls back to the tag. With a salt, every third salty
  serving puts one in the salt slot; a full slot (or one holding another item) holds the salt water,
  fresh water still runs. With none, the salty servings wait counted at three, so a salt added later
  comes out at once. `distillerSaltItem` is in the file only, like the keyword lists: no widget.

## Building it in the world

Three of the four parts are blocks of their own, each placed with a facing like the distiller and drawn
with that part of its model; the copper pipe stays an item.

| Block | Model | Alone it |
|---|---|---|
| Brick Firebox | the firebox, its ledge and chimney | is decoration |
| Distiller Boiler | the boiler, standing on the ground | is decoration |
| Cooling Tub | the tub, tap and basin, empty or `cooled` | takes its coolant |

Assembly:

1. **The boiler onto the firebox.** Placing a Distiller Boiler on the top of a Brick Firebox (clicking its
   top face) merges the two into one block, the firebox with the boiler on it, facing as the firebox
   faces: the distiller's left half without its swan neck. Placing a boiler anywhere else just places it.
2. **The tub beside it.** A Cooling Tub to the right of that block as seen from the front, facing the
   same way, lines up with it. Nothing changes yet: the two stand side by side, unjoined.
3. **The pipe.** Right-clicking either of them with a Copper Pipe, when both are in place, spends the
   pipe and turns the two into a whole Copper Distiller: the swan neck appears and the GUI works. The
   order of steps 1 and 2 does not matter.

The crafting recipe stays: a distiller crafted from the four parts and one built in the world are the
same block, made of the same four things.

Breaking:

| What is broken | Drops |
|---|---|
| A whole distiller, either half | the Copper Distiller, as now (its contents too, once it has a GUI) |
| The firebox with a boiler on it, unjoined | a Brick Firebox and a Distiller Boiler |
| A lone part | itself |

The tub's coolant goes with a broken tub: its item carries no state, so a tub placed again is dry.

As built: a firebox alone is drawn cold (unlit logs, no fire) and gives no light, and so is a boiler set
on its firebox until the pipe joins it; the basin under the tap shows water only on a joined machine.

In code: `DistillerBlock` gains `piped` (false for the firebox-with-boiler before the pipe; a half only
watches its other half while piped). The three parts are blocks with a facing; the boiler's and the
pipe's items are item classes of their own that do the merging on use, the way `HangingPotItem`
changes where a pot goes. The generator writes the extra models: the boiler half without the swan neck,
the lone firebox, the lone boiler shifted to the ground, the lone tub without its pipe, and each tub
empty and cooled.

## What the code needs that the mod does not have yet

The mod has no block entity, menu or screen today, and each of the three differs across the supported
versions. Every difference goes into `platform/` (common) or `client/platform/` (client), as the root
`AGENTS.md` requires, with a row in [VERSION-DIFFERENCES.md](../VERSION-DIFFERENCES.md).

| Need | Where it differs | Seam |
|---|---|---|
| Register a block entity type | `BlockEntityType` built through a builder before 1.21.2, a constructor after; both take a supplier interface vanilla keeps private | `Vanilla.registerBlockEntity` |
| Save and load a block entity | `CompoundTag` (1.20.1); `CompoundTag` plus registries (1.20.5–1.21.4); `ValueInput`/`ValueOutput` (1.21.5+) | an abstract `platform/SavedBlockEntity` with one `save`/`load` pair over a small reader/writer |
| Sync the block entity to the client (the gauges in the model, if any) | `getUpdatePacket`/`getUpdateTag` signatures | in `SavedBlockEntity` |
| Register a menu type | the constructor is private in vanilla on every version; NeoForge and Forge make it public, Fabric API opens it to itself only | `Vanilla.registerMenu`, the same call everywhere once the mod's Fabric access widener opens it (built: no loader seam needed) |
| Register a screen for it | Fabric `MenuScreens.register` (or `HandledScreens`), NeoForge `RegisterMenuScreensEvent`, Forge client setup | `ClientLoader.registerScreen` |
| Draw the screen | `GuiGraphics` → `GuiGraphicsExtractor` (a replacement already), `blit` gained a render type in 1.21.2 | `ClientVanilla.blit`, beside `blitSprite` (it was already there, tinted) |
| Burn time of a fuel | a static map before 1.21.2, `FuelValues` from the level after, the item's cooking fuel component in a container loot context from 26.3; NeoForge 1.21.1 and Forge add fuel of their own | `Loader.burnTime(entity, stack)` over `Vanilla.burnTime`, taking the block entity, which must be a container (built) |
| Hoppers | `WorldlyContainer` is the same everywhere; NeoForge also offers capabilities, not needed | none |

The water side needs nothing new: `WaterPurity`, `ItemWaterData` and `WaterContainers` already read a
container's quality and servings and fill one with a given quality.

## Steps

Step 5, building it in the world, needs no block entity or GUI and can go first: it is blockstates,
models and item use only, on every node, and gives something to see while the seams are built.

1. **Seams first** (done), each with a gametest that only registers and round-trips: `Vanilla.registerBlockEntity`,
   `SavedBlockEntity`, `Vanilla.registerMenu`, `ClientLoader.registerScreen`, `ClientVanilla.blit`,
   `Loader.burnTime`. Build every node; this is where the version work is. A block entity type cannot
   be built once the registries are frozen (26.3 gives it an intrusive holder), so the distiller's is
   registered for real, with `DistillerBlockEntity` holding and saving its state and slots for step 2
   to run; `MachineSeamsGameTest` covers the four server-side seams.
2. **`DistillerBlockEntity`** (done) on the boiler half: tanks, fuel, progress, salt counter, the tick, and the
   `WorldlyContainer` faces. `DistillerBlock` becomes an `EntityBlock`; the tub half forwards use to it.
   As built: either half is a `WorldlyContainerHolder`, so a hopper works on both; the right-click
   shortcuts (`DistillerInteractions.useMachine`) draw into a carried container on a plain click and
   pour it on a sneaking one, as at a hanging pot, and a bucket moves its three servings or nothing,
   both ways. A lit fire holds rather than burns down while there is nothing to do. Salty servings are
   counted, up to three, and wait for step 7 to become salt. The bottom face also gives back what is
   spent in the water and fuel slots, an emptied bucket or bottle, as a furnace's does. Breaking it
   spills the slots (`SupportedBlock` before 1.21.5, vanilla from it); the tanks' water is lost, as a
   cauldron's is.
3. **`DistillerMenu`** (done) with its slots and a `ContainerData` for fuel, progress and both tank levels.
   As built the data also carries the boiler's quality, whether the tub is cooled, and whether there is
   salt to make, which shows the salt slot. A click with anything the shortcuts do not use, an empty hand
   included, opens it from either half; sneaking leaves the click to the item, as at a furnace. Its slots
   refuse on the client what the machine would, through `Loader.isFuel`, so a click is never undone.
4. **`DistillerScreen`** (done) and its texture, `textures/gui/container/copper_distiller.png`, in the style of
   the furnace's (drawn from vanilla's palette, checked beside it). The texture is written by
   `tools/distiller/generate_distiller_gui.py`; nothing is copied from another texture, and the gauges
   show the mod's own water sprites. In place of a furnace's flame, a small bar of fuel between the water
   and the fuel slot empties as the fire burns. The screen stands on `client/platform/MachineScreen`. The
   gauges name their servings and the boiler's water when hovered, and the arrow turns red, saying why,
   while the tub is dry.
5. **Building it in the world** (done): the three part blocks, the merge on placing a boiler on a firebox,
   `piped`, the pipe joining them, the drops, and the extra models.
6. **`lit`** (done) in the blockstate: the generator writes a cold firebox (logs unlit, no fire plane),
   `copper_distiller_boiler_cold`, beside the burning one; light and smoke follow.
7. **Salt** (done): the `distiller_salt` tag in datagen (`ThirstItemTagProvider`), the config option,
   the slot shown only when it resolves. The gametest mod tags sugar as salt.
8. **The cooling tub** (done): `cooled`, filled by a right-click (`block/DistillerInteractions`, any
   water container, a skin pouring up to a bucket), required to condense.
9. **Config** (done): `distillerServingSeconds` and `distillerTankServings`, one size for both tanks,
   at least a bucket, in a Copper Distiller tab of the Containers page. The menu reads the tank size
   from the server through a data slot, so a client with another config still draws the gauges right.
10. **Tests.** Gametests: fill from each container, distil a bucket of sea water and get three Pure and
   one salt with a salt item tagged by a test data pack (done), no salt with the tag empty (done, on
   `DistillerSalt.resolve`, since the test pack's tag is never empty), fuel not burnt with
   nothing to do, the basin filling each container type, hoppers in and out, breaking drops the slots.
   Building: a boiler placed on a firebox merges, anywhere else does not; a pipe joins a lined-up pair
   and is spent, and does nothing to a pair facing apart or a tub on the wrong side; each break drops
   what the table above says.
   An agent script (`tools/agent/gameplay/distiller.jsonl`, extended) opens the GUI with a real click,
   reads `client.slots`, and captures the screen. As built (done): every item above has a gametest in
   `DistillerMachineGameTest` or `DistillerGameTest`, and the config's clamps one in `ThirstApiGameTest`.
11. **Docs** (done). WATER-REFERENCE.md (a Pure source, sea water no longer a dead end), the roadmap's item marked
    done (the roadmap has since been deleted), this file's status, the CHANGELOG and the docs site (the Copper Distiller section of the water
    page, and four keys on the configuration page). No screenshots yet.

## Jade (done)

`client/compat/JadeIntegration` shows, on either half, the boiler's servings and what they are, the
basin's, "The cooling tub is dry" when it is, and the salty servings counted toward the next salt when
there is a salt; on a lone cooling tub, only whether it is dry. Jade lists it as its own plugin entry.
The machine tells the client what it saves, but only when the tanks, the boiler's water or the salt
count change, at most a few packets a serving; the fire and the progress, which change every tick, are
the menu's alone. The tank size shown is the client's config.
