package com.thirstwastaken2.gametest;

import com.thirstwastaken2.effect.ThirstEffects;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;

/**
 * Water the sky does not reach is at best Murky, and milk cures Upset Stomach. The water stands in
 * a meadow, a mountain biome whose open water is Clean, so the cap has something to take away, and the
 * column over it is cleared first: the test area's own bounds block motion above it.
 */
public final class CoveredWaterGameTest {
    private static final String MEADOW = "minecraft:meadow";
    private static final WaterQuality MURKY = WaterQuality.fresh(1);

    @GameTest
    public void openWaterKeepsItsGrade(GameTestHelper helper) {
        BlockPos water = openWater(helper);
        WaterQuality open = WaterPurity.sampleAt(helper.getLevel(), water);
        TestFixtures.check(helper, open instanceof WaterQuality.Fresh fresh && fresh.purity() >= WaterPurity.BOILED,
                "open water in a meadow should be at least Clean, or the other tests prove nothing, got " + open);
        helper.succeed();
    }

    @GameTest
    public void aRoofOverWaterCapsItAtMurky(GameTestHelper helper) {
        BlockPos water = openWater(helper);
        helper.setBlock(TestFixtures.WATER.above(3), Blocks.STONE);
        TestFixtures.check(helper, MURKY.equals(WaterPurity.sampleAt(helper.getLevel(), water)),
                "water under a roof should be Murky at best, got " + WaterPurity.sampleAt(helper.getLevel(), water));
        helper.succeed();
    }

    @GameTest
    public void leavesCountAsCover(GameTestHelper helper) {
        BlockPos water = openWater(helper);
        helper.setBlock(TestFixtures.WATER.above(4), Blocks.OAK_LEAVES);
        TestFixtures.check(helper, MURKY.equals(WaterPurity.sampleAt(helper.getLevel(), water)),
                "water under a tree should be Murky at best, got " + WaterPurity.sampleAt(helper.getLevel(), water));
        helper.succeed();
    }

    /** A sample at the bottom of a column climbs to its surface, so deep open water is not covered by itself. */
    @GameTest
    public void deepOpenWaterIsNotCoveredByItsOwnWater(GameTestHelper helper) {
        BlockPos water = openWater(helper);
        helper.setBlock(TestFixtures.WATER.above(), Blocks.WATER);
        WaterQuality bottom = WaterPurity.sampleAt(helper.getLevel(), water);
        TestFixtures.check(helper, bottom instanceof WaterQuality.Fresh fresh && fresh.purity() >= WaterPurity.BOILED,
                "the bottom of open water two deep should keep its grade, got " + bottom);
        helper.succeed();
    }

    /** Water stored with a grade keeps it, roof or not: the cap is for water sampled from the world. */
    @GameTest
    public void storedWaterKeepsItsGradeUnderARoof(GameTestHelper helper) {
        openWater(helper);
        BlockPos cauldronAt = new BlockPos(1, 2, 1);
        helper.setBlock(cauldronAt, Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, 3)
                .setValue(WaterPurity.BLOCK_PURITY, WaterPurity.storedValue(WaterQuality.fresh(WaterPurity.MAX))));
        helper.setBlock(cauldronAt.above(2), Blocks.STONE);
        WaterQuality stored = WaterPurity.sampleAt(helper.getLevel(), helper.absolutePos(cauldronAt));
        TestFixtures.check(helper, WaterQuality.fresh(WaterPurity.MAX).equals(stored),
                "Pure water stored in a covered cauldron should stay Pure, got " + stored);
        helper.succeed();
    }

    /** The water fixture in a meadow, with everything that blocks motion above it taken away. */
    private static BlockPos openWater(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper, MEADOW);
        net.minecraft.server.level.ServerLevel level = helper.getLevel();
        for (int x = water.getX() - 1; x <= water.getX() + 1; x++) {
            for (int z = water.getZ() - 1; z <= water.getZ() + 1; z++) {
                int top;
                while ((top = level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING, x, z))
                        > water.getY() + 1) {
                    level.setBlockAndUpdate(new BlockPos(x, top - 1, z), Blocks.AIR.defaultBlockState());
                }
            }
        }
        return water;
    }

    /** Milk clears Poison and Upset Stomach alike, as it clears any effect. */
    @GameTest
    public void milkCuresPoisonAndUpsetStomach(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        player.addEffect(Vanilla.effectInstance(ThirstEffects.UPSET_STOMACH, 600, 1));
        player.addEffect(Vanilla.effectInstance(Vanilla.poison(), 600, 0));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.MILK_BUCKET));
        player.getMainHandItem().finishUsingItem(player.level(), player);

        TestFixtures.check(helper, Vanilla.getEffect(player, Vanilla.poison()) == null,
                "milk should still clear Poison, got " + player.getActiveEffects());
        TestFixtures.check(helper, Vanilla.getEffect(player, ThirstEffects.UPSET_STOMACH) == null,
                "milk should clear Upset Stomach, got " + player.getActiveEffects());
        helper.succeed();
    }

    @GameTest
    public void honeyCuresPoisonAndLeavesUpsetStomach(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        player.addEffect(Vanilla.effectInstance(ThirstEffects.UPSET_STOMACH, 600, 0));
        player.addEffect(Vanilla.effectInstance(Vanilla.poison(), 600, 0));
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND, new ItemStack(Items.HONEY_BOTTLE));
        player.getMainHandItem().finishUsingItem(player.level(), player);

        TestFixtures.check(helper, Vanilla.getEffect(player, Vanilla.poison()) == null,
                "honey should clear Poison, got " + player.getActiveEffects());
        TestFixtures.check(helper, Vanilla.getEffect(player, ThirstEffects.UPSET_STOMACH) != null,
                "honey should leave Upset Stomach, got " + player.getActiveEffects());
        helper.succeed();
    }
}
