package com.thirstwastaken2.gametest;

import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.purity.WaterInteractions;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LayeredCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The interactions the mod adds to water and cauldrons: scooping with the bowl and the waterskin,
 * drawing the waterskin from a cauldron, pouring it out, and a bottle drawn from a cauldron keeping
 * the cauldron's water.
 *
 * <p>Scooping and drawing go through the server's real right-click paths, so the loader's use
 * callbacks are exercised as well as the handlers behind them.
 */
public final class WaterInteractionsGameTest {
    private static final BlockPos CAULDRON = new BlockPos(2, 2, 2);

    @GameTest
    public void aTerracottaBowlScoopsTheWaterItIsAimedAt(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper);
        ServerPlayer player = TestFixtures.playerAboveWater(helper);
        WaterQuality expected = WaterPurity.sampleAt(helper.getLevel(), water);

        use(player, new ItemStack(ThirstItems.TERRACOTTA_BOWL));

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        TestFixtures.check(helper, held.is(ThirstItems.TERRACOTTA_WATER_BOWL),
                "a terracotta bowl used on water should become a filled bowl, got " + held);
        TestFixtures.check(helper, WaterPurity.quality(held).equals(expected),
                "the bowl should carry the sampled " + expected + ", got " + WaterPurity.quality(held));
        helper.succeed();
    }

    @GameTest
    public void aWaterskinFillsInOneScoop(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper);
        ServerPlayer player = TestFixtures.playerAboveWater(helper);
        WaterQuality expected = WaterPurity.sampleAt(helper.getLevel(), water);

        use(player, new ItemStack(ThirstItems.WATERSKIN));

        ItemStack held = player.getItemInHand(InteractionHand.MAIN_HAND);
        TestFixtures.check(helper, WaterskinItem.servings(held) == com.thirstwastaken2.config.ThirstConfig.get().waterskinCapacity,
                "a waterskin used on water should be filled, got " + WaterskinItem.servings(held));
        TestFixtures.check(helper, WaterPurity.quality(held).equals(expected),
                "the waterskin should carry the sampled " + expected + ", got " + WaterPurity.quality(held));
        helper.succeed();
    }

    @GameTest
    public void aClayBowlCannotHoldWater(GameTestHelper helper) {
        TestFixtures.water(helper);
        ServerPlayer player = TestFixtures.playerAboveWater(helper);

        use(player, new ItemStack(ThirstItems.CLAY_BOWL));

        TestFixtures.check(helper, player.getItemInHand(InteractionHand.MAIN_HAND).is(ThirstItems.CLAY_BOWL),
                "an unfired clay bowl should stay a clay bowl, got " + player.getItemInHand(InteractionHand.MAIN_HAND));
        helper.succeed();
    }

    @GameTest
    public void aWaterskinDrawsFromACauldronAndLowersIt(GameTestHelper helper) {
        WaterQuality stored = WaterQuality.fresh(1);
        BlockPos pos = cauldron(helper, 3, stored);
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThirstItems.WATERSKIN));

        WaterInteractions.fillWaterskinFromCauldron(player, helper.getLevel(), InteractionHand.MAIN_HAND, aimAt(pos));

        ItemStack skin = player.getItemInHand(InteractionHand.MAIN_HAND);
        int expected = Math.min(3, com.thirstwastaken2.config.ThirstConfig.get().waterskinCapacity);
        TestFixtures.check(helper, WaterskinItem.servings(skin) == expected,
                "drawing from a full cauldron should take all three levels the skin has room for, got "
                        + WaterskinItem.servings(skin));
        TestFixtures.check(helper, WaterPurity.quality(skin).equals(stored),
                "the serving should carry the cauldron's " + stored + ", got " + WaterPurity.quality(skin));
        TestFixtures.check(helper, helper.getLevel().getBlockState(pos).is(Blocks.CAULDRON),
                "the cauldron should drop a level per serving and be empty, got "
                        + helper.getLevel().getBlockState(pos));
        helper.succeed();
    }

    @GameTest
    public void aWaterskinTakesOnlyWhatACauldronHolds(GameTestHelper helper) {
        BlockPos pos = cauldron(helper, 2, WaterQuality.fresh(2));
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThirstItems.WATERSKIN));

        WaterInteractions.fillWaterskinFromCauldron(player, helper.getLevel(), InteractionHand.MAIN_HAND, aimAt(pos));

        TestFixtures.check(helper, WaterskinItem.servings(player.getItemInHand(InteractionHand.MAIN_HAND)) == 2,
                "a cauldron two levels deep should give two servings, got "
                        + WaterskinItem.servings(player.getItemInHand(InteractionHand.MAIN_HAND)));
        TestFixtures.check(helper, helper.getLevel().getBlockState(pos).is(Blocks.CAULDRON),
                "the cauldron should be empty, got " + helper.getLevel().getBlockState(pos));
        helper.succeed();
    }

    @GameTest
    public void anEmptyCauldronFillsNothing(GameTestHelper helper) {
        helper.setBlock(CAULDRON, Blocks.CAULDRON);
        BlockPos pos = helper.absolutePos(CAULDRON);
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThirstItems.WATERSKIN));

        WaterInteractions.fillWaterskinFromCauldron(player, helper.getLevel(), InteractionHand.MAIN_HAND, aimAt(pos));

        TestFixtures.check(helper, WaterskinItem.servings(player.getItemInHand(InteractionHand.MAIN_HAND)) == 0,
                "an empty cauldron has nothing to draw");
        helper.succeed();
    }

    @GameTest
    public void crouchingPoursAWaterskinOut(GameTestHelper helper) {
        helper.setBlock(CAULDRON, Blocks.STONE);
        BlockPos pos = helper.absolutePos(CAULDRON);
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        ItemStack skin = new ItemStack(ThirstItems.WATERSKIN);
        WaterskinItem.addWater(skin, WaterQuality.fresh(2), com.thirstwastaken2.config.ThirstConfig.get().waterskinCapacity);
        player.setItemInHand(InteractionHand.MAIN_HAND, skin);

        WaterInteractions.emptyWaterskinOnBlock(player, helper.getLevel(), InteractionHand.MAIN_HAND, aimAt(pos));
        TestFixtures.check(helper, WaterskinItem.servings(skin) == com.thirstwastaken2.config.ThirstConfig.get().waterskinCapacity,
                "a standing player should not pour the waterskin out");

        player.setPose(Pose.CROUCHING);
        WaterInteractions.emptyWaterskinOnBlock(player, helper.getLevel(), InteractionHand.MAIN_HAND, aimAt(pos));
        TestFixtures.check(helper, WaterskinItem.servings(skin) == 0,
                "a crouching player should pour it all out, " + WaterskinItem.servings(skin) + " servings left");
        helper.succeed();
    }

    /**
     * Drawing a bottle from a cauldron is vanilla's interaction; the mod only stamps the bottle
     * afterwards. This runs the whole thing: the use callback queues the stamp, vanilla fills the
     * bottle, and the end-of-tick queue stamps it.
     */
    @GameTest
    public void aBottleDrawnFromACauldronCarriesItsWater(GameTestHelper helper) {
        WaterQuality stored = WaterQuality.fresh(0);
        BlockPos pos = cauldron(helper, 3, stored);
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.GLASS_BOTTLE));

        player.gameMode.useItemOn(player, helper.getLevel(), player.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND, aimAt(pos));
        WaterInteractions.tick(helper.getLevel().getServer());

        ItemStack drawn = TestFixtures.findSampledWater(player);
        TestFixtures.check(helper, !drawn.isEmpty(), "drawing from the cauldron should give a stamped water bottle");
        TestFixtures.check(helper, WaterPurity.quality(drawn).equals(stored),
                "the bottle should carry the cauldron's " + stored + ", got " + WaterPurity.quality(drawn));
        helper.succeed();
    }

    private static BlockPos cauldron(GameTestHelper helper, int level, WaterQuality quality) {
        BlockState state = Blocks.WATER_CAULDRON.defaultBlockState()
                .setValue(LayeredCauldronBlock.LEVEL, level)
                .setValue(WaterPurity.BLOCK_PURITY, WaterPurity.storedValue(quality));
        helper.setBlock(CAULDRON, state);
        return helper.absolutePos(CAULDRON);
    }

    private static void use(ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.gameMode.useItem(player, player.level(), player.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND);
    }

    private static BlockHitResult aimAt(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }
}
