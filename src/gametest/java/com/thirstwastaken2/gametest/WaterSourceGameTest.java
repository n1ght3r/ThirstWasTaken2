package com.thirstwastaken2.gametest;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.config.WaterSourceUse;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.purity.SourceDrawing;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.SlabBlock;

/**
 * Scooping water where the source is taken, for worlds with finite water: the waterskin, bowl and
 * bottle take the source they fill from, as a bucket does, refuse flowing water, and a vessel draws a
 * bucket's three servings; switched off, the world keeps its water. Auto follows the
 * waterSourceConversion game rule. Every fill goes through the server's real right-click path.
 */
public final class WaterSourceGameTest {
    @GameTest
    public void aWaterskinTakesTheSourceWhenOn(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper);
        ServerPlayer player = TestFixtures.playerAboveWater(helper);
        WaterQuality expected = WaterPurity.sampleAt(helper.getLevel(), water);

        TestFixtures.withConfig(config -> config.waterFillTakesSource = WaterSourceUse.ON, () -> {
            ItemStack flask = use(player, new ItemStack(ThirstItems.IRON_FLASK));
            int servings = Math.min(WaterskinItem.BUCKET_SERVINGS, WaterskinItem.capacity(flask));
            TestFixtures.check(helper, WaterskinItem.servings(flask) == servings,
                    "a flask should draw a bucket's " + servings + " servings from a source, got " + WaterskinItem.servings(flask));
            TestFixtures.check(helper, WaterPurity.quality(flask).equals(expected),
                    "the flask should carry the sampled " + expected + ", got " + WaterPurity.quality(flask));
            TestFixtures.check(helper, !hasWater(helper, water),
                    "the source should be gone, got " + helper.getLevel().getBlockState(water));
        });
        helper.succeed();
    }

    @GameTest
    public void aWaterskinRefusesFlowingWaterWhenOn(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper);
        helper.setBlock(TestFixtures.WATER, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
        ServerPlayer player = TestFixtures.playerAboveWater(helper);

        TestFixtures.withConfig(config -> config.waterFillTakesSource = WaterSourceUse.ON, () -> {
            ItemStack skin = use(player, new ItemStack(ThirstItems.WATERSKIN));
            TestFixtures.check(helper, WaterskinItem.servings(skin) == 0,
                    "a waterskin should not fill from flowing water, got " + WaterskinItem.servings(skin));
            TestFixtures.check(helper, hasWater(helper, water), "the flowing water should be left alone");
        });
        helper.succeed();
    }

    @GameTest
    public void aBowlTakesTheSourceWhenOn(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper);
        ServerPlayer player = TestFixtures.playerAboveWater(helper);

        TestFixtures.withConfig(config -> config.waterFillTakesSource = WaterSourceUse.ON, () -> {
            ItemStack held = use(player, new ItemStack(ThirstItems.TERRACOTTA_BOWL));
            TestFixtures.check(helper, held.is(ThirstItems.TERRACOTTA_WATER_BOWL),
                    "a terracotta bowl should fill from a source, got " + held);
            TestFixtures.check(helper, !hasWater(helper, water), "the source should be gone");
        });
        helper.succeed();
    }

    @GameTest
    public void aBottleTakesTheSourceWhenOn(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper);
        ServerPlayer player = TestFixtures.playerAboveWater(helper);

        TestFixtures.withConfig(config -> config.waterFillTakesSource = WaterSourceUse.ON, () -> {
            use(player, new ItemStack(Items.GLASS_BOTTLE));
            TestFixtures.check(helper, !TestFixtures.findSampledWater(player).isEmpty(),
                    "a glass bottle should fill from a source");
            TestFixtures.check(helper, !hasWater(helper, water), "the source should be gone");
        });
        helper.succeed();
    }

    @GameTest
    public void aBottleRefusesFlowingWaterWhenOn(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper);
        helper.setBlock(TestFixtures.WATER, Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 1));
        ServerPlayer player = TestFixtures.playerAboveWater(helper);

        TestFixtures.withConfig(config -> config.waterFillTakesSource = WaterSourceUse.ON, () -> {
            use(player, new ItemStack(Items.GLASS_BOTTLE));
            TestFixtures.check(helper, TestFixtures.findSampledWater(player).isEmpty(),
                    "a glass bottle should not fill from flowing water");
            TestFixtures.check(helper, hasWater(helper, water), "the flowing water should be left alone");
        });
        helper.succeed();
    }

    /** A waterlogged block is a source a bucket takes too: it is drained, not broken. */
    @GameTest
    public void aWaterloggedBlockIsDrainedWhenOn(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper);
        helper.setBlock(TestFixtures.WATER, Blocks.OAK_SLAB.defaultBlockState().setValue(SlabBlock.WATERLOGGED, true));
        ServerPlayer player = TestFixtures.playerAboveWater(helper);

        TestFixtures.withConfig(config -> config.waterFillTakesSource = WaterSourceUse.ON, () -> {
            ItemStack skin = use(player, new ItemStack(ThirstItems.WATERSKIN));
            TestFixtures.check(helper, WaterskinItem.servings(skin) > 0, "a waterskin should fill from a waterlogged slab");
            TestFixtures.check(helper, helper.getLevel().getBlockState(water).is(Blocks.OAK_SLAB)
                            && !helper.getLevel().getBlockState(water).getValue(SlabBlock.WATERLOGGED),
                    "the slab should stay and be drained, got " + helper.getLevel().getBlockState(water));
        });
        helper.succeed();
    }

    /** The positive control: off, a waterskin fills whole and the source stays. */
    @GameTest
    public void offLeavesTheSource(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper);
        ServerPlayer player = TestFixtures.playerAboveWater(helper);

        TestFixtures.withConfig(config -> config.waterFillTakesSource = WaterSourceUse.OFF, () -> {
            ItemStack flask = use(player, new ItemStack(ThirstItems.IRON_FLASK));
            TestFixtures.check(helper, WaterskinItem.servings(flask) == WaterskinItem.capacity(flask),
                    "off, a flask should fill whole, got " + WaterskinItem.servings(flask));
            use(player, new ItemStack(Items.GLASS_BOTTLE));
            TestFixtures.check(helper, hasWater(helper, water) && helper.getLevel().getFluidState(water).isSource(),
                    "off, the source should stay, got " + helper.getLevel().getBlockState(water));
        });
        helper.succeed();
    }

    /** Auto takes the source only where water does not refill itself; the test world's rule is on. */
    @GameTest
    public void autoFollowsTheGameRule(GameTestHelper helper) {
        TestFixtures.check(helper, new ThirstConfig().waterFillTakesSource == WaterSourceUse.AUTO,
                "Auto should be the default");
        TestFixtures.check(helper, SourceDrawing.takesSource(WaterSourceUse.AUTO, false)
                        && !SourceDrawing.takesSource(WaterSourceUse.AUTO, true),
                "Auto should take the source only with waterSourceConversion off");
        TestFixtures.check(helper, SourceDrawing.takesSource(WaterSourceUse.ON, true)
                        && !SourceDrawing.takesSource(WaterSourceUse.OFF, false),
                "On and Off should ignore the game rule");
        TestFixtures.withConfig(config -> config.waterFillTakesSource = WaterSourceUse.AUTO, () ->
                TestFixtures.check(helper, !SourceDrawing.takesSource(helper.getLevel()),
                        "Auto in a world whose water refills itself should leave the source"));
        helper.succeed();
    }

    private static boolean hasWater(GameTestHelper helper, BlockPos pos) {
        return helper.getLevel().getFluidState(pos).is(FluidTags.WATER);
    }

    /** Runs the real server-side right-click path and hands back what is in the hand afterwards. */
    private static ItemStack use(ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.gameMode.useItem(player, player.level(), player.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND);
        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }
}
