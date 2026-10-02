# Brewin' and Chewin' integration plan

What ThirstWasTaken2 should do with [Brewin' and Chewin'](https://modrinth.com/mod/brewin-and-chewin)
(mod id `brewinandchewin`, package `umpaz.brewinandchewin`), the fermenting addon for Farmer's
Delight. This file sets the order of work, what each step needs and how each one is checked. Once the
work is built, how it works goes in `src/main/brewinandchewin/AGENTS.md`.

Written on 2026-09-25 from:

- the repository [Monad-Modding/BrewinAndChewin](https://github.com/Monad-Modding/BrewinAndChewin),
  branch `1.21.1` at `7560564` (2026-09-22). That branch already says `5.0.0` in `Versions.kt`, which
  is **not released**. The names the mixins use were checked against the 4.5.0 jars and the tag
  `4.5.0+1.21.1`, whose `KegBlockEntity.fluidExtract` and `getPouringRecipe` are the same code;
- the Modrinth project `hIu9KJTT`, whose newest uploads are `v4.5.0+1.21.1` (2026-06-24).

## Which build for which node

The mod is multi loader (a `common` module plus `fabric` and `neoforge`), and **both loaders are
published for 1.21.1 only**. So two nodes, not one:

| Node | Build | Modrinth id | Requires on `runClient` |
|---|---|---|---|
| `1.21.1` | `v4.5.0+1.21.1-fabric` | `O3PobqCR` | Farmer's Delight Refabricated (`>=1.21.1-3.3.0`, we pin `1.21.1-3.3.6`), Greenhouse Config `>=2.2.3` |
| `1.21.1-neoforge` | `v4.5.0+1.21.1-neoforge` | `MbcR48Ou` | Farmer's Delight (`[1.3.2,)`, we pin `1.21.1-1.3.4`), Greenhouse Config `[2.2.3,)` |
| every other node | none | — | — |

The Fabric `1.21.1` jar also claims 1.21; Brewin' and Chewin' asks for exactly 1.21.1, so on 1.21
the integration is simply absent, and the gate handles that like any missing mod.

**Why one directory serves both loaders.** Everything the integration touches is in the `common`
module, which names neither loader: `KegBlockEntity`, `KegPouringRecipe`, `AbstractedFluidStack`,
`AbstractedFluidTank`. The loader modules only wrap the tank (`KegFluidTankFabric`,
`KegFluidTankNeoForge`). So the row gets both loaders, as Supplementaries and Kaleidoscope Cookery do,
and `checkLoaderSeam` keeps it that way.

Greenhouse Config, a hard dependency of the mod on both loaders, is **nested in its jar**, so it needs
no key of its own. NeoForge loads the nested jars itself. Loom does not, and on Fabric the nesting is two
deep (Greenhouse Config's TOML support nests its own Night Config), so `nestedMods` in
`build.gradle.kts` follows each jar's `fabric.mod.json` down.

## Where water lives in Brewin' and Chewin'

Only one block holds water: the **keg**. It has a fluid tank of one bucket in 4.5.0 (`kegCapacity =
81000` droplets in `brewinandchewin-common.toml`; the 5.0.0 branch says 4000 mB), four ingredient slots, a
container slot and an output slot. Water is the base fluid of eleven fermenting recipes (beer, vodka,
rice wine, the five grape wines, glittering grenadine, kombucha), all matching the fluid tag
`#c:water`.

Water gets in and out of the tank through `KegBlockEntity.fluidExtract`, by one of two paths:

| Path | Used for | In | Out | What happens to the grade |
|---|---|---|---|---|
| **Pouring recipe** (`brewinandchewin:keg_pouring`) | a water bucket (`pouring/water_bucket.json`), a water bottle (`pouring/potion.json`, `strict`) | fills the tank from the **recipe's** fluid, `recipe.getFluid(slot)`, whose components are the recipe's, not the item's | `recipe.assemble` builds a brand-new result item | **lost both ways.** A Dirty bucket in, a plain bucket out. The bottle that comes out lacks `water_salty: false` too, so it is not cookable (see [purity/AGENTS.md](../../../../src/main/java/com/thirstwastaken2/purity/AGENTS.md)) |
| **Generic fluid container**, when no pouring recipe matches | anything with the loader's item fluid capability: our waterskin, canteen, flask and terracotta bowls, other mods' tanks | `fluidTank.fill(itemFluidContainer.drain(...))`, the item's own fluid stack, but only if some pouring recipe's fluid matches the tank's | `itemFluidContainer.fill(fluidTank.drain(...))` | kept, but **an empty keg refuses a canteen**: no recipe's fluid matches an empty tank. See step 3 |

Two more consequences followed from the code; step 3 confirmed them:

- **A graded water bottle may be refused.** The bottle recipe is `strict`, compared with
  `ItemStack.isSameItemSameComponents`. Every bottle this mod stamps carries `water_purity` and
  `water_salty`, so it is not the same components as the recipe's plain water bottle. On NeoForge a
  vanilla potion has no fluid capability, so nothing takes it and the bottle is refused. On Fabric it
  is refused too.
- **The tank does not mix fluids with different components**
  (`fluidTank.getAbstractedFluid().matches(...)`). Once the grade rides on the fluid, a keg holding
  Clean water refuses a Dirty canteen, which is the rule every tank already follows
  ([purity/AGENTS.md](../../../../src/main/java/com/thirstwastaken2/purity/AGENTS.md), "The carried
  containers"). Water poured from a plain bucket today and from a canteen do not mix either.

The keg picked up keeps its tank in `BLOCK_ENTITY_DATA` (`writeDrink` writes `FluidTank`), so once the
grade is a component of the fluid, a picked-up keg keeps it with no work of ours. Confirmed: both tanks
write the fluid's components.

Pipes: on NeoForge the keg exposes its tank as a fluid capability (`KegFluidTankNeoForge`), so a
Create pipe on `1.21.1-neoforge` fills it with fluid stacks our `SampledWater` already stamps.

## The drinks

None of the mod's drinks is tagged `c:drinks` (they are in `brewinandchewin:fermented_drinks`), so
today they restore nothing. They go into `ThirstConfig` by id, like Kaleidoscope Cookery's teas: a
`brewinAndChewinDrinks` / `brewinAndChewinFoods` pair merged with `putMissing`, which reaches every
node and matches nothing where the mod is absent. The values below are a proposal (thirst,
quenched); the principle is that a drink restores less the stronger it is, and spirits nothing.

| Group | Items | Proposed |
|---|---|---|
| Light, in a tankard | `kombucha` | 6, 8 |
| | `beer`, `mead`, `egg_grog`, `glittering_grenadine` | 5, 6 |
| | `bloody_mary` (tomato juice) | 4, 5 |
| Strong, in a tankard | `pale_jane`, `strongroot_ale`, `dread_nog` | 3, 3 |
| | `saccharine_rum`, `steel_toe_stout`, `red_rum` | 2, 2 |
| Salty or cursed | `salty_folly`, `withering_dross` | 0, 0: left out rather than listed as zero |
| Wine, a bottle | `red_wine`, `white_wine`, `currant_wine`, `verruca_wine`, `twisted_wine`, `rice_wine`, `old_wine` | 3, 4, **if** a bottle is one drink; `WineItem.finishUsingItem` shrinks the stack, to confirm there are no servings |
| Spirits and tinctures | `vodka`, `brandy`, `aqua_vitae`, `sickening_tincture`, `delicious_tincture` | left out |
| Soups in a bowl (foods) | `creamy_onion_soup` | 4, 5, as Farmer's Delight's onion soup |
| | `fiery_fondue`, `grits`, `chopped_liver` | 2, 3 |

Milk from the keg comes out as `farmersdelight:milk_bottle`, which already has a value.

## Status

| # | Item | Kind | Nodes | Status |
|---|---|---|---|---|
| 1 | Build dependency and gate | build | `1.21.1`, `1.21.1-neoforge` | **Done** (2026-09-25) |
| 2 | Thirst values for the drinks and soups | data | all (config) | **Done** (2026-09-25), values as proposed |
| 3 | What actually happens to a grade, per loader, per path | investigation | two | **Done** (2026-09-25) |
| 4 | The keg keeps the grade: bucket and bottle in and out | bug | two | **Done** (2026-09-25): buckets both ways, bottles out |
| 5 | A graded water bottle is accepted by the keg | bug | two | **Done** (2026-09-25) |
| 6 | Sea water in the keg | decision | two | **Decided** (2026-09-25): (b), and **done** |
| 7 | What fermenting does with a grade | decision | two | **Decided** (2026-09-25): (a), as recommended; nothing to build |
| 8 | The grade is visible: Jade line on the keg | feature | two | **Done** (2026-09-25) |
| 9 | Changelog and player docs | docs | — | **Done** (2026-09-25) |
| 10 | Nothing crashes without the mod: `checkOptionalSeam`, `-PwithoutOptional`, `boot.jsonl` | test | all | **Done** (2026-09-25), again after 8 |

## 1. Build dependency and gate (done)

- A row in [the integration table](../../../../build-logic/src/main/kotlin/com/thirstwastaken2/buildlogic/Integrations.kt):
  `dir = "brewinandchewin"`, `depsKey = "deps.brewin_and_chewin"`, both loaders, mixin config
  `thirstwastaken2.brewinandchewin.mixins.json`, `neoForgeDependencies = listOf("brewinandchewin")`.
  Step 8 added the client directory, with `client = true` and a Fabric `jade` entrypoint.
- `deps.brewin_and_chewin` pinned by Modrinth version id in `[fabric."1.21.1"]` and
  `[neoforge."1.21.1"]`, and in `MODRINTH_DEPS`. Greenhouse Config needs no key: see above.
- `BrewinAndChewinPresence` and `BrewinAndChewinMixinPlugin`, as in
  [src/main/brewinandchewin/AGENTS.md](../../../../src/main/brewinandchewin/AGENTS.md).
- **Names on Fabric**: the 4.5.0 Fabric jar is in intermediary for Minecraft's names, as
  Refabricated's is, so the mod's own names are matched with `remap = false` and no descriptor.

**Checked:** both nodes build, `checkOptionalSeam`, `checkLoaderSeam`, `runGametest` (181 passed on
each), and `boot.jsonl` with `-PwithoutOptional=brewinandchewin` on both.

## 2. Thirst values (done)

`brewinAndChewinDrinks` and `brewinAndChewinFoods` in `ThirstConfig`, the table above as proposed,
merged into an older config with `putMissing`. `brewinAndChewinDrinksAreMergedIntoAnOlderConfig`
checks that the defaults list them, leave the spirits out, and merge back without overwriting a
player's value. The docs page listing supported mods is step 9.

## 3. Investigation: what happens to a grade (done)

A throwaway agent script ran on both nodes with the mod and no mixin. What it found, 2026-09-25:

| Case | `1.21.1` (Fabric) | `1.21.1-neoforge` |
|---|---|---|
| Dirty bucket in, bucket out | tank plain water, plain bucket out: **laundered** | the same |
| Dirty water bottle in | **refused** | taken as `create:potion`, through Create's bottle handler (Create is on this node's `runClient`), which the keg then refuses water on top of |
| Glass bottle drawn from plain water | plain bottle with no `water_salty`: **not cookable** | not reached (the keg held `create:potion`) |
| Dirty canteen into an empty keg | **refused**: no pouring recipe's fluid matches an empty tank | the same |
| Sea water canteen / bucket | canteen refused; bucket taken as **plain water** | the same |
| Keg broken | the item keeps `FluidTank` with its components | the same |

Fermenting matches the base fluid by the tag `#c:water` (NeoForge's tag ingredient, Fabric's own), which
ignores components, so any grade and sea water ferment. That is decisions 6 and 7.

## 4. The keg keeps the grade (done)

The grade lives on the keg's fluid, as one component. Two mixins, described in
[src/main/brewinandchewin/AGENTS.md](../../../../src/main/brewinandchewin/AGENTS.md):
`KegPouringRecipeMixin` makes a pouring recipe's fluid carry the grade of the container pouring it,
which fills, matches and picks the recipe by the grade at once; `KegBlockEntityMixin` stamps the
container `fluidExtract` hands back with the tank's grade. Simpler than planned: no wrap of the fill
call, since the fill already reads the recipe's fluid.

**Checked** with [tools/agent/integrations/brewin-and-chewin.jsonl](../../../../tools/agent/integrations/brewin-and-chewin.jsonl),
passed whole on both nodes: a Dirty bucket in and out, a drawn bottle Dirty, a Clean bucket refused by a
Dirty keg with room and a Dirty one taken, a plain bucket and a plain bottle still working with a plain
keg and the drawn bottle cookable, sea water in and out salty, and a broken keg's item keeping the grade.

## 5. A graded water bottle is accepted (done)

Step 3 confirmed the refusal on Fabric, and on NeoForge without Create. `KegBottleMixin` wraps every
`ItemStack.isSameItemSameComponents` in `getPouringRecipe`'s filter lambda and in `fluidExtract`: when
the stack in hand is a stamped water container and the recipe's stack is not, it also compares the
recipe's stack with the held one unstamped. So a graded bottle matches the strict bottle recipe as its
plain self, and its grade goes in through `getFluid` as a bucket's does. A drawn container compared
with the output slot is stamped itself, so that comparison stays exact and two grades never stack
there. On `1.21.1-neoforge` the keg's own recipe now matches before Create's bottle handler, so a
bottle goes in as water rather than `create:potion`.

## 6. Decision: sea water in the keg

Options:

- **(a) The keg refuses sea water**, from a bucket, a bottle or a canteen, with the action bar message
  the teapot uses. Nothing ferments from it, and it cannot be stored.
- (b) The keg takes it and hands it back salty, like the stockpot, but no recipe ferments from it.
- (c) Nothing: it ferments into ordinary drinks, and salt water is laundered into beer.

Recommended was (a). **Chosen: (b)**, 2026-09-25. Step 4 already hands sea water back salty;
`KegFermentingMixin` makes `canFerment` answer false while the keg holds sea water, on every tick, so
a keg of sea water with beer's ingredients never starts and one already brewing stops. Every water
recipe matches the tag `#c:water`, which ignores components, so this is the only place that can tell
sea water apart. The recipe still shows in the keg's screen (`getRecipeWithoutTemperature` is not
changed); it just does not progress.

## 7. Decision: what fermenting does with a grade

Options:

- **(a) Fermented drinks are safe whatever the water**, as tea is: the drink is its own item with its
  own thirst value, and Brewin' and Chewin' has its own penalties (intoxication).
- (b) A drink remembers the grade of its water, and a drink from Dirty water rolls sickness.

**Recommended: (a).** (b) means stamping a grade on 14 of another mod's items and a
drink-with-grade path the config values do not have, for a trade-off players will not see. Only
`kombucha` and the grape wines are close to water; none is.

**Chosen: (a)**, 2026-09-25. Nothing to build: a finished brew replaces the tank's fluid with the
recipe's result, which carries no component of ours, and the drinks' values in `ThirstConfig` have no
grade. The agent script checks that beer brewed from Dirty water carries none.

## 8. Jade line on the keg (done)

The mod has no Jade plugin of its own (only JEI and EMI), so the keg joins ours, as Kaleidoscope's
blocks do: `KegHeldWaterMixin` gives `KegBlockEntity` our interface `HeldKegWater`, which reads the grade
off the tank, and `BrewinAndChewinJade` hands it to `JadeIntegration.addContainer`. `writeUpdateTag`
sends the `FluidTank` with its components, so the client's keg has the grade. The Jade class names only
`HeldKegWater` and the gate, and asks the gate before adding anything.

**Checked:** the agent script's last case pours a Dirty bucket into a keg and captures Jade over it and
over the sea water keg: "Dirty" and "Salty". Those captures sit on the script's test floor, so the
docs image `docs/public/screenshots/integrations/brewin-and-chewin/brewin-keg.png` was staged again on a lakeshore (see the
`capture-screenshots` skill).

## 9. Docs (done)

`CHANGELOG.md` (Unreleased), [the site's page](../../../docs/integrations/farmers-delight/brewin-and-chewin.md) and its sidebar
entry, the Fabric and NeoForge rows in `docs/docs/installation.md`, and the Modrinth and CurseForge
pages. The root `AGENTS.md` already had the row.

The page lists only what 4.5.0 ships. The grape wines other than Rice Wine, Old Wine, Brandy, Aqua
Vitae, the tinctures, Grits and Chopped Liver are on the unreleased 5.0.0 branch only; their ids are in
`ThirstConfig` already and match nothing until then. So the "eleven fermenting recipes" above are
5.0.0's count; add the new items to the page when 5.0.0 is released.

## 10. Optional seam (done)

Run again on 2026-09-25 after step 8, on `1.21.1` and `1.21.1-neoforge`:

- `checkOptionalSeam` and `checkLoaderSeam` pass, finding the plugin, the gate and the Jade class
  loaded without the mod.
- `tools/agent/smoke/boot.jsonl` with `-PwithoutOptional=brewinandchewin` and with
  `brewinandchewin,jade`: all four come up, stay up and verify. Without the mod, Jade still loads
  `BrewinAndChewinJade`, which is the case 1.0.9 crashed in, and nothing fails.
- `tools/agent/integrations/brewin-and-chewin.jsonl` with the mod passes whole on both, in worlds made
  by `tools/agent/new_world.py`, with the two Jade captures.
- `runGametest` needs no rerun: the mod is never on its classpath, and nothing it loads changed.

## Not planned

- **Intoxication and thirst.** Tipsy could raise thirst drain, a hangover. Not planned: it changes a
  balance the mod owns, and a player can already see the cost in the low values of step 2.
- **The Aging Cask, Heating Cask and Fiery Fondue Pot.** None holds water.
- **Tankards and wine bottles as fluid containers** (NeoForge's `TankardItemFluidHandlerNeoForge`,
  `WineBottleItemFluidHandlerNeoForge`): they hold the mod's own fluids, never water.
- **Newer Minecraft versions**, until the mod publishes one.
