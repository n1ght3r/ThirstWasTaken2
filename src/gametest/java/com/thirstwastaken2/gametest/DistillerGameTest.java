package com.thirstwastaken2.gametest;

import com.thirstwastaken2.block.CoolingTubBlock;
import com.thirstwastaken2.block.DistillerBlock;
import com.thirstwastaken2.block.DistillerPartBlock;
import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.item.ThirstItems;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The copper distiller as a two-block structure: placing it puts the tub beside the boiler, a taken spot
 * refuses it, and mining either half takes both and gives back exactly one, none in creative.
 *
 * <p>Built in the world: a boiler placed on a firebox merges into it and anywhere else stands alone; a
 * pipe joins a lined-up pair and is spent, and does nothing to a pair facing apart or a tub on the
 * wrong side; an unjoined boiler gives back both parts. And the tub's coolant, poured once.
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

        clearDrops(helper);
        player.gameMode.destroyBlock(helper.absolutePos(target));

        TestFixtures.check(helper, helper.getLevel().getBlockState(helper.absolutePos(BOILER)).isAir()
                        && helper.getLevel().getBlockState(helper.absolutePos(BOILER.west())).isAir(),
                "mining one half should take the other with it");
        int dropped = dropped(helper, ThirstItems.COPPER_DISTILLER);
        TestFixtures.check(helper, dropped == expected,
                "mining the " + (target.equals(BOILER) ? "boiler" : "tub") + " in " + mode + " should drop "
                        + expected + " distiller, dropped " + dropped);
        helper.succeed();
    }

    @GameTest
    public void aBoilerPlacedOnAFireboxMergesIntoIt(GameTestHelper helper) {
        helper.setBlock(BOILER, ThirstBlocks.BRICK_FIREBOX.defaultBlockState()
                .setValue(DistillerPartBlock.FACING, Direction.EAST));
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThirstItems.DISTILLER_BOILER));

        useOnTop(helper, player, BOILER);

        BlockState merged = helper.getLevel().getBlockState(helper.absolutePos(BOILER));
        TestFixtures.check(helper, merged.is(ThirstBlocks.COPPER_DISTILLER)
                        && merged.getValue(DistillerBlock.PART) == DistillerBlock.Part.BOILER
                        && !DistillerBlock.isWhole(merged) && merged.getValue(DistillerBlock.FACING) == Direction.EAST,
                "the firebox should become an unpiped boiler half facing as it did, got " + merged);
        TestFixtures.check(helper, helper.getLevel().getBlockState(helper.absolutePos(BOILER.above())).isAir(),
                "nothing should be placed above the firebox");
        TestFixtures.check(helper, player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "merging should spend the boiler");
        helper.succeed();
    }

    @GameTest
    public void aBoilerPlacedOnAnythingElseStandsAlone(GameTestHelper helper) {
        helper.setBlock(FLOOR, Blocks.STONE.defaultBlockState());
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThirstItems.DISTILLER_BOILER));

        place(helper, player);

        BlockState placed = helper.getLevel().getBlockState(helper.absolutePos(BOILER));
        TestFixtures.check(helper, placed.is(ThirstBlocks.DISTILLER_BOILER),
                "on stone the boiler should stand alone, got " + placed);
        helper.succeed();
    }

    @GameTest
    public void aPipeJoinsALinedUpPairAndIsSpent(GameTestHelper helper) {
        unjoined(helper, BOILER.west(), Direction.NORTH, true);
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThirstItems.COPPER_PIPE));

        useOnTop(helper, player, BOILER.west());

        BlockState boiler = helper.getLevel().getBlockState(helper.absolutePos(BOILER));
        BlockState tub = helper.getLevel().getBlockState(helper.absolutePos(BOILER.west()));
        TestFixtures.check(helper, boiler.is(ThirstBlocks.COPPER_DISTILLER) && DistillerBlock.isWhole(boiler)
                        && boiler.getValue(DistillerBlock.PART) == DistillerBlock.Part.BOILER,
                "the boiler should be a whole distiller's boiler half, got " + boiler);
        TestFixtures.check(helper, tub.is(ThirstBlocks.COPPER_DISTILLER) && DistillerBlock.isWhole(tub)
                        && tub.getValue(DistillerBlock.PART) == DistillerBlock.Part.TUB
                        && tub.getValue(DistillerBlock.FACING) == Direction.NORTH,
                "the tub should be its tub half, got " + tub);
        TestFixtures.check(helper, tub.getValue(DistillerBlock.COOLED), "the tub should keep its coolant");
        TestFixtures.check(helper, player.getItemInHand(InteractionHand.MAIN_HAND).isEmpty(),
                "joining should spend the pipe");
        helper.succeed();
    }

    @GameTest
    public void aPipeUsedOnTheBoilerJoinsThePairToo(GameTestHelper helper) {
        unjoined(helper, BOILER.west(), Direction.NORTH, false);
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThirstItems.COPPER_PIPE));

        useOnTop(helper, player, BOILER);

        BlockState tub = helper.getLevel().getBlockState(helper.absolutePos(BOILER.west()));
        TestFixtures.check(helper, tub.is(ThirstBlocks.COPPER_DISTILLER) && !tub.getValue(DistillerBlock.COOLED),
                "the tub should be the tub half, still dry, got " + tub);
        helper.succeed();
    }

    @GameTest
    public void aPipeDoesNothingToAPairFacingApart(GameTestHelper helper) {
        unjoined(helper, BOILER.west(), Direction.SOUTH, false);
        pipeDoesNothing(helper, BOILER.west());
    }

    @GameTest
    public void aPipeDoesNothingToATubOnTheWrongSide(GameTestHelper helper) {
        unjoined(helper, BOILER.east(), Direction.NORTH, false);
        pipeDoesNothing(helper, BOILER.east());
    }

    @GameTest
    public void anUnjoinedBoilerGivesBackBothParts(GameTestHelper helper) {
        helper.setBlock(BOILER, ThirstBlocks.COPPER_DISTILLER.unpiped(Direction.NORTH));
        ServerPlayer player = TestFixtures.survivalPlayer(helper);

        clearDrops(helper);
        player.gameMode.destroyBlock(helper.absolutePos(BOILER));

        int fireboxes = dropped(helper, ThirstItems.BRICK_FIREBOX);
        int boilers = dropped(helper, ThirstItems.DISTILLER_BOILER);
        int distillers = dropped(helper, ThirstItems.COPPER_DISTILLER);
        TestFixtures.check(helper, fireboxes == 1 && boilers == 1 && distillers == 0,
                "an unjoined boiler should drop one firebox and one boiler, dropped " + fireboxes + " and "
                        + boilers + ", and " + distillers + " distillers");
        helper.succeed();
    }

    @GameTest
    public void aLoneTubDropsItselfAndLeavesTheBoilerBesideIt(GameTestHelper helper) {
        unjoined(helper, BOILER.west(), Direction.NORTH, true);
        ServerPlayer player = TestFixtures.survivalPlayer(helper);

        clearDrops(helper);
        player.gameMode.destroyBlock(helper.absolutePos(BOILER.west()));

        TestFixtures.check(helper, dropped(helper, ThirstItems.COOLING_TUB) == 1,
                "the tub should drop itself, dropped " + dropped(helper, ThirstItems.COOLING_TUB));
        TestFixtures.check(helper,
                helper.getLevel().getBlockState(helper.absolutePos(BOILER)).is(ThirstBlocks.COPPER_DISTILLER),
                "an unjoined boiler should not break with the tub beside it");
        helper.succeed();
    }

    @GameTest
    public void aBucketFillsATubWithItsCoolantOnce(GameTestHelper helper) {
        helper.setBlock(BOILER, ThirstBlocks.COOLING_TUB.defaultBlockState());
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));

        useOnTop(helper, player, BOILER);

        TestFixtures.check(helper,
                helper.getLevel().getBlockState(helper.absolutePos(BOILER)).getValue(CoolingTubBlock.COOLED),
                "a bucket should fill the tub");
        TestFixtures.check(helper, player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET),
                "the player should be left holding an empty bucket, got "
                        + player.getItemInHand(InteractionHand.MAIN_HAND));

        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));
        useOnTop(helper, player, BOILER);
        TestFixtures.check(helper, player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.WATER_BUCKET),
                "a tub already cooled should take no more water");
        helper.succeed();
    }

    @GameTest
    public void aBottleFillsTheDistillersTubHalf(GameTestHelper helper) {
        BlockState boiler = ThirstBlocks.COPPER_DISTILLER.defaultBlockState()
                .setValue(DistillerBlock.FACING, Direction.NORTH);
        helper.setBlock(BOILER, boiler);
        helper.setBlock(BOILER.west(), boiler.setValue(DistillerBlock.PART, DistillerBlock.Part.TUB));
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, TestFixtures.waterBottle());

        useOnTop(helper, player, BOILER.west());

        TestFixtures.check(helper, helper.getLevel().getBlockState(helper.absolutePos(BOILER.west()))
                .getValue(DistillerBlock.COOLED), "a water bottle should fill the tub half");
        TestFixtures.check(helper, player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.GLASS_BOTTLE),
                "the player should be left holding a glass bottle, got " + player.getItemInHand(InteractionHand.MAIN_HAND));
        helper.succeed();
    }

    /** An unpiped boiler at {@link #BOILER} facing north, and a lone tub at {@code tub} facing {@code facing}. */
    private static void unjoined(GameTestHelper helper, BlockPos tub, Direction facing, boolean cooled) {
        helper.setBlock(BOILER, ThirstBlocks.COPPER_DISTILLER.unpiped(Direction.NORTH));
        helper.setBlock(tub, ThirstBlocks.COOLING_TUB.defaultBlockState()
                .setValue(CoolingTubBlock.FACING, facing).setValue(CoolingTubBlock.COOLED, cooled));
    }

    /** Uses a pipe on the tub at {@code tub} and on the boiler, and checks nothing joined and the pipe was kept. */
    private static void pipeDoesNothing(GameTestHelper helper, BlockPos tub) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThirstItems.COPPER_PIPE));

        useOnTop(helper, player, tub);
        useOnTop(helper, player, BOILER);

        TestFixtures.check(helper, !DistillerBlock.isWhole(helper.getLevel().getBlockState(helper.absolutePos(BOILER)))
                        && helper.getLevel().getBlockState(helper.absolutePos(tub)).is(ThirstBlocks.COOLING_TUB),
                "a pair not lined up should not be joined");
        TestFixtures.check(helper, player.getItemInHand(InteractionHand.MAIN_HAND).is(ThirstItems.COPPER_PIPE),
                "the player should keep the pipe");
        helper.succeed();
    }

    /**
     * Clears the ground round the distiller before something is broken. Batches can reuse a test's spot
     * (seen on Forge 1.20.1), and an earlier test's drops still lying there would be counted.
     */
    private static void clearDrops(GameTestHelper helper) {
        helper.getLevel().getEntitiesOfClass(ItemEntity.class, around(helper)).forEach(ItemEntity::discard);
    }

    private static AABB around(GameTestHelper helper) {
        return new AABB(helper.absolutePos(BOILER)).inflate(3.0);
    }

    /** How many of {@code item} lie on the ground around the distiller. */
    private static int dropped(GameTestHelper helper, Item item) {
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, around(helper)).stream()
                .filter(entity -> entity.getItem().is(item))
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
    }

    /** Uses the held item on top of {@link #FLOOR} through the game mode, the way a player places a block. */
    private static void place(GameTestHelper helper, ServerPlayer player) {
        useOnTop(helper, player, FLOOR);
    }

    /** Uses the held item on the top of the block at {@code pos}, through the game mode, where both loaders fire their hooks. */
    private static void useOnTop(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0.0, 0.5, 0.0), Direction.UP, absolute, false);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND, hit);
    }
}
