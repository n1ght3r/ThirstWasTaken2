package com.thirstwastaken2.hearthandharvest;

import com.thirstwastaken2.neoforge.WaterFluids;
import com.thirstwastaken2.purity.WaterQuality;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;

/**
 * A tank seen through a container about to pour into it: water filled in is stamped with
 * {@code quality} first, so the tank compares the water it will really receive. Hearth and Harvest
 * checks a pour by simulating it with plain water, which a tank of graded water would refuse and an
 * empty tank would take as Clean. Everything else passes straight through.
 *
 * <p>{@link #sink} is the Sink's own view of itself: whatever is poured in joins its standing water, and
 * sea water is refused, since it would come back out fresh.
 */
public final class StampingTank implements IFluidHandler {
    private final IFluidHandler delegate;
    private final WaterQuality quality;
    private final boolean refusesSalt;

    private StampingTank(IFluidHandler delegate, WaterQuality quality, boolean refusesSalt) {
        this.delegate = delegate;
        this.quality = quality;
        this.refusesSalt = refusesSalt;
    }

    /** {@code tank} receiving water of {@code quality}, whatever the pour says. */
    public static StampingTank of(IFluidHandler tank, WaterQuality quality) {
        return new StampingTank(tank, quality, false);
    }

    /** A Sink's tank: everything poured in is {@link HearthWater#STANDING}, and sea water is refused. */
    public static StampingTank sink(IFluidHandler tank) {
        return new StampingTank(tank, HearthWater.STANDING, true);
    }

    @Override
    public int getTanks() {
        return delegate.getTanks();
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return delegate.getFluidInTank(tank);
    }

    @Override
    public int getTankCapacity(int tank) {
        return delegate.getTankCapacity(tank);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        if (refused(stack)) return false;
        return delegate.isFluidValid(tank, HearthWater.stamped(stack, quality));
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (refused(resource)) return 0;
        return delegate.fill(HearthWater.stamped(resource, quality), action);
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        return delegate.drain(resource, action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        return delegate.drain(maxDrain, action);
    }

    private boolean refused(FluidStack stack) {
        return refusesSalt && WaterFluids.isWater(stack) && WaterFluids.quality(stack) == WaterQuality.SALT;
    }
}
