package com.thirstwastaken2.datagen;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.block.DistillerBlock;
import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.platform.Vanilla;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * What the mod's blocks drop. A hanging pot drops itself; the water in it is lost, as a cauldron's is.
 * The distiller drops itself from its boiler half only, as a bed drops from its head, so mining either
 * half gives one back: the other half breaks with it.
 *
 * <p>Built with vanilla's loot builders and written through vanilla's codec. Fabric's block loot
 * provider would say the same thing, but it was renamed and reshaped for 26.1, and one table is not
 * worth a branch per version.
 */
public final class ThirstBlockLootProvider implements DataProvider {
    private final PackOutput.PathProvider tables;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public ThirstBlockLootProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.tables = output.createPathProvider(PackOutput.Target.DATA_PACK, DataDirectories.of("loot_table"));
        this.registries = registries;
    }

    @Override
    public String getName() {
        return "ThirstWasTaken2 Block Loot Tables";
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registries.thenCompose(lookup -> {
            //? if >=1.20.5 {
            DynamicOps<JsonElement> ops = lookup.createSerializationContext(JsonOps.INSTANCE);
            //?} else {
            /*DynamicOps<JsonElement> ops = JsonOps.INSTANCE;
            *///?}
            Map<String, Item> pots = Map.of(
                    "copper_hanging_pot", ThirstItems.COPPER_HANGING_POT,
                    "iron_hanging_pot", ThirstItems.IRON_HANGING_POT);
            return CompletableFuture.allOf(
                    CompletableFuture.allOf(pots.entrySet().stream()
                            .map(pot -> dropsItself(cache, ops, pot.getKey(), pot.getValue(), null))
                            .toArray(CompletableFuture[]::new)),
                    dropsItself(cache, ops, "copper_distiller", ThirstItems.COPPER_DISTILLER, boilerHalf(lookup)));
        });
    }

    /**
     * The condition that the block is the distiller's boiler half. 26.3 renamed the condition to
     * {@code MatchBlock}, and its state predicate moved package twice before that: {@code critereon}
     * before 1.21.11, {@code criterion} until 26.2, {@code predicates} since.
     */
    //? if >=26.3 {
    private static LootItemCondition.Builder boilerHalf(HolderLookup.Provider lookup) {
        return net.minecraft.world.level.storage.loot.predicates.MatchBlock.blockMatches(
                lookup.lookupOrThrow(net.minecraft.core.registries.Registries.BLOCK), ThirstBlocks.COPPER_DISTILLER,
                net.minecraft.advancements.predicates.StatePropertiesPredicate.Builder.properties()
                        .hasProperty(DistillerBlock.PART, DistillerBlock.Part.BOILER.getSerializedName()));
    }
    //?}
    //? if >=26.2 <26.3 {
    /*private static LootItemCondition.Builder boilerHalf(HolderLookup.Provider lookup) {
        return net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition
                .hasBlockStateProperties(ThirstBlocks.COPPER_DISTILLER)
                .setProperties(net.minecraft.advancements.predicates.StatePropertiesPredicate.Builder.properties()
                        .hasProperty(DistillerBlock.PART, DistillerBlock.Part.BOILER.getSerializedName()));
    }
    *///?}
    //? if >=1.21.11 <26.2 {
    /*private static LootItemCondition.Builder boilerHalf(HolderLookup.Provider lookup) {
        return net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition
                .hasBlockStateProperties(ThirstBlocks.COPPER_DISTILLER)
                .setProperties(net.minecraft.advancements.criterion.StatePropertiesPredicate.Builder.properties()
                        .hasProperty(DistillerBlock.PART, DistillerBlock.Part.BOILER.getSerializedName()));
    }
    *///?}
    //? if <1.21.11 {
    /*private static LootItemCondition.Builder boilerHalf(HolderLookup.Provider lookup) {
        return net.minecraft.world.level.storage.loot.predicates.LootItemBlockStatePropertyCondition
                .hasBlockStateProperties(ThirstBlocks.COPPER_DISTILLER)
                .setProperties(net.minecraft.advancements.critereon.StatePropertiesPredicate.Builder.properties()
                        .hasProperty(DistillerBlock.PART, DistillerBlock.Part.BOILER.getSerializedName()));
    }
    *///?}

    /** A table dropping {@code item}, when it survives the explosion and, if given, {@code condition} holds. */
    private CompletableFuture<?> dropsItself(CachedOutput cache, DynamicOps<JsonElement> ops, String block, Item item,
                                             LootItemCondition.Builder condition) {
        Identifier id = ThirstWasTaken2.id("blocks/" + block);
        LootPool.Builder pool = Vanilla.lootPool(1)
                .add(LootItem.lootTableItem(item))
                .when(ExplosionCondition.survivesExplosion());
        if (condition != null) pool.when(condition);
        LootTable table = LootTable.lootTable()
                .setParamSet(LootContextParamSets.BLOCK)
                .withPool(pool)
                .setRandomSequence(id)
                .build();
        // Before 1.20.5 a loot table is written by Gson rather than by a codec.
        //? if >=1.20.5 {
        JsonElement json = LootTable.DIRECT_CODEC.encodeStart(ops, table).getOrThrow();
        //?} else {
        /*JsonElement json = net.minecraft.world.level.storage.loot.LootDataType.TABLE.parser().toJsonTree(table);
        *///?}
        return DataProvider.saveStable(cache, json, tables.json(id));
    }
}
