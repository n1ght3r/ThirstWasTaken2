package com.thirstwastaken2.extradelight;

import com.thirstwastaken2.neoforge.WaterFluids;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

import java.util.function.Supplier;

/**
 * The grades Extra Delight's water is given where it has none, and what its recipes ask of it. Loaded
 * only from the mixins, which only apply with the mod installed.
 */
public final class ExtraDelightWater {
    /**
     * Murky: the Tap and the Sink Cabinet pour water from nothing, for ever. The grade every other endless
     * sink gives, Candlelight's, Hearth and Harvest's and Farm & Charm's trough, so every sink in a pack
     * gives the same water.
     */
    public static final WaterQuality TAP = WaterQuality.fresh(1);

    /**
     * How deep the server thread is inside one of Extra Delight's own transfers between a tank and a
     * container in hand or in a slot. Only there does {@code FluidUtilMixin} wrap a container's handler,
     * so no other mod's use of NeoForge's {@code FluidUtil} changes.
     */
    private static final ThreadLocal<int[]> TRANSFER = ThreadLocal.withInitial(() -> new int[1]);

    private ExtraDelightWater() { }

    /** Runs {@code transfer} with containers' handlers carrying the grade; see {@link #TRANSFER}. */
    public static <T> T graded(Supplier<T> transfer) {
        int[] depth = TRANSFER.get();
        depth[0]++;
        try {
            return transfer.get();
        } finally {
            depth[0]--;
        }
    }

    public static boolean inTransfer() {
        return TRANSFER.get()[0] > 0;
    }

    /**
     * Ice melted in the Chiller or the Melting Pot: frozen fallen water, so rainwater, the grade a cauldron
     * left in the rain gets. Read each time, since the config can change.
     */
    public static WaterQuality meltwater() {
        return WaterQuality.fresh(WaterPurity.rainwaterPurity());
    }

    /** A copy of {@code stack} stamped with {@code quality} if it is water, else {@code stack} itself. */
    public static FluidStack stamped(FluidStack stack, WaterQuality quality) {
        return WaterFluids.isWater(stack) ? WaterFluids.stamp(stack.copy(), quality) : stack;
    }

    /** Whether any of {@code fluids} is sea water. */
    public static boolean holdsSeaWater(Iterable<FluidStack> fluids) {
        for (FluidStack fluid : fluids) {
            if (isSeaWater(fluid)) return true;
        }
        return false;
    }

    public static boolean isSeaWater(FluidStack fluid) {
        return WaterFluids.isWater(fluid) && WaterFluids.quality(fluid) == WaterQuality.SALT;
    }

    /** Whether {@code stack} is a container of sea water: a bottle, a bucket or a bowl. */
    public static boolean isSeaWater(ItemStack stack) {
        return WaterPurity.isSalty(stack);
    }
}
