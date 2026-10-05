# Planned content

Ideas for content to add to ThirstWasTaken2, with what the mod does today and what the change would
be. Nothing here is committed to a release yet.

## 1. Not enough places to find water

**Today.** Water bottles are seeded into abandoned mineshafts, bastions, nether fortresses,
shipwreck supply chests and simple dungeons, plus Piglin barters — see
[LootIntegration.java](../../../src/main/java/com/thirstwastaken2/compat/LootIntegration.java). Every one of
those is an underground or hostile structure. The dry, surface biomes where thirst actually hurts
have none.

**Proposal.**

- **More chests.** Desert pyramids, desert and savanna village houses, and trail ruins. These are
  exactly the places a player is when water is scarce, and the water found there should be dirty or
  slightly dirty, not purified.
- **Villager trades.** Leatherworkers sell waterskins; clerics sell clean water. This gives a
  village a role in the water economy and gives emeralds a use for a player who cannot boil.

## 2. Sea water is a dead end

**Done** (2026-10-05): the Copper Distiller, a fuelled two-block still, makes sea water Pure and leaves
another mod's salt behind. What was built differs from the proposal below; see
[DISTILLATION-PLAN.md](DISTILLATION-PLAN.md).

**Today.** Sea water is a kind of its own, not a grade of fresh water — see `WaterQuality.SALT` in
[WaterPurity.java](../../../src/main/java/com/thirstwastaken2/purity/WaterPurity.java). It never quenches
thirst, and boiling does not desalinate it: the purify recipes only move fresh water up a grade. A
player who spawns on an island or a coastline is surrounded by water and has no way to use any of
it.

**Proposal.** Distillation. A cauldron holding sea water placed over a heat source (campfire, fire,
lava, magma block) slowly evaporates, one layer at a time, and leaves behind clean water plus salt.
This turns the coast from a dead end into a slow but reliable water source, and rewards building a
setup rather than clicking one recipe.

**Open questions.** Whether salt is a new item and what it is good for (food preservation? a
crafting ingredient?), how long a layer should take, and whether the cauldron needs to stay chunk
loaded for the process to continue.
