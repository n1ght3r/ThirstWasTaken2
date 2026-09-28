package com.thirstwastaken2.gametest.platform;

import com.thirstwastaken2.forge.WaterFluids;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;

/**
 * The water containers through Forge's item fluid capability, the way another mod's pipe or tank
 * reaches them. Every call works on a copy, so a test's own stack is never changed behind its back.
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

    public static FluidMove drain(ItemStack container, int amount) {
        IFluidHandlerItem handler = handler(container);
        if (handler == null) return new FluidMove(-1, null, container);
        FluidStack drained = handler.drain(amount, IFluidHandler.FluidAction.EXECUTE);
        WaterQuality quality = drained.isEmpty() || !WaterFluids.stamped(drained) ? null : WaterFluids.quality(drained);
        return new FluidMove(drained.getAmount(), quality, handler.getContainer());
    }

    private static FluidMove fill(ItemStack container, FluidStack fluid) {
        IFluidHandlerItem handler = handler(container);
        if (handler == null) return new FluidMove(-1, null, container);
        int filled = handler.fill(fluid, IFluidHandler.FluidAction.EXECUTE);
        return new FluidMove(filled, null, handler.getContainer());
    }

    private static IFluidHandlerItem handler(ItemStack container) {
        return container.copy().getCapability(ForgeCapabilities.FLUID_HANDLER_ITEM).resolve().orElse(null);
    }
}
