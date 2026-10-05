package com.thirstwastaken2.gametest;

import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * The water the mod adds to loot: one pool on five structure chests and on piglin bartering, graded
 * clean or pure, one on three dry surface chests, graded dirty or murky, and nothing added anywhere else.
 */
public final class LootGameTest {
    private static final List<Identifier> CHESTS = List.of(
            Vanilla.lootTableId(BuiltInLootTables.ABANDONED_MINESHAFT),
            Vanilla.lootTableId(BuiltInLootTables.BASTION_OTHER),
            Vanilla.lootTableId(BuiltInLootTables.NETHER_BRIDGE),
            Vanilla.lootTableId(BuiltInLootTables.SHIPWRECK_SUPPLY),
            Vanilla.lootTableId(BuiltInLootTables.SIMPLE_DUNGEON));
    private static final List<Identifier> DRY_CHESTS = List.of(
            Vanilla.lootTableId(BuiltInLootTables.DESERT_PYRAMID),
            Vanilla.lootTableId(BuiltInLootTables.VILLAGE_DESERT_HOUSE),
            Vanilla.lootTableId(BuiltInLootTables.VILLAGE_SAVANNA_HOUSE));
    /** The chest pool yields water half the time, so this many rolls cannot all miss by chance. */
    private static final int CHEST_ROLLS = 80;
    /** Bartering yields water 3 times in 40, so this many rolls cannot all miss by chance. */
    private static final int BARTER_ROLLS = 600;

    @GameTest
    public void everySeededChestCanHoldGradedWater(GameTestHelper helper) {
        StringBuilder counts = new StringBuilder();
        boolean everyChest = true;
        for (Identifier chest : CHESTS) {
            int water = countGradedWater(helper, table(helper, chest), chestParams(helper), CHEST_ROLLS);
            counts.append(chest).append('=').append(water).append(' ');
            everyChest &= water > 0;
        }
        TestFixtures.check(helper, everyChest,
                "every seeded chest should hold clean or pure water in " + CHEST_ROLLS + " rolls, got " + counts);
        helper.succeed();
    }

    @GameTest
    public void dryChestsHoldDirtyOrMurkyWater(GameTestHelper helper) {
        StringBuilder counts = new StringBuilder();
        boolean everyChest = true;
        for (Identifier chest : DRY_CHESTS) {
            int water = 0;
            for (int roll = 0; roll < CHEST_ROLLS; roll++) {
                for (ItemStack stack : table(helper, chest).getRandomItems(chestParams(helper), roll)) {
                    if (!WaterPurity.isStamped(stack)) continue;
                    TestFixtures.check(helper, !WaterPurity.isSalty(stack) && WaterPurity.get(stack) <= 1,
                            chest + " water should be fresh and dirty or murky, got " + WaterPurity.quality(stack));
                    water++;
                }
            }
            counts.append(chest).append('=').append(water).append(' ');
            everyChest &= water > 0;
        }
        TestFixtures.check(helper, everyChest,
                "every dry chest should hold dirty or murky water in " + CHEST_ROLLS + " rolls, got " + counts);
        helper.succeed();
    }

    /**
     * A table a data pack replaced still gets the water. Vanilla's Villager Trade Rebalance experiment
     * is such a pack: it replaces the mineshaft chest, and the test server enables every experiment.
     * The mod used to skip replaced tables, which left that chest dry on some versions and not on
     * others, because Fabric API changed how it reports experiment packs.
     */
    // The trade rebalance experiment arrived with 1.20.2.
    //? if >=1.20.5 {
    @GameTest
    public void aTableADataPackReplacedStillGetsWater(GameTestHelper helper) {
        var packs = helper.getLevel().getServer().getPackRepository().getSelectedIds();
        TestFixtures.check(helper, packs.contains("trade_rebalance"),
                "this test relies on the test server enabling the trade rebalance pack, which replaces the "
                        + "mineshaft chest; enabled packs are " + packs);

        int water = countGradedWater(helper, table(helper, Vanilla.lootTableId(BuiltInLootTables.ABANDONED_MINESHAFT)),
                chestParams(helper), CHEST_ROLLS);
        TestFixtures.check(helper, water > 0,
                "the mineshaft chest the trade rebalance pack replaced should still hold water, got none");
        helper.succeed();
    }
    //?}

    @GameTest
    public void piglinsBarterGradedWater(GameTestHelper helper) {
        Entity piglin = helper.spawn(TestFixtures.piglinType(), new BlockPos(1, 2, 1));
        LootParams params = new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.THIS_ENTITY, piglin)
                .create(LootContextParamSets.PIGLIN_BARTER);

        int water = countGradedWater(helper, table(helper, Vanilla.lootTableId(BuiltInLootTables.PIGLIN_BARTERING)), params, BARTER_ROLLS);

        TestFixtures.check(helper, water > 0, "piglins should barter water in " + BARTER_ROLLS + " rolls, got none");
        helper.succeed();
    }

    @GameTest
    public void otherChestsAreLeftAlone(GameTestHelper helper) {
        int water = countAnyStampedWater(table(helper, Vanilla.lootTableId(BuiltInLootTables.JUNGLE_TEMPLE)), chestParams(helper), CHEST_ROLLS);

        TestFixtures.check(helper, water == 0,
                "a chest the mod does not seed should never hold its water, got " + water + " stacks");
        helper.succeed();
    }

    /**
     * Counts stacks that are stamped, fresh and clean or pure, and fails on any other water: loot water
     * the furnace could not accept, or that came out dirty or salty, is a bug in the pool.
     */
    private static int countGradedWater(GameTestHelper helper, LootTable table, LootParams params, int rolls) {
        int water = 0;
        for (int roll = 0; roll < rolls; roll++) {
            for (ItemStack stack : table.getRandomItems(params, roll)) {
                if (!WaterPurity.isStamped(stack)) continue;
                TestFixtures.check(helper, !WaterPurity.isSalty(stack) && WaterPurity.get(stack) >= 2,
                        "loot water should be fresh and graded clean or pure, got " + WaterPurity.quality(stack));
                water++;
            }
        }
        return water;
    }

    private static int countAnyStampedWater(LootTable table, LootParams params, int rolls) {
        int water = 0;
        for (int roll = 0; roll < rolls; roll++) {
            for (ItemStack stack : table.getRandomItems(params, roll)) {
                if (WaterPurity.isStamped(stack)) water++;
            }
        }
        return water;
    }

    private static LootTable table(GameTestHelper helper, Identifier id) {
        return TestFixtures.lootTable(helper, id);
    }

    private static LootParams chestParams(GameTestHelper helper) {
        return new LootParams.Builder(helper.getLevel())
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .create(LootContextParamSets.CHEST);
    }
}
