package com.thirstwastaken2.effect;

import com.thirstwastaken2.config.SicknessPreset;
import com.thirstwastaken2.config.SicknessTable;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.purity.WaterQuality;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.core.Holder;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;

/**
 * What a drink of fresh water does to the player, from docs/dev/mechanics/WATER-SICKNESS.md: a taste of
 * Nausea for Dirty and Murky water, then one roll that picks at most one illness, worst first. Pure water
 * is always safe, and salt water is handled by {@code WaterPurity.applyEffects} before this.
 *
 * <p>Dysentery, the worst illness in the design, is not built yet; its range will go in front of
 * Poisoning's.
 */
public final class WaterSickness {
    /**
     * The Nausea every Dirty or Murky drink gives, whatever the difficulty: 7 seconds. The design's 2
     * seconds never showed, because vanilla only warps the screen while more than 3 seconds of Nausea
     * are left; 7 seconds reach about half strength, a wobble rather than the full spin.
     */
    public static final int TASTE_TICKS = 140;
    private static final int TICKS_PER_SECOND = 20;
    /** Classic: Nausea and Poison chances and Nausea seconds by grade, Dirty first. */
    private static final int[] CLASSIC_NAUSEA_CHANCE = {100, 50, 5, 0};
    private static final int[] CLASSIC_POISON_CHANCE = {30, 10, 0, 0};
    private static final int[] CLASSIC_NAUSEA_SECONDS = {12, 8, 5, 5};
    private static final int CLASSIC_POISON_TICKS = 10 * TICKS_PER_SECOND;

    /** What one roll gives, mildest first, so a later constant is always worse. */
    public enum Outcome { NONE, UPSET_STOMACH, POISONING }

    private WaterSickness() { }

    /** Makes {@code player} ill, or not, from a drink of {@code water}, rolling with the player's random. */
    public static void drink(Player player, WaterQuality.Fresh water) {
        ThirstConfig config = ThirstConfig.get();
        float roll = player.getRandom().nextFloat() * 100.0F;
        if (config.sicknessPreset == SicknessPreset.CLASSIC) {
            classic(player, water.purity(), roll);
            return;
        }
        drink(player, water.purity(), player.level().getDifficulty(), roll);
    }

    /** {@link #drink(Player, WaterQuality.Fresh)} with the difficulty and the roll, 0 to 100, given. */
    public static void drink(Player player, int purity, Difficulty difficulty, float roll) {
        if (purity <= 1) player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, TASTE_TICKS));
        SicknessTable table = SicknessTable.of(difficulty);
        if (table == null || purity >= SicknessTable.GRADES) return;
        Outcome outcome = outcome(table, purity, roll);
        if (outcome != Outcome.NONE) catchIllness(player, difficulty, outcome, table.upsetStomachLevel()[purity] - 1);
    }

    /** Where {@code roll} lands in the ranges for {@code purity}, walked from worst to mildest. */
    public static Outcome outcome(SicknessTable table, int purity, float roll) {
        float poisoning = table.poisoningChance()[purity];
        if (roll < poisoning) return Outcome.POISONING;
        if (roll < poisoning + table.upsetStomachChance()[purity]) return Outcome.UPSET_STOMACH;
        return Outcome.NONE;
    }

    /**
     * Gives {@code outcome}, or does nothing if the player is already worse off. The same illness again
     * extends every effect it gives, up to twice its time, and turns Upset Stomach I into II. A worse one
     * adds its effects on top.
     */
    public static void catchIllness(Player player, Difficulty difficulty, Outcome outcome, int upsetAmplifier) {
        Outcome current = current(player);
        if (outcome.compareTo(current) < 0) return;
        boolean again = outcome == current;
        MobEffectInstance upset = Vanilla.getEffect(player, ThirstEffects.UPSET_STOMACH);
        int amplifier = again && upset != null ? Math.max(upsetAmplifier, Math.min(upset.getAmplifier() + 1,
                SicknessTable.MAX_UPSET_STOMACH_LEVEL - 1)) : upsetAmplifier;
        give(player, ThirstEffects.UPSET_STOMACH, upsetStomachTicks(difficulty), amplifier, again);
        if (outcome == Outcome.POISONING) {
            for (PoisoningEffect effect : poisoning(difficulty)) {
                give(player, effect.effect(), effect.ticks(), effect.amplifier(), again);
            }
        }
    }

    /**
     * The illness the player has now. Poisoning always comes with Upset Stomach and Poison, so both
     * together read as Poisoning. Once the Poison wears off, what is left is Upset Stomach.
     */
    public static Outcome current(Player player) {
        if (!Vanilla.hasEffect(player, ThirstEffects.UPSET_STOMACH)) return Outcome.NONE;
        return player.hasEffect(MobEffects.POISON) ? Outcome.POISONING : Outcome.UPSET_STOMACH;
    }

    public static int upsetStomachTicks(Difficulty difficulty) {
        return TICKS_PER_SECOND * switch (difficulty) {
            case PEACEFUL, EASY -> 45;
            case NORMAL -> 60;
            case HARD -> 90;
        };
    }

    /** One vanilla effect Poisoning gives. */
    public record PoisoningEffect(Holder<MobEffect> effect, int amplifier, int ticks) { }

    /**
     * What Poisoning gives on {@code difficulty}, on top of Upset Stomach: Poison, longer the harder the
     * difficulty. Vanilla Poison stops at half a heart, so it never kills. The design's Weakness, Mining
     * Fatigue and Slowness were dropped: the illness should hurt, not stop the player working.
     */
    public static PoisoningEffect[] poisoning(Difficulty difficulty) {
        return switch (difficulty) {
            case PEACEFUL, EASY -> new PoisoningEffect[]{
                    new PoisoningEffect(Vanilla.poison(), 0, 10 * TICKS_PER_SECOND)};
            case NORMAL -> new PoisoningEffect[]{
                    new PoisoningEffect(Vanilla.poison(), 0, 20 * TICKS_PER_SECOND)};
            case HARD -> new PoisoningEffect[]{
                    new PoisoningEffect(Vanilla.poison(), 0, 30 * TICKS_PER_SECOND)};
        };
    }

    /**
     * Adds an effect for {@code ticks}. {@code extend} adds that time to what is left instead, capped at
     * twice {@code ticks}. Vanilla keeps whichever instance is stronger or longer, so this never cuts an
     * effect the player already has short.
     */
    private static void give(Player player, Holder<MobEffect> effect, int ticks, int amplifier, boolean extend) {
        MobEffectInstance existing = Vanilla.getEffect(player, effect);
        int duration = ticks;
        if (extend && existing != null) duration = Math.min(existing.getDuration() + ticks, 2 * ticks);
        player.addEffect(Vanilla.effectInstance(effect, duration, amplifier));
    }

    /** The roll before the sickness rework, which ignored the difficulty. */
    private static void classic(Player player, int purity, float roll) {
        if (roll < CLASSIC_NAUSEA_CHANCE[purity]) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, TICKS_PER_SECOND * CLASSIC_NAUSEA_SECONDS[purity]));
        }
        if (roll < CLASSIC_POISON_CHANCE[purity]) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, CLASSIC_POISON_TICKS));
        }
    }
}
