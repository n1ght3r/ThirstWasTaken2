package com.thirstwastaken2.gametest;

import com.thirstwastaken2.block.DistillerBlock;
import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.item.ThirstItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The copper distiller as a two-block structure: placing it puts the tub beside the boiler, a taken spot
 * refuses it, and mining either half takes both and gives back exactly one, none in creative.
 */
public final class DistillerGameTest {
    private static final BlockPos FLOOR = new BlockPos(3, 1, 3);
    private static final BlockPos BOILER = new BlockPos(3, 2, 3);

    @GameTest
    public void placingPutsTheTubToTheBoilersRight(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE.defaultBlockState());
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        // Looking south: the front faces north, back at the player, and the tub goes west, which is the
        // player's right.
        player.setYRot(0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThirstItems.COPPER_DISTILLER));

        place(helper, player);

        BlockState boiler = helper.getLevel().getBlockState(helper.absolutePos(BOILER));
        TestFixtures.check(helper, boiler.is(ThirstBlocks.COPPER_DISTILLER)
                        && boiler.getValue(DistillerBlock.PART) == DistillerBlock.Part.BOILER,
                "the clicked spot should hold the boiler half, got " + boiler);
        TestFixtures.check(helper, boiler.getValue(DistillerBlock.FACING) == Direction.NORTH,
                "the front should face the player, north, got " + boiler.getValue(DistillerBlock.FACING));
        BlockState tub = helper.getLevel().getBlockState(helper.absolutePos(BOILER.west()));
        TestFixtures.check(helper, tub.is(ThirstBlocks.COPPER_DISTILLER)
                        && tub.getValue(DistillerBlock.PART) == DistillerBlock.Part.TUB
                        && tub.getValue(DistillerBlock.FACING) == Direction.NORTH,
                "the tub half should stand west of the boiler, facing the same way, got " + tub);
        TestFixtures.check(helper, player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "placing should spend the item");
        helper.succeed();
    }

    @GameTest
    public void aTakenSpotForTheTubRefusesIt(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE.defaultBlockState());
        helper.setBlock(BOILER.west(), Blocks.STONE.defaultBlockState());
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        player.setYRot(0.0F);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThirstItems.COPPER_DISTILLER));

        place(helper, player);

        TestFixtures.check(helper, helper.getLevel().getBlockState(helper.absolutePos(BOILER)).isAir(),
                "with the tub's spot taken nothing should be placed");
        TestFixtures.check(helper, player.getItemInHand(InteractionHand.MAIN_HAND).is(ThirstItems.COPPER_DISTILLER),
                "the player should keep the item");
        helper.succeed();
    }

    @GameTest
    public void miningTheBoilerTakesBothAndDropsOne(GameTestHelper helper) {
        mineAndCount(helper, BOILER, GameType.SURVIVAL, 1);
    }

    @GameTest
    public void miningTheTubTakesBothAndDropsOne(GameTestHelper helper) {
        mineAndCount(helper, BOILER.west(), GameType.SURVIVAL, 1);
    }

    @GameTest
    public void miningTheTubInCreativeDropsNothing(GameTestHelper helper) {
        mineAndCount(helper, BOILER.west(), GameType.CREATIVE, 0);
    }

    /** Builds a whole distiller facing north, mines {@code target} and checks both halves go and what drops. */
    private static void mineAndCount(GameTestHelper helper, BlockPos target, GameType mode, int expected) {
        BlockState boiler = ThirstBlocks.COPPER_DISTILLER.defaultBlockState()
                .setValue(DistillerBlock.FACING, Direction.NORTH);
        helper.setBlock(BOILER, boiler);
        helper.setBlock(BOILER.west(), boiler.setValue(DistillerBlock.PART, DistillerBlock.Part.TUB));
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        player.setGameMode(mode);

        player.gameMode.destroyBlock(helper.absolutePos(target));

        TestFixtures.check(helper, helper.getLevel().getBlockState(helper.absolutePos(BOILER)).isAir()
                        && helper.getLevel().getBlockState(helper.absolutePos(BOILER.west())).isAir(),
                "mining one half should take the other with it");
        AABB around = new AABB(helper.absolutePos(BOILER)).inflate(3.0);
        int dropped = helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream()
                .filter(entity -> entity.getItem().is(ThirstItems.COPPER_DISTILLER))
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
        TestFixtures.check(helper, dropped == expected,
                "mining the " + (target.equals(BOILER) ? "boiler" : "tub") + " in " + mode + " should drop "
                        + expected + " distiller, dropped " + dropped);
        helper.succeed();
    }

    /** Uses the held item on top of {@link #FLOOR} through the game mode, the way a player places a block. */
    private static void place(GameTestHelper helper, ServerPlayer player) {
        BlockPos floor = helper.absolutePos(FLOOR);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(floor).add(0.0, 0.5, 0.0), Direction.UP, floor, false);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND, hit);
    }
}
