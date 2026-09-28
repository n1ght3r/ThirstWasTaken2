package com.thirstwastaken2.forge;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.fluids.FluidStack;

/**
 * Water quality on Forge's fluid stacks, which carry an NBT tag the way item stacks do on 1.20.1.
 *
 * <p>The same rules as the NeoForge copy: a fluid stack keeps exactly one value for its quality, a
 * {@code thirstwastaken2} compound holding {@code purity} for a grade or {@code salty} for sea water, so
 * two stacks of the same water always compare equal and share a tank or a pipe. It is the same compound
 * {@code ItemWaterData} keeps on an item, less the servings.
 */
public final class WaterFluids {
    private static final String TAG = "thirstwastaken2";
    private static final String PURITY = "purity";
    private static final String SALTY = "salty";

    private WaterFluids() { }

    public static boolean isWater(FluidStack stack) {
        return !stack.isEmpty() && stack.getFluid().is(FluidTags.WATER);
    }

    /** Unstamped water, from a creative tank or another mod, counts as {@code defaultPurity}. */
    public static WaterQuality quality(FluidStack stack) {
        CompoundTag data = data(stack);
        if (data != null && data.getBoolean(SALTY)) return WaterQuality.SALT;
        return WaterQuality.fresh(data != null && data.contains(PURITY) ? data.getInt(PURITY)
                : ThirstConfig.get().defaultPurity);
    }

    /** Whether the stack carries a quality at all. */
    public static boolean stamped(FluidStack stack) {
        return data(stack) != null;
    }

    /** Writes {@code quality} onto water and returns the same stack. Anything else is left alone. */
    public static FluidStack stamp(FluidStack stack, WaterQuality quality) {
        if (!isWater(stack)) return stack;
        CompoundTag data = new CompoundTag();
        if (quality instanceof WaterQuality.Fresh fresh) data.putInt(PURITY, fresh.purity());
        else data.putBoolean(SALTY, true);
        stack.getOrCreateTag().put(TAG, data);
        return stack;
    }

    private static CompoundTag data(FluidStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG, CompoundTag.TAG_COMPOUND) ? tag.getCompound(TAG) : null;
    }
}
