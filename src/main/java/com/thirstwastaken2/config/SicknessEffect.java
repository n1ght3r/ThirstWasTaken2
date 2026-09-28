package com.thirstwastaken2.config;

import net.minecraft.world.Difficulty;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * One line of the Custom sickness preset's tables: a drink of water of some grade, on some difficulty,
 * gives {@link #effect} for {@link #seconds} at {@link #level} with {@link #chance} percent. Each line
 * rolls on its own, so one drink may give several. A plain class with public fields rather than a record,
 * so Gson reads it the same way on every Minecraft version.
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

    private static final String NAUSEA = "minecraft:nausea";
    private static final String POISON = "minecraft:poison";
    private static final String UPSET_STOMACH = "thirstwastaken2:upset_stomach";
    /** Realistic's taste of Nausea, {@code WaterSickness.TASTE_TICKS} in seconds. */
    private static final int TASTE_SECONDS = 7;

    /** A mob effect id, which may name a mod that is not installed; such a line is skipped. */
    public String effect;
    /** Percent, 0 to 100. */
    public int chance;
    public int seconds;
    /** 1 for level I, as the game names it; the amplifier is one less. */
    public int level;

    public SicknessEffect() { }

    public SicknessEffect(String effect, int chance, int seconds, int level) {
        this.effect = effect;
        this.chance = chance;
        this.seconds = seconds;
        this.level = level;
    }

    public SicknessEffect copy() {
        return new SicknessEffect(effect, chance, seconds, level);
    }

    /** The name the tables use for {@code difficulty}. */
    public static String key(Difficulty difficulty) {
        return difficulty.name().toLowerCase(Locale.ROOT);
    }

    /**
     * The tables a fresh config holds. They began as the Realistic preset's numbers, turned into lines
     * that roll on their own: Realistic's one roll gave Poisoning or Upset Stomach, and Poisoning brought
     * Upset Stomach with it, so Upset Stomach's chance is the two added together. Since then Poison is
     * more likely on every grade that can give it, and Clean water, whose water is only a little off,
     * makes the player ill for a shorter time than Murky or Dirty. Peaceful still gives only the taste.
     */
    public static Map<String, Map<String, List<SicknessEffect>>> defaults() {
        Map<String, Map<String, List<SicknessEffect>>> tables = new LinkedHashMap<>();
        tables.put("peaceful", grades(
                List.of(taste()),
                List.of(taste()),
                List.of(),
                List.of()));
        tables.put("easy", grades(
                List.of(taste(), upset(65, 45, 1), poison(25, 10)),
                List.of(taste(), upset(35, 45, 1), poison(10, 10)),
                List.of(upset(5, 20, 1), poison(3, 5)),
                List.of()));
        tables.put("normal", grades(
                List.of(taste(), upset(75, 60, 2), poison(35, 20)),
                List.of(taste(), upset(50, 60, 1), poison(18, 20)),
                List.of(upset(12, 30, 1), poison(5, 8)),
                List.of()));
        tables.put("hard", grades(
                List.of(taste(), upset(78, 90, 2), poison(45, 30)),
                List.of(taste(), upset(66, 90, 2), poison(30, 30)),
                List.of(upset(20, 45, 1), poison(10, 12)),
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

    private static SicknessEffect taste() {
        return new SicknessEffect(NAUSEA, 100, TASTE_SECONDS, 1);
    }

    private static SicknessEffect upset(int chance, int seconds, int level) {
        return new SicknessEffect(UPSET_STOMACH, chance, seconds, level);
    }

    private static SicknessEffect poison(int chance, int seconds) {
        return new SicknessEffect(POISON, chance, seconds, 1);
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SicknessEffect)) return false;
        SicknessEffect line = (SicknessEffect) other;
        return Objects.equals(effect, line.effect) && chance == line.chance && seconds == line.seconds
                && level == line.level;
    }

    @Override
    public int hashCode() {
        return Objects.hash(effect, chance, seconds, level);
    }

    @Override
    public String toString() {
        return effect + " " + chance + "% " + seconds + "s L" + level;
    }
}
