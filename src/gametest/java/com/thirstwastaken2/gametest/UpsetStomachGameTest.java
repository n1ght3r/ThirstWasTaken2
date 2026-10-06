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
 * Upset Stomach, bad water's common illness: it stops natural healing and cramps now and then, never
 * below a floor. It does not drain thirst or cut the saturation food gives.
 *
 * <p>Players are ticked through {@code ThirstManager.tickPlayer} directly, so other tests' players do
 * not tick with them.
 */
public final class UpsetStomachGameTest {
    /** Several sync steps of baseline drain, so an extra drain would show. */
    private static final int DRAIN_TICKS = 200;
    /** Thirty rolls, enough that level II cramps on any difficulty but Peaceful. */
    private static final int HEALTH_TICKS = 30 * UpsetStomach.INTERVAL;
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

    /** The table, rolled by hand: each difficulty and level cramps under its chance and not at it. */
    @GameTest
    public void crampsRollByDifficultyAndLevel(GameTestHelper helper) {
        Difficulty[] difficulties = {Difficulty.EASY, Difficulty.NORMAL, Difficulty.HARD};
        float[][] chances = {{0.15F, 0.25F}, {0.25F, 0.45F}, {0.40F, 0.60F}};
        for (int d = 0; d < difficulties.length; d++) {
            for (int amplifier = 0; amplifier <= 1; amplifier++) {
                float chance = chances[d][amplifier];
                float under = UpsetStomach.cramp(difficulties[d], amplifier, 20.0F, chance - 0.01F);
                float at = UpsetStomach.cramp(difficulties[d], amplifier, 20.0F, chance);
                TestFixtures.check(helper, under == UpsetStomach.DAMAGE && at == 0.0F,
                        difficulties[d] + " level " + (amplifier + 1) + " should cramp under " + chance
                                + " and not at it, got " + under + " and " + at);
            }
        }
        TestFixtures.check(helper, UpsetStomach.cramp(Difficulty.PEACEFUL, 1, 20.0F, 0.0F) == 0.0F,
                "Peaceful should never cramp");
        helper.succeed();
    }

    /** A cramp never takes the player under the difficulty's floor, and never kills. */
    @GameTest
    public void crampsStopAtTheFloor(GameTestHelper helper) {
        float[][] cases = {
                {10.0F, 10.5F, 0.5F}, {10.0F, 10.0F, 0.0F},
                {4.0F, 4.5F, 0.5F}, {4.0F, 3.0F, 0.0F},
                {1.0F, 1.5F, 0.5F}, {1.0F, 1.0F, 0.0F}};
        Difficulty[] difficulties = {Difficulty.EASY, Difficulty.EASY, Difficulty.NORMAL, Difficulty.NORMAL,
                Difficulty.HARD, Difficulty.HARD};
        for (int i = 0; i < cases.length; i++) {
            float floor = UpsetStomach.floor(difficulties[i]);
            float damage = UpsetStomach.cramp(difficulties[i], 1, cases[i][1], 0.0F);
            TestFixtures.check(helper, floor == cases[i][0] && damage == cases[i][2],
                    difficulties[i] + " at " + cases[i][1] + " health should take " + cases[i][2]
                            + " above a floor of " + cases[i][0] + ", took " + damage + " above " + floor);
        }
        helper.succeed();
    }

    /**
     * The worst level, rolled every tick for long enough to cramp many times on the test world's
     * difficulty: cramps land only on the beat, health drops, and stays at the floor or above.
     *
     * <p>Vanilla will not hurt a mock player (it never finishes loading in, and reports creative), so the
     * test takes each roll's damage off itself; that the hit lands in game is a manual check.
     */
    @GameTest
    public void upsetStomachHurtsButNeverBelowTheFloor(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        Difficulty difficulty = player.level().getDifficulty();
        float floor = UpsetStomach.floor(difficulty);
        float start = floor + 3.0F;
        player.setHealth(start);
        player.addEffect(Vanilla.effectInstance(ThirstEffects.UPSET_STOMACH, HEALTH_TICKS * 2, 1));

        for (int i = 0; i < HEALTH_TICKS; i++) {
            player.tickCount++;
            float damage = UpsetStomach.crampNow(player);
            TestFixtures.check(helper, damage == 0.0F || player.tickCount % UpsetStomach.INTERVAL == 0,
                    "a cramp should only roll every " + UpsetStomach.INTERVAL + " ticks, one landed on " + player.tickCount);
            player.setHealth(player.getHealth() - damage);
        }

        TestFixtures.check(helper, player.getHealth() >= floor,
                "Upset Stomach must stop at " + floor + " health on " + difficulty + ", went to " + player.getHealth());
        if (difficulty != Difficulty.PEACEFUL) {
            TestFixtures.check(helper, player.getHealth() < start,
                    "Upset Stomach II should cramp in " + HEALTH_TICKS + " ticks on " + difficulty);
        }
        player.removeAllEffects();
        TestFixtures.check(helper, UpsetStomach.crampNow(player) == 0.0F,
                "without Upset Stomach there should be no cramp, even on the beat");
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
