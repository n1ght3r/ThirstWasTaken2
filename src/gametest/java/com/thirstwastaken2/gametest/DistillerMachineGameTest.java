package com.thirstwastaken2.gametest;

import com.thirstwastaken2.block.CoolingTubBlock;
import com.thirstwastaken2.block.DistillerBlock;
import com.thirstwastaken2.block.DistillerBlockEntity;
import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The distiller at work: water poured in from its slot, a serving at a time distilled to Pure while the
 * fire burns, the fire waiting when there is nothing to do or no coolant, the basin filling each kind of
 * container, hoppers in and out through either half, the right-click shortcuts, and breaking it giving
 * back what its slots held. The machine is ticked by hand, as many times as the test needs, rather than
 * waited for: a bucket takes 24 seconds.
 */
public final class DistillerMachineGameTest {
    private static final BlockPos BOILER = new BlockPos(3, 2, 3);
    private static final BlockPos TUB = BOILER.west();
    private static final WaterQuality SALT = WaterQuality.SALT;

    @GameTest
    public void aBucketOfSeaWaterPoursIntoTheBoiler(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper, true);
        machine.setItem(DistillerBlockEntity.WATER_IN, WaterPurity.setQuality(new ItemStack(Items.WATER_BUCKET), SALT));

        machine.tick();

        TestFixtures.check(helper, machine.boilerServings() == 3 && SALT.equals(machine.boilerQuality()),
                "the boiler should hold the bucket's 3 salty servings, got " + machine.boilerServings() + " of "
                        + machine.boilerQuality());
        TestFixtures.check(helper, machine.getItem(DistillerBlockEntity.WATER_IN).is(Items.BUCKET),
                "the empty bucket should be left in the slot, got " + machine.getItem(DistillerBlockEntity.WATER_IN));
        helper.succeed();
    }

    @GameTest
    public void aBucketWaitsForRoomForAllThree(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper, true);
        machine.pour(7, WaterQuality.fresh(2));
        machine.setItem(DistillerBlockEntity.WATER_IN, new ItemStack(Items.WATER_BUCKET));

        machine.tick();

        TestFixtures.check(helper, machine.boilerServings() == 7
                        && machine.getItem(DistillerBlockEntity.WATER_IN).is(Items.WATER_BUCKET),
                "with room for 2 the bucket should wait, got " + machine.boilerServings() + " servings and "
                        + machine.getItem(DistillerBlockEntity.WATER_IN));
        helper.succeed();
    }

    @GameTest
    public void aBucketOfSeaWaterDistilsToThreePureServings(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper, true);
        machine.pour(3, SALT);
        machine.setItem(DistillerBlockEntity.FUEL, new ItemStack(Items.COAL));

        tick(machine, 3 * DistillerBlockEntity.SERVING_TICKS - 1);
        TestFixtures.check(helper, machine.basinServings() == 2,
                "a tick short of the third serving the basin should hold 2, got " + machine.basinServings());
        machine.tick();

        TestFixtures.check(helper, machine.basinServings() == 3 && machine.boilerServings() == 0,
                "the basin should hold 3 and the boiler none, got " + machine.basinServings() + " and "
                        + machine.boilerServings());
        TestFixtures.check(helper, machine.saltServings() == DistillerBlockEntity.SALT_SERVINGS,
                "three salty servings should be counted toward salt, got " + machine.saltServings());
        TestFixtures.check(helper, machine.getItem(DistillerBlockEntity.FUEL).isEmpty()
                        && machine.burnLeft() == 1600 - 3 * DistillerBlockEntity.SERVING_TICKS,
                "one coal should have been lit and burnt for three servings, " + machine.burnLeft() + " ticks left");
        helper.succeed();
    }

    @GameTest
    public void aDryTubCondensesNothingAndBurnsNoFuel(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper, false);
        machine.pour(3, SALT);
        machine.setItem(DistillerBlockEntity.FUEL, new ItemStack(Items.COAL));

        tick(machine, DistillerBlockEntity.SERVING_TICKS * 2);

        TestFixtures.check(helper, machine.basinServings() == 0 && machine.boilerServings() == 3,
                "without coolant nothing should condense, the basin holds " + machine.basinServings());
        TestFixtures.check(helper, machine.getItem(DistillerBlockEntity.FUEL).getCount() == 1 && machine.burnLeft() == 0,
                "the coal should not be lit, " + machine.getItem(DistillerBlockEntity.FUEL));
        helper.succeed();
    }

    @GameTest
    public void aLavaBucketBurnsAndLeavesItsBucket(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper, true);
        machine.pour(1, SALT);
        machine.setItem(DistillerBlockEntity.FUEL, new ItemStack(Items.LAVA_BUCKET));

        machine.tick();

        TestFixtures.check(helper, machine.getItem(DistillerBlockEntity.FUEL).is(Items.BUCKET) && machine.burnLeft() == 20000 - 1,
                "the lava should be lit and leave an empty bucket, got " + machine.getItem(DistillerBlockEntity.FUEL)
                        + " with " + machine.burnLeft() + " ticks left");
        helper.succeed();
    }

    @GameTest
    public void withNothingToBoilTheFuelWaits(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper, true);
        machine.setItem(DistillerBlockEntity.FUEL, new ItemStack(Items.COAL));

        tick(machine, 100);

        TestFixtures.check(helper, machine.getItem(DistillerBlockEntity.FUEL).getCount() == 1 && machine.burnLeft() == 0,
                "an empty boiler should leave the coal unlit, " + machine.getItem(DistillerBlockEntity.FUEL));
        helper.succeed();
    }

    @GameTest
    public void aLitFireWaitsWhenTheBasinIsFull(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper, true);
        distil(machine, DistillerBlockEntity.TANK);
        machine.pour(1, WaterQuality.fresh(0));
        int left = machine.burnLeft();

        tick(machine, 50);

        TestFixtures.check(helper, machine.burnLeft() == left && machine.boilerServings() == 1,
                "with the basin full the fire should hold at " + left + ", got " + machine.burnLeft());
        helper.succeed();
    }

    @GameTest
    public void theBasinFillsEachContainer(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper, true);
        distil(machine, DistillerBlockEntity.TANK);

        ItemStack bucket = filledFrom(machine, new ItemStack(Items.BUCKET));
        ItemStack bottle = filledFrom(machine, new ItemStack(Items.GLASS_BOTTLE));
        ItemStack bowl = filledFrom(machine, new ItemStack(ThirstItems.TERRACOTTA_BOWL));
        ItemStack skin = filledFrom(machine, new ItemStack(ThirstItems.WATERSKIN));

        TestFixtures.check(helper, bucket.is(Items.WATER_BUCKET) && pure(bucket), "a bucket should come out Pure water, got " + bucket);
        TestFixtures.check(helper, bottle.is(Items.POTION) && WaterPurity.isWaterContainer(bottle) && pure(bottle),
                "a bottle should come out Pure water, got " + bottle);
        TestFixtures.check(helper, bowl.is(ThirstItems.TERRACOTTA_WATER_BOWL) && pure(bowl), "a bowl should come out Pure water, got " + bowl);
        TestFixtures.check(helper, WaterskinItem.servings(skin) == WaterskinItem.capacity(skin) && pure(skin),
                "a waterskin should come out full of Pure water, got " + WaterskinItem.servings(skin));
        TestFixtures.check(helper, machine.basinServings() == DistillerBlockEntity.TANK - 3 - 1 - 1 - WaterskinItem.capacity(skin),
                "each should have drawn its own servings, the basin holds " + machine.basinServings());
        helper.succeed();
    }

    @GameTest
    public void aBucketWaitsForThreeServingsInTheBasin(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper, true);
        distil(machine, 2);
        machine.setItem(DistillerBlockEntity.EMPTY_IN, new ItemStack(Items.BUCKET));

        machine.tick();

        TestFixtures.check(helper, machine.getItem(DistillerBlockEntity.FILLED_OUT).isEmpty() && machine.basinServings() == 2,
                "two servings should not fill a bucket, got " + machine.getItem(DistillerBlockEntity.FILLED_OUT));
        helper.succeed();
    }

    @GameTest
    public void eachSideTakesItsOwnSlots(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper, true);
        BlockState tub = helper.getLevel().getBlockState(helper.absolutePos(TUB));
        WorldlyContainer throughTub = ((DistillerBlock) tub.getBlock()).getContainer(tub, helper.getLevel(), helper.absolutePos(TUB));

        TestFixtures.check(helper, throughTub == machine, "the tub half should hand a hopper the boiler half's machine");
        TestFixtures.check(helper, machine.canPlaceItemThroughFace(DistillerBlockEntity.WATER_IN, new ItemStack(Items.WATER_BUCKET), Direction.UP)
                        && !machine.canPlaceItemThroughFace(DistillerBlockEntity.WATER_IN, new ItemStack(Items.BUCKET), Direction.UP),
                "the top should take water and only water");
        TestFixtures.check(helper, machine.canPlaceItemThroughFace(DistillerBlockEntity.FUEL, new ItemStack(Items.COAL), Direction.NORTH)
                        && machine.canPlaceItemThroughFace(DistillerBlockEntity.EMPTY_IN, new ItemStack(Items.GLASS_BOTTLE), Direction.EAST)
                        && !machine.canPlaceItemThroughFace(DistillerBlockEntity.FUEL, new ItemStack(Items.GLASS_BOTTLE), Direction.EAST),
                "a side should take fuel and empty containers, each into its own slot");
        TestFixtures.check(helper, machine.canTakeItemThroughFace(DistillerBlockEntity.FILLED_OUT, new ItemStack(Items.WATER_BUCKET), Direction.DOWN)
                        && machine.canTakeItemThroughFace(DistillerBlockEntity.WATER_IN, new ItemStack(Items.BUCKET), Direction.DOWN)
                        && !machine.canTakeItemThroughFace(DistillerBlockEntity.WATER_IN, new ItemStack(Items.WATER_BUCKET), Direction.DOWN)
                        && !machine.canTakeItemThroughFace(DistillerBlockEntity.FUEL, new ItemStack(Items.COAL), Direction.DOWN)
                        && !machine.canTakeItemThroughFace(DistillerBlockEntity.FILLED_OUT, new ItemStack(Items.WATER_BUCKET), Direction.NORTH),
                "the bottom should give what was made and what was spent, and nothing else, and no other side anything");
        helper.succeed();
    }

    @GameTest
    public void aHopperAboveTheTubFeedsTheBoiler(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper, true);
        helper.setBlock(TUB.above(), Blocks.HOPPER);
        HopperBlockEntity hopper = (HopperBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(TUB.above()));
        hopper.setItem(0, TestFixtures.waterBottle());

        helper.succeedWhen(() -> {
            machine.tick();
            TestFixtures.check(helper, machine.boilerServings() == 1,
                    "the hopper's bottle should have been poured into the boiler, it holds " + machine.boilerServings());
        });
    }

    @GameTest
    public void aHopperBelowTakesWhatWasMade(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper, true);
        helper.setBlock(BOILER.below(), Blocks.HOPPER);
        HopperBlockEntity hopper = (HopperBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(BOILER.below()));
        machine.setItem(DistillerBlockEntity.FILLED_OUT, new ItemStack(Items.WATER_BUCKET));

        helper.succeedWhen(() -> TestFixtures.check(helper, hopper.getItem(0).is(Items.WATER_BUCKET),
                "the hopper below should have taken the filled bucket, it holds " + hopper.getItem(0)));
    }

    @GameTest
    public void aBucketOnTheTubPoursIntoTheBoilerAndAnEmptyOneDraws(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper, true);
        distil(machine, 3);
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.WATER_BUCKET));

        use(helper, player, TUB);

        TestFixtures.check(helper, machine.boilerServings() == 3 && player.getItemInHand(InteractionHand.MAIN_HAND).is(Items.BUCKET),
                "a water bucket on the tub should pour into the boiler, it holds " + machine.boilerServings());

        use(helper, player, BOILER);

        ItemStack drawn = player.getItemInHand(InteractionHand.MAIN_HAND);
        TestFixtures.check(helper, drawn.is(Items.WATER_BUCKET) && pure(drawn) && machine.basinServings() == 0,
                "an empty bucket should draw the basin's 3 Pure servings, got " + drawn + " with "
                        + machine.basinServings() + " left");
        helper.succeed();
    }

    @GameTest
    public void breakingTheDistillerGivesBackItsSlots(GameTestHelper helper) {
        DistillerBlockEntity machine = machine(helper, true);
        machine.setItem(DistillerBlockEntity.FUEL, new ItemStack(Items.COAL, 5));
        machine.setItem(DistillerBlockEntity.EMPTY_IN, new ItemStack(Items.GLASS_BOTTLE, 2));
        ServerPlayer player = TestFixtures.survivalPlayer(helper);

        player.gameMode.destroyBlock(helper.absolutePos(TUB));

        TestFixtures.check(helper, dropped(helper, Items.COAL) == 5 && dropped(helper, Items.GLASS_BOTTLE) == 2
                        && dropped(helper, ThirstItems.COPPER_DISTILLER) == 1,
                "breaking it should drop the distiller, the coal and the bottles, dropped " + dropped(helper, Items.COAL)
                        + " coal and " + dropped(helper, Items.GLASS_BOTTLE) + " bottles");
        helper.succeed();
    }

    @GameTest
    public void joiningWithAPipeStartsTheMachine(GameTestHelper helper) {
        helper.setBlock(BOILER, ThirstBlocks.COPPER_DISTILLER.unpiped(Direction.NORTH));
        helper.setBlock(TUB, ThirstBlocks.COOLING_TUB.defaultBlockState().setValue(CoolingTubBlock.COOLED, true));
        TestFixtures.check(helper, helper.getLevel().getBlockEntity(helper.absolutePos(BOILER)) == null,
                "a boiler not yet piped should have no machine");
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ThirstItems.COPPER_PIPE));

        use(helper, player, TUB);

        TestFixtures.check(helper, helper.getLevel().getBlockEntity(helper.absolutePos(BOILER)) instanceof DistillerBlockEntity,
                "once piped the boiler half should hold the machine");
        TestFixtures.check(helper, helper.getLevel().getBlockEntity(helper.absolutePos(TUB)) == null,
                "the tub half should hold none of its own");
        helper.succeed();
    }

    /** A whole distiller facing north, its tub {@code cooled} or dry, and its machine. */
    private static DistillerBlockEntity machine(GameTestHelper helper, boolean cooled) {
        BlockState boiler = ThirstBlocks.COPPER_DISTILLER.defaultBlockState().setValue(DistillerBlock.FACING, Direction.NORTH);
        helper.setBlock(BOILER, boiler);
        helper.setBlock(TUB, boiler.setValue(DistillerBlock.PART, DistillerBlock.Part.TUB).setValue(DistillerBlock.COOLED, cooled));
        return (DistillerBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(BOILER));
    }

    /** Distils {@code servings} of fresh water into the basin, on coal of its own. */
    private static void distil(DistillerBlockEntity machine, int servings) {
        machine.pour(servings, WaterQuality.fresh(1));
        machine.setItem(DistillerBlockEntity.FUEL, new ItemStack(Items.COAL, 2));
        tick(machine, servings * DistillerBlockEntity.SERVING_TICKS);
    }

    /** Puts {@code empty} in the input, ticks once and takes out what was filled. */
    private static ItemStack filledFrom(DistillerBlockEntity machine, ItemStack empty) {
        machine.setItem(DistillerBlockEntity.EMPTY_IN, empty);
        machine.tick();
        return machine.removeItemNoUpdate(DistillerBlockEntity.FILLED_OUT);
    }

    private static void tick(DistillerBlockEntity machine, int ticks) {
        for (int i = 0; i < ticks; i++) machine.tick();
    }

    private static boolean pure(ItemStack stack) {
        return WaterQuality.fresh(WaterPurity.MAX).equals(WaterPurity.quality(stack));
    }

    private static int dropped(GameTestHelper helper, Item item) {
        AABB around = new AABB(helper.absolutePos(BOILER)).inflate(3.0);
        return helper.getLevel().getEntitiesOfClass(ItemEntity.class, around).stream()
                .filter(entity -> entity.getItem().is(item))
                .mapToInt(entity -> entity.getItem().getCount())
                .sum();
    }

    /** Uses the held item on top of the block at {@code pos} through the game mode, where both loaders fire their hooks. */
    private static void use(GameTestHelper helper, ServerPlayer player, BlockPos pos) {
        BlockPos absolute = helper.absolutePos(pos);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute).add(0.0, 0.5, 0.0), Direction.UP, absolute, false);
        player.gameMode.useItemOn(player, helper.getLevel(), player.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND, hit);
    }
}
