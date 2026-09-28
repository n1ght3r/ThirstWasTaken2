package com.thirstwastaken2.forge;

import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

/**
 * A waterskin or terracotta bowl as Forge's {@code IFluidHandlerItem}. The rules are in
 * {@link WaterContainerFluids}. Like Forge's bucket wrapper, it only works on a stack of one.
 */
public final class WaterContainerFluidHandler implements IFluidHandlerItem {
    private ItemStack container;

    public WaterContainerFluidHandler(ItemStack container) {
        this.container = container;
    }

    @Override
    public ItemStack getContainer() {
        return container;
    }

    @Override
    public int getTanks() {
        return 1;
    }

    @Override
    public FluidStack getFluidInTank(int tank) {
        return WaterContainerFluids.contents(container);
    }

    @Override
    public int getTankCapacity(int tank) {
        return WaterContainerFluids.capacity(container);
    }

    @Override
    public boolean isFluidValid(int tank, FluidStack stack) {
        return WaterContainerFluids.accepts(stack.getFluid());
    }

    @Override
    public int fill(FluidStack resource, FluidAction action) {
        if (container.getCount() != 1 || resource.isEmpty() || !WaterContainerFluids.accepts(resource.getFluid())) {
            return 0;
        }
        FluidStack held = WaterContainerFluids.contents(container);
        if (!held.isEmpty() && !held.isFluidEqual(resource)) return 0;
        int room = WaterContainerFluids.capacity(container) - held.getAmount();
        int filled = WaterContainerFluids.wholeServings(Math.min(room, resource.getAmount()));
        if (filled <= 0) return 0;
        ItemStack filledContainer = WaterContainerFluids.holding(container, WaterFluids.quality(resource),
                held.getAmount() + filled);
        if (filledContainer == null) return 0;
        if (action.execute()) container = filledContainer;
        return filled;
    }

    @Override
    public FluidStack drain(FluidStack resource, FluidAction action) {
        FluidStack held = WaterContainerFluids.contents(container);
        if (held.isEmpty() || !held.isFluidEqual(resource)) return FluidStack.EMPTY;
        return drain(resource.getAmount(), action);
    }

    @Override
    public FluidStack drain(int maxDrain, FluidAction action) {
        if (container.getCount() != 1) return FluidStack.EMPTY;
        FluidStack held = WaterContainerFluids.contents(container);
        int drained = WaterContainerFluids.wholeServings(Math.min(maxDrain, held.getAmount()));
        if (drained <= 0) return FluidStack.EMPTY;
        ItemStack emptier = WaterContainerFluids.holding(container, null, held.getAmount() - drained);
        if (emptier == null) return FluidStack.EMPTY;
        if (action.execute()) container = emptier;
        return new FluidStack(held, drained);
    }
}
