package com.thirstwastaken2.gametest;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * The once-a-tick part of thirst: spending exhaustion, peaceful regeneration, who is exempt, and what
 * slows dehydration down.
 *
 * <p>Each test ticks its own player through {@code ThirstManager.tickPlayer} rather than ticking the
 * server, so no other test's player is touched. Every player's {@code tickCount} is pinned, because
 * the peaceful refill fires on multiples of 11 and dehydration damage on multiples of 40, and a test
 * that landed on one by chance would measure something else.
 *
 * <p>The damage itself is not here: a mock player cannot be hurt. See docs/dev/MANUAL-TESTING.md.
 */
public final class ThirstTickGameTest {
    /** Neither a multiple of 11 nor of 40, so neither the peaceful refill nor dehydration damage fires. */
    private static final int QUIET_TICK = 1;
    /** A multiple of 11 that is not one of 40: the peaceful refill fires, damage does not. */
    private static final int REFILL_TICK = 11;
    /** Past one point, so the tick has to spend one. */
    private static final float OVER_A_POINT = 4.5F;
    /** Raw exhaustion charged in the modifier tests: well past a sync step, well short of a point. */
    private static final float CHARGE = 2.0F;

    @GameTest
    public void aTickSpendsQuenchedBeforeThirst(GameTestHelper helper) {
        atDifficulty(helper, Difficulty.NORMAL, () -> {
            ServerPlayer quenched = player(helper, new ThirstData(ThirstData.MAX, 2, OVER_A_POINT, true));
            ThirstManager.tickPlayer(quenched);
            ThirstData after = ThirstManager.get(quenched);
            TestFixtures.check(helper, after.thirst() == ThirstData.MAX && after.quenched() == 1,
                    "the tick should spend a point of quenched, got " + after);

            ServerPlayer dry = player(helper, new ThirstData(10, 0, OVER_A_POINT, true));
            ThirstManager.tickPlayer(dry);
            TestFixtures.check(helper, ThirstManager.get(dry).thirst() == 9,
                    "with no quenched the tick should spend a point of thirst, got " + ThirstManager.get(dry));
        });
        helper.succeed();
    }

    @GameTest
    public void peacefulNeitherDrainsNorLeavesTheBarLow(GameTestHelper helper) {
        atDifficulty(helper, Difficulty.PEACEFUL, () -> {
            ServerPlayer spending = player(helper, new ThirstData(10, 0, OVER_A_POINT, true));
            ThirstManager.tickPlayer(spending);
            TestFixtures.check(helper, ThirstManager.get(spending).thirst() == 10,
                    "exhaustion on peaceful should not cost thirst, got " + ThirstManager.get(spending));

            // Leftover exhaustion is drawn against the last droplet, so a full bar that kept it would
            // look one droplet short forever.
            ServerPlayer leftover = player(helper, new ThirstData(ThirstData.MAX, 0, 1.0F, true));
            leftover.causeFoodExhaustion(CHARGE);
            ThirstManager.tickPlayer(leftover);
            TestFixtures.check(helper, ThirstManager.get(leftover).exhaustion() == 0.0F,
                    "peaceful with no quenched should drop exhaustion, got " + ThirstManager.get(leftover));

            ServerPlayer quenched = player(helper, new ThirstData(ThirstData.MAX, 2, OVER_A_POINT, true));
            ThirstManager.tickPlayer(quenched);
            TestFixtures.check(helper, ThirstManager.get(quenched).quenched() == 1,
                    "peaceful should still spend quenched, got " + ThirstManager.get(quenched));

            ServerPlayer refilling = player(helper, new ThirstData(10, 0, 0.0F, true));
            refilling.tickCount = REFILL_TICK;
            ThirstManager.tickPlayer(refilling);
            TestFixtures.check(helper, ThirstManager.get(refilling).thirst() == 11,
                    "peaceful should refill a point every 11 ticks, got " + ThirstManager.get(refilling));
        });
        helper.succeed();
    }

    @GameTest
    public void peacefulDrainsWhenConfiguredTo(GameTestHelper helper) {
        ThirstConfig config = ThirstConfig.get();
        boolean original = config.thirstDepletionInPeaceful;
        try {
            config.thirstDepletionInPeaceful = true;
            atDifficulty(helper, Difficulty.PEACEFUL, () -> {
                ServerPlayer spending = player(helper, new ThirstData(10, 0, OVER_A_POINT, true));
                ThirstManager.tickPlayer(spending);
                TestFixtures.check(helper, ThirstManager.get(spending).thirst() == 9,
                        "thirst_depletion_in_peaceful should make peaceful drain, got " + ThirstManager.get(spending));

                ServerPlayer notRefilled = player(helper, new ThirstData(10, 0, 0.0F, true));
                notRefilled.tickCount = REFILL_TICK;
                ThirstManager.tickPlayer(notRefilled);
                TestFixtures.check(helper, ThirstManager.get(notRefilled).thirst() == 10,
                        "a peaceful that drains should not refill either, got " + ThirstManager.get(notRefilled));
            });
        } finally {
            config.thirstDepletionInPeaceful = original;
        }
        helper.succeed();
    }

    @GameTest
    public void disabledAndInvulnerablePlayersAreSkipped(GameTestHelper helper) {
        atDifficulty(helper, Difficulty.NORMAL, () -> {
            ThirstData off = new ThirstData(10, 0, OVER_A_POINT, false);
            ServerPlayer disabled = player(helper, off);
            ThirstManager.tickPlayer(disabled);
            TestFixtures.check(helper, ThirstManager.get(disabled).equals(off),
                    "a player with thirst disabled should not drain, got " + ThirstManager.get(disabled));

            ThirstData on = new ThirstData(10, 0, OVER_A_POINT, true);
            ServerPlayer invulnerable = player(helper, on);
            invulnerable.getAbilities().invulnerable = true;
            ThirstManager.tickPlayer(invulnerable);
            TestFixtures.check(helper, ThirstManager.get(invulnerable).equals(on),
                    "an invulnerable player should not drain, got " + ThirstManager.get(invulnerable));

            ServerPlayer control = player(helper, on);
            ThirstManager.tickPlayer(control);
            TestFixtures.check(helper, !ThirstManager.get(control).equals(on),
                    "the same state should drain for an ordinary player, or the two checks above prove nothing");
        });
        helper.succeed();
    }

    @GameTest
    public void fireResistanceSlowsDehydration(GameTestHelper helper) {
        atDifficulty(helper, Difficulty.NORMAL, () -> {
            ServerPlayer plain = player(helper, ThirstData.full());
            ServerPlayer resistant = player(helper, ThirstData.full());
            resistant.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200));

            float normal = charge(plain);
            float slowed = charge(resistant);
            TestFixtures.check(helper, normal > 0.0F && slowed > 0.0F && slowed < normal,
                    "Fire Resistance should charge less exhaustion for the same effort, got " + slowed
                            + " against " + normal);
        });
        helper.succeed();
    }

    @GameTest
    public void fireProtectionSlowsDehydration(GameTestHelper helper) {
        atDifficulty(helper, Difficulty.NORMAL, () -> {
            ServerPlayer plain = player(helper, ThirstData.full());
            ServerPlayer armoured = player(helper, ThirstData.full());
            ItemStack boots = new ItemStack(Items.DIAMOND_BOOTS);
            // Enchantments became data in 1.21, looked up through the registries.
            //? if >=1.21 {
            boots.enchant(helper.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                    .getOrThrow(Enchantments.FIRE_PROTECTION), 4);
            //?} else {
            /*boots.enchant(Enchantments.FIRE_PROTECTION, 4);
            *///?}
            armoured.setItemSlot(EquipmentSlot.FEET, boots);

            float normal = charge(plain);
            float slowed = charge(armoured);
            TestFixtures.check(helper, normal > 0.0F && slowed > 0.0F && slowed < normal,
                    "Fire Protection should charge less exhaustion for the same effort, got " + slowed
                            + " against " + normal);
        });
        helper.succeed();
    }

    /** Salt water charges exhaustion when drunk, without waiting for the tick. */
    @GameTest
    public void saltWaterIsChargedStraightAway(GameTestHelper helper) {
        ServerPlayer player = player(helper, ThirstData.full());
        ThirstData before = ThirstManager.get(player);

        WaterPurity.applyEffects(player,
                WaterPurity.setQuality(new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), WaterQuality.SALT));

        TestFixtures.check(helper, spent(before, ThirstManager.get(player)) > 0.0F,
                "salt water should charge exhaustion straight away, got " + ThirstManager.get(player));
        helper.succeed();
    }

    @GameTest
    public void aFullBarRefusesPlainWater(GameTestHelper helper) {
        ServerPlayer player = player(helper, ThirstData.full());
        TestFixtures.check(helper, !ThirstManager.canDrinkWater(player), "a full bar should refuse plain water");

        ThirstManager.set(player, ThirstData.full().withLevels(ThirstData.MAX - 1, 0));
        TestFixtures.check(helper, ThirstManager.canDrinkWater(player), "one point short should accept water");

        ThirstManager.set(player, ThirstData.full().withEnabled(false));
        TestFixtures.check(helper, ThirstManager.canDrinkWater(player),
                "a player with thirst disabled should drink as vanilla lets them");

        ServerPlayer invulnerable = player(helper, ThirstData.full());
        invulnerable.getAbilities().invulnerable = true;
        TestFixtures.check(helper, ThirstManager.canDrinkWater(invulnerable),
                "an invulnerable player should drink as vanilla lets them");
        helper.succeed();
    }

    private static ServerPlayer player(GameTestHelper helper, ThirstData data) {
        ServerPlayer player = TestFixtures.survivalPlayer(helper);
        player.tickCount = QUIET_TICK;
        ThirstManager.set(player, data);
        return player;
    }

    /** Charges {@link #CHARGE} through vanilla's exhaustion hook, ticks, and returns what it cost. */
    private static float charge(ServerPlayer player) {
        ThirstData before = ThirstManager.get(player);
        player.causeFoodExhaustion(CHARGE);
        ThirstManager.tickPlayer(player);
        return spent(before, ThirstManager.get(player));
    }

    /** Exhaustion charged between two states, counting points already spent at four apiece. */
    private static float spent(ThirstData before, ThirstData after) {
        int points = (before.quenched() - after.quenched()) + (before.thirst() - after.thirst());
        return after.exhaustion() - before.exhaustion() + points * ThirstData.EXHAUSTION_PER_POINT;
    }

    /** Runs {@code body} at a difficulty, then restores the one the test server had. */
    private static void atDifficulty(GameTestHelper helper, Difficulty difficulty, Runnable body) {
        MinecraftServer server = helper.getLevel().getServer();
        Difficulty original = helper.getLevel().getDifficulty();
        server.setDifficulty(difficulty, true);
        try {
            body.run();
        } finally {
            server.setDifficulty(original, true);
        }
    }
}
