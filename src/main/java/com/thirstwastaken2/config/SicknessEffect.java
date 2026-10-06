package com.thirstwastaken2.config;

import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * One line of the sickness tables: a drink of water of some grade, on some difficulty, gives
 * {@link #effect} for {@link #seconds} at {@link #level} with {@link #chance} percent. A plain class with
 * public fields rather than a record, so Gson reads it the same way on every Minecraft version.
 *
 * <p>Lines with the same {@link #group} share one roll per drink, so their chances nest: the default
 * Poison line is in Upset Stomach's group with a lower chance, which makes Poison come only with Upset
 * Stomach, its chance inside Upset Stomach's rather than added to it. A line with no group rolls on its
 * own, as every line did before groups existed.
 *
 * <p>The tables are {@code ThirstConfig.sicknessEffects}: difficulty name, then grade name, then lines.
 * The names are {@link #DIFFICULTIES} and {@link #GRADES}, so the file reads as words rather than indices.
 */
public final class SicknessEffect {
    /** Every difficulty, Peaceful included, as the file names them. */
    public static final String[] DIFFICULTIES = {"peaceful", "easy", "normal", "hard"};
    /** Every fresh water grade, Dirty first, as the file names them. Salt water has its own settings. */
    public static final String[] GRADES = {"dirty", "murky", "clean", "pure"};
    public static final int MAX_SECONDS = 600;
    public static final int MAX_LEVEL = 10;
    /** The group the default Upset Stomach and Poison lines share. */
    public static final String ILLNESS = "illness";

    public static final String POISON = "minecraft:poison";
    public static final String UPSET_STOMACH = "thirstwastaken2:upset_stomach";

    /** A mob effect id, which may name a mod that is not installed; such a line is skipped. */
    public String effect;
    /** Percent, 0 to 100. */
    public int chance;
    public int seconds;
    /** 1 for level I, as the game names it; the amplifier is one less. */
    public int level;
    /** Lines with the same group share one roll per drink; {@code null} rolls on its own. */
    public String group;

    /**
     * The effect {@link #effect} names, looked up once rather than parsed on every drink. A line is
     * built anew by every {@code sanitize()}, which is what resets it; the effect registry itself is
     * fixed at startup, so a lookup never goes stale while the game runs.
     */
    private transient Holder<MobEffect> resolved;
    private transient boolean looked;

    public SicknessEffect() { }

    public SicknessEffect(String effect, int chance, int seconds, int level) {
        this(effect, chance, seconds, level, null);
    }

    public SicknessEffect(String effect, int chance, int seconds, int level, String group) {
        this.effect = effect;
        this.chance = chance;
        this.seconds = seconds;
        this.level = level;
        this.group = group;
    }

    public SicknessEffect copy() {
        return new SicknessEffect(effect, chance, seconds, level, group);
    }

    /** The effect this line gives, or {@code null} when nothing installed registers it. */
    public Holder<MobEffect> holder() {
        if (!looked) {
            Identifier id = effect == null ? null : Identifier.tryParse(effect);
            resolved = id == null ? null : Vanilla.mobEffect(id);
            looked = true;
        }
        return resolved;
    }

    /** The name the tables use for {@code difficulty}. */
    public static String key(Difficulty difficulty) {
        return difficulty.name().toLowerCase(Locale.ROOT);
    }

    /**
     * The tables a fresh config holds, from the purification rework's design. Clean and Pure water make
     * nobody ill, and neither does any water on Peaceful. Dirty and Murky water roll Upset Stomach, with
     * Poison nested inside it in one shared roll, more often and for longer the harder the difficulty.
     * There is no taste of Nausea any more: the illness and its blocked healing carry the consequence.
     */
    public static Map<String, Map<String, List<SicknessEffect>>> defaults() {
        Map<String, Map<String, List<SicknessEffect>>> tables = new LinkedHashMap<>();
        tables.put("peaceful", grades(List.of(), List.of(), List.of(), List.of()));
        tables.put("easy", grades(
                List.of(upset(65, 45, 1), poison(15, 10)),
                List.of(upset(35, 30, 1), poison(5, 8)),
                List.of(),
                List.of()));
        tables.put("normal", grades(
                List.of(upset(90, 60, 2), poison(35, 20)),
                List.of(upset(65, 45, 1), poison(15, 15)),
                List.of(),
                List.of()));
        tables.put("hard", grades(
                List.of(upset(100, 90, 2), poison(50, 30)),
                List.of(upset(85, 60, 2), poison(25, 20)),
                List.of(),
                List.of()));
        return tables;
    }

    /** The default lines for one difficulty and grade, as fresh copies. */
    public static List<SicknessEffect> defaults(String difficulty, String grade) {
        return copyOf(defaults().get(difficulty).get(grade));
    }

    public static List<SicknessEffect> copyOf(List<SicknessEffect> lines) {
        List<SicknessEffect> copy = new ArrayList<>(lines.size());
        for (SicknessEffect line : lines) copy.add(line.copy());
        return copy;
    }

    private static Map<String, List<SicknessEffect>> grades(List<SicknessEffect> dirty, List<SicknessEffect> murky,
                                                           List<SicknessEffect> clean, List<SicknessEffect> pure) {
        Map<String, List<SicknessEffect>> grades = new LinkedHashMap<>();
        grades.put("dirty", new ArrayList<>(dirty));
        grades.put("murky", new ArrayList<>(murky));
        grades.put("clean", new ArrayList<>(clean));
        grades.put("pure", new ArrayList<>(pure));
        return grades;
    }

    private static SicknessEffect upset(int chance, int seconds, int level) {
        return new SicknessEffect(UPSET_STOMACH, chance, seconds, level, ILLNESS);
    }

    private static SicknessEffect poison(int chance, int seconds) {
        return new SicknessEffect(POISON, chance, seconds, 1, ILLNESS);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SicknessEffect)) return false;
        SicknessEffect line = (SicknessEffect) other;
        return Objects.equals(effect, line.effect) && chance == line.chance && seconds == line.seconds
                && level == line.level && Objects.equals(group, line.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(effect, chance, seconds, level, group);
    }

    @Override
    public String toString() {
        return effect + " " + chance + "% " + seconds + "s L" + level + (group == null ? "" : " [" + group + "]");
    }
}
