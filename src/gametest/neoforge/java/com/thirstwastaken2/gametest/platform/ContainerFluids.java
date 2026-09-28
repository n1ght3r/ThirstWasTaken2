package com.thirstwastaken2.gametest.platform;

import com.thirstwastaken2.neoforge.WaterFluids;
import com.thirstwastaken2.platform.ThirstComponents;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidStack;

/**
 * The waterskin and the terracotta bowls through NeoForge's item fluid capability, the way another
 * mod's pipe or tank reaches them: {@code IFluidHandlerItem} on 1.21.1 and the transfer API from
 * 1.21.11. Every call works on a copy, so a test's own stack is never changed behind its back.
 */
public final class ContainerFluids {
    private ContainerFluids() { }

    public static boolean available() {
        return true;
    }

    public static String unavailable() {
        return "";
    }

    /** Fills with {@code amount} of water of {@code quality}, or of water with no grade when null. */
    public static FluidMove fillWater(ItemStack container, WaterQuality quality, int amount) {
        FluidStack water = new FluidStack(Fluids.WATER, amount);
        return fill(container, quality == null ? water : WaterFluids.stamp(water, quality));
    }

    public static FluidMove fillLava(ItemStack container, int amount) {
        return fill(container, new FluidStack(Fluids.LAVA, amount));
    }

    //? if >=1.21.2 {
    public static FluidMove drain(ItemStack container, int amount) {
        var items = items(container);
        var handler = handler(items);
        if (handler == null) return new FluidMove(-1, null, container);
        var held = handler.getResource(0);
        int drained;
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            drained = held.isEmpty() ? 0 : handler.extract(0, held, amount, transaction);
            transaction.commit();
        }
        return new FluidMove(drained, drained == 0 ? null : graded(held.toStack(drained)), result(items));
    }

    private static FluidMove fill(ItemStack container, FluidStack fluid) {
        var items = items(container);
        var handler = handler(items);
        if (handler == null) return new FluidMove(-1, null, container);
        int filled;
        try (var transaction = net.neoforged.neoforge.transfer.transaction.Transaction.openRoot()) {
            filled = handler.insert(0, net.neoforged.neoforge.transfer.fluid.FluidResource.of(fluid), fluid.getAmount(),
                    transaction);
            transaction.commit();
        }
        return new FluidMove(filled, null, result(items));
    }

    /**
     * The container in a two-slot item handler, reached through {@code ItemAccess.forHandlerIndex} rather
     * than {@code ItemAccess.forStack}: an empty bowl becomes a different item when it is filled, and a
     * stack's item cannot change in place. The second slot takes what does not fit back in the first.
     */
    private static net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler items(ItemStack container) {
        return new net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler(
                net.minecraft.core.NonNullList.of(ItemStack.EMPTY, container.copy(), ItemStack.EMPTY));
    }

    private static net.neoforged.neoforge.transfer.ResourceHandler<net.neoforged.neoforge.transfer.fluid.FluidResource> handler(
            net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler items) {
        return net.neoforged.neoforge.transfer.access.ItemAccess.forHandlerIndex(items, 0)
                .getCapability(Capabilities.Fluid.ITEM);
    }

    /** Whichever slot the container ended up in, read back through the handler. */
    private static ItemStack result(net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler items) {
        int slot = items.getResource(0).isEmpty() ? 1 : 0;
        return items.getResource(slot).toStack((int) items.getAmountAsLong(slot));
    }
    //?} else {
    /*public static FluidMove drain(ItemStack container, int amount) {
        var handler = container.copy().getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) return new FluidMove(-1, null, container);
        FluidStack drained = handler.drain(amount, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        return new FluidMove(drained.getAmount(), drained.isEmpty() ? null : graded(drained), handler.getContainer());
    }

    private static FluidMove fill(ItemStack container, FluidStack fluid) {
        var handler = container.copy().getCapability(Capabilities.FluidHandler.ITEM);
        if (handler == null) return new FluidMove(-1, null, container);
        int filled = handler.fill(fluid, net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE);
        return new FluidMove(filled, null, handler.getContainer());
    }
    *///?}

    /** The grade the water carries, or {@code null} when it carries none. */
    private static WaterQuality graded(FluidStack water) {
        boolean stamped = water.has(ThirstComponents.WATER_PURITY) || water.has(ThirstComponents.WATER_SALTY);
        return stamped ? WaterFluids.quality(water) : null;
    }
}
