package com.thirstwastaken2.effect;

import com.thirstwastaken2.config.SicknessEffect;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.purity.WaterQuality;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;

import java.util.List;
import java.util.Map;

/**
 * What a drink of fresh water does to the player: the effects the config's {@code sicknessEffects} lists
 * for the difficulty and the water's grade, each rolling on its own. Salt water is handled by
 * {@code WaterPurity.applyEffects} before this. The defaults, and why they are what they are, are in
 * docs/dev/mechanics/WATER-SICKNESS.md.
 *
 * <p>This replaced the Realistic and Classic presets, whose chances were fixed in code; the tables'
 * defaults are Realistic's numbers.
 */
public final class WaterSickness {
    private static final int TICKS_PER_SECOND = 20;

    private WaterSickness() { }

    /** Makes {@code player} ill, or not, from a drink of {@code water}, rolling with the player's random. */
    public static void drink(Player player, WaterQuality.Fresh water) {
        drink(player, water.purity(), player.level().getDifficulty(), player.getRandom()::nextFloat);
    }

    /**
     * Every line the config lists for {@code difficulty} and {@code purity} rolls on its own, {@code roll}
     * giving 0 to 1 once per line, and gives its effect when it lands under the line's chance. A line
     * whose effect nothing registers is skipped. With {@code extendSicknessEffects} on, an effect the
     * player already has lasts longer; see {@link #give}.
     */
    public static void drink(Player player, int purity, Difficulty difficulty, FloatSupplier roll) {
        ThirstConfig config = ThirstConfig.get();
        Map<String, List<SicknessEffect>> grades = config.sicknessEffects.get(SicknessEffect.key(difficulty));
        if (grades == null || purity < 0 || purity >= SicknessEffect.GRADES.length) return;
        List<SicknessEffect> lines = grades.get(SicknessEffect.GRADES[purity]);
        if (lines == null) return;
        for (SicknessEffect line : lines) {
            if (roll.getAsFloat() * 100.0F >= line.chance) continue;
            Identifier id = Identifier.tryParse(line.effect);
            Holder<MobEffect> effect = id == null ? null : Vanilla.mobEffect(id);
            if (effect == null) continue;
            give(player, effect, line.seconds * TICKS_PER_SECOND, line.level - 1, config.extendSicknessEffects);
        }
    }

    /**
     * Adds {@code effect} for {@code ticks}. {@code extend} adds that time to what is left of it instead,
     * capped at twice {@code ticks}, and keeps the higher level of the two. Vanilla keeps whichever
     * instance is stronger or longer, so this never cuts an effect the player already has short.
     */
    private static void give(Player player, Holder<MobEffect> effect, int ticks, int amplifier, boolean extend) {
        MobEffectInstance existing = Vanilla.getEffect(player, effect);
        int duration = ticks;
        if (extend && existing != null) {
            duration = Math.min(existing.getDuration() + ticks, 2 * ticks);
            amplifier = Math.max(amplifier, existing.getAmplifier());
        }
        player.addEffect(Vanilla.effectInstance(effect, duration, amplifier));
    }

    /** A source of rolls from 0 to 1, so a test can force them. */
    @FunctionalInterface
    public interface FloatSupplier {
        float getAsFloat();
    }
}
