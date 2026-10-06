package com.thirstwastaken2.neoforge;

import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.tags.FluidTags;
import net.neoforged.neoforge.transfer.fluid.FluidResource;

/**
 * {@link WaterFluids} for the transfer API's {@link FluidResource}, which carries the same data
 * components as a fluid stack, so the same one-component rule applies.
 */
public final class WaterFluidResources {
    private WaterFluidResources() { }

    public static boolean isWater(FluidResource resource) {
        return !resource.isEmpty() && resource.is(FluidTags.WATER);
    }

    /** Unstamped water counts as {@code defaultQuality}, as it does on a fluid stack. */
    public static WaterQuality quality(FluidResource resource) {
        return WaterFluids.quality(resource.toStack(1));
    }

    /** Water with {@code quality} written on it. Anything else comes back as it was. */
    public static FluidResource stamp(FluidResource resource, WaterQuality quality) {
        return isWater(resource) ? FluidResource.of(WaterFluids.stamp(resource.toStack(1), quality)) : resource;
    }

    /** Water without its quality, the plain water other mods compare against. */
    public static FluidResource unstamped(FluidResource resource) {
        return isWater(resource) ? FluidResource.of(WaterFluids.unstamped(resource.toStack(1))) : resource;
    }
}
