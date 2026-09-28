package com.thirstwastaken2.datagen;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.thirstwastaken2.ThirstWasTaken2;
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

import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * What the mod's blocks drop. A hanging pot drops itself; the water in it is lost, as a cauldron's is.
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
            return CompletableFuture.allOf(pots.entrySet().stream()
                    .map(pot -> dropsItself(cache, ops, pot.getKey(), pot.getValue()))
                    .toArray(CompletableFuture[]::new));
        });
    }

    private CompletableFuture<?> dropsItself(CachedOutput cache, DynamicOps<JsonElement> ops, String block, Item item) {
        Identifier id = ThirstWasTaken2.id("blocks/" + block);
        LootTable table = LootTable.lootTable()
                .setParamSet(LootContextParamSets.BLOCK)
                .withPool(Vanilla.lootPool(1)
                        .add(LootItem.lootTableItem(item))
                        .when(ExplosionCondition.survivesExplosion()))
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
