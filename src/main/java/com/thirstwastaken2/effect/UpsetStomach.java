package com.thirstwastaken2.effect;

import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Upset Stomach's cramps: half a heart of damage each, at random times. The wait before each one is
 * drawn afresh, between half a second and 15 seconds, shorter on a harder difficulty and at level II.
 * Like Poison, a cramp takes the player down to half a heart but never kills, Peaceful included. Fixed
 * numbers with no config, like its stop on healing in {@code HealthRegen}.
 *
 * <p>The damage is magic, as Poison's is, so armour does not soften it, and like any damage it wakes a
 * sleeping player: being ill keeps you from sleeping the night away.
 */
public final class UpsetStomach {
    /** Half a heart a cramp. */
    public static final float DAMAGE = 1.0F;
    /** Poison's floor: a cramp never takes health under half a heart. */
    public static final float MIN_HEALTH = 1.0F;
    /** The shortest and longest wait between two cramps, half a second and 15 seconds. */
    public static final int MIN_WAIT = 10;
    public static final int MAX_WAIT = 300;

    private static final Step NONE = new Step(0, 0.0F);

    private UpsetStomach() { }

    /**
     * What one tick of the illness does: the ticks left until the next cramp, and the damage this tick
     * deals. {@code countdown} is the previous tick's; 0 means no cramp is waiting, so the first one
     * after the effect starts comes a whole wait later rather than at once.
     */
    public record Step(int countdown, float damage) { }

    /** One server tick of {@code player}'s cramps; see {@link Step}. Nothing without the effect. */
    public static Step step(ServerPlayer player, int countdown) {
        MobEffectInstance effect = Vanilla.getEffect(player, ThirstEffects.UPSET_STOMACH);
        if (effect == null) return NONE;
        if (countdown > 1) return new Step(countdown - 1, 0.0F);
        int next = waitTicks(player.level().getDifficulty(), effect.getAmplifier(), player.getRandom().nextFloat());
        return new Step(next, countdown == 1 ? cramp(player.getHealth()) : 0.0F);
    }

    /** Deals a cramp's damage, as magic. */
    public static void hurt(ServerPlayer player, float damage) {
        Vanilla.hurt(player, player.damageSources().magic(), damage);
    }

    /** One cramp's damage at {@code health}: {@link #DAMAGE}, cut so health stays at {@link #MIN_HEALTH}. */
    public static float cramp(float health) {
        return Math.max(0.0F, Math.min(DAMAGE, health - MIN_HEALTH));
    }

    /**
     * The wait before the next cramp, {@code roll} from 0 to 1 picking evenly between the difficulty's
     * shortest and longest, and level II waiting three quarters as long, never under {@link #MIN_WAIT}:
     *
     * <ul>
     *   <li>Peaceful: 10 to 15 seconds.</li>
     *   <li>Easy: 6 to 15 seconds.</li>
     *   <li>Normal: 3 to 12 seconds.</li>
     *   <li>Hard: half a second to 8 seconds.</li>
     * </ul>
     */
    public static int waitTicks(Difficulty difficulty, int amplifier, float roll) {
        int shortest;
        int longest;
        switch (difficulty) {
            case PEACEFUL: shortest = 200; longest = MAX_WAIT; break;
            case EASY: shortest = 120; longest = MAX_WAIT; break;
            case NORMAL: shortest = 60; longest = 240; break;
            default: shortest = MIN_WAIT; longest = 160; break;
        }
        int wait = shortest + Math.min((int) (roll * (longest - shortest + 1)), longest - shortest);
        if (amplifier >= 1) wait = wait * 3 / 4;
        return Math.max(MIN_WAIT, wait);
    }
}
