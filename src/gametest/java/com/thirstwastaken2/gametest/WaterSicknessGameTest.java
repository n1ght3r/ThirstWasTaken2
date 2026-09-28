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
 * The sickness tables: every line the config lists for the difficulty and the grade rolls on its own.
 * The rolls are forced rather than drawn, so every test is exact. Players are fresh and never ticked, so
 * their effects are exactly what the drink gave.
 */
public final class WaterSicknessGameTest {
    private static final int DIRTY = 0;
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
            // Only the taste is certain: every drink of Dirty water leaves Nausea, and nothing else.
            ServerPlayer lucky = TestFixtures.mockPlayer(helper);
            WaterSickness.drink(lucky, DIRTY, difficulty, () -> BEST);
            TestFixtures.check(helper, lucky.hasEffect(MobEffects.NAUSEA) && lucky.getActiveEffects().size() == 1,
                    difficulty + ": the best roll on Dirty water leaves only the taste, got " + lucky.getActiveEffects());

            ServerPlayer pure = TestFixtures.mockPlayer(helper);
            WaterSickness.drink(pure, PURE, difficulty, () -> WORST);
            TestFixtures.check(helper, pure.getActiveEffects().isEmpty(),
                    difficulty + ": Pure water never makes anyone ill, got " + pure.getActiveEffects());
        }
        ServerPlayer unlucky = TestFixtures.mockPlayer(helper);
        WaterSickness.drink(unlucky, DIRTY, Difficulty.HARD, () -> WORST);
        MobEffectInstance upset = Vanilla.getEffect(unlucky, ThirstEffects.UPSET_STOMACH);
        TestFixtures.check(helper, upset != null && upset.getAmplifier() == 1 && unlucky.hasEffect(MobEffects.POISON),
                "the worst roll on Hard gives Upset Stomach II and Poison, got " + unlucky.getActiveEffects());
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
        helper.succeed();
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
