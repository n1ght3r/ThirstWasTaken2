package com.thirstwastaken2.gametest;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
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
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Drinking, end to end: starting to drink through the real right-click path, finishing, what is
 * left in the hand, what thirst it restores, and the advancements it earns.
 *
 * <p>This is the part 1.21.1 does differently. From 1.21.2 a drink is a data component; before it
 * {@code DrinkItem} overrides use, animation, duration and finishing itself. The same tests run on
 * both, so the two implementations are held to one behaviour.
 */
public final class DrinkingGameTest {
    /** A potion takes 32 ticks to drink, and the mod's drinks match it. */
    private static final int DRINK_TICKS = 32;

    @GameTest
    public void drinkingAWaterBowlRaisesThirstAndGivesTheBowlBack(GameTestHelper helper) {
        ServerPlayer player = thirstyPlayer(helper);
        ItemStack bowl = bowl(WaterQuality.fresh(WaterPurity.MAX));

        startDrinking(player, bowl);
        TestFixtures.check(helper, player.isUsingItem(), "a filled bowl should start being drunk");
        ItemStack left = player.getUseItem().finishUsingItem(player.level(), player);

        TestFixtures.check(helper, left.is(ThirstItems.TERRACOTTA_BOWL),
                "finishing a bowl should leave the empty terracotta bowl, got " + left);
        TestFixtures.check(helper, ThirstManager.get(player).thirst() > 10,
                "a purified bowl should restore thirst, got " + ThirstManager.get(player));
        helper.succeed();
    }

    @GameTest
    public void aWaterskinLosesOneServingPerDrinkAndStays(GameTestHelper helper) {
        ServerPlayer player = thirstyPlayer(helper);
        ItemStack skin = new ItemStack(ThirstItems.WATERSKIN);
        WaterskinItem.addWater(skin, WaterQuality.fresh(WaterPurity.MAX), com.thirstwastaken2.config.ThirstConfig.get().waterskinCapacity);

        startDrinking(player, skin);
        TestFixtures.check(helper, player.isUsingItem(), "a filled waterskin should start being drunk");
        ItemStack left = player.getUseItem().finishUsingItem(player.level(), player);

        int rest = com.thirstwastaken2.config.ThirstConfig.get().waterskinCapacity - 1;
        TestFixtures.check(helper, left.is(ThirstItems.WATERSKIN) && WaterskinItem.servings(left) == rest,
                "a drink should leave the waterskin with " + rest + " servings, got " + left);
        TestFixtures.check(helper, WaterPurity.quality(left).equals(WaterQuality.fresh(WaterPurity.MAX)),
                "the water left behind keeps its grade, got " + WaterPurity.quality(left));
        TestFixtures.check(helper, ThirstManager.get(player).thirst() > 10,
                "a waterskin drink should restore thirst, got " + ThirstManager.get(player));
        helper.succeed();
    }

    /**
     * Every carried container restores one serving of plain water, whatever its grade. The canteen and
     * the flask once restored nothing at any grade, when values were looked up per item and they had
     * no entry; murky water is where it was noticed.
     */
    @GameTest
    public void aCanteenOrFlaskDrinkRestoresLikeAWaterskin(GameTestHelper helper) {
        for (Item item : new Item[] {ThirstItems.WATERSKIN, ThirstItems.COPPER_CANTEEN, ThirstItems.IRON_FLASK}) {
            for (int grade : new int[] {WaterPurity.MAX, 1}) {
                ServerPlayer player = thirstyPlayer(helper);
                ItemStack vessel = new ItemStack(item);
                // Full, so it has no room: a vessel with room fills instead of drinking when the player
                // happens to face water, and the mock player stands at world spawn, where other tests'
                // water can be in reach. 1.20.1 on Forge failed this test that way now and then.
                WaterskinItem.addWater(vessel, WaterQuality.fresh(grade), WaterskinItem.capacity(vessel));

                startDrinking(player, vessel);
                player.getUseItem().finishUsingItem(player.level(), player);

                TestFixtures.check(helper, ThirstManager.get(player).thirst() == 16,
                        item + " of grade " + grade + " should restore a serving's 6 thirst, got "
                                + ThirstManager.get(player));
            }
        }
        helper.succeed();
    }

    @GameTest
    public void anEmptyWaterskinCannotBeDrunk(GameTestHelper helper) {
        ServerPlayer player = thirstyPlayer(helper);

        startDrinking(player, new ItemStack(ThirstItems.WATERSKIN));

        TestFixtures.check(helper, !player.isUsingItem(), "an empty waterskin has nothing to drink");
        helper.succeed();
    }

    @GameTest
    public void aDrinkLooksLikeOne(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        ItemStack skin = new ItemStack(ThirstItems.WATERSKIN);
        WaterskinItem.addWater(skin, WaterQuality.fresh(2), 1);

        for (ItemStack drink : new ItemStack[] {bowl(WaterQuality.fresh(2)), skin}) {
            TestFixtures.check(helper, drink.getUseAnimation().name().equals("DRINK"),
                    drink + " should use the drinking animation, got " + drink.getUseAnimation());
            //? if >=1.20.5 {
            int ticks = drink.getUseDuration(player);
            //?} else {
            /*int ticks = drink.getUseDuration();
            *///?}
            TestFixtures.check(helper, ticks == DRINK_TICKS,
                    drink + " should take " + DRINK_TICKS + " ticks like a potion, got " + ticks);
        }
        helper.succeed();
    }

    @GameTest
    public void plainWaterIsRefusedOnAFullBarButOtherDrinksAreNot(GameTestHelper helper) {
        // Murky: Clean and Pure may top quenched up at a full bar, which RecoveryGameTest covers.
        for (ItemStack water : new ItemStack[] {bowl(WaterQuality.fresh(1)), WaterPurity.set(TestFixtures.waterBottle(), 1)}) {
            ServerPlayer full = TestFixtures.survivalPlayer(helper);
            startDrinking(full, water);
            TestFixtures.check(helper, !full.isUsingItem(), water + " should be refused on a full bar");
        }

        ServerPlayer thirsty = thirstyPlayer(helper);
        startDrinking(thirsty, bowl(WaterQuality.fresh(2)));
        TestFixtures.check(helper, thirsty.isUsingItem(), "the same bowl should be drunk one point short of full");

        // Honey is a drink with a use of its own, so a full thirst bar must not stand in its way.
        ServerPlayer honey = TestFixtures.survivalPlayer(helper);
        startDrinking(honey, new ItemStack(Items.HONEY_BOTTLE));
        TestFixtures.check(helper, honey.isUsingItem(), "honey should still be drinkable on a full thirst bar");
        helper.succeed();
    }

    @GameTest
    public void drinkingEarnsTheAdvancementForWhatWasDrunk(GameTestHelper helper) {
        ServerPlayer player = thirstyPlayer(helper);

        ThirstManager.drinkItem(player, bowl(WaterQuality.fresh(WaterPurity.MIN)));
        TestFixtures.check(helper, earned(helper, player, "first_drink"), "any water should earn first_drink");
        TestFixtures.check(helper, earned(helper, player, "dirty_water"), "dirty water should earn dirty_water");
        TestFixtures.check(helper, !earned(helper, player, "purified_water") && !earned(helper, player, "sea_water"),
                "dirty water should earn neither purified_water nor sea_water");

        ThirstManager.drinkItem(player, bowl(WaterQuality.fresh(WaterPurity.MAX)));
        TestFixtures.check(helper, earned(helper, player, "purified_water"), "purified water should earn purified_water");

        ThirstManager.drinkItem(player, bowl(WaterQuality.SALT));
        TestFixtures.check(helper, earned(helper, player, "sea_water"), "salt water should earn sea_water");
        TestFixtures.check(helper, !earned(helper, player, "nether_drink"),
                "water drunk in the overworld should not earn nether_drink");
        helper.succeed();
    }

    @GameTest
    public void crouchingWithAnEmptyHandDrinksFromWater(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper);
        ServerPlayer player = thirstyPlayer(helper);
        player.setPose(Pose.CROUCHING);

        InteractionResult result = ThirstManager.drinkByHand(player, helper.getLevel(), InteractionHand.MAIN_HAND, aimAt(water));

        TestFixtures.check(helper, result != InteractionResult.PASS, "hand drinking should handle the click, got " + result);
        TestFixtures.check(helper, ThirstManager.get(player).thirst() == 13,
                "a sip should restore 3 thirst, got " + ThirstManager.get(player));

        // Looking down at water, the crosshair usually lands on the block under the surface; the water
        // on the face it hit still counts.
        ServerPlayer below = thirstyPlayer(helper);
        below.setPose(Pose.CROUCHING);
        ThirstManager.drinkByHand(below, helper.getLevel(), InteractionHand.MAIN_HAND, aimAt(water.below()));
        TestFixtures.check(helper, ThirstManager.get(below).thirst() > 10,
                "aiming at the block under the water should still drink it, got " + ThirstManager.get(below));
        helper.succeed();
    }

    @GameTest
    public void handDrinkingNeedsACrouchAnEmptyHandAndRoom(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper);

        ServerPlayer standing = thirstyPlayer(helper);
        refused(helper, standing, water, "a standing player");

        ServerPlayer holding = thirstyPlayer(helper);
        holding.setPose(Pose.CROUCHING);
        holding.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        refused(helper, holding, water, "a player holding something");

        ServerPlayer full = TestFixtures.survivalPlayer(helper);
        full.setPose(Pose.CROUCHING);
        refused(helper, full, water, "a player with a full bar");

        // The fixture's stone floor at (1, 1, 1) has air above it, not water.
        ServerPlayer dryLand = thirstyPlayer(helper);
        dryLand.setPose(Pose.CROUCHING);
        refused(helper, dryLand, helper.absolutePos(new BlockPos(1, 1, 1)), "a player aiming at dry stone");

        TestFixtures.withConfig(config -> config.canDrinkByHand = false, () -> {
            ServerPlayer disabled = thirstyPlayer(helper);
            disabled.setPose(Pose.CROUCHING);
            refused(helper, disabled, water, "a player on a server with can_drink_by_hand off");
        });
        helper.succeed();
    }

    @GameTest
    public void handDrinkingTakesOneSipPerClick(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper);

        // Vanilla sends a click it does not handle once per hand, main hand first.
        ServerPlayer bothEmpty = thirstyPlayer(helper);
        bothEmpty.setPose(Pose.CROUCHING);
        ThirstManager.drinkByHand(bothEmpty, helper.getLevel(), InteractionHand.MAIN_HAND, aimAt(water));
        refused(helper, bothEmpty, InteractionHand.OFF_HAND, water, "the off hand of a player whose main hand already drank");

        ServerPlayer holding = thirstyPlayer(helper);
        holding.setPose(Pose.CROUCHING);
        holding.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.STONE));
        ThirstManager.drinkByHand(holding, helper.getLevel(), InteractionHand.OFF_HAND, aimAt(water));
        TestFixtures.check(helper, ThirstManager.get(holding).thirst() > 10,
                "an empty off hand should drink while the main hand holds something, got " + ThirstManager.get(holding));

        helper.succeed();
    }

    private static void refused(GameTestHelper helper, ServerPlayer player, BlockPos target, String who) {
        refused(helper, player, InteractionHand.MAIN_HAND, target, who);
    }

    private static void refused(GameTestHelper helper, ServerPlayer player, InteractionHand hand, BlockPos target, String who) {
        ThirstData before = ThirstManager.get(player);
        InteractionResult result = ThirstManager.drinkByHand(player, helper.getLevel(), hand, aimAt(target));
        TestFixtures.check(helper, result == InteractionResult.PASS && ThirstManager.get(player).equals(before),
                who + " should not drink by hand, got " + result + " and " + ThirstManager.get(player));
    }

    /** A survival player ten points short of full, so drinking is allowed and visible. */
    private static ServerPlayer thirstyPlayer(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        ThirstManager.set(player, ThirstData.full().withLevels(10, 0));
        return player;
    }

    /** Puts the stack in the main hand and right-clicks with it through the server's own path. */
    private static void startDrinking(ServerPlayer player, ItemStack stack) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        player.gameMode.useItem(player, player.level(), player.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND);
    }

    private static BlockHitResult aimAt(BlockPos pos) {
        return new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false);
    }

    private static ItemStack bowl(WaterQuality quality) {
        return WaterPurity.setQuality(new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), quality);
    }

    private static boolean earned(GameTestHelper helper, ServerPlayer player, String name) {
        //? if >=1.20.5 {
        var advancement = helper.getLevel().getServer().getAdvancements().get(ThirstWasTaken2.id(name));
        //?} else {
        /*var advancement = helper.getLevel().getServer().getAdvancements().getAdvancement(ThirstWasTaken2.id(name));
        *///?}
        return advancement != null && player.getAdvancements().getOrStartProgress(advancement).isDone();
    }
}
