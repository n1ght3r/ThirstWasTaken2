package com.thirstwastaken2.hearthandharvestforge;

import com.thirstwastaken2.forge.WaterContainerFluidHandler;
import com.thirstwastaken2.forge.WaterFluids;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

/**
 * A container's fluid handler with its water's grade carried across it, for Hearth and Harvest's Jug.
 *
 * <p>Forge's bucket wrapper drains plain water from a graded bucket and fills a plain bucket, so a
 * Dirty bucket poured into a jug came back out Clean. Through this, water leaving the container carries
 * the grade it held (Clean for an unstamped one), so the jug's tank keeps it and refuses another grade,
 * and water entering it goes down plain and is stamped on the container afterwards. Loaded only from the
 * mixin, which only applies with Hearth and Harvest installed.
 */
public final class GradedItemFluidHandler implements IFluidHandlerItem {
    private final IFluidHandlerItem delegate;
    /** The water the container holds or was just filled with, or {@code null} while it holds none. */
    private WaterQuality quality;

    private GradedItemFluidHandler(IFluidHandlerItem delegate, WaterQuality quality) {
        this.delegate = delegate;
        this.quality = quality;
    }

    /**
     * {@code handler} wrapped. The mod's own waterskin, canteen, flask and bowls already carry the grade
     * on their handler and are handed back as they are.
     */
    public static IFluidHandlerItem of(IFluidHandlerItem handler) {
        if (handler instanceof WaterContainerFluidHandler) return handler;
        ItemStack container = handler.getContainer();
        return new GradedItemFluidHandler(handler,
                WaterPurity.isWaterContainer(container) ? WaterPurity.quality(container) : null);
    }

    @Override
    public ItemStack getContainer() {
        return WaterFluids.stampContainer(delegate.getContainer(), quality);
    }

    @Override
    public int getTanks() {
        return delegate.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return stamped(delegate.getFluidInTank(tank));
    }

    @Override
    public int getTankCapacity(int tank) {
        return delegate.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return delegate.isFluidValid(tank, plain(stack));
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!WaterFluids.isWater(resource)) return delegate.fill(resource, action);
        int filled = delegate.fill(plain(resource), action);
        if (filled > 0 && action.execute()) quality = WaterFluids.quality(resource);
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return stamped(delegate.drain(plain(resource), action));
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return stamped(delegate.drain(maxDrain, action));
    }

    private FluidStack stamped(FluidStack fluid) {
        return quality != null && WaterFluids.isWater(fluid) ? WaterFluids.stamp(fluid.copy(), quality) : fluid;
    }

    /** Water with no grade on it, the plain water the bucket wrapper knows. */
    private static FluidStack plain(FluidStack fluid) {
        return WaterFluids.isWater(fluid) && WaterFluids.stamped(fluid) ? new FluidStack(fluid.getFluid(), fluid.getAmount()) : fluid;
    }
}
