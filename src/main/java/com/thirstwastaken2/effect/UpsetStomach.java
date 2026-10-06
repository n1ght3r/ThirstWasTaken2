package com.thirstwastaken2.effect;

import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;

/**
 * Upset Stomach's cramps: every few seconds a roll, by difficulty and level, for half a heart of damage.
 * They never take the player below a floor that falls as the difficulty rises, so the illness alone
 * cannot kill. Fixed numbers with no config, like its stop on healing in {@code HealthRegen}.
 *
 * <p>The damage is magic, as Poison's is, so armour does not soften it, and like any damage it wakes a
 * sleeping player: being ill keeps you from sleeping the night away.
 */
public final class UpsetStomach {
    /** How often the cramps roll, vanilla's beat for starving. */
    public static final int INTERVAL = 80;
    /** Half a heart a cramp. */
    public static final float DAMAGE = 1.0F;

    private UpsetStomach() { }

    /** Rolls for a cramp on {@code player}'s tick, every {@link #INTERVAL} ticks. Server side only. */
    public static void tick(ServerPlayer player) {
        float damage = crampNow(player);
        if (damage > 0.0F) Vanilla.hurt(player, player.damageSources().magic(), damage);
    }

    /**
     * The damage this tick's roll does to {@code player}: nothing off the {@link #INTERVAL} beat or
     * without the effect, otherwise {@link #cramp} on the world's difficulty and the player's health.
     * Apart from {@link #tick} so the gametests can check it, since vanilla will not hurt their mock player.
     */
    public static float crampNow(ServerPlayer player) {
        if (player.tickCount % INTERVAL != 0) return 0.0F;
        MobEffectInstance effect = Vanilla.getEffect(player, ThirstEffects.UPSET_STOMACH);
        if (effect == null) return 0.0F;
        return cramp(player.level().getDifficulty(), effect.getAmplifier(), player.getHealth(),
                player.getRandom().nextFloat());
    }

    /**
     * The damage one roll does: {@link #DAMAGE} when {@code roll}, from 0 to 1, lands under the chance
     * for {@code difficulty} and {@code amplifier}, cut so that {@code health} stays at the difficulty's
     * floor or above; otherwise nothing.
     */
    public static float cramp(Difficulty difficulty, int amplifier, float health, float roll) {
        if (roll >= chance(difficulty, amplifier)) return 0.0F;
        return Math.max(0.0F, Math.min(DAMAGE, health - floor(difficulty)));
    }

    /** The chance of a cramp on each roll: Easy 15% and 25%, Normal 25% and 45%, Hard 40% and 60%. */
    public static float chance(Difficulty difficulty, int amplifier) {
        boolean severe = amplifier >= 1;
        switch (difficulty) {
            case EASY: return severe ? 0.25F : 0.15F;
            case NORMAL: return severe ? 0.45F : 0.25F;
            case HARD: return severe ? 0.60F : 0.40F;
            default: return 0.0F;
        }
    }

    /**
     * The health a cramp never takes the player under: five hearts on Easy, two on Normal, half a heart
     * on Hard, after the way starving stops on Easy and only kills on Hard.
     */
    public static float floor(Difficulty difficulty) {
        switch (difficulty) {
            case EASY: return 10.0F;
            case NORMAL: return 4.0F;
            default: return 1.0F;
        }
    }
}
