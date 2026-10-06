package com.thirstwastaken2.effect;

import com.thirstwastaken2.platform.Vanilla;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

/**
 * Upset Stomach's numbers and its one rule of its own. Pure functions of the level, but for the milk
 * exception, which is server-thread state for the length of one call.
 */
public final class UpsetStomach {
    /**
     * Extra thirst exhaustion per tick per level, charged in {@code ThirstManager.tickPlayer} as illness:
     * four points of thirst a minute at I and eight at II, whatever the climate. Nausea's own drain is
     * not charged on top while the player has Upset Stomach.
     */
    public static final float EXHAUSTION = 16.0F / 1200.0F;
    /**
     * What saturation is multiplied by, per amplifier: x0.5 at I, x0.25 at II and above. The quenched a
     * drink gives is cut the same way, rounded down.
     */
    private static final float[] SATURATION = {0.5F, 0.25F};

    /** The Upset Stomach a milk drinker had going in, between the two halves of {@code MilkMixin}. */
    private static MobEffectInstance heldThroughMilk;

    private UpsetStomach() { }

    /**
     * What the saturation a food gives, and the quenched a drink gives, is multiplied by while
     * {@code player} has the effect; 1 without it.
     */
    public static float saturationScale(Player player) {
        MobEffectInstance effect = Vanilla.getEffect(player, ThirstEffects.UPSET_STOMACH);
        if (effect == null) return 1.0F;
        return SATURATION[Math.min(effect.getAmplifier(), SATURATION.length - 1)];
    }

    /**
     * Milk clears every effect, and Upset Stomach is the one it must not: time ends it, and commands and
     * a mod's own cures still do. Not in the original, where milk cured everything. Whatever each loader
     * does to clear the effects, the whole of it runs between these two calls, so the illness is noted
     * going in and given back coming out, as it was. Server side only.
     */
    public static void beforeMilk(LivingEntity entity) {
        MobEffectInstance effect = entity.level().isClientSide() ? null : Vanilla.getEffect(entity, ThirstEffects.UPSET_STOMACH);
        heldThroughMilk = effect == null ? null : new MobEffectInstance(effect);
    }

    /** The second half of {@link #beforeMilk}. */
    public static void afterMilk(LivingEntity entity) {
        MobEffectInstance held = heldThroughMilk;
        heldThroughMilk = null;
        if (held != null && !entity.level().isClientSide() && !Vanilla.hasEffect(entity, ThirstEffects.UPSET_STOMACH)) {
            entity.addEffect(held);
        }
    }
}
