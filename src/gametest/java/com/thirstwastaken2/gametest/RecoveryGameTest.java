package com.thirstwastaken2.gametest;

import com.thirstwastaken2.api.ThirstApi;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.effect.ThirstEffects;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import com.thirstwastaken2.tooltip.ThirstTooltip;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * What a drink restores since the purification rework: one serving of plain water is the same 6 and 4
 * from any container, its quenched cut by the grade, thirst past a full bar is lost from every source,
 * and Clean or Pure water may be drunk at a full bar to build quenched.
 */
public final class RecoveryGameTest {
    /** A water bottle and an Awkward Potion are one item; the lookup must tell them apart, in any order. */
    @GameTest
    public void waterAndPotionsAreToldApartPerStack(GameTestHelper helper) {
        ThirstConfig config = ThirstConfig.get();
        ItemStack water = WaterPurity.set(Vanilla.waterBottle(), 2);
        ItemStack awkward = Vanilla.awkwardPotion();
        for (int round = 0; round < 2; round++) {
            TestFixtures.check(helper, ThirstApi.thirstValues(awkward) == config.drinks.get("minecraft:potion"),
                    "an Awkward Potion should keep the potion's own value");
            TestFixtures.check(helper, ThirstApi.thirstValues(water) == config.plainWaterValue,
                    "a water bottle should restore the plain water serving, after an Awkward Potion too");
        }
        helper.succeed();
    }

    @GameTest
    public void everyContainerRestoresTheSameServing(GameTestHelper helper) {
        int[] serving = ThirstConfig.get().plainWaterValue;
        TestFixtures.check(helper, serving[0] == 6 && serving[1] == 4,
                "a serving of plain water should be 6 and 4 by default, got " + serving[0] + " and " + serving[1]);
        for (Item item : new Item[] {ThirstItems.WATERSKIN, ThirstItems.COPPER_CANTEEN, ThirstItems.IRON_FLASK}) {
            ItemStack vessel = new ItemStack(item);
            WaterskinItem.addWater(vessel, WaterQuality.fresh(2), 1);
            TestFixtures.check(helper, ThirstApi.thirstValues(vessel) == serving, item + " should restore a serving");
        }
        TestFixtures.check(helper, ThirstApi.thirstValues(bowl(2)) == serving, "the bowl should restore a serving");
        TestFixtures.check(helper, ThirstApi.thirstValues(WaterPurity.set(Vanilla.waterBottle(), 2)) == serving,
                "a water bottle should restore a serving");
        helper.succeed();
    }

    /** Dirty 6 + 0, Murky 6 + 1, Clean 6 + 2, Pure 6 + 4: the grade cuts only the quenched, rounding down. */
    @GameTest
    public void eachGradeGivesItsShareOfQuenched(GameTestHelper helper) {
        int[] quenched = {0, 1, 2, 4};
        withoutSickness(() -> {
            for (int grade = WaterPurity.MIN; grade <= WaterPurity.MAX; grade++) {
                ServerPlayer player = TestFixtures.survivalPlayer(helper);
                TestFixtures.setState(player, 10, 0, 20, 20.0F, null);
                ThirstManager.drinkItem(player, bowl(grade));
                ThirstData data = ThirstManager.get(player);
                TestFixtures.check(helper, data.thirst() == 16 && data.quenched() == quenched[grade],
                        "grade " + grade + " should give 6 thirst and " + quenched[grade] + " quenched, got " + data);
            }
        });
        helper.succeed();
    }

    /** Thirst past 20 is thrown away rather than turned into quenched, for water and for food alike. */
    @GameTest
    public void thirstOverAFullBarIsLost(GameTestHelper helper) {
        int[][] cases = {{18, 0, 20, 2}, {19, 5, 20, 7}, {20, 5, 20, 7}};
        withoutSickness(() -> {
            for (int[] c : cases) {
                ServerPlayer player = TestFixtures.survivalPlayer(helper);
                TestFixtures.setState(player, c[0], c[1], 20, 20.0F, null);
                ThirstManager.drinkItem(player, bowl(2));
                ThirstData data = ThirstManager.get(player);
                TestFixtures.check(helper, data.thirst() == c[2] && data.quenched() == c[3],
                        "Clean water at " + c[0] + "/" + c[1] + " should end at " + c[2] + "/" + c[3] + ", got " + data);
            }
        });
        ThirstData melon = new ThirstData(19, 0, 0.0F, true).drink(2, 0);
        TestFixtures.check(helper, melon.thirst() == 20 && melon.quenched() == 0,
                "food at 19 should not turn its extra thirst into quenched, got " + melon);
        helper.succeed();
    }

    @GameTest
    public void quenchedNeverPassesThirst(GameTestHelper helper) {
        ThirstData data = new ThirstData(2, 0, 0.0F, true).drink(1, 10);
        TestFixtures.check(helper, data.thirst() == 3 && data.quenched() == 3,
                "quenched should stop at thirst, got " + data);
        helper.succeed();
    }

    /**
     * At a full bar only Clean and Pure water may be drunk, to build quenched, and not once quenched is
     * full. Dirty and Murky water follow vanilla's food rule.
     */
    @GameTest
    public void onlyTreatedWaterTopsUpAFullBar(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        for (int grade = WaterPurity.MIN; grade <= WaterPurity.MAX; grade++) {
            TestFixtures.setState(player, 20, 5, 20, 20.0F, null);
            boolean allowed = ThirstManager.canDrinkWater(player, bowl(grade));
            TestFixtures.check(helper, allowed == (grade >= WaterPurity.BOILED),
                    "grade " + grade + " at a full bar should " + (grade >= WaterPurity.BOILED ? "" : "not ")
                            + "be drinkable");
        }
        TestFixtures.check(helper, !ThirstManager.canDrinkWater(player, bowl(WaterQuality.SALT)),
                "sea water should never top a full bar up");
        TestFixtures.setState(player, 20, 20, 20, 20.0F, null);
        TestFixtures.check(helper, !ThirstManager.canDrinkWater(player, bowl(WaterPurity.MAX)),
                "nothing should be drunk once quenched is full too");

        TestFixtures.setState(player, 20, 5, 20, 20.0F, null);
        player.setItemInHand(InteractionHand.MAIN_HAND, bowl(WaterPurity.MAX));
        player.gameMode.useItem(player, player.level(), player.getItemInHand(InteractionHand.MAIN_HAND),
                InteractionHand.MAIN_HAND);
        TestFixtures.check(helper, player.isUsingItem(), "Pure water should start being drunk at a full bar");
        player.getUseItem().finishUsingItem(player.level(), player);
        TestFixtures.check(helper, ThirstManager.get(player).quenched() == 9,
                "the top-up should add Pure's 4 quenched, got " + ThirstManager.get(player));
        helper.succeed();
    }

    /** The droplet rows a player sees say what drinking would give them now, cap and illness included. */
    @GameTest
    public void tooltipRowsShowTheActualGain(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        ThirstTooltip.setViewer(() -> player);
        try {
            // A full bar with room for 2 more quenched: no thirst row at all, and 2 of Pure's 4 quenched.
            TestFixtures.setState(player, 20, 18, 20, 20.0F, null);
            java.util.List<net.minecraft.network.chat.Component> lines = new java.util.ArrayList<>();
            ThirstTooltip.appendTo(bowl(WaterPurity.MAX), lines::add, true);
            String quenched = ThirstTooltip.quenched(2, com.thirstwastaken2.compat.AppleSkin.quenchedOverlay()).getString();
            String anyThirst = ThirstTooltip.thirst(1).getString().substring(0, 1);
            TestFixtures.check(helper, lines.stream().noneMatch(line -> line.getString().startsWith(anyThirst)),
                    "at a full bar there should be no thirst row, got " + lines);
            TestFixtures.check(helper, lines.stream().anyMatch(line -> line.getString().equals(quenched)),
                    "only the 2 quenched that fit should show, got " + lines);

            TestFixtures.setState(player, 10, 0, 20, 20.0F,
                    Vanilla.effectInstance(ThirstEffects.UPSET_STOMACH, 200, 0));
            lines.clear();
            ThirstTooltip.appendTo(bowl(WaterPurity.MAX), lines::add, true);
            int cut = (int) (4 * com.thirstwastaken2.effect.UpsetStomach.saturationScale(player));
            String reduced = ThirstTooltip.quenched(cut, com.thirstwastaken2.compat.AppleSkin.quenchedOverlay()).getString();
            TestFixtures.check(helper, cut < 4 && lines.stream().anyMatch(line -> line.getString().equals(reduced)),
                    "Upset Stomach should cut Pure's 4 quenched to " + cut + " in the row, got " + lines);
        } finally {
            ThirstTooltip.setViewer(() -> null);
        }
        helper.succeed();
    }

    /** Runs {@code checks} with no sickness table, so no rolled Upset Stomach cuts the quenched a drink gives. */
    private static void withoutSickness(Runnable checks) {
        TestFixtures.withConfig(config -> config.sicknessEffects = WaterSicknessGameTest.everyDifficulty(java.util.List.of()),
                checks);
    }

    private static ItemStack bowl(int grade) {
        return bowl(WaterQuality.fresh(grade));
    }

    private static ItemStack bowl(WaterQuality quality) {
        return WaterPurity.setQuality(new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), quality);
    }
}
