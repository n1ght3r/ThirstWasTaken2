package com.thirstwastaken2.gametest;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.effect.ThirstEffects;
import com.thirstwastaken2.platform.Vanilla;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.GameType;

/**
 * What thirst does to the player itself, which is everything {@code PlayerMixin} hooks.
 *
 * <p>Both hooks only take effect on top of a vanilla decision, so each test sets up the state where
 * vanilla would say yes before checking that the mod says no.
 *
 * <p>Mirrored exhaustion is collected during the tick and applied by {@code ThirstManager.tick}, so
 * the exhaustion tests run that by hand, the same way {@code CauldronGameTest} drains its queue.
 */
public final class PlayerStateGameTest {
    /** Vanilla gates sprinting on food above 6, and the mod applies the same cut-off to thirst. */
    private static final int SPRINT_CUTOFF = 6;
    private static final float EXHAUSTION = 4.0F;
    /** What {@code HungerMobEffect#applyEffectTick} charges per amplifier level, every tick. */
    private static final float HUNGER_EXHAUSTION = 0.005F;
    /** Three of these stay inside one quarter-point sync step under any climate modifier the test world has. */
    private static final float SMALL_EXHAUSTION = 0.02F;
    private static final int PARCHED_TICKS = 100;

    @GameTest
    public void sprintingIsBlockedWhenThirsty(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);

        setThirst(player, SPRINT_CUTOFF + 1);
        TestFixtures.check(helper, TestFixtures.canSprint(player),
                "thirst " + (SPRINT_CUTOFF + 1) + " is above the cut-off and should allow sprinting");

        setThirst(player, SPRINT_CUTOFF);
        TestFixtures.check(helper, !TestFixtures.canSprint(player),
                "thirst " + SPRINT_CUTOFF + " should block sprinting");
        helper.succeed();
    }

    @GameTest
    public void sprintingIsAllowedWhenTheSettingIsOff(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        setThirst(player, SPRINT_CUTOFF);

        ThirstConfig config = ThirstConfig.get();
        boolean original = config.preventSprintingWhenThirsty;
        try {
            config.preventSprintingWhenThirsty = false;
            TestFixtures.check(helper, TestFixtures.canSprint(player),
                    "prevent_sprinting_when_thirsty is off, so thirst should not block sprinting");
        } finally {
            config.preventSprintingWhenThirsty = original;
        }
        helper.succeed();
    }

    @GameTest
    public void disabledThirstNeverBlocksSprinting(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);

        setThirst(player, SPRINT_CUTOFF);
        ThirstManager.set(player, ThirstManager.get(player).withEnabled(false));

        TestFixtures.check(helper, TestFixtures.canSprint(player),
                "a player with thirst disabled should sprint regardless of the value");
        helper.succeed();
    }

    @GameTest
    public void hungerExhaustionMirrorsIntoThirst(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        ThirstData before = ThirstManager.get(player);

        player.causeFoodExhaustion(EXHAUSTION);
        TestFixtures.check(helper, ThirstManager.get(player).equals(before),
                "mirrored exhaustion should wait for the tick, so it costs one sync packet at most, "
                        + "but thirst already moved to " + ThirstManager.get(player));

        ThirstManager.tick(helper.getLevel().getServer());
        ThirstData after = ThirstManager.get(player);
        TestFixtures.check(helper, after.exhaustion() > before.exhaustion() || after.quenched() < before.quenched(),
                "spending hunger should also spend thirst, got " + after);
        helper.succeed();
    }

    @GameTest
    public void hungerEffectDoesNotDehydrate(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        int amplifier = 1;
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 200, amplifier));
        ThirstData before = ThirstManager.get(player);

        // Charge exactly what the effect charges in one tick; the tick has to cancel it back out.
        player.causeFoodExhaustion(HUNGER_EXHAUSTION * (amplifier + 1));
        ThirstManager.tick(helper.getLevel().getServer());

        TestFixtures.check(helper, ThirstManager.get(player).equals(before),
                "the Hunger effect should not dehydrate, thirst moved to " + ThirstManager.get(player));
        helper.succeed();
    }

    /**
     * Parched is a marker, so the drain lives in the tick, not the effect. Ticked directly rather than
     * through {@code ThirstManager.tick}, so that other tests' players do not tick with it. A hundred
     * ticks spend a point or two of exhaustion, well past a sync step and short of a whole thirst point.
     */
    @GameTest
    public void parchedDrainsThirstByLevel(GameTestHelper helper) {
        ServerPlayer control = survivalPlayer(helper);
        ServerPlayer parched = survivalPlayer(helper);
        ServerPlayer veryParched = survivalPlayer(helper);
        parched.addEffect(Vanilla.effectInstance(ThirstEffects.PARCHED, 200, 0));
        veryParched.addEffect(Vanilla.effectInstance(ThirstEffects.PARCHED, 200, 1));

        for (int i = 0; i < PARCHED_TICKS; i++) {
            ThirstManager.tickPlayer(control);
            ThirstManager.tickPlayer(parched);
            ThirstManager.tickPlayer(veryParched);
        }

        float none = ThirstManager.get(control).exhaustion();
        float one = ThirstManager.get(parched).exhaustion();
        float two = ThirstManager.get(veryParched).exhaustion();
        TestFixtures.check(helper, one > none,
                "Parched should add thirst exhaustion, got " + one + " against " + none + " without it");
        TestFixtures.check(helper, two > one,
                "Parched II should drain faster than Parched I, got " + two + " against " + one);
        helper.succeed();
    }

    @GameTest
    public void smallExhaustionIsCarriedUntilItShows(GameTestHelper helper) {
        ServerPlayer carrying = survivalPlayer(helper);
        ServerPlayer control = survivalPlayer(helper);
        ThirstData before = ThirstManager.get(carrying);

        // A few ticks of light exhaustion: too little to change what the client draws.
        for (int i = 0; i < 3; i++) {
            carrying.causeFoodExhaustion(SMALL_EXHAUSTION);
            ThirstManager.tick(helper.getLevel().getServer());
        }
        TestFixtures.check(helper, ThirstManager.get(carrying).equals(before),
                "exhaustion that has not crossed a sync step should not be written, since every write is "
                        + "a sync packet, but thirst moved to " + ThirstManager.get(carrying));

        // Once a write happens it has to include what was carried until then.
        carrying.causeFoodExhaustion(EXHAUSTION);
        control.causeFoodExhaustion(EXHAUSTION);
        ThirstManager.tick(helper.getLevel().getServer());
        float carried = ThirstManager.get(carrying).exhaustion();
        float plain = ThirstManager.get(control).exhaustion();
        TestFixtures.check(helper, carried > plain,
                "the carried exhaustion should be written with the next change, got " + carried
                        + " against " + plain + " without it");
        helper.succeed();
    }

    @GameTest
    public void ridingDoesNotDehydrate(GameTestHelper helper) {
        ServerPlayer player = survivalPlayer(helper);
        Entity mount = helper.spawn(TestFixtures.mountType(), new BlockPos(2, 2, 2));
        // Forced. A release after 1.21.1 added the second flag, which also ignores the mount's own rules.
        //? if >1.21.1 {
        player.startRiding(mount, true, true);
        //?} else
        //player.startRiding(mount, true);
        TestFixtures.check(helper, player.isPassenger(), "the player should be riding the mount");

        ThirstData before = ThirstManager.get(player);
        player.causeFoodExhaustion(EXHAUSTION);
        ThirstManager.tick(helper.getLevel().getServer());

        TestFixtures.check(helper, ThirstManager.get(player).equals(before),
                "riding should not dehydrate, thirst moved to " + ThirstManager.get(player));
        helper.succeed();
    }

    /** Survival with a full hunger bar, so vanilla allows sprinting and charges exhaustion. */
    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        player.setGameMode(GameType.SURVIVAL);
        player.getFoodData().setFoodLevel(20);
        return player;
    }

    private static void setThirst(ServerPlayer player, int thirst) {
        ThirstManager.set(player, ThirstManager.get(player).withLevels(thirst, 0));
    }
}
