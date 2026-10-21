package com.thirstwastaken2.hearthandharvestforge;

import com.thirstwastaken2.forge.WaterFluids;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;

/**
 * The grade Hearth and Harvest's water is given where it has none: the Sink, which fills itself, and
 * the sponge squeezed in a Stomping Basin. The Forge copy of {@code hearthandharvest.HearthWater}, for
 * 1.4.1 on, which brought those blocks to 1.20.1. Loaded only from the mixins, which only apply with the
 * mod installed.
 */
public final class HearthWater {
    /**
     * Murky: water left standing in a basin, or wrung out of a sponge, is not clean. The same grade as
     * on 1.21.1 and as Farm & Charm's {@code StandingWater.STANDING}, so every sink gives the same water.
     */
    public static final WaterQuality STANDING = WaterQuality.fresh(1);

    /** The one Keg result sea water may ferment into: brine is salt water anyway, and is not drunk. */
    private static final Identifier BRINE = Identifier.fromNamespaceAndPath("hearthandharvest", "brine");

    private HearthWater() { }

    /**
     * Whether the Keg may ferment {@code input} into {@code result}: anything but sea water into a drink.
     * Read only while sea water is in the keg, so the registry lookup costs nothing otherwise.
     */
    public static boolean fermentsFrom(FluidStack input, FluidStack result) {
        if (!WaterFluids.isWater(input) || WaterFluids.quality(input) != WaterQuality.SALT) return true;
        return BRINE.equals(BuiltInRegistries.FLUID.getKey(result.getFluid()));
    }

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
        if (held.isFluidStackIdentical(wanted)) return;
        tank.drain(held.getAmount(), IFluidHandler.FluidAction.EXECUTE);
        tank.fill(wanted, IFluidHandler.FluidAction.EXECUTE);
    }
}
