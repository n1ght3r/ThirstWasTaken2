package com.thirstwastaken2.effect;

import com.thirstwastaken2.platform.Vanilla;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

/** Upset Stomach's numbers, from docs/dev/mechanics/WATER-SICKNESS.md. Pure functions of the level. */
public final class UpsetStomach {
    /**
     * Extra thirst exhaustion per tick per level, charged in {@code ThirstManager.tickPlayer}. It covers
     * the bursts too: Nausea's own drain is not charged on top while the player has Upset Stomach, so
     * level I costs the design's 2.4 thirst a minute however long a burst lasts.
     */
    public static final float EXHAUSTION = 0.008F;
    /**
     * How long each Nausea burst lasts: 10 seconds. Vanilla fades the warp in over 150 ticks and starts
     * fading it out 60 ticks before the end, so Nausea of 3 seconds or less shows nothing at all. Ten
     * seconds brings it close to full strength before it fades.
     */
    public static final int NAUSEA_TICKS = 200;
    /** Nausea bursts a minute per level: one at I, two at II. */
    private static final float BURSTS_PER_MINUTE_PER_LEVEL = 1.0F;
    private static final float TICKS_PER_MINUTE = 1200.0F;
    /** What saturation is multiplied by, per amplifier: ×0.75 at I, ×0.5 at II and above. */
    private static final float[] SATURATION = {0.75F, 0.5F};

    private UpsetStomach() { }

    /**
     * The chance that a roll made every {@code interval} ticks starts a Nausea burst, so that bursts
     * average {@link #BURSTS_PER_MINUTE_PER_LEVEL} a minute per level whatever the interval.
     */
    public static float burstChance(int amplifier, int interval) {
        return Math.min(1.0F, BURSTS_PER_MINUTE_PER_LEVEL * (amplifier + 1) * interval / TICKS_PER_MINUTE);
    }

    /**
     * What the saturation a food gives, and the quenched a drink gives, is multiplied by while
     * {@code player} has the effect; 1 without it.
     */
    public static float saturationScale(Player player) {
        MobEffectInstance effect = Vanilla.getEffect(player, ThirstEffects.UPSET_STOMACH);
        if (effect == null) return 1.0F;
        return SATURATION[Math.min(effect.getAmplifier(), SATURATION.length - 1)];
    }
}
