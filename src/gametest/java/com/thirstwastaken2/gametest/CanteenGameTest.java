package com.thirstwastaken2.gametest;

import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterContainers;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * The copper canteen and the iron flask: their capacity, boiling on a campfire through the real use
 * path, and which of them a furnace takes. Boiling is driven by calling the use path once per step, the
 * way a held right click repeats it.
 */
public final class CanteenGameTest {
    private static final BlockPos CAMPFIRE = new BlockPos(2, 1, 2);
    private static final WaterQuality DIRTY = WaterQuality.fresh(WaterPurity.MIN);
    private static final WaterQuality PURE = WaterQuality.fresh(WaterPurity.MAX);

    @GameTest
    public void eachVesselHoldsItsOwnCapacity(GameTestHelper helper) {
        checkCapacity(helper, ThirstItems.WATERSKIN, 3);
        checkCapacity(helper, ThirstItems.COPPER_CANTEEN, 4);
        checkCapacity(helper, ThirstItems.IRON_FLASK, 6);
        helper.succeed();
    }

    @GameTest
    public void aRigidVesselKeepsOneSprite(GameTestHelper helper) {
        ItemStack canteen = filled(ThirstItems.COPPER_CANTEEN, DIRTY, 2);
        TestFixtures.check(helper, Vanilla.modelSelectorOf(canteen) == null,
                "a canteen has one sprite, so it should carry no custom model data, got "
                        + Vanilla.modelSelectorOf(canteen));
        TestFixtures.check(helper, Vanilla.modelSelectorOf(filled(ThirstItems.WATERSKIN, DIRTY, 2)) != null,
                "the waterskin should still pick its sprite by fill level");
        helper.succeed();
    }

    @GameTest
    public void aCanteenBoilsPureOverALitCampfire(GameTestHelper helper) {
        ServerPlayer player = playerAtCampfire(helper, Blocks.CAMPFIRE.defaultBlockState(),
                filled(ThirstItems.COPPER_CANTEEN, DIRTY, 2));
        int steps = 2 * canteenBoilTicks() / WaterskinItem.BOIL_STEP_TICKS;

        use(helper, player, steps - 1);
        TestFixtures.check(helper, WaterPurity.quality(held(player)).equals(DIRTY),
                "one step short of the boil the water should still be dirty, got " + WaterPurity.quality(held(player)));
        use(helper, player, 1);
        TestFixtures.check(helper, WaterPurity.quality(held(player)).equals(PURE),
                "a full boil should leave the canteen Purified, got " + WaterPurity.quality(held(player)));
        TestFixtures.check(helper, WaterskinItem.servings(held(player)) == 2,
                "boiling must not change how much water there is, got " + WaterskinItem.servings(held(player)));
        helper.succeed();
    }

    @GameTest
    public void copperBoilsFasterThanIron(GameTestHelper helper) {
        TestFixtures.check(helper, canteenBoilTicks() < flaskBoilTicks(),
                "copper carries heat better, so a canteen serving should boil sooner than a flask one");
        ServerPlayer player = playerAtCampfire(helper, Blocks.SOUL_CAMPFIRE.defaultBlockState(),
                filled(ThirstItems.IRON_FLASK, DIRTY, 1));
        use(helper, player, flaskBoilTicks() / WaterskinItem.BOIL_STEP_TICKS);
        TestFixtures.check(helper, WaterPurity.quality(held(player)).equals(PURE),
                "a flask should boil over a soul campfire too, got " + WaterPurity.quality(held(player)));
        helper.succeed();
    }

    @GameTest
    public void stoppingKeepsTheProgressAndMoreWaterResetsIt(GameTestHelper helper) {
        ServerPlayer player = playerAtCampfire(helper, Blocks.CAMPFIRE.defaultBlockState(),
                filled(ThirstItems.IRON_FLASK, DIRTY, 3));
        use(helper, player, 5);
        int kept = WaterskinItem.boilProgress(player, held(player));
        TestFixtures.check(helper, kept == 5 * WaterskinItem.BOIL_STEP_TICKS,
                "five steps should be kept between uses, got " + kept + " ticks");

        WaterskinItem.addWater(held(player), DIRTY, 1);
        use(helper, player, 1);
        int restarted = WaterskinItem.boilProgress(player, held(player));
        TestFixtures.check(helper, restarted == WaterskinItem.BOIL_STEP_TICKS,
                "water poured in has not boiled, so the count should restart, got " + restarted + " ticks");
        helper.succeed();
    }

    @GameTest
    public void saltEmptyAndUnlitDoNotBoil(GameTestHelper helper) {
        ServerPlayer salty = playerAtCampfire(helper, Blocks.CAMPFIRE.defaultBlockState(),
                filled(ThirstItems.COPPER_CANTEEN, WaterQuality.SALT, 4));
        use(helper, salty, 4 * canteenBoilTicks() / WaterskinItem.BOIL_STEP_TICKS);
        TestFixtures.check(helper, WaterPurity.isSalty(held(salty)), "boiling must not take the salt out");

        ServerPlayer unlit = playerAtCampfire(helper, Blocks.CAMPFIRE.defaultBlockState().setValue(CampfireBlock.LIT, false),
                filled(ThirstItems.COPPER_CANTEEN, DIRTY, 1));
        use(helper, unlit, canteenBoilTicks() / WaterskinItem.BOIL_STEP_TICKS);
        TestFixtures.check(helper, WaterPurity.quality(held(unlit)).equals(DIRTY),
                "an unlit campfire should not boil anything, got " + WaterPurity.quality(held(unlit)));
        TestFixtures.check(helper, WaterskinItem.boilProgress(unlit, held(unlit)) == 0,
                "an unlit campfire should not count toward a boil");

        ServerPlayer skin = playerAtCampfire(helper, Blocks.CAMPFIRE.defaultBlockState(),
                filled(ThirstItems.WATERSKIN, DIRTY, 3));
        use(helper, skin, 3 * flaskBoilTicks() / WaterskinItem.BOIL_STEP_TICKS);
        TestFixtures.check(helper, WaterPurity.quality(held(skin)).equals(DIRTY),
                "a leather waterskin cannot go on the fire, got " + WaterPurity.quality(held(skin)));
        helper.succeed();
    }

    @GameTest
    public void onlyTheFlaskGoesInAFurnace(GameTestHelper helper) {
        for (int servings = 1; servings <= WaterskinItem.MAX_CAPACITY; servings++) {
            ItemStack flask = filled(ThirstItems.IRON_FLASK, DIRTY, servings);
            ItemStack result = TestFixtures.cook(helper, RecipeType.SMELTING, flask);
            TestFixtures.check(helper, result.is(ThirstItems.IRON_FLASK) && WaterskinItem.servings(result) == servings
                            && WaterPurity.quality(result).equals(WaterQuality.fresh(2)),
                    "a furnace should raise a flask of " + servings + " dirty servings two grades and keep them, got "
                            + result + " holding " + WaterskinItem.servings(result) + " of " + WaterPurity.quality(result));
        }
        TestFixtures.check(helper, TestFixtures.cook(helper, RecipeType.SMELTING,
                        filled(ThirstItems.IRON_FLASK, WaterQuality.SALT, 2)).isEmpty(),
                "salt water in a flask should not smelt");
        TestFixtures.check(helper, TestFixtures.cook(helper, RecipeType.SMELTING,
                        filled(ThirstItems.COPPER_CANTEEN, DIRTY, 2)).isEmpty(),
                "the copper canteen has no furnace recipe");
        TestFixtures.check(helper, TestFixtures.cook(helper, RecipeType.CAMPFIRE_COOKING,
                        filled(ThirstItems.IRON_FLASK, DIRTY, 2)).isEmpty(),
                "a campfire recipe would put the flask in the campfire's slots instead of boiling it in hand");
        helper.succeed();
    }

    @GameTest
    public void bothAreCraftedFromScratch(GameTestHelper helper) {
        ItemStack copper = new ItemStack(Items.COPPER_INGOT);
        ItemStack iron = new ItemStack(Items.IRON_INGOT);
        ItemStack canteen = TestFixtures.craftGrid(helper, 3, 3, grid(new ItemStack(Items.LEATHER), copper));
        ItemStack flask = TestFixtures.craftGrid(helper, 3, 3, grid(new ItemStack(Items.IRON_NUGGET), iron));
        TestFixtures.check(helper, canteen.is(ThirstItems.COPPER_CANTEEN), "leather over five copper should make a canteen, got " + canteen);
        TestFixtures.check(helper, flask.is(ThirstItems.IRON_FLASK), "a nugget over five iron should make a flask, got " + flask);
        TestFixtures.check(helper, WaterContainers.handles(canteen) && WaterContainers.capacity(flask) == 6,
                "both should be fluid containers, the flask of six servings");
        helper.succeed();
    }

    private static void checkCapacity(GameTestHelper helper, Item item, int capacity) {
        ItemStack stack = new ItemStack(item);
        WaterskinItem.addWater(stack, DIRTY, 99);
        TestFixtures.check(helper, WaterskinItem.servings(stack) == capacity && WaterskinItem.capacity(stack) == capacity,
                item + " should hold exactly " + capacity + ", got " + WaterskinItem.servings(stack));
        TestFixtures.check(helper, !WaterskinItem.addWater(stack, DIRTY, 1), item + " should refuse water when full");
    }

    private static ItemStack filled(Item item, WaterQuality quality, int servings) {
        ItemStack stack = new ItemStack(item);
        WaterskinItem.addWater(stack, quality, servings);
        return stack;
    }

    /** A U of {@code metal} under {@code top}, the shape both recipes share. */
    private static List<ItemStack> grid(ItemStack top, ItemStack metal) {
        return new ArrayList<>(List.of(
                ItemStack.EMPTY, top.copy(), ItemStack.EMPTY,
                metal.copy(), ItemStack.EMPTY, metal.copy(),
                metal.copy(), metal.copy(), metal.copy()));
    }

    private static ServerPlayer playerAtCampfire(GameTestHelper helper, BlockState campfire, ItemStack stack) {
        helper.setBlock(CAMPFIRE, campfire);
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        return player;
    }

    private static ItemStack held(ServerPlayer player) {
        return player.getItemInHand(InteractionHand.MAIN_HAND);
    }

    /** Uses the held item on the campfire {@code times}, as a held right click repeats it. */
    private static void use(GameTestHelper helper, ServerPlayer player, int times) {
        BlockPos pos = helper.absolutePos(CAMPFIRE);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
        for (int i = 0; i < times; i++) {
            player.gameMode.useItemOn(player, helper.getLevel(), held(player), InteractionHand.MAIN_HAND, hit);
        }
    }

    /** Ticks a serving takes to boil in the copper canteen, from the config. */
    private static int canteenBoilTicks() {
        return WaterskinItem.boilTicksPerServing(new ItemStack(ThirstItems.COPPER_CANTEEN));
    }

    /** Ticks a serving takes to boil in the iron flask, from the config. */
    private static int flaskBoilTicks() {
        return WaterskinItem.boilTicksPerServing(new ItemStack(ThirstItems.IRON_FLASK));
    }
}
