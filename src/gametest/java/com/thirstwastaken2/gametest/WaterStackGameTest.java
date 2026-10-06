package com.thirstwastaken2.gametest;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Filled terracotta water bowls stack, to {@code terracottaWaterBowlStackSize}, only with bowls of the
 * same water, and a drink takes one of them. Water bottles stay vanilla's, one to a stack.
 */
public final class WaterStackGameTest {
    @GameTest
    public void waterBowlsStackAndWaterBottlesDoNot(GameTestHelper helper) {
        int size = ThirstConfig.get().terracottaWaterBowlStackSize;
        ItemStack murky = bowls(WaterQuality.fresh(1), 1);
        TestFixtures.check(helper, murky.getMaxStackSize() == size,
                "a water bowl should stack to " + size + ", got " + murky.getMaxStackSize());
        TestFixtures.check(helper, !TestFixtures.sameData(murky, bowls(WaterQuality.fresh(2), 1)),
                "bowls of two grades should not stack together");

        ItemStack bottle = WaterPurity.setQuality(TestFixtures.waterBottle(), WaterQuality.fresh(1));
        TestFixtures.check(helper, bottle.getMaxStackSize() == 1,
                "a water bottle should not stack, got " + bottle.getMaxStackSize());
        helper.succeed();
    }

    @GameTest
    public void drinkingFromAStackOfBowlsLeavesTheRestAndOneEmpty(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        ThirstManager.set(player, ThirstData.full().withLevels(10, 0));
        player.setItemInHand(InteractionHand.MAIN_HAND, bowls(WaterQuality.fresh(2), 3));
        player.gameMode.useItem(player, player.level(), player.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND);
        TestFixtures.check(helper, player.isUsingItem(), "a stack of water bowls should start being drunk");

        ItemStack left = player.getUseItem().finishUsingItem(player.level(), player);

        TestFixtures.check(helper, left.is(ThirstItems.TERRACOTTA_WATER_BOWL) && left.getCount() == 2,
                "drinking should leave two water bowls in hand, got " + left);
        TestFixtures.check(helper, player.getInventory().countItem(ThirstItems.TERRACOTTA_BOWL) == 1,
                "drinking should hand back one empty bowl, got " + player.getInventory().countItem(ThirstItems.TERRACOTTA_BOWL));
        helper.succeed();
    }

    private static ItemStack bowls(WaterQuality quality, int count) {
        ItemStack stack = WaterPurity.setQuality(new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), quality);
        stack.setCount(count);
        return stack;
    }
}
