package com.thirstwastaken2.gametest;

import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.effect.ThirstEffects;
import com.thirstwastaken2.platform.Vanilla;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Upset Stomach, bad water's common illness: it drains thirst faster, more at II, cuts the saturation
 * food gives, and never hurts on its own.
 *
 * <p>Players are ticked through {@code ThirstManager.tickPlayer} directly, so other tests' players do
 * not tick with them.
 */
public final class UpsetStomachGameTest {
    /** Long enough that level II's extra drain over I is several sync steps, whatever the climate. */
    private static final int DRAIN_TICKS = 200;
    private static final int HEALTH_TICKS = 400;
    private static final int NUTRITION = 6;
    private static final float SATURATION_MODIFIER = 0.6F;
    private static final float HURT_HEALTH = 10.0F;

    /**
     * The players tick side by side, so the effect's own drain is all that differs. What is compared is
     * everything spent, whole points included, since the sync step holds part of the exhaustion back.
     */
    @GameTest
    public void upsetStomachDrainsThirstByLevel(GameTestHelper helper) {
        ServerPlayer control = quietPlayer(helper);
        ServerPlayer sick = quietPlayer(helper);
        ServerPlayer verySick = quietPlayer(helper);
        sick.addEffect(Vanilla.effectInstance(ThirstEffects.UPSET_STOMACH, DRAIN_TICKS * 2, 0));
        verySick.addEffect(Vanilla.effectInstance(ThirstEffects.UPSET_STOMACH, DRAIN_TICKS * 2, 1));
        ThirstData start = ThirstManager.get(control);

        for (int i = 0; i < DRAIN_TICKS; i++) {
            ThirstManager.tickPlayer(control);
            ThirstManager.tickPlayer(sick);
            ThirstManager.tickPlayer(verySick);
        }

        float none = spent(start, ThirstManager.get(control));
        float one = spent(start, ThirstManager.get(sick));
        float two = spent(start, ThirstManager.get(verySick));
        TestFixtures.check(helper, one > none,
                "Upset Stomach should drain thirst faster, spent " + one + " against " + none + " without it");
        TestFixtures.check(helper, two > one,
                "Upset Stomach II should drain faster than I, spent " + two + " against " + one);
        helper.succeed();
    }

    /** Upset Stomach's own drain stands for being ill, so Nausea is not charged on top of it. */
    @GameTest
    public void nauseaCostsNothingOnTopOfUpsetStomach(GameTestHelper helper) {
        ServerPlayer sick = quietPlayer(helper);
        ServerPlayer bursting = quietPlayer(helper);
        sick.addEffect(Vanilla.effectInstance(ThirstEffects.UPSET_STOMACH, DRAIN_TICKS * 2, 0));
        bursting.addEffect(Vanilla.effectInstance(ThirstEffects.UPSET_STOMACH, DRAIN_TICKS * 2, 0));
        bursting.addEffect(new MobEffectInstance(MobEffects.NAUSEA, DRAIN_TICKS * 2, 0));
        ThirstData start = ThirstManager.get(sick);

        for (int i = 0; i < DRAIN_TICKS; i++) {
            ThirstManager.tickPlayer(sick);
            ThirstManager.tickPlayer(bursting);
        }

        float plain = spent(start, ThirstManager.get(sick));
        float withNausea = spent(start, ThirstManager.get(bursting));
        TestFixtures.check(helper, Math.abs(withNausea - plain) < 0.01F,
                "Nausea during Upset Stomach should cost nothing extra, spent " + withNausea + " against " + plain);
        helper.succeed();
    }

    @GameTest
    public void upsetStomachCutsSaturationByLevel(GameTestHelper helper) {
        float none = saturationFromMeal(helper, -1);
        float one = saturationFromMeal(helper, 0);
        float two = saturationFromMeal(helper, 1);

        TestFixtures.check(helper, Math.abs(one - none * 0.5F) < 0.01F,
                "Upset Stomach I should give half the saturation, got " + one + " against " + none);
        TestFixtures.check(helper, Math.abs(two - none * 0.25F) < 0.01F,
                "Upset Stomach II should give a quarter of the saturation, got " + two + " against " + none);
        helper.succeed();
    }

    /** A full bar and the worst level, rolled as often as the tick allows: health must not move. */
    @GameTest
    public void upsetStomachNeverHurts(GameTestHelper helper) {
        // A full bar heals from quenched, which would hide a loss of health, so that heal is off here.
        TestFixtures.withConfig(config -> config.quenchedHealthRegen = 0.0, () -> {
            ServerPlayer player = TestFixtures.survivalPlayer(helper);
            player.setHealth(HURT_HEALTH);
            player.addEffect(Vanilla.effectInstance(ThirstEffects.UPSET_STOMACH, HEALTH_TICKS * 2, 1));

            for (int i = 0; i < HEALTH_TICKS; i++) ThirstManager.tickPlayer(player);

            TestFixtures.check(helper, player.getHealth() == HURT_HEALTH,
                    "Upset Stomach must never hurt on its own, health went to " + player.getHealth());
        });
        helper.succeed();
    }

    /** A survival player whose tick count is off the slow tick, where peaceful's refill would land. */
    private static ServerPlayer quietPlayer(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        player.tickCount = 1;
        return player;
    }

    /** Everything drained since {@code start}, in exhaustion: whole points spent plus what is left over. */
    private static float spent(ThirstData start, ThirstData now) {
        int points = start.thirst() + start.quenched() - now.thirst() - now.quenched();
        return points * ThirstData.EXHAUSTION_PER_POINT + now.exhaustion() - start.exhaustion();
    }

    /**
     * The saturation one meal gives a player with Upset Stomach at {@code amplifier}, or without it
     * at -1. {@code FoodData} reads the effect on its tick, so it ticks once before the meal.
     */
    private static float saturationFromMeal(GameTestHelper helper, int amplifier) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        if (amplifier >= 0) {
            player.addEffect(Vanilla.effectInstance(ThirstEffects.UPSET_STOMACH, 200, amplifier));
        }
        player.getFoodData().setFoodLevel(10);
        player.getFoodData().setSaturation(0.0F);
        player.getFoodData().tick(player);
        float before = player.getFoodData().getSaturationLevel();
        player.getFoodData().eat(NUTRITION, SATURATION_MODIFIER);
        return player.getFoodData().getSaturationLevel() - before;
    }
}
