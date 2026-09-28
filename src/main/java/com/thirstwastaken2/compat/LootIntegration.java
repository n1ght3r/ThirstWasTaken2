package com.thirstwastaken2.compat;

import com.thirstwastaken2.platform.ItemWaterData;
import com.thirstwastaken2.platform.Loader;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.EmptyLootItem;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.UniformContainerBase;
import net.minecraft.world.level.storage.loot.functions.SetPotionFunction;

import java.util.Set;

/**
 * Replacement for the original Forge global loot modifiers, which seeded structure chests and
 * Piglin barters with water bottles of varying purity.
 */
public final class LootIntegration {
    private static final Set<Identifier> CHESTS = Set.of(
            Vanilla.lootTableId(BuiltInLootTables.ABANDONED_MINESHAFT),
            Vanilla.lootTableId(BuiltInLootTables.BASTION_OTHER),
            Vanilla.lootTableId(BuiltInLootTables.NETHER_BRIDGE),
            Vanilla.lootTableId(BuiltInLootTables.SHIPWRECK_SUPPLY),
            Vanilla.lootTableId(BuiltInLootTables.SIMPLE_DUNGEON));
    private static final Identifier PIGLIN_BARTERING = Vanilla.lootTableId(BuiltInLootTables.PIGLIN_BARTERING);

    private LootIntegration() { }

    public static void register() {
        // Every table with one of these ids gets the pool, including one a data pack replaced: water
        // bottles are an addition a pack can live with, and see Loader.onLootTable for why no pack
        // can be excluded reliably.
        Loader.onLootTable((key, addPool) -> {
            if (CHESTS.contains(key)) {
                addPool.accept(waterPool(true));
            } else if (PIGLIN_BARTERING.equals(key)) {
                addPool.accept(waterPool(false));
            }
        });
    }

    private static LootPool.Builder waterPool(boolean chest) {
        return Vanilla.lootPool(1)
                .add(water(2).setWeight(chest ? 10 : 2))
                .add(water(3).setWeight(chest ? 10 : 1))
                .add(EmptyLootItem.emptyItem().setWeight(chest ? 20 : 37));
    }

    private static UniformContainerBase.Builder<?> water(int purity) {
        var bottle = LootItem.lootTableItem(Items.POTION).apply(SetPotionFunction.setPotion(Potions.WATER));
        // Fresh, and stamped as such: the purification recipes match on the salt too, so a looted
        // bottle that left it out could never be boiled.
        ItemWaterData.stampFreshLoot(bottle, purity);
        return bottle.apply(Vanilla.setCount(1, 3));
    }
}
