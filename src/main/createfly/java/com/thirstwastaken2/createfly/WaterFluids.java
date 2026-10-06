package com.thirstwastaken2.createfly;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.platform.ThirstComponents;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * Water quality on Create's fluid stacks, which carry data components the same way item stacks do.
 *
 * <p>A fluid stack keeps exactly one component for its quality, {@code water_purity} for a grade or
 * {@code water_salty} for sea water, so two stacks of the same water always compare equal and share a
 * tank or a pipe. Items get both components back when {@code WaterPurity.setQuality} stamps them on the
 * way out.
 */
public final class WaterFluids {
    /** Grades the Sand Filter adds per pass, the original mod's default. */
    private static final int FILTRATION = 1;

    private WaterFluids() { }

    public static boolean isWater(FluidStack stack) {
        return !stack.isEmpty() && stack.isIn(FluidTags.WATER);
    }

    /** Unstamped water, from a creative tank or another mod, counts as {@code defaultQuality}. */
    public static WaterQuality quality(FluidStack stack) {
        if (Boolean.TRUE.equals(stack.get(ThirstComponents.WATER_SALTY))) return WaterQuality.SALT;
        Integer purity = stack.get(ThirstComponents.WATER_PURITY);
        return WaterQuality.fresh(purity != null ? purity : ThirstConfig.get().defaultQuality);
    }

    /** Writes {@code quality} onto water and returns the same stack. Anything else is left alone. */
    public static FluidStack stamp(FluidStack stack, WaterQuality quality) {
        if (!isWater(stack)) return stack;
        // Removing a component a stack does not carry can still copy its component map, so ask first.
        switch (quality) {
            case WaterQuality.Salt ignored -> {
                if (stack.has(ThirstComponents.WATER_PURITY)) stack.remove(ThirstComponents.WATER_PURITY);
                stack.set(ThirstComponents.WATER_SALTY, true);
            }
            case WaterQuality.Fresh fresh -> {
                if (stack.has(ThirstComponents.WATER_SALTY)) stack.remove(ThirstComponents.WATER_SALTY);
                stack.set(ThirstComponents.WATER_PURITY, fresh.purity());
            }
        }
        return stack;
    }

    /** {@link #stamp}, unless there is no quality to write. */
    public static FluidStack stampIfKnown(FluidStack stack, @Nullable WaterQuality quality) {
        return quality == null ? stack : stamp(stack, quality);
    }

    /** Stamps a container Create just filled with the quality of the water it was filled from. */
    public static ItemStack stampContainer(ItemStack filled, @Nullable WaterQuality quality) {
        if (quality != null && WaterPurity.isWaterContainer(filled)) WaterPurity.setQuality(filled, quality);
        return filled;
    }

    /**
     * One pass through sand. Sea water comes out as it went in: sand does not take the salt out, and
     * cooking cannot either.
     */
    public static FluidStack filter(FluidStack stack) {
        WaterQuality quality = quality(stack);
        if (quality instanceof WaterQuality.Fresh fresh) quality = WaterQuality.fresh(fresh.purity() + FILTRATION);
        return stamp(stack, quality);
    }
}
