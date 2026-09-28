package com.thirstwastaken2.gametest;

import com.thirstwastaken2.config.SicknessPreset;
import com.thirstwastaken2.config.SicknessTable;
import com.thirstwastaken2.effect.ThirstEffects;
import com.thirstwastaken2.effect.WaterSickness;
import com.thirstwastaken2.effect.WaterSickness.PoisoningEffect;
import com.thirstwastaken2.purity.WaterQuality;
import com.thirstwastaken2.platform.Vanilla;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/**
 * The one roll per drink: the taste, then at most one illness, by difficulty and grade. The roll is
 * forced into each range of the live config's table rather than drawn, so every test is exact whatever
 * the tables say. Players are fresh and never ticked, so the effects are exactly what the drink gave.
 */
public final class WaterSicknessGameTest {
    private static final int DIRTY = 0;
    private static final int MURKY = 1;
    private static final int CLEAN = 2;
    private static final int PURE = 3;
    /** Below the smallest step between two ranges, so a forced roll lands inside the one aimed at. */
    private static final float INSIDE = 0.5F;

    @GameTest
    public void easyGivesEachOutcomeOfItsTable(GameTestHelper helper) {
        checkEveryOutcome(helper, Difficulty.EASY);
        helper.succeed();
    }

    @GameTest
    public void normalGivesEachOutcomeOfItsTable(GameTestHelper helper) {
        checkEveryOutcome(helper, Difficulty.NORMAL);
        helper.succeed();
    }

    @GameTest
    public void hardGivesEachOutcomeOfItsTable(GameTestHelper helper) {
        checkEveryOutcome(helper, Difficulty.HARD);
        helper.succeed();
    }

    @GameTest
    public void peacefulGivesOnlyTheTaste(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        WaterSickness.drink(player, DIRTY, Difficulty.PEACEFUL, 0.0F);
        checkOnlyTheTaste(helper, player, "Peaceful, dirty water, the worst roll");
        helper.succeed();
    }

    @GameTest
    public void pureWaterGivesNothingOnHard(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        WaterSickness.drink(player, PURE, Difficulty.HARD, 0.0F);
        TestFixtures.check(helper, player.getActiveEffects().isEmpty(),
                "pure water must never make anyone ill, got " + player.getActiveEffects());
        helper.succeed();
    }

    @GameTest
    public void cleanWaterHasNoTaste(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        WaterSickness.drink(player, CLEAN, Difficulty.HARD, 99.9F);
        TestFixtures.check(helper, player.getActiveEffects().isEmpty(),
                "clean water that rolls nothing leaves nothing, not even the taste, got " + player.getActiveEffects());
        helper.succeed();
    }

    @GameTest
    public void theSameIllnessAgainExtendsItAndRaisesUpsetStomach(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        int ticks = WaterSickness.upsetStomachTicks(Difficulty.NORMAL);
        WaterSickness.catchIllness(player, Difficulty.NORMAL, WaterSickness.Outcome.UPSET_STOMACH, 0);
        WaterSickness.catchIllness(player, Difficulty.NORMAL, WaterSickness.Outcome.UPSET_STOMACH, 0);

        MobEffectInstance upset = Vanilla.getEffect(player, ThirstEffects.UPSET_STOMACH);
        TestFixtures.check(helper, upset != null && upset.getAmplifier() == 1,
                "catching Upset Stomach I again should make it II, got " + upset);
        TestFixtures.check(helper, upset.getDuration() > ticks && upset.getDuration() <= 2 * ticks,
                "catching it again should extend it, up to twice its time, got " + upset.getDuration()
                        + " ticks against " + ticks);

        WaterSickness.catchIllness(player, Difficulty.NORMAL, WaterSickness.Outcome.UPSET_STOMACH, 0);
        TestFixtures.check(helper, Vanilla.getEffect(player, ThirstEffects.UPSET_STOMACH).getDuration() == 2 * ticks,
                "a third time should stop at twice its time, got "
                        + Vanilla.getEffect(player, ThirstEffects.UPSET_STOMACH).getDuration());
        helper.succeed();
    }

    @GameTest
    public void aWorseIllnessAddsItsEffectsAndAMilderOneDoesNothing(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        WaterSickness.catchIllness(player, Difficulty.HARD, WaterSickness.Outcome.UPSET_STOMACH, 0);
        WaterSickness.catchIllness(player, Difficulty.HARD, WaterSickness.Outcome.POISONING, 0);
        TestFixtures.check(helper, WaterSickness.current(player) == WaterSickness.Outcome.POISONING,
                "Poisoning over Upset Stomach should add its effects, got " + player.getActiveEffects());

        MobEffectInstance before = Vanilla.getEffect(player, ThirstEffects.UPSET_STOMACH);
        WaterSickness.catchIllness(player, Difficulty.HARD, WaterSickness.Outcome.UPSET_STOMACH, 0);
        MobEffectInstance after = Vanilla.getEffect(player, ThirstEffects.UPSET_STOMACH);
        TestFixtures.check(helper, after.getDuration() == before.getDuration() && after.getAmplifier() == before.getAmplifier(),
                "Upset Stomach caught while poisoned is milder and should change nothing, went from " + before
                        + " to " + after);
        helper.succeed();
    }

    @GameTest
    public void theClassicPresetKeepsTheOldRoll(GameTestHelper helper) {
        TestFixtures.withConfig(config -> config.sicknessPreset = SicknessPreset.CLASSIC, () -> {
            ServerPlayer player = TestFixtures.mockPlayer(helper);
            WaterSickness.drink(player, (WaterQuality.Fresh) WaterQuality.fresh(DIRTY));
            MobEffectInstance nausea = player.getEffect(MobEffects.NAUSEA);
            TestFixtures.check(helper, nausea != null && nausea.getDuration() == 12 * 20,
                    "classic dirty water always gives 12 s of Nausea, got " + nausea);
            TestFixtures.check(helper, !Vanilla.hasEffect(player, ThirstEffects.UPSET_STOMACH),
                    "classic never gives Upset Stomach, got " + player.getActiveEffects());
        });
        helper.succeed();
    }

    /** Forces the roll into every range of every grade that has one, each on a fresh player. */
    private static void checkEveryOutcome(GameTestHelper helper, Difficulty difficulty) {
        SicknessTable table = SicknessTable.of(difficulty);
        for (int grade = DIRTY; grade <= CLEAN; grade++) {
            String where = difficulty + ", grade " + grade;
            int poisoning = table.poisoningChance()[grade];
            int upset = table.upsetStomachChance()[grade];
            int level = table.upsetStomachLevel()[grade] - 1;
            if (poisoning > 0) {
                ServerPlayer player = TestFixtures.mockPlayer(helper);
                WaterSickness.drink(player, grade, difficulty, poisoning - INSIDE);
                checkPoisoning(helper, player, difficulty, grade, level, where);
            }
            if (upset > 0) {
                ServerPlayer player = TestFixtures.mockPlayer(helper);
                WaterSickness.drink(player, grade, difficulty, poisoning + upset - INSIDE);
                checkUpsetStomach(helper, player, difficulty, grade, level, where);
            }
            if (poisoning + upset < 100) {
                ServerPlayer player = TestFixtures.mockPlayer(helper);
                WaterSickness.drink(player, grade, difficulty, 99.9F);
                if (grade == CLEAN) {
                    TestFixtures.check(helper, player.getActiveEffects().isEmpty(),
                            where + ": a lucky roll on clean water should leave nothing, got " + player.getActiveEffects());
                } else {
                    checkOnlyTheTaste(helper, player, where + ", a lucky roll");
                }
            }
        }
    }

    private static void checkPoisoning(GameTestHelper helper, ServerPlayer player, Difficulty difficulty, int grade,
                                       int level, String where) {
        PoisoningEffect[] expected = WaterSickness.poisoning(difficulty);
        for (PoisoningEffect effect : expected) {
            MobEffectInstance got = Vanilla.getEffect(player, effect.effect());
            TestFixtures.check(helper, got != null && got.getAmplifier() == effect.amplifier()
                            && got.getDuration() == effect.ticks(),
                    where + ": Poisoning should give " + effect + ", got " + got);
        }
        checkUpsetStomachEffect(helper, player, difficulty, level, where + ", Poisoning");
        int count = expected.length + 1 + (grade == CLEAN ? 0 : 1);
        TestFixtures.check(helper, player.getActiveEffects().size() == count,
                where + ": Poisoning should give exactly its " + expected.length
                        + " effects, Upset Stomach and the taste, got " + player.getActiveEffects());
    }

    private static void checkUpsetStomach(GameTestHelper helper, ServerPlayer player, Difficulty difficulty, int grade,
                                          int level, String where) {
        checkUpsetStomachEffect(helper, player, difficulty, level, where);
        int count = 1 + (grade == CLEAN ? 0 : 1);
        TestFixtures.check(helper, player.getActiveEffects().size() == count,
                where + ": Upset Stomach should come alone, with the taste, got " + player.getActiveEffects());
    }

    private static void checkUpsetStomachEffect(GameTestHelper helper, ServerPlayer player, Difficulty difficulty,
                                                int level, String where) {
        MobEffectInstance upset = Vanilla.getEffect(player, ThirstEffects.UPSET_STOMACH);
        TestFixtures.check(helper, upset != null && upset.getAmplifier() == level
                        && upset.getDuration() == WaterSickness.upsetStomachTicks(difficulty),
                where + ": should give Upset Stomach " + (level + 1) + " for "
                        + WaterSickness.upsetStomachTicks(difficulty) + " ticks, got " + upset);
    }

    private static void checkOnlyTheTaste(GameTestHelper helper, ServerPlayer player, String where) {
        MobEffectInstance nausea = player.getEffect(MobEffects.NAUSEA);
        TestFixtures.check(helper, nausea != null && nausea.getDuration() == WaterSickness.TASTE_TICKS
                        && player.getActiveEffects().size() == 1,
                where + ": should give only the taste of Nausea, got " + player.getActiveEffects());
    }
}
