package com.thirstwastaken2.neoforge;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.platform.ThirstComponents;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * Water quality on NeoForge's fluid stacks, which carry data components the same way item stacks do.
 *
 * <p>A fluid stack keeps exactly one component for its quality, {@code water_purity} for a grade or
 * {@code water_salty} for sea water, so two stacks of the same water always compare equal and share a
 * tank or a pipe. Items get both components back when {@code WaterPurity.setQuality} stamps them on the
 * way out.
 *
 * <p>Shared by every NeoForge integration that moves water as a fluid: Create's pipes and the Sand
 * Filter, and Sophisticated Core's Tank upgrade.
 */
public final class WaterFluids {
    private WaterFluids() { }

    public static boolean isWater(FluidStack stack) {
        return !stack.isEmpty() && stack.is(FluidTags.WATER);
    }

    /** Unstamped water, from a creative tank or another mod, counts as {@code defaultPurity}. */
    public static WaterQuality quality(FluidStack stack) {
        if (Boolean.TRUE.equals(stack.get(ThirstComponents.WATER_SALTY))) return WaterQuality.SALT;
        Integer purity = stack.get(ThirstComponents.WATER_PURITY);
        return WaterQuality.fresh(purity != null ? purity : ThirstConfig.get().defaultPurity);
    }

    /** Writes {@code quality} onto water and returns the same stack. Anything else is left alone. */
    public static FluidStack stamp(FluidStack stack, WaterQuality quality) {
        if (!isWater(stack)) return stack;
        // Removing a component a stack does not carry can still copy its component map, so ask first.
        if (quality instanceof WaterQuality.Fresh fresh) {
            if (stack.has(ThirstComponents.WATER_SALTY)) stack.remove(ThirstComponents.WATER_SALTY);
            stack.set(ThirstComponents.WATER_PURITY, fresh.purity());
        } else {
            if (stack.has(ThirstComponents.WATER_PURITY)) stack.remove(ThirstComponents.WATER_PURITY);
            stack.set(ThirstComponents.WATER_SALTY, true);
        }
        return stack;
    }

    /** {@link #stamp}, unless there is no quality to write. */
    public static FluidStack stampIfKnown(FluidStack stack, WaterQuality quality) {
        return quality == null ? stack : stamp(stack, quality);
    }

    /**
     * A copy of {@code stack} without its quality, the plain water other mods compare against. Only a
     * stamped stack is copied.
     */
    public static FluidStack unstamped(FluidStack stack) {
        if (!stack.has(ThirstComponents.WATER_PURITY) && !stack.has(ThirstComponents.WATER_SALTY)) return stack;
        FluidStack plain = stack.copy();
        plain.remove(ThirstComponents.WATER_PURITY);
        plain.remove(ThirstComponents.WATER_SALTY);
        return plain;
    }

    /** Stamps a container just filled with the quality of the water it was filled from. */
    public static ItemStack stampContainer(ItemStack filled, WaterQuality quality) {
        if (quality != null && WaterPurity.isWaterContainer(filled)) WaterPurity.setQuality(filled, quality);
        return filled;
    }
}
