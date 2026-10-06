package com.thirstwastaken2.fabric;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterContainers;
import com.thirstwastaken2.platform.FabricTransfer;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.transfer.v1.context.ContainerItemContext;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidConstants;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.world.item.ItemStack;

import java.util.Iterator;
import java.util.List;
import net.minecraft.world.level.material.Fluids;

/**
 * The waterskin and the terracotta bowls as Fabric Transfer API fluid storage, so a Create Fly Spout
 * or Item Drain, and any other mod's pipe or tank, can fill and empty them. The same rules as NeoForge's
 * {@code WaterContainerFluids}:
 *
 * <ul>
 *     <li>A serving is 250 mB, 20250 droplets, NeoForge's size rather than Fabric's 27000-droplet bottle,
 *     so both loaders move the same water. Only whole servings move.</li>
 *     <li>A container that holds water only takes more of the same grade.</li>
 *     <li>Water with no grade fills an empty container as {@code defaultQuality}.</li>
 * </ul>
 *
 * <p>How the grade travels on the fluid is {@link FabricTransfer}'s.
 *
 * <p>A storage with one view rather than a {@code SingleSlotStorage}: Create Fly wraps a slotted item
 * storage with a capacity of zero, so its Spout would take every waterskin for full.
 */
public final class WaterContainerStorage implements Storage<FluidVariant>, StorageView<FluidVariant> {
    public static final long SERVING = FluidConstants.BUCKET / 4;

    private final ContainerItemContext context;

    private WaterContainerStorage(ContainerItemContext context) {
        this.context = context;
    }

    /** Called once by {@link ThirstWasTaken2Fabric}, after the items are registered. */
    public static void register() {
        FluidStorage.ITEM.registerForItems((stack, context) -> new WaterContainerStorage(context),
                ThirstItems.WATERSKIN, ThirstItems.COPPER_CANTEEN, ThirstItems.IRON_FLASK,
                ThirstItems.TERRACOTTA_BOWL, ThirstItems.TERRACOTTA_WATER_BOWL);
    }

    /** Water of {@code quality} as a variant. */
    public static FluidVariant water(WaterQuality quality) {
        return FabricTransfer.water(quality);
    }

    /**
     * The grade {@code variant} carries, {@code defaultQuality} when none, or {@code null} for anything but
     * water. Only this mod's own data is read: other mods add their own to the water they hold, as
     * Create Fly's tanks add {@code create:fluid_max_capacity} after their first fill.
     */
    public static WaterQuality quality(FluidVariant variant) {
        if (!variant.isOf(Fluids.WATER)) return null;
        if (FabricTransfer.salty(variant)) return WaterQuality.SALT;
        Integer purity = FabricTransfer.grade(variant);
        return WaterQuality.fresh(purity != null ? purity : ThirstConfig.get().defaultQuality);
    }

    private ItemStack container() {
        return context.getItemVariant().toStack();
    }

    @Override
    public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        ItemStack container = container();
        WaterQuality added = quality(resource);
        if (added == null || !WaterContainers.handles(container) || context.getAmount() == 0) return 0;
        int held = WaterContainers.servings(container);
        if (held > 0 && !added.equals(WaterPurity.quality(container))) return 0;
        int servings = (int) Math.min(maxAmount / SERVING, WaterContainers.capacity(container) - held);
        if (servings <= 0) return 0;
        ItemStack filled = WaterContainers.holding(container, added, held + servings);
        if (filled == null || context.exchange(ItemVariant.of(filled), 1, transaction) != 1) return 0;
        return servings * SERVING;
    }

    @Override
    public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
        ItemStack container = container();
        int held = WaterContainers.servings(container);
        if (held == 0 || context.getAmount() == 0
                || !WaterPurity.quality(container).equals(quality(resource))) {
            return 0;
        }
        int servings = (int) Math.min(maxAmount / SERVING, held);
        if (servings <= 0) return 0;
        ItemStack emptied = WaterContainers.holding(container, WaterPurity.quality(container), held - servings);
        if (emptied == null || context.exchange(ItemVariant.of(emptied), 1, transaction) != 1) return 0;
        return servings * SERVING;
    }

    @Override
    public Iterator<StorageView<FluidVariant>> iterator() {
        return List.<StorageView<FluidVariant>>of(this).iterator();
    }

    @Override
    public boolean isResourceBlank() {
        return WaterContainers.servings(container()) == 0;
    }

    @Override
    public FluidVariant getResource() {
        ItemStack container = container();
        return WaterContainers.servings(container) == 0 ? FluidVariant.blank() : water(WaterPurity.quality(container));
    }

    @Override
    public long getAmount() {
        return WaterContainers.servings(container()) * SERVING;
    }

    @Override
    public long getCapacity() {
        return WaterContainers.capacity(container()) * SERVING;
    }
}
