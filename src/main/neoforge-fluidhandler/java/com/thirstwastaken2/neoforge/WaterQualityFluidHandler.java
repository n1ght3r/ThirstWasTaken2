package com.thirstwastaken2.neoforge;

import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;

import java.util.Optional;
import java.util.function.Function;

/**
 * A container's fluid handler with the water's quality carried across it.
 *
 * <p>The handlers underneath know nothing of quality: NeoForge's bucket wrapper hands out and takes
 * plain water, and Sophisticated Core's own bottle handler only recognises an unstamped water bottle.
 * So a stamped container is looked up as its {@link WaterPurity#unstamped unstamped} copy, water
 * leaving it is stamped with the grade it held, water entering it is passed down plain, and the
 * container is stamped again on the way out of {@link #getContainer}.
 *
 * <p>The fluid side keeps the one-component rule of {@link WaterFluids}, so water of two grades never
 * merges into one tank.
 *
 * <p>Shared by the integrations whose tanks fill and empty containers through NeoForge 21.1's
 * {@code IFluidHandlerItem}: Sophisticated Core's Tank and Pump upgrades, and Extra Delight's jars,
 * kegs and kitchen blocks.
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
     * {@code lookup}'s handler for {@code stack}, wrapped. The mod's own waterskin and bowls are the
     * exception: their handler already carries the grade, and would lose it if it were handed the
     * unstamped copy.
     */
    public static Optional<IFluidHandlerItem> wrap(ItemStack stack, Function<ItemStack, Optional<IFluidHandlerItem>> lookup) {
        if (WaterContainerFluids.handles(stack)) return lookup.apply(stack);
        if (!WaterPurity.isWaterContainer(stack)) {
            return lookup.apply(stack).map(handler -> new WaterQualityFluidHandler(handler, null));
        }
        WaterQuality quality = WaterPurity.quality(stack);
        return lookup.apply(WaterPurity.unstamped(stack)).map(handler -> new WaterQualityFluidHandler(handler, quality));
    }

    /**
     * A handler the caller already looked up on the real, stamped stack. That only suits a handler that
     * does not compare its container's components, such as NeoForge's bucket wrapper; Core's own bottle
     * handler has to go through {@link #wrap} instead.
     */
    public static IFluidHandlerItem of(IFluidHandlerItem handler) {
        if (handler instanceof WaterContainerFluidHandler) return handler;
        ItemStack container = handler.getContainer();
        return new WaterQualityFluidHandler(handler,
                WaterPurity.isWaterContainer(container) ? WaterPurity.quality(container) : null);
    }

    /**
     * A stamped copy, never the delegate's own stack: Sophisticated Core's bottle handler drains only while
     * its container still equals a plain water bottle, and the Tank upgrade ignores a drain that returns
     * nothing after it has already filled itself, so stamping in place would pour the same bottle in
     * forever.
     */
    @Override
    public ItemStack getContainer() {
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
    public FluidStack getFluidInTank(int tank) {
        return stamped(delegate.getFluidInTank(tank));
    }

    @Override
    public int getTankCapacity(int tank) {
        return delegate.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
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
    public FluidStack drain(FluidStack resource, FluidAction action) {
        if (quality == null || !WaterFluids.isWater(resource)) return delegate.drain(resource, action);
        // Only water of exactly this grade matches, the same comparison the asking tank makes.
        if (!FluidStack.isSameFluidSameComponents(WaterFluids.stamp(resource.copy(), quality), resource)) {
            return FluidStack.EMPTY;
        }
        return stamped(delegate.drain(WaterFluids.unstamped(resource), action));
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
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
