package com.thirstwastaken2.gametest;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.gametest.platform.ContainerFluids;
import com.thirstwastaken2.gametest.platform.FluidMove;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;

/**
 * The waterskin and the terracotta bowls as fluid containers, through the item capability other mods'
 * pipes and tanks use. The same assertions run against NeoForge 1.21.1's {@code IFluidHandlerItem}, its
 * transfer API from 1.21.11, and Fabric's Transfer API, three implementations of one set of rules.
 * Amounts are in millibuckets on every loader.
 */
public final class ContainerFluidGameTest {
    private static final int SERVING = 250;
    private static final WaterQuality DIRTY = WaterQuality.fresh(WaterPurity.MIN);
    private static final WaterQuality MURKY = WaterQuality.fresh(1);
    private static final WaterQuality CLEAN = WaterQuality.fresh(2);
    private static final WaterQuality PURE = WaterQuality.fresh(WaterPurity.MAX);

    @GameTest
    public void aWaterskinFillsWithWholeServingsOfOneGrade(GameTestHelper helper) {
        if (skipped(helper)) return;
        FluidMove bucket = ContainerFluids.fillWater(new ItemStack(ThirstItems.WATERSKIN), MURKY, 1000);
        int capacity = ThirstConfig.get().waterskinCapacity;
        TestFixtures.check(helper, bucket.amount() == capacity * SERVING && WaterskinItem.servings(bucket.container()) == capacity
                        && WaterPurity.quality(bucket.container()).equals(MURKY),
                "a bucket of murky water should fill an empty waterskin with " + capacity + " murky servings, got "
                        + describe(bucket));

        ItemStack nearlyFull = skin(MURKY, capacity - 1);
        FluidMove same = ContainerFluids.fillWater(nearlyFull, MURKY, 500);
        TestFixtures.check(helper, same.amount() == SERVING && WaterskinItem.servings(same.container()) == capacity,
                "a waterskin with room for one serving takes one of the same grade, got " + describe(same));
        FluidMove other = ContainerFluids.fillWater(nearlyFull, PURE, 500);
        TestFixtures.check(helper, other.amount() == 0,
                "water of another grade must not be piped into a waterskin, got " + describe(other));

        FluidMove sip = ContainerFluids.fillWater(new ItemStack(ThirstItems.WATERSKIN), MURKY, 100);
        TestFixtures.check(helper, sip.amount() == 0,
                "part of a serving must be refused, got " + describe(sip));
        FluidMove lava = ContainerFluids.fillLava(new ItemStack(ThirstItems.WATERSKIN), 1000);
        TestFixtures.check(helper, lava.amount() == 0, "a waterskin must not take lava, got " + describe(lava));
        helper.succeed();
    }

    @GameTest
    public void aWaterskinEmptiesWithItsGrade(GameTestHelper helper) {
        if (skipped(helper)) return;
        FluidMove all = ContainerFluids.drain(skin(CLEAN, 3), 1000);
        TestFixtures.check(helper, all.amount() == 3 * SERVING && CLEAN.equals(all.quality())
                        && WaterskinItem.servings(all.container()) == 0 && !WaterPurity.isStamped(all.container()),
                "draining a full clean waterskin should hand out 750 mB of clean water and leave it empty "
                        + "and ungraded, got " + describe(all));

        FluidMove partial = ContainerFluids.drain(skin(CLEAN, 3), 300);
        TestFixtures.check(helper, partial.amount() == SERVING && WaterskinItem.servings(partial.container()) == 2,
                "asking for 300 mB should drain one whole serving, got " + describe(partial));

        FluidMove salt = ContainerFluids.drain(
                ContainerFluids.fillWater(new ItemStack(ThirstItems.WATERSKIN), WaterQuality.SALT, 250).container(), 250);
        TestFixtures.check(helper, WaterQuality.SALT.equals(salt.quality()),
                "sea water piped in has to come out as sea water, got " + describe(salt));
        helper.succeed();
    }

    @GameTest
    public void waterWithNoGradeFillsAsTheDefault(GameTestHelper helper) {
        if (skipped(helper)) return;
        WaterQuality expected = WaterQuality.fresh(ThirstConfig.get().defaultQuality);
        FluidMove plain = ContainerFluids.fillWater(new ItemStack(ThirstItems.WATERSKIN), null, 250);
        TestFixtures.check(helper, plain.amount() == SERVING && WaterPurity.isStamped(plain.container())
                        && WaterPurity.quality(plain.container()).equals(expected),
                "water from another mod with no grade should be graded defaultQuality, got " + describe(plain));
        helper.succeed();
    }

    @GameTest
    public void aTerracottaBowlFillsAndEmpties(GameTestHelper helper) {
        if (skipped(helper)) return;
        FluidMove filled = ContainerFluids.fillWater(new ItemStack(ThirstItems.TERRACOTTA_BOWL), DIRTY, 1000);
        TestFixtures.check(helper, filled.amount() == SERVING && filled.container().is(ThirstItems.TERRACOTTA_WATER_BOWL)
                        && WaterPurity.quality(filled.container()).equals(DIRTY),
                "an empty bowl should take one serving and become a dirty water bowl, got " + describe(filled));

        FluidMove emptied = ContainerFluids.drain(filled.container(), 1000);
        TestFixtures.check(helper, emptied.amount() == SERVING && DIRTY.equals(emptied.quality())
                        && emptied.container().is(ThirstItems.TERRACOTTA_BOWL),
                "a dirty water bowl should hand out one dirty serving and become an empty bowl, got "
                        + describe(emptied));

        FluidMove unstamped = ContainerFluids.drain(new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL), 250);
        TestFixtures.check(helper, PURE.equals(unstamped.quality()),
                "a water bowl straight from the creative tab is pure, and its water too, got " + describe(unstamped));
        helper.succeed();
    }

    private static boolean skipped(GameTestHelper helper) {
        if (ContainerFluids.available()) return false;
        ThirstWasTaken2.LOGGER.info("[ThirstGameTest] container fluid capability is not checked on this loader: {}",
                ContainerFluids.unavailable());
        helper.succeed();
        return true;
    }

    private static ItemStack skin(WaterQuality quality, int servings) {
        ItemStack skin = new ItemStack(ThirstItems.WATERSKIN);
        WaterskinItem.addWater(skin, quality, servings);
        return skin;
    }

    private static String describe(FluidMove move) {
        ItemStack container = move.container();
        String held = container.is(ThirstItems.WATERSKIN) ? WaterskinItem.servings(container) + " servings" : "";
        return move.amount() + " mB moved" + (move.quality() == null ? "" : " of " + move.quality()) + ", leaving "
                + container + " " + held + (WaterPurity.isStamped(container) ? " " + WaterPurity.quality(container) : "");
    }
}
