package com.thirstwastaken2.gametest;

import com.thirstwastaken2.config.SicknessEffect;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.effect.ThirstEffects;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.platform.ItemWaterData;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import com.thirstwastaken2.platform.Vanilla;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.List;

/**
 * What drinking water of a given quality does to the player, whatever the roll: Dirty water never
 * leaving Hunger or Parched, Pure water never doing anything, and salt water. The roll itself, forced into each
 * range, is {@code WaterSicknessGameTest}.
 */
public final class WaterEffectsGameTest {
    @GameTest
    public void saltWaterCausesNauseaAndDoesNotHydrate(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        ItemStack salty = bowl(WaterQuality.SALT);

        boolean hydrates = WaterPurity.applyEffects(player, salty);

        TestFixtures.check(helper, !hydrates, "salt water must not grant hydration");
        TestFixtures.check(helper, player.hasEffect(MobEffects.NAUSEA), "salt water should cause nausea");
        MobEffectInstance parched = Vanilla.getEffect(player, ThirstEffects.PARCHED);
        TestFixtures.check(helper, parched != null && parched.getAmplifier() == 1,
                "salt water should make the player Parched II, got " + parched);
        TestFixtures.check(helper, !parched.isVisible() && parched.showIcon(),
                "Parched from a drink has no particles but keeps its icon, got " + parched);
        helper.succeed();
    }

    @GameTest
    public void dirtyWaterGivesNoTasteHungerOrParched(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.mockPlayer(helper);

        boolean hydrates = WaterPurity.applyEffects(player, bowl(WaterQuality.fresh(0)));

        TestFixtures.check(helper, hydrates, "every drink of fresh water quenches, dirty water included");
        // The taste of Nausea every Dirty drink used to leave is gone: the illness carries the cost.
        TestFixtures.check(helper, !player.hasEffect(MobEffects.NAUSEA),
                "dirty water should no longer leave a taste of Nausea, got " + player.getEffect(MobEffects.NAUSEA));
        TestFixtures.check(helper, !player.hasEffect(MobEffects.HUNGER),
                "bad water makes the player ill, not hungry, so it must not apply hunger");
        TestFixtures.check(helper, !Vanilla.hasEffect(player, ThirstEffects.PARCHED),
                "only sea water makes the player Parched, bad fresh water must not");
        helper.succeed();
    }

    @GameTest
    public void purifiedWaterHasNoSideEffects(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.mockPlayer(helper);

        boolean hydrates = WaterPurity.applyEffects(player, bowl(WaterQuality.fresh(3)));

        TestFixtures.check(helper, hydrates, "purified water must grant hydration");
        TestFixtures.check(helper, !player.hasEffect(MobEffects.NAUSEA) && !player.hasEffect(MobEffects.POISON)
                        && !Vanilla.hasEffect(player, ThirstEffects.PARCHED),
                "purity 3 has no nausea, parched or poison chance in the default config");
        helper.succeed();
    }

    /** Bad water fills the bar but gives little quenched, the way rotten flesh gives little saturation. */
    @GameTest
    public void quenchedFollowsTheGrade(GameTestHelper helper) {
        int[] percent = {0, 25, 50, 100};
        for (int grade = WaterPurity.MIN; grade <= WaterPurity.MAX; grade++) {
            int expected = 8 * percent[grade] / 100;
            int got = WaterPurity.quenched(WaterQuality.fresh(grade), 8);
            TestFixtures.check(helper, got == expected,
                    "grade " + grade + " should give " + percent[grade] + "% of 8 quenched, " + expected + ", got " + got);
        }
        // No sickness at all, so the drink rolls nothing.
        TestFixtures.withConfig(config -> config.sicknessEffects = WaterSicknessGameTest.everyDifficulty(List.of()), () -> {
            ServerPlayer dirty = TestFixtures.mockPlayer(helper);
            ThirstManager.set(dirty, ThirstManager.get(dirty).withLevels(4, 0));
            ThirstManager.drinkItem(dirty, bowl(WaterQuality.fresh(0)));
            TestFixtures.check(helper, ThirstManager.get(dirty).thirst() > 4
                            && ThirstManager.get(dirty).quenched() == 0,
                    "a dirty bowl should restore thirst but no quenched, got " + ThirstManager.get(dirty));
        });
        helper.succeed();
    }

    @GameTest
    public void upsetStomachLeavesQuenchedWhole(GameTestHelper helper) {
        ServerPlayer healthy = TestFixtures.mockPlayer(helper);
        ServerPlayer sick = TestFixtures.mockPlayer(helper);
        ThirstManager.set(healthy, ThirstManager.get(healthy).withLevels(4, 0));
        ThirstManager.set(sick, ThirstManager.get(sick).withLevels(4, 0));
        sick.addEffect(Vanilla.effectInstance(ThirstEffects.UPSET_STOMACH, 200, 1));

        ThirstManager.drinkItem(healthy, bowl(WaterQuality.fresh(WaterPurity.MAX)));
        ThirstManager.drinkItem(sick, bowl(WaterQuality.fresh(WaterPurity.MAX)));

        int full = ThirstManager.get(healthy).quenched();
        int got = ThirstManager.get(sick).quenched();
        TestFixtures.check(helper, full > 0 && got == full,
                "Upset Stomach II should leave a pure drink's quenched whole, got " + got + " against " + full);
        helper.succeed();
    }

    /**
     * Milk and honey are the two drinks vanilla has that are not water, and both used to be worth
     * nothing. Their values live in the config, which merges them into files written before they
     * existed, so this asks the API rather than the defaults.
     */
    @GameTest
    public void milkAndHoneyQuenchThirst(GameTestHelper helper) {
        for (ItemStack drink : List.of(new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.HONEY_BOTTLE))) {
            ServerPlayer player = TestFixtures.mockPlayer(helper);
            ThirstManager.set(player, ThirstManager.get(player).withLevels(4, 0));

            ThirstManager.drinkItem(player, drink);

            int thirst = ThirstManager.get(player).thirst();
            TestFixtures.check(helper, thirst > 4,
                    drink.getItem() + " should raise thirst above 4, got " + thirst);
        }
        helper.succeed();
    }

    /**
     * Salt water has no grade, so there is nothing for boiling to raise. It must come back out of
     * the fire as salt water, without having picked up a grade on the way.
     */
    @GameTest
    public void boilingCannotDesalinate(GameTestHelper helper) {
        ItemStack salty = bowl(WaterQuality.SALT);

        WaterPurity.purify(salty, WaterPurity.MAX);

        TestFixtures.check(helper, WaterPurity.quality(salty) instanceof WaterQuality.Salt,
                "purifying salt water must leave it salt water, got " + WaterPurity.quality(salty));
        TestFixtures.check(helper, !ItemWaterData.hasGrade(salty),
                "salt water must not end up carrying a grade");
        helper.succeed();
    }

    private static ItemStack bowl(WaterQuality quality) {
        return WaterPurity.setQuality(new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), quality);
    }
}
