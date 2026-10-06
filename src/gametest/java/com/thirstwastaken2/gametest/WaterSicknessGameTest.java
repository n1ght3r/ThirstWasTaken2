package com.thirstwastaken2.gametest;

import com.thirstwastaken2.config.SicknessEffect;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.effect.ThirstEffects;
import com.thirstwastaken2.effect.WaterSickness;
import com.thirstwastaken2.purity.WaterQuality;
import com.thirstwastaken2.platform.Vanilla;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The sickness tables: every line the config lists for the difficulty and the grade gives its effect
 * when its roll lands under its chance, lines of one group sharing a roll. The rolls are forced rather
 * than drawn, so every test is exact. Players are fresh and never ticked, so
 * their effects are exactly what the drink gave.
 */
public final class WaterSicknessGameTest {
    private static final int DIRTY = 0;
    private static final int CLEAN = 2;
    private static final int PURE = 3;
    /** A roll no line under 100% passes, and one every line over 0% passes. */
    private static final float WORST = 0.0F;
    private static final float BEST = 0.999F;

    @GameTest
    public void eachLineGivesItsEffectWhenItRollsUnderItsChance(GameTestHelper helper) {
        TestFixtures.withConfig(config -> config.sicknessEffects = everyDifficulty(List.of(
                new SicknessEffect("minecraft:poison", 60, 5, 2),
                new SicknessEffect("minecraft:weakness", 50, 5, 1),
                new SicknessEffect("nosuchmod:no_such_effect", 100, 5, 1))), () -> {
            ServerPlayer player = TestFixtures.mockPlayer(helper);
            // Every roll is 0.5: under Poison's 60%, not under Weakness's 50%.
            WaterSickness.drink(player, DIRTY, Difficulty.NORMAL, () -> 0.5F);
            MobEffectInstance poison = Vanilla.getEffect(player, Vanilla.poison());
            TestFixtures.check(helper, poison != null && poison.getAmplifier() == 1 && poison.getDuration() == 5 * 20,
                    "a line rolling under its chance gives its effect at its level and seconds, got " + poison);
            TestFixtures.check(helper, !player.hasEffect(MobEffects.WEAKNESS),
                    "a line rolling at its chance gives nothing, got " + player.getActiveEffects());
            TestFixtures.check(helper, player.getActiveEffects().size() == 1,
                    "an effect nothing registers should be skipped, got " + player.getActiveEffects());

            ServerPlayer other = TestFixtures.mockPlayer(helper);
            WaterSickness.drink(other, PURE, Difficulty.NORMAL, () -> WORST);
            TestFixtures.check(helper, other.getActiveEffects().isEmpty(),
                    "a grade the table leaves empty gives nothing, got " + other.getActiveEffects());
        });
        helper.succeed();
    }

    @GameTest
    public void eachDifficultyReadsItsOwnTable(GameTestHelper helper) {
        TestFixtures.withConfig(config -> {
            Map<String, Map<String, List<SicknessEffect>>> tables = everyDifficulty(List.of());
            tables.get("peaceful").put("dirty", new ArrayList<>(List.of(new SicknessEffect("minecraft:hunger", 100, 5, 1))));
            tables.get("easy").put("dirty", new ArrayList<>(List.of(new SicknessEffect("minecraft:weakness", 100, 5, 1))));
            tables.get("normal").put("dirty", new ArrayList<>(List.of(new SicknessEffect("minecraft:poison", 100, 5, 1))));
            tables.get("hard").put("dirty", new ArrayList<>(List.of(new SicknessEffect("minecraft:nausea", 100, 5, 1))));
            config.sicknessEffects = tables;
        }, () -> {
            check(helper, Difficulty.PEACEFUL, player -> player.hasEffect(MobEffects.HUNGER));
            check(helper, Difficulty.EASY, player -> player.hasEffect(MobEffects.WEAKNESS));
            check(helper, Difficulty.NORMAL, player -> player.hasEffect(MobEffects.POISON));
            check(helper, Difficulty.HARD, player -> player.hasEffect(MobEffects.NAUSEA));
        });
        helper.succeed();
    }

    @GameTest
    public void aDrinkReadsTheTableForTheWorldsDifficulty(GameTestHelper helper) {
        TestFixtures.withConfig(config -> config.sicknessEffects = everyDifficulty(
                List.of(new SicknessEffect("minecraft:weakness", 100, 3, 1))), () -> {
            ServerPlayer player = TestFixtures.mockPlayer(helper);
            WaterSickness.drink(player, (WaterQuality.Fresh) WaterQuality.fresh(DIRTY));
            TestFixtures.check(helper, player.hasEffect(MobEffects.WEAKNESS) && !player.hasEffect(MobEffects.NAUSEA),
                    "dirty water gives the table's effects and nothing else, got " + player.getActiveEffects());
        });
        helper.succeed();
    }

    @GameTest
    public void theDefaultTablesKeepTheSicknessDesign(GameTestHelper helper) {
        for (Difficulty difficulty : Difficulty.values()) {
            // Nothing is certain any more: the best roll on Dirty water gives nothing at all.
            ServerPlayer lucky = TestFixtures.mockPlayer(helper);
            WaterSickness.drink(lucky, DIRTY, difficulty, () -> BEST);
            TestFixtures.check(helper, lucky.getActiveEffects().isEmpty() || difficulty == Difficulty.HARD,
                    difficulty + ": the best roll on Dirty water should give nothing, got " + lucky.getActiveEffects());

            for (int grade = CLEAN; grade <= PURE; grade++) {
                ServerPlayer treated = TestFixtures.mockPlayer(helper);
                WaterSickness.drink(treated, grade, difficulty, () -> WORST);
                TestFixtures.check(helper, treated.getActiveEffects().isEmpty(),
                        difficulty + ": grade " + grade + " water never makes anyone ill, got " + treated.getActiveEffects());
            }
        }
        ServerPlayer peaceful = TestFixtures.mockPlayer(helper);
        WaterSickness.drink(peaceful, DIRTY, Difficulty.PEACEFUL, () -> WORST);
        TestFixtures.check(helper, peaceful.getActiveEffects().isEmpty(),
                "no water makes anyone ill on Peaceful, got " + peaceful.getActiveEffects());

        // Normal Dirty's worst roll: Upset Stomach II for 60 seconds and Poison for 20.
        ServerPlayer unlucky = TestFixtures.mockPlayer(helper);
        WaterSickness.drink(unlucky, DIRTY, Difficulty.NORMAL, () -> WORST);
        MobEffectInstance upset = Vanilla.getEffect(unlucky, ThirstEffects.UPSET_STOMACH);
        MobEffectInstance poison = Vanilla.getEffect(unlucky, Vanilla.poison());
        TestFixtures.check(helper, upset != null && upset.getAmplifier() == 1 && upset.getDuration() == 60 * 20
                        && poison != null && poison.getDuration() == 20 * 20,
                "the worst roll on Normal Dirty should give Upset Stomach II for 60 s and Poison for 20 s, got "
                        + unlucky.getActiveEffects());
        helper.succeed();
    }

    /** Upset Stomach and Poison share one roll by default, so Poison never comes without Upset Stomach. */
    @GameTest
    public void poisonOnlyEverComesWithUpsetStomach(GameTestHelper helper) {
        for (int grade = DIRTY; grade < CLEAN; grade++) {
            for (Difficulty difficulty : Difficulty.values()) {
                for (int percent = 0; percent < 100; percent++) {
                    float roll = percent / 100.0F;
                    ServerPlayer player = TestFixtures.mockPlayer(helper);
                    WaterSickness.drink(player, grade, difficulty, () -> roll);
                    boolean poisoned = Vanilla.getEffect(player, Vanilla.poison()) != null;
                    boolean upset = Vanilla.getEffect(player, ThirstEffects.UPSET_STOMACH) != null;
                    TestFixtures.check(helper, !poisoned || upset,
                            difficulty + " grade " + grade + " at roll " + roll + " gave Poison without Upset Stomach");
                }
            }
        }
        helper.succeed();
    }

    /** One roll per group in the order it first comes up, and one for every line with none. */
    @GameTest
    public void aGroupSharesOneRoll(GameTestHelper helper) {
        TestFixtures.withConfig(config -> config.sicknessEffects = everyDifficulty(List.of(
                new SicknessEffect("minecraft:weakness", 60, 5, 1, "g"),
                new SicknessEffect("minecraft:hunger", 50, 5, 1),
                new SicknessEffect("minecraft:luck", 40, 5, 1, "g"))), () -> {
            ServerPlayer player = TestFixtures.mockPlayer(helper);
            float[] rolls = {0.5F, 0.9F, 0.0F};
            int[] drawn = {0};
            WaterSickness.drink(player, DIRTY, Difficulty.NORMAL, () -> rolls[drawn[0]++]);
            TestFixtures.check(helper, drawn[0] == 2, "two rolls should be drawn, one per group or loner, got " + drawn[0]);
            TestFixtures.check(helper, player.hasEffect(MobEffects.WEAKNESS) && !player.hasEffect(MobEffects.HUNGER)
                            && !player.hasEffect(MobEffects.LUCK),
                    "0.5 is under Weakness's 60 but not Luck's 40 on the same roll, and 0.9 misses Hunger, got "
                            + player.getActiveEffects());
        });
        helper.succeed();
    }

    /**
     * Upset Stomach again while ill adds half the new time, held to one and a half times it, and never
     * shortens what is left: {@code max(R, min(R + D / 2, 1.5 * D))}, in ticks.
     */
    @GameTest
    public void upsetStomachAgainAddsHalfItsTimeUpToOneAndAHalf(GameTestHelper helper) {
        int ticks = 60 * 20;
        TestFixtures.withConfig(config -> config.sicknessEffects = everyDifficulty(
                List.of(new SicknessEffect(SicknessEffect.UPSET_STOMACH, 100, 60, 1))), () -> {
            ServerPlayer player = TestFixtures.mockPlayer(helper);
            WaterSickness.drink(player, DIRTY, Difficulty.NORMAL, () -> WORST);
            TestFixtures.check(helper, upsetTicks(player) == ticks, "a first drink gives its time, got " + upsetTicks(player));
            WaterSickness.drink(player, DIRTY, Difficulty.NORMAL, () -> WORST);
            TestFixtures.check(helper, upsetTicks(player) == ticks * 3 / 2,
                    "a second drink adds half, up to one and a half times, got " + upsetTicks(player));
            WaterSickness.drink(player, DIRTY, Difficulty.NORMAL, () -> WORST);
            TestFixtures.check(helper, upsetTicks(player) == ticks * 3 / 2,
                    "a third drink stays at one and a half times, got " + upsetTicks(player));

            ServerPlayer longer = TestFixtures.mockPlayer(helper);
            longer.addEffect(Vanilla.effectInstance(ThirstEffects.UPSET_STOMACH, 3 * ticks, 0));
            WaterSickness.drink(longer, DIRTY, Difficulty.NORMAL, () -> WORST);
            TestFixtures.check(helper, upsetTicks(longer) == 3 * ticks,
                    "a longer illness already there should not be cut short, got " + upsetTicks(longer));
        });
        // Fractions of a second are kept in ticks: half of 1201 ticks rounds down, once.
        TestFixtures.check(helper, WaterSickness.extended(true, 1000, 1201) == 1600,
                "1000 left and 1201 more should give 1600, got " + WaterSickness.extended(true, 1000, 1201));
        helper.succeed();
    }

    @GameTest
    public void drinkingAgainWhileIllExtendsTheEffectUpToTwiceItsTime(GameTestHelper helper) {
        int ticks = 10 * 20;
        TestFixtures.withConfig(config -> config.sicknessEffects = everyDifficulty(
                List.of(new SicknessEffect("minecraft:weakness", 100, 10, 1))), () -> {
            ServerPlayer player = TestFixtures.mockPlayer(helper);
            WaterSickness.drink(player, DIRTY, Difficulty.NORMAL, () -> WORST);
            WaterSickness.drink(player, DIRTY, Difficulty.NORMAL, () -> WORST);
            MobEffectInstance twice = player.getEffect(MobEffects.WEAKNESS);
            TestFixtures.check(helper, twice != null && twice.getDuration() == 2 * ticks,
                    "a second drink should add the line's time, got " + twice);
            WaterSickness.drink(player, DIRTY, Difficulty.NORMAL, () -> WORST);
            TestFixtures.check(helper, player.getEffect(MobEffects.WEAKNESS).getDuration() == 2 * ticks,
                    "a third drink should stop at twice the line's time, got " + player.getEffect(MobEffects.WEAKNESS));

            ServerPlayer stronger = TestFixtures.mockPlayer(helper);
            stronger.addEffect(Vanilla.effectInstance(Vanilla.mobEffect(Identifier.parse("minecraft:weakness")), 20, 2));
            WaterSickness.drink(stronger, DIRTY, Difficulty.NORMAL, () -> WORST);
            MobEffectInstance kept = stronger.getEffect(MobEffects.WEAKNESS);
            TestFixtures.check(helper, kept.getAmplifier() == 2 && kept.getDuration() == 20 + ticks,
                    "extending should keep the higher level it already had, got " + kept);
        });
        // The control: switched off, the second drink leaves the time as it was.
        TestFixtures.withConfig(config -> {
            config.extendSicknessEffects = false;
            config.sicknessEffects = everyDifficulty(List.of(new SicknessEffect("minecraft:weakness", 100, 10, 1)));
        }, () -> {
            ServerPlayer player = TestFixtures.mockPlayer(helper);
            WaterSickness.drink(player, DIRTY, Difficulty.NORMAL, () -> WORST);
            WaterSickness.drink(player, DIRTY, Difficulty.NORMAL, () -> WORST);
            TestFixtures.check(helper, player.getEffect(MobEffects.WEAKNESS).getDuration() == ticks,
                    "switched off, drinking again should not extend, got " + player.getEffect(MobEffects.WEAKNESS));
        });
        helper.succeed();
    }

    @GameTest
    public void handEditedTablesAreClampedAndFilledIn(GameTestHelper helper) {
        TestFixtures.withConfig(config -> {
            Map<String, Map<String, List<SicknessEffect>>> tables = new LinkedHashMap<>();
            Map<String, List<SicknessEffect>> hard = new LinkedHashMap<>();
            List<SicknessEffect> dirty = new ArrayList<>();
            dirty.add(new SicknessEffect(" Minecraft:Poison ", 250, 0, 99));
            dirty.add(new SicknessEffect(null, 50, 5, 1));
            dirty.add(null);
            hard.put("dirty", dirty);
            hard.put("murky", new ArrayList<>());
            tables.put("hard", hard);
            config.sicknessEffects = tables;
        }, () -> {
            Map<String, Map<String, List<SicknessEffect>>> tables = ThirstConfig.get().sicknessEffects;
            List<SicknessEffect> dirty = tables.get("hard").get("dirty");
            TestFixtures.check(helper, dirty.equals(List.of(new SicknessEffect("minecraft:poison", 100, 1, SicknessEffect.MAX_LEVEL))),
                    "a line should be trimmed and clamped, and one with no effect dropped, got " + dirty);
            TestFixtures.check(helper, tables.get("hard").get("murky").isEmpty(),
                    "a grade the file leaves empty should stay empty, got " + tables.get("hard").get("murky"));
            TestFixtures.check(helper, tables.get("hard").get("clean").equals(SicknessEffect.defaults("hard", "clean")),
                    "a grade missing from the file should get its defaults, got " + tables.get("hard").get("clean"));
            TestFixtures.check(helper, tables.get("normal").equals(SicknessEffect.defaults().get("normal")),
                    "a difficulty missing from the file should get its defaults, got " + tables.get("normal"));
        });
        // A Poison line in Upset Stomach's group cannot be likelier than it; split apart, it may be.
        TestFixtures.withConfig(config -> config.sicknessEffects = everyDifficulty(List.of(
                new SicknessEffect(SicknessEffect.UPSET_STOMACH, 40, 10, 1, "sick"),
                new SicknessEffect(SicknessEffect.POISON, 80, 10, 1, "sick"),
                new SicknessEffect(SicknessEffect.POISON, 90, 10, 1))), () -> {
            List<SicknessEffect> dirty = ThirstConfig.get().sicknessEffects.get("normal").get("dirty");
            TestFixtures.check(helper, dirty.get(1).chance == 40 && dirty.get(2).chance == 90,
                    "grouped Poison should be held to Upset Stomach's 40 and a loner left alone, got " + dirty);
        });
        helper.succeed();
    }

    private static int upsetTicks(ServerPlayer player) {
        MobEffectInstance effect = Vanilla.getEffect(player, ThirstEffects.UPSET_STOMACH);
        return effect == null ? 0 : effect.getDuration();
    }

    /** Tables giving {@code dirty} for Dirty water on every difficulty, and nothing for any other grade. */
    static Map<String, Map<String, List<SicknessEffect>>> everyDifficulty(List<SicknessEffect> dirty) {
        Map<String, Map<String, List<SicknessEffect>>> tables = new LinkedHashMap<>();
        for (String difficulty : SicknessEffect.DIFFICULTIES) {
            Map<String, List<SicknessEffect>> grades = new LinkedHashMap<>();
            for (String grade : SicknessEffect.GRADES) grades.put(grade, new ArrayList<>());
            grades.put("dirty", new ArrayList<>(dirty));
            tables.put(difficulty, grades);
        }
        return tables;
    }

    /** A fresh player drinks Dirty water on {@code difficulty}, with every line passing its roll. */
    private static void check(GameTestHelper helper, Difficulty difficulty,
                              java.util.function.Predicate<ServerPlayer> expected) {
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        WaterSickness.drink(player, DIRTY, difficulty, () -> WORST);
        TestFixtures.check(helper, expected.test(player) && player.getActiveEffects().size() == 1,
                difficulty + " should give only its own table's effect, got " + player.getActiveEffects());
    }
}
