package com.thirstwastaken2.gametest;

import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

/**
 * Filling a container from a water block has to stamp the quality of the water it was actually
 * filled from, every time.
 *
 * <p>{@code BottleItemMixin} and {@code BucketItemMixin} do that in two steps: sample the water the
 * player is aiming at as the vanilla method starts, then stamp the container vanilla hands back.
 * Both steps share one capture in {@code FillCapture}, so the tests here care less about the value
 * being right once than about it being right on the second fill as well.
 */
public final class WaterFillingGameTest {
    /** Mud inside the sampling neighbourhood is the cheapest way to change the water's quality. */
    private static final BlockPos MUD = new BlockPos(4, TestFixtures.WATER.getY(), 2);
    /** Farmland adds to mud, which is what takes the sample a whole grade down rather than a little. */
    private static final BlockPos FARMLAND = new BlockPos(4, TestFixtures.WATER.getY(), 3);

    @GameTest
    public void sampledGradeReflectsSurroundings(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper);

        int before = WaterPurity.nearbyPollution(helper.getLevel(), water);
        WaterQuality clean = WaterPurity.sampleAt(helper.getLevel(), water);
        pollute(helper);
        int after = WaterPurity.nearbyPollution(helper.getLevel(), water);
        WaterQuality polluted = WaterPurity.sampleAt(helper.getLevel(), water);

        // The points are checked on their own as well: whether they cross a grade boundary depends on
        // the biome's starting score, which TestFixtures.water pins to plains, Murky, where they do.
        TestFixtures.check(helper, before == 0 && after == 25,
                "mud and farmland beside the water should add 15 and 10 to its score, got " + before + " then " + after);
        TestFixtures.check(helper, clean instanceof WaterQuality.Fresh fresh
                        && polluted instanceof WaterQuality.Fresh dirtied
                        && dirtied.purity() < fresh.purity(),
                "mud and farmland beside the water should lower its grade, got " + clean + " then " + polluted);
        helper.succeed();
    }

    @GameTest
    public void bottleFillStampsSampledQuality(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper);
        ServerPlayer player = TestFixtures.playerAboveWater(helper);
        WaterQuality expected = WaterPurity.sampleAt(helper.getLevel(), water);

        fill(player, Items.GLASS_BOTTLE);

        ItemStack filled = TestFixtures.findSampledWater(player);
        TestFixtures.check(helper, !filled.isEmpty(),
                "using a glass bottle on water should produce a container carrying water quality");
        TestFixtures.check(helper, WaterPurity.quality(filled).equals(expected),
                "bottle should carry the sampled " + expected + ", got " + WaterPurity.quality(filled));
        helper.succeed();
    }

    @GameTest
    public void bucketFillStampsSampledQuality(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper);
        ServerPlayer player = TestFixtures.playerAboveWater(helper);
        WaterQuality expected = WaterPurity.sampleAt(helper.getLevel(), water);

        fill(player, Items.BUCKET);

        ItemStack filled = TestFixtures.findSampledWater(player);
        TestFixtures.check(helper, !filled.isEmpty(),
                "using a bucket on water should produce a water bucket carrying water quality");
        TestFixtures.check(helper, WaterPurity.quality(filled).equals(expected),
                "bucket should carry the sampled " + expected + ", got " + WaterPurity.quality(filled));
        helper.succeed();
    }

    /**
     * The regression this suite exists for. A second fill must sample the water again rather than
     * reuse whatever the first fill captured.
     */
    @GameTest
    public void secondFillResamplesTheWater(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper);
        ServerPlayer player = TestFixtures.playerAboveWater(helper);

        fill(player, Items.GLASS_BOTTLE);
        ItemStack first = TestFixtures.findSampledWater(player);
        TestFixtures.check(helper, !first.isEmpty(), "the first fill should have produced a water bottle");
        WaterQuality before = WaterPurity.quality(first);
        // Take the bottle back out of the inventory so the second one is unambiguous.
        first.setCount(0);

        pollute(helper);
        WaterQuality expected = WaterPurity.sampleAt(helper.getLevel(), water);
        TestFixtures.check(helper, !expected.equals(before),
                "the fixture should have made the water dirtier, got " + before + " then " + expected);

        fill(player, Items.GLASS_BOTTLE);
        ItemStack second = TestFixtures.findSampledWater(player);
        TestFixtures.check(helper, !second.isEmpty(), "the second fill should have produced a water bottle");
        TestFixtures.check(helper, WaterPurity.quality(second).equals(expected),
                "the second fill should resample the water and report " + expected + ", got "
                        + WaterPurity.quality(second));
        helper.succeed();
    }

    /**
     * A fill that never happens must not leave a sample behind for the next one. Using a filled
     * bucket while aiming at water is the branch that captures a sample and then stamps nothing.
     */
    @GameTest
    public void abandonedFillLeavesNoSampleBehind(GameTestHelper helper) {
        TestFixtures.water(helper);
        ServerPlayer player = TestFixtures.playerAboveWater(helper);

        // A water bucket used on a water source places nothing and fills nothing.
        fill(player, Items.WATER_BUCKET);
        ItemStack leaked = TestFixtures.findSampledWater(player);
        TestFixtures.check(helper, leaked.isEmpty() || leaked.getItem() == Items.WATER_BUCKET,
                "using a water bucket should not have produced a freshly stamped container");

        // Now aim at nothing and fill: with no water in reach there is nothing to stamp, so a stale
        // sample from the previous interaction would show up here.
        player.snapTo(player.getX(), player.getY(), player.getZ(), 0.0F, -90.0F);
        player.getInventory().clearContent();
        fill(player, Items.GLASS_BOTTLE);

        ItemStack stale = TestFixtures.findSampledWater(player);
        TestFixtures.check(helper, stale.isEmpty(),
                "filling with no water in reach must not stamp a quality, got " + stale);
        helper.succeed();
    }

    /** Drops the sampled grade of the test's water source by putting pollution next to it. */
    private static void pollute(GameTestHelper helper) {
        helper.setBlock(MUD, Blocks.MUD);
        helper.setBlock(FARMLAND, Blocks.FARMLAND);
    }

    /** Runs the real server-side right-click path so the mixins see exactly what vanilla sees. */
    private static void fill(ServerPlayer player, net.minecraft.world.item.Item item) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        player.gameMode.useItem(player, player.level(), player.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND);
    }
}
