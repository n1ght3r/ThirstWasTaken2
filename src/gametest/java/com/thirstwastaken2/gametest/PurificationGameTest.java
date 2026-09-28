package com.thirstwastaken2.gametest;

import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Which water a furnace will accept.
 *
 * <p>The recipes live entirely in datapack files and match on components, so nothing on the Java
 * side fails to compile when a container stops carrying what they look for. These tests are that
 * missing compiler: they ask the real recipe manager about real stacks.
 */
public final class PurificationGameTest {
    /** Enough rolls that the water pool, at half weight, is certain to have produced bottles. */
    private static final int ROLLS = 40;

    /**
     * Looted water used to carry a grade and nothing else, which no recipe could match: every
     * cooking recipe also requires the container to say it is fresh.
     */
    @GameTest
    public void lootedWaterBottlesCanBeBoiled(GameTestHelper helper) {
        ServerLevel level = helper.getLevel();
        LootTable table = TestFixtures.lootTable(helper, Vanilla.lootTableId(BuiltInLootTables.SIMPLE_DUNGEON));
        LootParams params = new LootParams.Builder(level)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(helper.absolutePos(BlockPos.ZERO)))
                .create(LootContextParamSets.CHEST);

        int bottles = 0;
        int cookable = 0;
        for (int roll = 0; roll < ROLLS; roll++) {
            for (ItemStack stack : table.getRandomItems(params, roll)) {
                if (!WaterPurity.isWaterContainer(stack)) continue;
                bottles++;
                // Purified water is already clean and deliberately has no recipe of its own.
                boolean needsBoiling = WaterPurity.get(stack) < WaterPurity.MAX;
                if (!needsBoiling || hasSmeltingRecipe(helper, stack)) cookable++;
            }
        }

        TestFixtures.check(helper, bottles > 0,
                "the loot pool should have produced water bottles in " + ROLLS + " rolls");
        TestFixtures.check(helper, cookable == bottles,
                "every looted water bottle should be boilable, got " + cookable + " of " + bottles);
        helper.succeed();
    }

    /** Salt water carries no grade, so no cooking recipe can match it. That is the whole guard. */
    @GameTest
    public void saltWaterIsRejectedByTheFurnace(GameTestHelper helper) {
        ItemStack salty = WaterPurity.setQuality(
                new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), WaterQuality.SALT);
        ItemStack dirty = WaterPurity.setQuality(
                new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), WaterQuality.fresh(0));

        TestFixtures.check(helper, !hasSmeltingRecipe(helper, salty),
                "salt water must not be smeltable, or boiling it would quietly desalinate it");
        TestFixtures.check(helper, TestFixtures.cook(helper, RecipeType.SMOKING, salty).isEmpty(),
                "salt water must not be smokable either");
        TestFixtures.check(helper, hasSmeltingRecipe(helper, dirty),
                "dirty fresh water must still be smeltable, otherwise the test above proves nothing");
        helper.succeed();
    }

    /**
     * The table in ThirstRecipeProvider: two grades up, stopping at pure, the same in a furnace, a smoker
     * and on a campfire.
     */
    @GameTest
    public void boilingRaisesTheGradeByTwoAndStopsAtPure(GameTestHelper helper) {
        for (ItemStack container : List.of(TestFixtures.waterBottle(),
                new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), new ItemStack(Items.WATER_BUCKET))) {
            for (int grade = WaterPurity.MIN; grade < WaterPurity.MAX; grade++) {
                ItemStack input = WaterPurity.setQuality(container.copy(), WaterQuality.fresh(grade));
                WaterQuality expected = WaterQuality.fresh(Math.min(grade + 2, WaterPurity.MAX));
                boiled(helper, input, TestFixtures.cook(helper, RecipeType.SMELTING, input), expected, "a furnace");
                boiled(helper, input, TestFixtures.cook(helper, RecipeType.SMOKING, input), expected, "a smoker");
                boiled(helper, input, TestFixtures.cook(helper, RecipeType.CAMPFIRE_COOKING, input), expected, "a campfire");
            }
        }
        helper.succeed();
    }

    @GameTest
    public void pureWaterHasNothingToBoil(GameTestHelper helper) {
        ItemStack pure = WaterPurity.setQuality(new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL),
                WaterQuality.fresh(WaterPurity.MAX));

        TestFixtures.check(helper, !hasSmeltingRecipe(helper, pure),
                "pure water deliberately has no recipe, so it cannot be burned for nothing");
        helper.succeed();
    }

    /** A water bowl crafted from a bucket of fresh water is graded clean; sea water cannot be poured in. */
    @GameTest
    public void aWaterBowlIsCraftedFromABucketOfFreshWater(GameTestHelper helper) {
        ItemStack fresh = craftBowl(helper, WaterQuality.fresh(WaterPurity.MIN));
        TestFixtures.check(helper, fresh.is(ThirstItems.TERRACOTTA_WATER_BOWL)
                        && WaterPurity.quality(fresh).equals(WaterQuality.fresh(2)),
                "a bowl and a bucket of fresh water should craft a clean water bowl, got "
                        + fresh + " holding " + WaterPurity.quality(fresh));

        TestFixtures.check(helper, craftBowl(helper, WaterQuality.SALT).isEmpty(),
                "a bucket of sea water must not craft a water bowl");
        helper.succeed();
    }

    private static ItemStack craftBowl(GameTestHelper helper, WaterQuality bucket) {
        return TestFixtures.craftGrid(helper, 2, 1, List.of(
                new ItemStack(ThirstItems.TERRACOTTA_BOWL),
                WaterPurity.setQuality(new ItemStack(Items.WATER_BUCKET), bucket)));
    }

    private static void boiled(GameTestHelper helper, ItemStack input, ItemStack result, WaterQuality expected, String where) {
        TestFixtures.check(helper, result.is(input.getItem()) && WaterPurity.isStamped(result)
                        && WaterPurity.quality(result).equals(expected),
                "boiling " + WaterPurity.quality(input) + " " + input.getItem() + " on " + where + " should give "
                        + expected + ", got " + result + " holding " + WaterPurity.quality(result));
    }

    private static boolean hasSmeltingRecipe(GameTestHelper helper, ItemStack stack) {
        return !TestFixtures.cook(helper, RecipeType.SMELTING, stack).isEmpty();
    }
}
