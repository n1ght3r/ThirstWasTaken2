package com.thirstwastaken2.forge;

import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.jetbrains.annotations.NotNull;

/**
 * A container's fluid handler with the water's quality carried across it, the Forge copy of the
 * NeoForge one in {@code src/main/neoforge-fluidhandler}.
 *
 * <p>Forge's bucket wrapper hands out and takes plain water, and its {@code drain(FluidStack)} compares
 * the tag, so it refuses stamped water. Water leaving the container is stamped with the grade it held,
 * water entering it is passed down plain, and the container is stamped again on the way out of
 * {@link #getContainer}. The fluid side keeps the one-tag rule of {@link WaterFluids}, so water of two
 * grades never merges into one tank.
 *
 * <p>Used by Immersive Engineering's transfers between a tank and a container.
 */
public final class WaterQualityFluidHandler implements IFluidHandlerItem {
    private final IFluidHandlerItem delegate;
    /** The water the container holds, or {@code null} while it holds none of the mod's water. */
    private WaterQuality quality;

    private WaterQualityFluidHandler(IFluidHandlerItem delegate, WaterQuality quality) {
        this.delegate = delegate;
        this.quality = quality;
    }

    /**
     * A handler the caller already looked up on the real, stamped stack, which suits a handler that does
     * not compare its container's tag, such as Forge's bucket wrapper. The mod's own containers are left
     * alone: their handler already carries the grade.
     */
    public static IFluidHandlerItem of(IFluidHandlerItem handler) {
        if (handler instanceof WaterContainerFluidHandler) return handler;
        ItemStack container = handler.getContainer();
        return new WaterQualityFluidHandler(handler,
                WaterPurity.isWaterContainer(container) ? WaterPurity.quality(container) : null);
    }

    /** A stamped copy, never the delegate's own stack. */
    @Override
    public @NotNull ItemStack getContainer() {
        ItemStack container = delegate.getContainer();
        if (quality == null || !WaterPurity.isWaterContainer(container)) return container;
        if (WaterPurity.isStamped(container) && quality.equals(WaterPurity.quality(container))) return container;
        return WaterPurity.setQuality(container.copy(), quality);
    }

    @Override
    public int getTanks() {
        return delegate.getTanks();
    }

    @Override
    public @NotNull FluidStack getFluidInTank(int tank) {
        return stamped(delegate.getFluidInTank(tank));
    }

    @Override
    public int getTankCapacity(int tank) {
        return delegate.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, @NotNull FluidStack stack) {
        return delegate.isFluidValid(tank, WaterFluids.isWater(stack) ? WaterFluids.unstamped(stack) : stack);
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (!WaterFluids.isWater(resource)) return delegate.fill(resource, action);
        WaterQuality incoming = WaterFluids.quality(resource);
        // A container cannot hold two grades at once, and a stack with room left could otherwise be
        // topped up with better water and take its grade.
        if (quality != null && !quality.equals(incoming) && holdsWater()) return 0;
        int filled = delegate.fill(WaterFluids.unstamped(resource), action);
        if (filled > 0 && action.execute()) quality = incoming;
        return filled;
    }

    @Override
    public @NotNull FluidStack drain(FluidStack resource, FluidAction action) {
        if (quality == null || !WaterFluids.isWater(resource)) return delegate.drain(resource, action);
        // Only water of exactly this grade matches, the same comparison the asking tank makes.
        if (!WaterFluids.stamp(resource.copy(), quality).isFluidEqual(resource)) return FluidStack.EMPTY;
        return stamped(delegate.drain(WaterFluids.unstamped(resource), action));
    }

    @Override
    public @NotNull FluidStack drain(int maxDrain, FluidAction action) {
        return stamped(delegate.drain(maxDrain, action));
    }

    private FluidStack stamped(FluidStack fluid) {
        return quality != null && WaterFluids.isWater(fluid) ? WaterFluids.stamp(fluid.copy(), quality) : fluid;
    }

    private boolean holdsWater() {
        for (int tank = 0; tank < delegate.getTanks(); tank++) {
            if (WaterFluids.isWater(delegate.getFluidInTank(tank))) return true;
        }
        return false;
    }
}
