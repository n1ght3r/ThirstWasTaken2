package com.thirstwastaken2.block;

import com.thirstwastaken2.platform.SavedBlockEntity;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The copper distiller's machine, on its boiler half: its two tanks, its fire, how far the serving on the
 * boil has got, the salt it has left behind, and its slots. See
 * {@code docs/dev/mechanics/DISTILLATION-PLAN.md}.
 *
 * <p>For now it only holds and saves that state; the block does not create it yet and nothing ticks it.
 */
public final class DistillerBlockEntity extends SavedBlockEntity implements Container {
    public static final int WATER_IN = 0;
    public static final int FUEL = 1;
    public static final int EMPTY_IN = 2;
    public static final int FILLED_OUT = 3;
    public static final int SALT_OUT = 4;
    public static final int SLOTS = 5;
    /** Servings each tank holds: three buckets. */
    public static final int TANK = 9;

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    /** Servings waiting in the boiler, and their one quality as {@code WaterPurity.storedValue} keeps it, 0 for none. */
    int boilerServings;
    int boilerQuality;
    /** Servings of Pure water in the basin. */
    int basinServings;
    /** Ticks of fire left, and how long the fuel burning now lasts in all. */
    int burnLeft;
    int burnTotal;
    /** Ticks the serving on the boil has had. */
    int progress;
    /** Salty servings distilled since the last salt was made. */
    int saltServings;

    public DistillerBlockEntity(BlockPos pos, BlockState state) {
        super(ThirstBlockEntities.COPPER_DISTILLER, pos, state);
    }

    /** Servings waiting in the boiler. */
    public int boilerServings() {
        return boilerServings;
    }

    /** What the boiler holds, or {@code null} when it is empty. */
    public WaterQuality boilerQuality() {
        if (boilerServings == 0 || boilerQuality == 0) return null;
        return boilerQuality == WaterPurity.BLOCK_SALT ? WaterQuality.SALT : WaterQuality.fresh(boilerQuality - 1);
    }

    /** Servings of Pure water in the basin. */
    public int basinServings() {
        return basinServings;
    }

    /**
     * Pours up to {@code servings} of {@code quality} into the boiler, mixed the way a cauldron mixes:
     * salt stays salt, fresh water keeps the worse grade. Returns how many went in.
     */
    public int pour(int servings, WaterQuality quality) {
        int poured = Math.min(servings, TANK - boilerServings);
        if (poured <= 0) return 0;
        WaterQuality held = boilerQuality();
        boilerQuality = WaterPurity.storedValue(held == null ? quality : WaterQuality.worse(held, quality));
        boilerServings += poured;
        changed();
        return poured;
    }

    @Override
    protected void save(Output output) {
        output.putInt("boiler", boilerServings);
        output.putInt("boiler_quality", boilerQuality);
        output.putInt("basin", basinServings);
        output.putInt("burn_left", burnLeft);
        output.putInt("burn_total", burnTotal);
        output.putInt("progress", progress);
        output.putInt("salt_servings", saltServings);
        output.putItems(items);
    }

    @Override
    protected void load(Input input) {
        boilerServings = input.getInt("boiler", 0);
        boilerQuality = input.getInt("boiler_quality", 0);
        basinServings = input.getInt("basin", 0);
        burnLeft = input.getInt("burn_left", 0);
        burnTotal = input.getInt("burn_total", 0);
        progress = input.getInt("progress", 0);
        saltServings = input.getInt("salt_servings", 0);
        input.getItems(items);
    }

    @Override
    public int getContainerSize() {
        return SLOTS;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : items) {
            if (!stack.isEmpty()) return false;
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) setChanged();
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        return ContainerHelper.takeItem(items, slot);
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        setChanged();
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }
}
