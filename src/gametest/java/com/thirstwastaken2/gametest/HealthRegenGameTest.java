package com.thirstwastaken2.gametest;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.HealthRegen;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.effect.ThirstEffects;
import com.thirstwastaken2.platform.Vanilla;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.food.FoodData;

/**
 * Natural healing. Food heals, on both of vanilla's paths, while the thirst bar is at
 * {@code foodHealMinThirstPercent} or more and there is no Upset Stomach, and a withheld heal refunds
 * the food it would have cost. Quenched heals on its own, once saturation is spent, with a full thirst
 * bar, the food bar at {@code quenchedHealMinFoodPercent} or more and no Upset Stomach.
 *
 * <p>The refund is the half that is easy to lose: vanilla spends exhaustion for every point it heals,
 * so withholding a heal without giving that back drains hunger for a heal the player never received.
 * {@code aHydratedPlayerStillRegenerates} is the positive control, without which the refund test would
 * pass even if regeneration never triggered at all.
 */
public final class HealthRegenGameTest {
    private static final float START_HEALTH = 10.0F;
    /** Vanilla's saturation heal comes every 10 ticks, so 10 ticks from a fresh timer are one heal. */
    private static final int ONE_SATURATION_HEAL = 10;
    /** Vanilla's hunger heal comes every 80 ticks. */
    private static final int ONE_HUNGER_HEAL = 80;
    /** Quenched heals on the same beat as saturation. */
    private static final int ONE_QUENCHED_HEAL = 10;

    @GameTest
    public void foodHealNeedsHalfTheThirstBarAndNoIllness(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        Object[][] cases = {
                {9, 0, false, true}, {10, 0, false, false}, {20, 0, false, false},
                {19, 5, false, false}, {20, 6, true, true}};
        for (Object[] c : cases) {
            TestFixtures.setState(player, (int) c[0], (int) c[1], 20, START_HEALTH, (boolean) c[2] ? upset(0) : null);
            TestFixtures.check(helper, HealthRegen.blocksFoodHeal(player) == (boolean) c[3],
                    "thirst " + c[0] + ", quenched " + c[1] + (((boolean) c[2]) ? " with" : " without")
                            + " Upset Stomach should " + (((boolean) c[3]) ? "" : "not ") + "block food healing");
        }
        helper.succeed();
    }

    @GameTest
    public void theThirstShareFoodNeedsIsASetting(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        TestFixtures.withConfig(config -> config.foodHealMinThirstPercent = 100, () -> {
            TestFixtures.setState(player, 19, 5, 20, START_HEALTH, null);
            TestFixtures.check(helper, HealthRegen.blocksFoodHeal(player), "at 100% thirst 19 should block food healing");
            TestFixtures.setState(player, 20, 0, 20, START_HEALTH, null);
            TestFixtures.check(helper, !HealthRegen.blocksFoodHeal(player), "at 100% a full bar should let food heal");
        });
        TestFixtures.withConfig(config -> config.foodHealMinThirstPercent = 0, () -> {
            TestFixtures.setState(player, 0, 0, 20, START_HEALTH, null);
            TestFixtures.check(helper, !HealthRegen.blocksFoodHeal(player), "at 0% an empty bar should let food heal");
        });
        helper.succeed();
    }

    @GameTest
    public void dehydrationsSwitchLeavesUpsetStomachsGate(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        TestFixtures.withConfig(config -> config.dehydrationHaltsHealthRegen = false, () -> {
            TestFixtures.setState(player, 2, 0, 20, START_HEALTH, null);
            TestFixtures.check(helper, !HealthRegen.blocksFoodHeal(player),
                    "with dehydration's gate off, thirst should not block healing");
            TestFixtures.setState(player, 2, 0, 20, START_HEALTH, upset(0));
            TestFixtures.check(helper, HealthRegen.blocksFoodHeal(player),
                    "Upset Stomach should still block healing with only dehydration's gate off");
        });
        helper.succeed();
    }

    @GameTest
    public void aBlockedSaturationHealIsRefunded(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        TestFixtures.setState(player, 9, 5, 20, START_HEALTH, null);
        FoodData food = player.getFoodData();
        float saturationBefore = food.getSaturationLevel();
        for (int i = 0; i < 4 * ONE_SATURATION_HEAL; i++) food.tick(player);

        TestFixtures.check(helper, player.getHealth() == START_HEALTH,
                "thirst 9 must not regenerate, health went to " + player.getHealth());
        TestFixtures.check(helper, food.getFoodLevel() == 20 && food.getSaturationLevel() == saturationBefore,
                "the withheld heals must be refunded, food went to " + food.getFoodLevel() + "/"
                        + food.getSaturationLevel());
        helper.succeed();
    }

    /** The hunger path heals at 18 food and no saturation, every 80 ticks; it is gated and refunded too. */
    @GameTest
    public void theHungerHealIsGatedAndRefundedToo(GameTestHelper helper) {
        ServerPlayer blocked = TestFixtures.survivalPlayer(helper);
        TestFixtures.setState(blocked, 20, 3, 18, START_HEALTH, upset(0));
        blocked.getFoodData().setSaturation(0.0F);
        for (int i = 0; i < ONE_HUNGER_HEAL; i++) blocked.getFoodData().tick(blocked);
        TestFixtures.check(helper, blocked.getHealth() == START_HEALTH && blocked.getFoodData().getFoodLevel() == 18,
                "Upset Stomach should stop the hunger heal and refund it, got health " + blocked.getHealth()
                        + " and food " + blocked.getFoodData().getFoodLevel());

        ServerPlayer allowed = TestFixtures.survivalPlayer(helper);
        TestFixtures.setState(allowed, 12, 3, 18, START_HEALTH, null);
        allowed.getFoodData().setSaturation(0.0F);
        for (int i = 0; i < ONE_HUNGER_HEAL; i++) allowed.getFoodData().tick(allowed);
        TestFixtures.check(helper, allowed.getHealth() == START_HEALTH + 1.0F,
                "a player at 12 thirst and 18 food should take the hunger heal's 1 health, got " + allowed.getHealth());
        helper.succeed();
    }

    @GameTest
    public void aHydratedPlayerStillRegenerates(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        TestFixtures.setState(player, 10, 0, 20, START_HEALTH, null);
        for (int i = 0; i < 4 * ONE_SATURATION_HEAL; i++) player.getFoodData().tick(player);
        TestFixtures.check(helper, player.getHealth() > START_HEALTH,
                "a player at half thirst with full food must still regenerate, health stayed at " + player.getHealth());
        helper.succeed();
    }

    /** With six quenched at the default half speed, one heal draws 3 exhaustion and restores half a point. */
    @GameTest
    public void quenchedHealsOnceSaturationIsSpentAndPaysForIt(GameTestHelper helper) {
        ServerPlayer player = quenchedHealer(helper, 20, 6, 20, null);
        for (int i = 0; i < ONE_QUENCHED_HEAL; i++) ThirstManager.tickPlayer(player);
        float share = (float) ThirstConfig.get().quenchedHealthRegen;
        TestFixtures.check(helper, share == 0.5F, "quenched should heal at half saturation's speed by default, got " + share);
        TestFixtures.check(helper, Math.abs(player.getHealth() - (START_HEALTH + 0.5F)) < 1.0E-4F,
                "one quenched heal should restore half a point, got " + (player.getHealth() - START_HEALTH));
        float exhaustion = spent(ThirstManager.get(player), 6);
        // The ticks add a little baseline drain on top of the cost.
        TestFixtures.check(helper, exhaustion >= 3.0F - 1.0E-4F && exhaustion < 3.1F,
                "the heal should cost 3 exhaustion, got " + exhaustion);
        helper.succeed();
    }

    @GameTest
    public void saturationHealsFirst(GameTestHelper helper) {
        ServerPlayer player = quenchedHealer(helper, 20, 6, 20, null);
        player.getFoodData().setSaturation(2.0F);
        for (int i = 0; i < 3 * ONE_QUENCHED_HEAL; i++) ThirstManager.tickPlayer(player);
        TestFixtures.check(helper, player.getHealth() == START_HEALTH,
                "quenched should not heal while saturation is left, got " + (player.getHealth() - START_HEALTH));
        helper.succeed();
    }

    @GameTest
    public void quenchedNeedsAFullBarHalfTheFoodBarAndNoIllness(GameTestHelper helper) {
        Object[][] cases = {
                {19, 20, false, false}, {20, 9, false, false}, {20, 10, false, true}, {20, 20, true, false}};
        for (Object[] c : cases) {
            ServerPlayer player = quenchedHealer(helper, (int) c[0], 6, (int) c[1], (boolean) c[2] ? upset(0) : null);
            for (int i = 0; i < ONE_QUENCHED_HEAL; i++) ThirstManager.tickPlayer(player);
            boolean healed = player.getHealth() > START_HEALTH;
            TestFixtures.check(helper, healed == (boolean) c[3],
                    "thirst " + c[0] + ", food " + c[1] + (((boolean) c[2]) ? " with" : " without")
                            + " Upset Stomach: quenched should " + (((boolean) c[3]) ? "" : "not ") + "heal");
        }
        TestFixtures.withConfig(config -> config.quenchedHealMinFoodPercent = 100, () -> {
            ServerPlayer player = quenchedHealer(helper, 20, 6, 19, null);
            for (int i = 0; i < ONE_QUENCHED_HEAL; i++) ThirstManager.tickPlayer(player);
            TestFixtures.check(helper, player.getHealth() == START_HEALTH, "at 100% food 19 should stop quenched healing");
        });
        TestFixtures.withConfig(config -> config.quenchedHealthRegen = 0.0, () -> {
            ServerPlayer player = quenchedHealer(helper, 20, 6, 20, null);
            for (int i = 0; i < ONE_QUENCHED_HEAL; i++) ThirstManager.tickPlayer(player);
            TestFixtures.check(helper, player.getHealth() == START_HEALTH, "0% should turn quenched healing off");
        });
        helper.succeed();
    }

    /** A hurt player with no saturation, ready for quenched to heal them. */
    private static ServerPlayer quenchedHealer(GameTestHelper helper, int thirst, int quenched, int food,
                                               MobEffectInstance effect) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        TestFixtures.setState(player, thirst, quenched, food, START_HEALTH, effect);
        player.getFoodData().setSaturation(0.0F);
        return player;
    }

    /** The exhaustion a state has spent since it held {@code startQuenched} quenched at a full bar. */
    private static float spent(ThirstData data, int startQuenched) {
        return (ThirstData.MAX - data.thirst() + startQuenched - data.quenched()) * ThirstData.EXHAUSTION_PER_POINT
                + data.exhaustion();
    }

    private static MobEffectInstance upset(int amplifier) {
        return Vanilla.effectInstance(ThirstEffects.UPSET_STOMACH, 20 * 60, amplifier);
    }
}
