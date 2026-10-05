package com.thirstwastaken2.gametest;

import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.purity.WaterPurity;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The villager trades the mod adds: novice leatherworkers sell waterskins and novice clerics sell
 * clean water. Up to 1.21.11 they are code, from 26.1 data; these ask real villagers either way.
 */
public final class TradeGameTest {
    /**
     * A novice draws two of its level's trades, and the mod's is one of four for a leatherworker and of
     * three for a cleric, so at least half the villagers offer it and this many cannot all miss by chance.
     */
    private static final int VILLAGERS = 40;
    private static final Identifier LEATHERWORKER = Identifier.withDefaultNamespace("leatherworker");
    private static final Identifier CLERIC = Identifier.withDefaultNamespace("cleric");

    @GameTest
    public void noviceLeatherworkersSellWaterskins(GameTestHelper helper) {
        int offered = 0;
        for (int i = 0; i < VILLAGERS; i++) {
            for (ItemStack result : TestFixtures.noviceOffers(helper, LEATHERWORKER)) {
                if (result.is(ThirstItems.WATERSKIN)) offered++;
            }
        }
        TestFixtures.check(helper, offered > 0,
                "a novice leatherworker should sell a waterskin in " + VILLAGERS + " villagers, got none");
        helper.succeed();
    }

    @GameTest
    public void noviceClericsSellCleanWater(GameTestHelper helper) {
        int offered = 0;
        for (int i = 0; i < VILLAGERS; i++) {
            for (ItemStack result : TestFixtures.noviceOffers(helper, CLERIC)) {
                if (!result.is(Items.POTION)) continue;
                TestFixtures.check(helper, WaterPurity.isStamped(result) && !WaterPurity.isSalty(result)
                                && WaterPurity.get(result) == 2,
                        "a cleric's water should be stamped fresh and Clean, got " + WaterPurity.quality(result));
                offered++;
            }
        }
        TestFixtures.check(helper, offered > 0,
                "a novice cleric should sell clean water in " + VILLAGERS + " villagers, got none");
        helper.succeed();
    }

    // From 26.1 the trade is a data file, decided when the world loads, not when a villager draws it;
    // ModItemsGameTest checks its load condition with the recipes'.
    //? if <26.1 {
    /*@GameTest
    public void aSwitchedOffWaterskinIsNotSold(GameTestHelper helper) {
        TestFixtures.withConfig(config -> config.enableWaterskin = false, () -> {
            for (int i = 0; i < VILLAGERS; i++) {
                for (ItemStack result : TestFixtures.noviceOffers(helper, LEATHERWORKER)) {
                    TestFixtures.check(helper, !result.is(ThirstItems.WATERSKIN),
                            "a leatherworker should not sell a waterskin while the config switches it off");
                }
            }
        });
        helper.succeed();
    }
    *///?}
}
