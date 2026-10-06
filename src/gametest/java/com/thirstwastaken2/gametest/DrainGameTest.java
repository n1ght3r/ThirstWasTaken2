package com.thirstwastaken2.gametest;

import com.thirstwastaken2.api.ThirstEvents;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.effect.ThirstEffects;
import com.thirstwastaken2.platform.Vanilla;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * The drain since the purification rework, in its three parts: a baseline of one point a minute that
 * every survival player spends, the activity vanilla charges, both scaled by the climate, and illness,
 * which nothing scales. Over 1200 ticks, a minute, in plains: a temperate, rainy biome whose factor is
 * 0.875, times the default 1.2, so 1.05.
 */
public final class DrainGameTest {
    private static final int MINUTE = 1200;
    /** Plains: 0.8 + 0.25 * (0.8 - 0.5), rainy, times thirstDepletionModifier's default 1.2. */
    private static final float PLAINS = 1.2F * 0.875F;
    /**
     * What a reading may fall short by: the tick only writes exhaustion once it crosses a quarter-point
     * sync step, and carries the rest unwritten, so the state read back can lag the drain by a step.
     */
    private static final float TOLERANCE = 0.25F;

    @GameTest
    public void theClimateCurveNeverFallsAsItWarms(GameTestHelper helper) {
        for (boolean wet : new boolean[] {true, false}) {
            float previous = ThirstManager.climateFactor(-1.0F, wet);
            for (float temperature = -0.95F; temperature <= 2.5F; temperature += 0.05F) {
                float factor = ThirstManager.climateFactor(temperature, wet);
                TestFixtures.check(helper, factor >= previous - 1.0E-6F,
                        "the climate factor should not fall from " + previous + " to " + factor + " at " + temperature);
                TestFixtures.check(helper, factor - previous < 0.02F,
                        "the climate factor should have no step, jumped " + (factor - previous) + " at " + temperature);
                previous = factor;
            }
        }
        TestFixtures.check(helper, ThirstManager.climateFactor(0.8F, false) > ThirstManager.climateFactor(0.8F, true),
                "a dry biome should drain more than a wet one at the same temperature");
        helper.succeed();
    }

    @GameTest
    public void standingStillCostsAPointAMinute(GameTestHelper helper) {
        ServerPlayer player = inPlains(helper);
        TestFixtures.setState(player, 20, 20, 20, 20.0F, null);
        tick(player, MINUTE);
        float spent = spent(ThirstManager.get(player));
        TestFixtures.check(helper, Math.abs(spent - 4.0F * PLAINS) < TOLERANCE,
                "a minute standing in plains should spend " + 4.0F * PLAINS + " exhaustion, got " + spent);
        helper.succeed();
    }

    @GameTest
    public void upsetStomachDrainsFourOrEightPointsAMinuteWhateverTheClimate(GameTestHelper helper) {
        BlockPos water = TestFixtures.water(helper);
        for (int amplifier = 0; amplifier <= 1; amplifier++) {
            ServerPlayer player = inPlains(helper, water);
            TestFixtures.setState(player, 20, 20, 20, 20.0F,
                    Vanilla.effectInstance(ThirstEffects.UPSET_STOMACH, 2 * MINUTE, amplifier));
            tick(player, MINUTE);
            float illness = spent(ThirstManager.get(player)) - 4.0F * PLAINS;
            float expected = 16.0F * (amplifier + 1);
            TestFixtures.check(helper, Math.abs(illness - expected) < TOLERANCE,
                    "Upset Stomach " + (amplifier + 1) + " should add " + expected + " exhaustion a minute, got " + illness);
        }
        helper.succeed();
    }

    @GameTest
    public void activityIsScaledByTheClimate(GameTestHelper helper) {
        ServerPlayer player = inPlains(helper);
        TestFixtures.setState(player, 20, 20, 20, 20.0F, null);
        for (int tick = 0; tick < MINUTE; tick++) {
            player.tickCount++;
            // Four exhaustion over the minute, the way vanilla charges it, a little every tick.
            ThirstManager.mirrorExhaustion(player, 4.0F / MINUTE);
            ThirstManager.tickPlayer(player);
        }
        float spent = spent(ThirstManager.get(player));
        TestFixtures.check(helper, Math.abs(spent - 8.0F * PLAINS) < TOLERANCE,
                "four of activity and the baseline's four should both be scaled to " + 8.0F * PLAINS + ", got " + spent);
        helper.succeed();
    }

    @GameTest
    public void theGlobalSpeedScalesTheBaseline(GameTestHelper helper) {
        TestFixtures.withConfig(config -> config.thirstDepletionModifier = 2.4, () -> {
            ServerPlayer player = inPlains(helper);
            TestFixtures.setState(player, 20, 20, 20, 20.0F, null);
            tick(player, MINUTE);
            float spent = spent(ThirstManager.get(player));
            TestFixtures.check(helper, Math.abs(spent - 8.0F * PLAINS) < TOLERANCE,
                    "doubling the speed should double the baseline to " + 8.0F * PLAINS + ", got " + spent);
        });
        helper.succeed();
    }

    /** A listener sees the activity and the illness, and never the baseline, which is nothing the player did. */
    @GameTest
    public void listenersDoNotSeeTheBaseline(GameTestHelper helper) {
        ServerPlayer player = inPlains(helper);
        float[] seen = new float[1];
        ThirstEvents.EXHAUSTION.register((who, amount) -> {
            if (who == player) seen[0] += amount;
            return amount;
        });
        TestFixtures.setState(player, 20, 20, 20, 20.0F, null);
        tick(player, 20);
        TestFixtures.check(helper, seen[0] == 0.0F, "a player doing nothing should give a listener nothing, got " + seen[0]);

        player.addEffect(Vanilla.effectInstance(ThirstEffects.UPSET_STOMACH, MINUTE, 0));
        tick(player, 20);
        TestFixtures.check(helper, seen[0] > 0.0F, "illness should reach a listener");
        helper.succeed();
    }

    /** A survival player standing in the plains patch the water fixture makes. */
    private static ServerPlayer inPlains(GameTestHelper helper) {
        return inPlains(helper, TestFixtures.water(helper));
    }

    /** The same, on a fixture already made: making it twice in one test finds nothing left to change. */
    private static ServerPlayer inPlains(GameTestHelper helper, BlockPos water) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        player.snapTo(water.getX() + 0.5, water.getY() + 1.0, water.getZ() + 0.5, 0.0F, 0.0F);
        return player;
    }

    private static void tick(ServerPlayer player, int ticks) {
        for (int tick = 0; tick < ticks; tick++) {
            player.tickCount++;
            ThirstManager.tickPlayer(player);
        }
    }

    /** The exhaustion spent since a full bar and a full reserve. */
    private static float spent(ThirstData data) {
        return (2 * ThirstData.MAX - data.thirst() - data.quenched()) * ThirstData.EXHAUSTION_PER_POINT
                + data.exhaustion();
    }
}
