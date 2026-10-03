package com.thirstwastaken2.hearthandharvest;

import com.thirstwastaken2.neoforge.WaterFluids;
import com.thirstwastaken2.purity.WaterQuality;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;

/**
 * The grade Hearth and Harvest's water is given where it has none: the Sink, which fills itself, and
 * the sponge squeezed in a Stomping Basin. Loaded only from the mixins, which only apply with the mod
 * installed.
 */
public final class HearthWater {
    /**
     * Murky: water left standing in a basin, or wrung out of a sponge, is not clean. The same grade as
     * Farm & Charm's {@code StandingWater.STANDING}, so every sink in a pack gives the same water.
     */
    public static final WaterQuality STANDING = WaterQuality.fresh(1);

    private HearthWater() { }

    /** A copy of {@code stack} stamped with {@code quality} if it is water, else {@code stack} itself. */
    public static FluidStack stamped(FluidStack stack, WaterQuality quality) {
        return WaterFluids.isWater(stack) ? WaterFluids.stamp(stack.copy(), quality) : stack;
    }

    /**
     * Restamps the water already in {@code tank} as {@code quality}, if it is anything else: water saved
     * before this integration, unstamped, would otherwise refuse every fill that is stamped.
     */
    public static void settle(FluidTank tank, WaterQuality quality) {
        FluidStack held = tank.getFluid();
        if (!WaterFluids.isWater(held)) return;
        FluidStack wanted = stamped(held, quality);
        if (FluidStack.isSameFluidSameComponents(held, wanted)) return;
        tank.drain(held.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        tank.fill(wanted, IFluidHandler.FluidAction.EXECUTE);
    }
}
