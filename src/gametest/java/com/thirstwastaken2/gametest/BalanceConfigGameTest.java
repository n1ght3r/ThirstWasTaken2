package com.thirstwastaken2.gametest;

import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.effect.ThirstEffects;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import com.thirstwastaken2.platform.Vanilla;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;

/**
 * The numbers modpack makers can change, which used to be constants: each follows the config the
 * moment it changes, with nothing cached. The default values themselves are what the other tests,
 * written against the defaults, already check.
 */
public final class BalanceConfigGameTest {
    @GameTest
    public void quenchedFollowsTheGradeShare(GameTestHelper helper) {
        TestFixtures.withConfig(config -> config.quenchedPercent[3] = 50, () -> {
            int quenched = WaterPurity.quenched(WaterQuality.fresh(3), 8);
            TestFixtures.check(helper, quenched == 4, "Pure water at 50% should quench 4 of 8, got " + quenched);
        });
        helper.succeed();
    }

    @GameTest
    public void seaWaterEffectsFollowTheirSeconds(GameTestHelper helper) {
        ServerPlayer player = TestFixtures.mockPlayer(helper);
        TestFixtures.withConfig(config -> {
            config.seaWaterNauseaSeconds = 0;
            config.seaWaterParchedSeconds = 10;
        }, () -> {
            WaterPurity.applyEffects(player, bowl(WaterQuality.SALT));
            TestFixtures.check(helper, !player.hasEffect(MobEffects.NAUSEA), "0 seconds of Nausea should give none");
            MobEffectInstance parched = Vanilla.getEffect(player, ThirstEffects.PARCHED);
            TestFixtures.check(helper, parched != null && parched.getDuration() == 200,
                    "Parched should last 10 seconds, got " + parched);
        });
        helper.succeed();
    }

    @GameTest
    public void naturalWaterGradesFollowTheConfig(GameTestHelper helper) {
        TestFixtures.withConfig(config -> {
            config.rainwaterQuality = 0;
            config.dripstoneQuality = 1;
        }, () -> TestFixtures.check(helper, WaterPurity.rainwaterQuality() == 0 && WaterPurity.dripstoneQuality() == 1,
                "rain and dripstone grades should follow the config, got " + WaterPurity.rainwaterQuality()
                        + " and " + WaterPurity.dripstoneQuality()));
        helper.succeed();
    }

    @GameTest
    public void canteenHoldsWhatTheConfigSays(GameTestHelper helper) {
        TestFixtures.withConfig(config -> config.copperCanteenCapacity = 2, () -> {
            ItemStack canteen = new ItemStack(ThirstItems.COPPER_CANTEEN);
            WaterskinItem.addWater(canteen, 3, 4);
            TestFixtures.check(helper, WaterskinItem.capacity(canteen) == 2 && WaterskinItem.servings(canteen) == 2,
                    "a canteen of capacity 2 should take 2 of 4 servings, holds " + WaterskinItem.servings(canteen));
        });
        helper.succeed();
    }

    @GameTest
    public void boilTimesFollowTheConfig(GameTestHelper helper) {
        TestFixtures.withConfig(config -> {
            config.ironFlaskBoilSeconds = 10;
            config.ironHangingPotBoilSeconds = 12;
        }, () -> {
            int flask = WaterskinItem.boilTicksPerServing(new ItemStack(ThirstItems.IRON_FLASK));
            TestFixtures.check(helper, flask == 200, "the flask should take 200 ticks a serving, got " + flask);
            int pot = ThirstBlocks.IRON_HANGING_POT.secondsPerServing();
            TestFixtures.check(helper, pot == 12, "the iron pot should take 12 seconds a serving, got " + pot);
        });
        helper.succeed();
    }

    @GameTest
    public void boilingInHandCanBeSwitchedOff(GameTestHelper helper) {
        TestFixtures.withConfig(config -> config.enableBoilingInHand = false, () -> {
            int canteen = WaterskinItem.boilTicksPerServing(new ItemStack(ThirstItems.COPPER_CANTEEN));
            TestFixtures.check(helper, canteen == 0, "with boiling in hand off the canteen should not boil, got " + canteen);
        });
        helper.succeed();
    }

    private static ItemStack bowl(WaterQuality quality) {
        return WaterPurity.setQuality(new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), quality);
    }
}
