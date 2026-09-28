package com.thirstwastaken2.gametest.platform;

import com.thirstwastaken2.fabric.WaterContainerStorage;
import com.thirstwastaken2.platform.FabricTransfer;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleVariantStorage;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;

/**
 * The waterskin and the terracotta bowls through Fabric's Transfer API, the way another mod's pipe or
 * tank reaches them. Amounts cross this class in millibuckets, 81 droplets each, so the shared test reads
 * the same numbers on both loaders. The container sits in a one-slot item storage, the way a pipe sees an
 * item in an inventory, so an empty bowl can become a water bowl.
 */
public final class ContainerFluids {
    private static final long DROPLETS_PER_MB = FluidConstants.BUCKET / 1000;

    private ContainerFluids() { }

    public static boolean available() {
        return true;
    }

    public static String unavailable() {
        return "";
    }

    /** Fills with {@code amount} of water of {@code quality}, or of water with no grade when null. */
    public static FluidMove fillWater(ItemStack container, WaterQuality quality, int amount) {
        return fill(container, quality == null ? FluidVariant.of(Fluids.WATER) : WaterContainerStorage.water(quality),
                amount);
    }

    public static FluidMove fillLava(ItemStack container, int amount) {
        return fill(container, FluidVariant.of(Fluids.LAVA), amount);
    }

    public static FluidMove drain(ItemStack container, int amount) {
        Slot slot = new Slot(container);
        Storage<FluidVariant> storage = FluidStorage.ITEM.find(container, slot.context());
        if (storage == null) return new FluidMove(-1, null, container);
        FluidVariant held = FluidVariant.blank();
        for (var view : storage) {
            if (!view.isResourceBlank()) held = view.getResource();
        }
        long drained;
        try (Transaction transaction = Transaction.openOuter()) {
            drained = held.isBlank() ? 0 : storage.extract(held, amount * DROPLETS_PER_MB, transaction);
            transaction.commit();
        }
        boolean graded = !FabricTransfer.isPlain(held);
        return new FluidMove((int) (drained / DROPLETS_PER_MB),
                drained == 0 || !graded ? null : WaterContainerStorage.quality(held), slot.stack());
    }

    private static FluidMove fill(ItemStack container, FluidVariant fluid, int amount) {
        Slot slot = new Slot(container);
        Storage<FluidVariant> storage = FluidStorage.ITEM.find(container, slot.context());
        if (storage == null) return new FluidMove(-1, null, container);
        long filled;
        try (Transaction transaction = Transaction.openOuter()) {
            filled = storage.insert(fluid, amount * DROPLETS_PER_MB, transaction);
            transaction.commit();
        }
        return new FluidMove((int) (filled / DROPLETS_PER_MB), null, slot.stack());
    }

    /** One slot holding a copy of the container, so a test's own stack is never changed. */
    private static final class Slot extends SingleVariantStorage<ItemVariant> {
        Slot(ItemStack stack) {
            variant = ItemVariant.of(stack);
            amount = stack.getCount();
        }

        @Override
        protected ItemVariant getBlankVariant() {
            return ItemVariant.blank();
        }

        @Override
        protected long getCapacity(ItemVariant variant) {
            return 64;
        }

        ContainerItemContext context() {
            return ContainerItemContext.ofSingleSlot(this);
        }

        ItemStack stack() {
            return variant.toStack((int) amount);
        }
    }
}
