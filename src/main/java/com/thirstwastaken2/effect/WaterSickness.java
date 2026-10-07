package com.thirstwastaken2.effect;

import com.thirstwastaken2.config.SicknessEffect;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.purity.WaterQuality;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.core.Holder;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;

/**
 * What a drink of fresh water does to the player: the effects the config's {@code sicknessEffects} lists
 * for the difficulty and the water's grade. Salt water is handled by {@code WaterPurity.applyEffects}
 * before this. The defaults are {@code SicknessEffect.defaults}, listed in
 * docs/dev/mechanics/WATER-REFERENCE.md.
 */
public final class WaterSickness {
    private static final int TICKS_PER_SECOND = 20;

    private WaterSickness() { }

    /** Makes {@code player} ill, or not, from a drink of {@code water}, rolling with the player's random. */
    public static void drink(Player player, WaterQuality.Fresh water) {
        drink(player, water.purity(), player.level().getDifficulty(), player.getRandom()::nextFloat);
    }

    /**
     * Every line the config lists for {@code difficulty} and {@code purity} gives its effect when its roll
     * lands under the line's chance. {@code roll} gives 0 to 1: once for every line that rolls on its own,
     * and once for each group, shared by every line in it, so a group's chances nest rather than add up.
     * A line whose effect nothing registers is skipped. With {@code extendSicknessEffects} on, an effect
     * the player already has lasts longer; see {@link #give}.
     */
    public static void drink(Player player, int purity, Difficulty difficulty, FloatSupplier roll) {
        ThirstConfig config = ThirstConfig.get();
        Map<String, List<SicknessEffect>> grades = config.sicknessEffects.get(SicknessEffect.key(difficulty));
        if (grades == null || purity < 0 || purity >= SicknessEffect.GRADES.length) return;
        List<SicknessEffect> lines = grades.get(SicknessEffect.GRADES[purity]);
        if (lines == null || lines.isEmpty()) return;
        // One roll per line or per group, in the order each first comes up. A drink has a handful of
        // lines, so the earlier ones are searched for a group rather than kept in a map.
        float[] rolls = new float[lines.size()];
        for (int index = 0; index < lines.size(); index++) {
            SicknessEffect line = lines.get(index);
            rolls[index] = sharedRoll(lines, rolls, index, roll);
            if (rolls[index] * 100.0F >= line.chance) continue;
            Holder<MobEffect> effect = line.holder();
            if (effect == null) continue;
            give(player, effect, line.seconds * TICKS_PER_SECOND, line.level - 1, config.extendSicknessEffects);
        }
    }

    /** The roll line {@code index} uses: an earlier line's of the same group, or a new one. */
    private static float sharedRoll(List<SicknessEffect> lines, float[] rolls, int index, FloatSupplier roll) {
        String group = lines.get(index).group;
        if (group != null) {
            for (int earlier = 0; earlier < index; earlier++) {
                if (group.equals(lines.get(earlier).group)) return rolls[earlier];
            }
        }
        return roll.getAsFloat();
    }

    /**
     * Adds {@code effect} for {@code ticks}, keeping the higher level of the old and the new. Vanilla
     * keeps whichever instance is stronger or longer, so this never cuts an effect the player already has
     * short. With {@code extend} on, a repeat lasts longer than either alone:
     *
     * <ul>
     *   <li>Upset Stomach adds half the new time to what is left, up to one and a half times the new
     *       time, but never less than is left: {@code max(R, min(R + D / 2, 1.5 * D))}. Drinking bad water
     *       while ill keeps the illness going without stacking it up without end.</li>
     *   <li>Any other effect adds the whole new time, up to twice it, as before the rework.</li>
     * </ul>
     */
    static void give(Player player, Holder<MobEffect> effect, int ticks, int amplifier, boolean extend) {
        MobEffectInstance existing = Vanilla.getEffect(player, effect);
        int duration = ticks;
        if (extend && existing != null) {
            duration = extended(effect.value() == ThirstEffects.UPSET_STOMACH.value(), existing.getDuration(), ticks);
            amplifier = Math.max(amplifier, existing.getAmplifier());
        }
        player.addEffect(Vanilla.effectInstance(effect, duration, amplifier));
    }

    /** How long a repeat of an effect lasts, {@code left} ticks left of it and {@code added} new; see {@link #give}. */
    public static int extended(boolean upsetStomach, int left, int added) {
        if (upsetStomach) return Math.max(left, Math.min(left + added / 2, added * 3 / 2));
        return Math.min(left + added, 2 * added);
    }

    /** A source of rolls from 0 to 1, so a test can force them. */
    @FunctionalInterface
    public interface FloatSupplier {
        float getAsFloat();
    }
}
