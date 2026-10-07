package com.thirstwastaken2.gametest;

import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.effect.ThirstEffects;
import com.thirstwastaken2.effect.UpsetStomach;
import com.thirstwastaken2.platform.Vanilla;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * Upset Stomach, bad water's common illness: it stops natural healing and cramps at random times, down
 * to half a heart. It does not drain thirst or cut the saturation food gives.
 *
 * <p>Players are ticked through {@code ThirstManager.tickPlayer} directly, so other tests' players do
 * not tick with them.
 */
public final class UpsetStomachGameTest {
    /** Several sync steps of baseline drain, so an extra drain would show. */
    private static final int DRAIN_TICKS = 200;
    /** A minute and a half: at least six cramps at level II on any difficulty. */
    private static final int HEALTH_TICKS = 1800;
    private static final int NUTRITION = 6;
    private static final float SATURATION_MODIFIER = 0.6F;

    /**
     * The players tick side by side, so the effect is all that differs. What is compared is everything
     * spent, whole points included, since the sync step holds part of the exhaustion back.
     */
    @GameTest
    public void upsetStomachDoesNotDrainThirst(GameTestHelper helper) {
        ServerPlayer control = quietPlayer(helper);
        ServerPlayer verySick = quietPlayer(helper);
        verySick.addEffect(Vanilla.effectInstance(ThirstEffects.UPSET_STOMACH, DRAIN_TICKS * 2, 1));
        ThirstData start = ThirstManager.get(control);

        for (int i = 0; i < DRAIN_TICKS; i++) {
            ThirstManager.tickPlayer(control);
            ThirstManager.tickPlayer(verySick);
        }

        float none = spent(start, ThirstManager.get(control));
        float two = spent(start, ThirstManager.get(verySick));
        TestFixtures.check(helper, Math.abs(two - none) < 0.01F,
                "Upset Stomach should not drain thirst, spent " + two + " against " + none + " without it");
        helper.succeed();
    }

    /** With Upset Stomach no longer draining, Nausea's own drain is charged as it is without it. */
    @GameTest
    public void nauseaDrainsOnTopOfUpsetStomach(GameTestHelper helper) {
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
        TestFixtures.check(helper, withNausea > plain,
                "Nausea during Upset Stomach should still drain, spent " + withNausea + " against " + plain);
        helper.succeed();
    }

    @GameTest
    public void upsetStomachLeavesSaturationWhole(GameTestHelper helper) {
        float none = saturationFromMeal(helper, -1);
        float two = saturationFromMeal(helper, 1);

        TestFixtures.check(helper, Math.abs(two - none) < 0.01F,
                "Upset Stomach should not cut saturation, got " + two + " against " + none);
        helper.succeed();
    }

    /** The wait before a cramp, rolled by hand: each difficulty's shortest and longest, level II's shorter. */
    @GameTest
    public void crampsWaitByDifficultyAndLevel(GameTestHelper helper) {
        Difficulty[] difficulties = {Difficulty.PEACEFUL, Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD};
        int[][] bounds = {{200, 300}, {120, 300}, {60, 240}, {10, 160}};
        for (int d = 0; d < difficulties.length; d++) {
            int shortest = UpsetStomach.waitTicks(difficulties[d], 0, 0.0F);
            int longest = UpsetStomach.waitTicks(difficulties[d], 0, 0.9999F);
            TestFixtures.check(helper, shortest == bounds[d][0] && longest == bounds[d][1],
                    difficulties[d] + " should wait " + bounds[d][0] + " to " + bounds[d][1] + " ticks, got "
                            + shortest + " to " + longest);
            int severe = UpsetStomach.waitTicks(difficulties[d], 1, 0.9999F);
            TestFixtures.check(helper, severe == Math.max(UpsetStomach.MIN_WAIT, bounds[d][1] * 3 / 4),
                    difficulties[d] + " level II should wait three quarters as long, got " + severe);
        }
        TestFixtures.check(helper, UpsetStomach.waitTicks(Difficulty.HARD, 1, 0.0F) == UpsetStomach.MIN_WAIT,
                "no wait should be shorter than half a second");
        helper.succeed();
    }

    /** Like Poison, a cramp takes health down to half a heart and never further. */
    @GameTest
    public void crampsStopAtHalfAHeart(GameTestHelper helper) {
        float[][] cases = {{20.0F, 1.0F}, {2.0F, 1.0F}, {1.5F, 0.5F}, {1.0F, 0.0F}};
        for (float[] c : cases) {
            float damage = UpsetStomach.cramp(c[0]);
            TestFixtures.check(helper, damage == c[1],
                    "at " + c[0] + " health a cramp should take " + c[1] + ", took " + damage);
        }
        helper.succeed();
    }

    /**
     * The worst level, ticked long enough for many cramps on the test world's difficulty: the first comes
     * a whole wait after the effect starts, each lands only when its countdown runs out, health drops and
     * stops at half a heart, and without the effect nothing waits.
     *
     * <p>Vanilla will not hurt a mock player (it never finishes loading in, and reports creative), so the
     * test takes each cramp's damage off itself; that the hit lands in game is a manual check.
     */
    @GameTest
    public void upsetStomachCrampsDownToHalfAHeart(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        Difficulty difficulty = player.level().getDifficulty();
        player.setHealth(4.0F);
        player.addEffect(Vanilla.effectInstance(ThirstEffects.UPSET_STOMACH, HEALTH_TICKS * 2, 1));

        UpsetStomach.Step first = UpsetStomach.step(player, 0);
        TestFixtures.check(helper, first.damage() == 0.0F && first.countdown() >= UpsetStomach.MIN_WAIT,
                "the first cramp should wait, got " + first);
        int countdown = first.countdown();
        int cramps = 0;
        for (int i = 0; i < HEALTH_TICKS; i++) {
            UpsetStomach.Step step = UpsetStomach.step(player, countdown);
            boolean due = countdown == 1;
            TestFixtures.check(helper, step.damage() == 0.0F || due,
                    "a cramp should land only when its countdown runs out, one landed at " + countdown);
            TestFixtures.check(helper, step.countdown() >= 1 && step.countdown() <= UpsetStomach.MAX_WAIT,
                    "the next wait should be half a second to 15 seconds, got " + step.countdown());
            if (due) cramps++;
            player.setHealth(player.getHealth() - step.damage());
            countdown = step.countdown();
        }

        TestFixtures.check(helper, cramps >= HEALTH_TICKS / UpsetStomach.MAX_WAIT,
                "Upset Stomach II should cramp at least every 15 seconds on " + difficulty + ", cramped " + cramps + " times");
        TestFixtures.check(helper, player.getHealth() == UpsetStomach.MIN_HEALTH,
                "cramps should take health down to half a heart and stop, went to " + player.getHealth());
        player.removeAllEffects();
        TestFixtures.check(helper, UpsetStomach.step(player, 1).equals(new UpsetStomach.Step(0, 0.0F)),
                "without Upset Stomach no cramp should land or wait");
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
     * at -1, ticking {@code FoodData} once first as a player's would.
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
