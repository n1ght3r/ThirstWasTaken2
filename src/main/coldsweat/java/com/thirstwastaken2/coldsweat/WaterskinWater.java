package com.thirstwastaken2.coldsweat;

import com.thirstwastaken2.neoforge.WaterFluids;
import com.thirstwastaken2.platform.ThirstComponents;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The grade of the water Cold Sweat's waterskin is filled with.
 *
 * <p>Cold Sweat fills it in {@code WaterskinItem.getFilledItem}, which only knows the block. That is
 * enough for water in the world, but not for the two other sources: a cauldron is lowered before the
 * skin is filled, and its last layer takes the stored grade with it, and a tank's water carries its
 * grade on the fluid, not on the block. So each of those notes what it gave up here first, and
 * {@link #stamp} prefers the note to a sample. Both happen on one thread, in one call to {@code useOn}.
 */
public final class WaterskinWater {
    private static final ThreadLocal<WaterQuality> PENDING = new ThreadLocal<>();

    private WaterskinWater() { }

    /** A cauldron about to be drawn from. An unstamped one is left to read as the default, as a bottle would. */
    public static void fromCauldron(BlockState cauldron) {
        WaterQuality stored = WaterPurity.storedQuality(cauldron);
        if (stored != null) PENDING.set(stored);
    }

    /** Water drained from a tank. Water with no grade, from a mod that knows none, fills as the default. */
    public static void fromFluid(FluidStack drained) {
        if (WaterFluids.isWater(drained)) PENDING.set(WaterFluids.quality(drained));
    }

    /** Forgets a note no fill used, so it cannot reach the next one. */
    public static void clear() {
        PENDING.remove();
    }

    /** Stamps a skin just filled at {@code pos}, server side only: the client's copy is replaced by the server's. */
    public static void stamp(ItemStack filled, Level level, BlockPos pos) {
        WaterQuality noted = PENDING.get();
        PENDING.remove();
        if (level.isClientSide()) return;
        WaterPurity.setQuality(filled, noted != null ? noted : WaterPurity.sampleAt(level, pos));
    }

    /**
     * An empty skin handed back when a filled one is used up. Cold Sweat copies every component off the
     * filled skin and strips its own; this strips ours, so an empty skin carries no grade to stack apart
     * by or to hand on to the next fill.
     */
    public static void strip(ItemStack empty) {
        if (empty.has(ThirstComponents.WATER_PURITY)) empty.remove(ThirstComponents.WATER_PURITY);
        if (empty.has(ThirstComponents.WATER_SALTY)) empty.remove(ThirstComponents.WATER_SALTY);
    }
}
