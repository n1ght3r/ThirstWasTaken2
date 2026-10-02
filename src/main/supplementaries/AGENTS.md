# src/main/supplementaries — water quality in Moonlight's soft fluids

[Supplementaries](https://modrinth.com/mod/supplementaries) keeps water in **soft fluids**, a system of
its required library [Moonlight Lib](https://modrinth.com/mod/moonlight), so a jar, a goblet and a
faucet all move water through the same three classes. Everything here targets those, which is why one
directory covers both of Supplementaries' blocks and its faucet. The only classes of Supplementaries'
own that are touched are the faucet's list of behaviours and the two behaviours of it that move water
this mod has something to say about.

Today it fixes four ways water lost its quality, adds two things Supplementaries cannot do alone,
and shows the grade wherever a player would look for it:

- **a jar or a goblet keeps the grade**: a dirty bottle poured in comes back out dirty, and a jar of
  Murky water will not take Clean water. Without it everything came back out plain, which reads as
  `defaultPurity`, so two decorative blocks purified water for free;
- **sea water stays sea water**: Moonlight converts a water bottle to its own water fluid, so without
  the salt flag a jar turned the sea drinkable;
- **a faucet keeps what a cauldron holds**, both when it drains one and when it fills one, and a
  cauldron poured into keeps the worse of the two, exactly as pouring a container in by hand does;
- **a faucet samples the water it draws out of the world**, so one over a swamp fills a jar with the
  same grade a bottle filled by hand there comes out with, rather than the config default;
- **a jar or a goblet of water can be drunk**, which Supplementaries cannot do on its own: Moonlight
  only drinks a fluid that names a food item, and `moonlight:water` names none;
- **a faucet fills and drains the hanging pots**, the mod's own water block, keeping their grade and
  their boiling;
- **the mod's terracotta bowl is a container Moonlight knows**, so a jar, a goblet and a faucet fill
  and empty it the way they do a vanilla bottle;
- and **the grade is visible**: on the tooltip of a jar that keeps its water, in the colour of the
  water in a placed jar or goblet, and in Jade's overlay.

What is still to do is in
[docs/dev/integration/storage/SUPPLEMENTARIES-INTEGRATION.md](../../../docs/dev/integration/storage/SUPPLEMENTARIES-INTEGRATION.md).

This directory is **only compiled by nodes that set `deps.supplementaries`** in
`stonecutter.properties.toml`, which is `1.21.1` and `1.21.1-neoforge` and nothing else:
Supplementaries has no release for any newer Minecraft version.

**It is the one optional integration both loaders compile.** Moonlight is multi-loader, so
`SoftFluid`, `SoftFluidStack` and `SoftFluidTank` are the same classes with the same signatures on
Fabric and NeoForge, and since it is one Minecraft version there is no Stonecutter branch either.
Nothing here may name a loader.

```
supplementaries/java/com/thirstwastaken2/supplementaries/     both 1.21.1 nodes
  SupplementariesPresence      the gate: a classpath probe for Moonlight, and one for Supplementaries
  SupplementariesMixinPlugin   applies each mixin only where its target is installed
  SoftFluidQuality             the only map between a SoftFluidStack and a WaterQuality
  WaterSoftFluid               the mark that says which registry entry is water
  SoftFluidDrinking            a serving out of a jar or a goblet, drunk as the bottle it would fill
  CauldronQuality              what a faucet and a vanilla water cauldron say about the water between them
  HangingPotFaucet             the hanging pots as something a faucet draws from and pours into
  SampledWater                 the grade of world water a faucet draws, remembered for a few seconds
  mixin/SoftFluidMixin                  the two components onto water, for the conversions Moonlight makes itself
  mixin/SoftFluidStackMixin             the mod's own stamping rules on every container a tank fills
  mixin/SoftFluidTankMixin              what goes into a tank, and drinking out of one
  mixin/SoftFluidInternalMixin          where a load finishes: the water mark and the bowl
  mixin/FluidContainerListAccessor      the one protected call that adds a container to a fluid
  mixin/WaterCauldronInteractionMixin   the faucet's behaviour for vanilla cauldrons
  mixin/LiquidBlockInteractionMixin     the faucet's behaviour for water in the world
  mixin/FaucetBehaviorsManagerMixin     registers the hanging pots with the faucet
  mixin/SoftFluidStackTintMixin         the colour water is drawn in, by grade
  mixin/SoftFluidTankViewMixin          the grade line on the tooltip of a jar that holds water
supplementaries/resources/
  thirstwastaken2.supplementaries.mixins.json
../../client/supplementaries/java/com/thirstwastaken2/client/supplementaries/
  SupplementariesJade          the integration's one entry point, borrowed from Jade
  SoftFluidTooltip             how to read the water out of a jar, for the mod's own Jade plugin
```

## How a grade moves

A `SoftFluidStack` carries data components the way an `ItemStack` does, so the grade lives on the
fluid itself. It keeps **exactly one** of `water_purity` and `water_salty`, the rule `WaterFluids`
already keeps for NeoForge fluid stacks, because Moonlight refuses to mix two stacks whose components
differ: that is what stops a jar of Murky water from quietly taking Clean water, and it only works if
water of one grade always looks identical.

Six hooks, and each is there for a reason the others do not cover:

| Hook | Does |
|---|---|
| `SoftFluidTank.addFluid` | stamps water entering a tank with the grade it already reads as, before the tank decides whether it fits. Gradeless water would otherwise refuse to share a tank with water that has a grade, although both read the same everywhere else |
| `SoftFluidStack.splitToItem` | runs the container a tank just filled through `WaterPurity.setQuality`. Moonlight builds that stack from scratch, so it needs the mod's own rules: `water_salty: false` on fresh water, which every purification recipe matches on, and the sprite that says a bottle is sea water |
| `SoftFluid.getPreservedComponents` | adds the two components to what Moonlight copies of its own accord. `moonlight:water` lists another thirst mod's component, not this one's. This is what carries the grade onto a **NeoForge fluid stack** and back: a jar is an `IFluidHandler` there, so a pipe or a faucet pointed at a tank moves its water through one |
| `WaterCauldronInteraction` | a cauldron keeps its quality in a blockstate, not a component, so no soft fluid hook reaches it. The hanging pots are the same and are answered for by `HangingPotFaucet` |
| `LiquidBlockInteraction` | water in the world has no grade until something samples it, and a faucet is the one collector that never did |
| `SoftFluidInternal` | where a load finishes: the water entry is marked and the terracotta bowl is added to its container list, one moment before Moonlight builds the map from item to fluid |

- **The quality is read before the call, never after.** `splitToItem` spends the serving, and a stack
  spent to nothing reads as empty, so the grade is taken first and applied to what comes back. The
  same shape as `GenericItemFillingMixin` in `src/main/createfly`.
- **`getPreservedComponents` is answered once per registry entry**, cached in a `@Unique` field,
  because a faucet asks on every transfer.
- **Water is recognised by key, never by tag.** With a stack in hand that is
  `stack.is(MLBuiltinSoftFluids.WATER)`. With only the registry entry it is the mark
  `WaterSoftFluid` carries, put on by the one lookup by key that the integration makes. Asking the
  entry itself is what does not work: a soft fluid's account of itself is the vanilla fluids it stands
  for, water's are the `c:water` tag, and the hook that has only an entry runs while a world is
  loading, where that tag is empty and water answers that it is not water.
- **That one lookup happens where the item map is built, and nowhere earlier.** Moonlight remembers
  the holder it resolves for a key, so a key looked up before the registry has finished is remembered
  wrong for the rest of the load, and with water wrong nothing that pours works at all.
- **Unstamped water is `defaultPurity`**, the same fallback every unstamped container already has, and
  it is made explicit on the way into a tank so that everything in a tank compares equal by grade.

## Drinking

`SoftFluidTank.tryDrinkUpFluid` only runs when the fluid names a food item, and water names none, so
the mixin is at `@At("HEAD")` and cancellable: it answers for water before that test and leaves every
other fluid to Moonlight. A serving is drunk as **the water bottle it would have been poured into**,
through `ThirstManager.drinkItem`, so the grade decides what it restores, one roll drives the nausea
and poison it may bring, and the advancement counts, exactly as for a bottle finished by hand. A full
thirst bar refuses it, the same rule as drinking that bottle.

The work is server side only. Both call sites hand the client a success so the arm swings and sync
their block entity afterwards, so the client has nothing to do but agree.

## The faucet

Three things beyond the cauldron, all in the faucet:

- **World water is sampled** where it lies, in `LiquidBlockInteraction`. A faucet asks on a tick, and
  one with nowhere to pour asks every tick, since its cooldown only starts once something moves, so
  the answer is kept for 100 ticks per position in `SampledWater`. The rule in
  [purity/AGENTS.md](../java/com/thirstwastaken2/purity/AGENTS.md) that sampling never reaches a tick
  path is what that is for. Unlike Create's and Sophisticated's, which remember one position each
  because a pump is one machine, this remembers a few: one behaviour serves every faucet in the world.
- **The hanging pots** are a faucet source and a faucet target, `HangingPotFaucet`. A serving of the
  pot is a bottle of soft fluid, which makes the two counts the same number, and everything about what
  the pot then holds is the pot's own: `HangingPotBlock.withPoured` keeps the worse of the two grades
  and what has boiled so far, exactly as pouring a container in by hand does.
- **It is registered where Supplementaries builds its own list.** Supplementaries has a listener for
  that, but a listener would need an entry point to be added from, and this integration has none:
  everything it does is a mixin. `FaucetBehaviorsManagerMixin` is the same place at the same moment,
  so it inherits the same order, and order does not matter for a block no built-in behaviour claims.

## The terracotta bowl

Moonlight learns that an item is a water container from `moonlight:water`'s own container list, which
names a handful of other mods' cups by hand. The mod's bowl is added to that list rather than shipped
as a data pack copy of the file, which would silently drop every other mod's containers the day
Moonlight adds one, and whose turn came first would not be something to rely on.

It has to be written **before Moonlight builds the map from item to fluid**, which is what decides
whether an item can be poured out at all, so it goes in beside the water mark. The waterskin is
deliberately left out: it holds three servings with a fill level of its own, and a container list maps
one empty item to one filled item at a fixed capacity, which cannot say that. On NeoForge a faucet
reaches the waterskin through its fluid capability anyway.

## Showing the grade

Three places, none of them load bearing, and none of them a new palette:

- **The tooltip** of a jar that was broken while it held water, which keeps what it held.
  `SoftFluidTankViewMixin` puts the mod's own grade line under Supplementaries' own fluid line, so a
  jar reads `Water: 4 mBtl` and then `Dirty`, in the words and the colour a bottle of the same water
  uses.
- **The colour** of the water in a placed jar or goblet, by grade rather than by biome, which is what
  Moonlight asks for otherwise. The five colours are read off the terracotta water bowl's own sprites,
  so a jar of water looks like a bowl of the same water: brown, grey blue, blue, cyan, and turquoise
  for the sea. They are deliberately not the tooltip palette in `WaterPurity`, which is tuned to stay
  legible on a dark tooltip and puts sea water in the pale cream of dried salt; `purity/AGENTS.md`
  already says the sprites differ for that reason. Only the still colour is hooked: Moonlight works
  the flowing and particle colours out from it for a fluid tinted both ways, which water is.
- **Jade**, through the mod's own plugin rather than a second one. `client/compat/JadeIntegration` is
  common client code and may name no foreign class, so it asks a list of container readers first, and
  `SupplementariesJade` adds one. One plugin means one entry in Jade's settings with one name, rather
  than two that say the same thing in nine languages.

`SupplementariesJade` is the integration's **only entry point**, and Jade's is the right one to
borrow: it is called on a client exactly when there is a Jade to show anything, and it costs one line
in the Fabric manifest and nothing at all on NeoForge, which finds it by its annotation.

## How it stays optional

The same three layers as [src/main/create](../create/AGENTS.md) and
[src/main/sophisticated](../sophisticated/AGENTS.md).

1. **Build.** Only when `deps.supplementaries` is set, both loader scripts add this directory and
   append the mixin config to the built manifest with the two mods as optional dependencies, from
   [its row in the integration table](../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt). Both loaders compile it, so `checkLoaderSeam` keeps it free of either loader's API. Fabric needs Loom to remap both mods, so they are `modCompileOnly`
   rather than plain libraries; Loom does not unpack a dependency's nested jars into a run, so
   Moonlight's own CodecUI is taken out of its jar while the build configures. It has to be done then
   and not by a task: Loom resolves the mod configurations while configuring, and a jar written
   afterwards is not there to be remapped.
2. **Runtime gate.** `SupplementariesPresence` probes the classpath for Moonlight's `SoftFluidTank`
   and for Supplementaries' `WaterCauldronInteraction`, as resources, which never loads a class. It
   names no loader, unlike the gates of the other two integrations, because both loaders compile it.
   A mod of a version that moved either class is skipped with a warning rather than crashing on a
   missing mixin target.
3. **Mixin plugin.** `SupplementariesMixinPlugin` asks the gate per mixin: Moonlight for the ones that
   target the soft fluid system, Supplementaries itself for the faucet's behaviours and the jar's
   tooltip, since other mods ship Moonlight without Supplementaries.

## Checking it

The gametests run without either mod, which is what proves the mod is unchanged when they are absent;
`runGametest` passing on both 1.21.1 nodes is that check. What the integration does is checked in a
real client, on both nodes, with
[tools/agent/integrations/supplementaries.jsonl](../../../tools/agent/integrations/supplementaries.jsonl):

```bash
./gradlew ":1.21.1:runClient" -Pagent=tools/agent/integrations/supplementaries.jsonl -Pquickplay=SupplementariesAgent
```

It pours a dirty bottle, a salty one and a terracotta bowl through a jar and draws each back, pours a
pure bottle into a goblet and drinks it, and runs five faucets: two between cauldrons, one over a pool,
one draining a hanging pot and one filling another. Both nodes answer the same: the grade is on the
fluid in the block, the container drawn back out still carries it, thirst goes from 4 to 10 out of the
goblet, a dirty cauldron drained through a faucet arrives dirty while pure water poured into a dirty
cauldron leaves it dirty, a pot fills and empties with its grade, and the pool arrives as the grade
plains water is sampled at rather than as the config default. The scene fixes its biome with
`/fillbiome` so that last one is the same answer in both nodes' worlds.

It also breaks a jar that holds water and reads the tooltip of the item that comes back, and leaves
two screenshots behind: five jars side by side, one of each grade and one of sea water, with Jade
naming the one under the crosshair.

The script addresses the player by selector: a Fabric dev client names its player `Player<NN>` and a
NeoForge one names it `Dev`.
