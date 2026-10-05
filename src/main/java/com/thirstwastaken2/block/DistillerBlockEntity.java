package com.thirstwastaken2.block;

import com.thirstwastaken2.platform.Loader;
import com.thirstwastaken2.platform.SavedBlockEntity;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The copper distiller's machine, on its boiler half: its two tanks, its fire, how far the serving on the
 * boil has got, the salt it has left behind, and its slots. See
 * {@code docs/dev/mechanics/DISTILLATION-PLAN.md}.
 *
 * <p>Each tick on the server it pours the container in {@link #WATER_IN} into the boiler, fills the one
 * in {@link #EMPTY_IN} from the basin, and boils: while the boiler holds water, the basin has room and
 * the tub holds its coolant, the fire burns and every {@link #SERVING_TICKS} one serving moves from the
 * boiler to the basin, Pure whatever it was. With nothing to do the fire waits rather than burning down,
 * so no fuel is wasted. A salty serving is counted toward the salt it leaves behind; making that salt is
 * still to come.
 *
 * <p>Hoppers reach it through either half: from above into {@link #WATER_IN}, from the sides into
 * {@link #FUEL} or {@link #EMPTY_IN}, whichever takes the item, and from below out of
 * {@link #FILLED_OUT}, {@link #SALT_OUT}, and whatever is left in the other two once it is spent, an
 * empty bucket or bottle, as a furnace gives back a lava bucket's bucket.
 */
public final class DistillerBlockEntity extends SavedBlockEntity implements WorldlyContainer {
    public static final int WATER_IN = 0;
    public static final int FUEL = 1;
    public static final int EMPTY_IN = 2;
    public static final int FILLED_OUT = 3;
    public static final int SALT_OUT = 4;
    public static final int SLOTS = 5;
    /** Servings each tank holds: three buckets. */
    public static final int TANK = 9;
    /** Ticks a serving takes to distil: 8 seconds, so coal's 80 runs ten. */
    public static final int SERVING_TICKS = 160;
    /** Salty servings that leave one salt behind: a bucket of sea water. */
    public static final int SALT_SERVINGS = 3;

    private static final int[] TOP = { WATER_IN };
    private static final int[] SIDES = { FUEL, EMPTY_IN };
    private static final int[] BOTTOM = { FILLED_OUT, SALT_OUT, WATER_IN, FUEL };

    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    /** Servings waiting in the boiler, and their one quality as {@code WaterPurity.storedValue} keeps it, 0 for none. */
    private int boilerServings;
    private int boilerQuality;
    /** Servings of Pure water in the basin. */
    private int basinServings;
    /** Ticks of fire left, and how long the fuel burning now lasts in all. */
    private int burnLeft;
    private int burnTotal;
    /** Ticks the serving on the boil has had. */
    private int progress;
    /** Salty servings distilled toward the next salt, up to {@link #SALT_SERVINGS}. */
    private int saltServings;

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

    /** Ticks of fire left. */
    public int burnLeft() {
        return burnLeft;
    }

    /** Ticks the serving on the boil has had. */
    public int progress() {
        return progress;
    }

    /** Salty servings distilled toward the next salt. */
    public int saltServings() {
        return saltServings;
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

    /** Takes up to {@code servings} of Pure water out of the basin. Returns how many came out. */
    public int draw(int servings) {
        int drawn = Math.min(servings, basinServings);
        if (drawn <= 0) return 0;
        basinServings -= drawn;
        changed();
        return drawn;
    }

    /** One tick of the machine, on the server. */
    public void tick() {
        Level level = getLevel();
        if (level == null || level.isClientSide()) return;
        boolean changed = takeWater();
        changed |= fillContainer();
        if (canDistil()) {
            if (burnLeft == 0) changed |= lightFuel();
            if (burnLeft > 0) {
                burnLeft--;
                if (++progress >= SERVING_TICKS) distilOne();
                changed = true;
            }
        } else if (boilerServings == 0 && progress > 0) {
            progress = 0;
            changed = true;
        }
        if (changed) changed();
    }

    /** Whether there is water to boil, room for it in the basin, and coolant in the tub to condense it. */
    private boolean canDistil() {
        return boilerServings > 0 && basinServings < TANK && tubCooled();
    }

    private boolean tubCooled() {
        BlockState state = getBlockState();
        if (!state.is(ThirstBlocks.COPPER_DISTILLER) || getLevel() == null) return false;
        BlockState tub = getLevel().getBlockState(getBlockPos().relative(DistillerBlock.towardOtherHalf(state)));
        return tub.is(ThirstBlocks.COPPER_DISTILLER) && tub.getValue(DistillerBlock.PART) == DistillerBlock.Part.TUB
                && tub.getValue(DistillerBlock.COOLED);
    }

    /** Pours the container in {@link #WATER_IN} into the boiler, a bucket only when all three fit. */
    private boolean takeWater() {
        ItemStack stack = items.get(WATER_IN);
        int held = DistillerWater.held(stack);
        int room = TANK - boilerServings;
        if (held == 0 || room == 0 || stack.getCount() != 1 || DistillerWater.whole(stack) && held > room) {
            return false;
        }
        WaterQuality quality = WaterPurity.quality(stack);
        int poured = Math.min(held, room);
        items.set(WATER_IN, DistillerWater.emptied(stack, poured));
        pour(poured, quality);
        return true;
    }

    /**
     * Fills the container in {@link #EMPTY_IN} from the basin. One that is filled whole, a bottle or a
     * bucket, goes to {@link #FILLED_OUT} at once when that is free; a carried one fills as far as the
     * basin goes and moves out once it is full.
     */
    private boolean fillContainer() {
        ItemStack stack = items.get(EMPTY_IN);
        int room = DistillerWater.room(stack);
        boolean carried = !stack.isEmpty() && !DistillerWater.whole(stack);
        if (carried && room == 0) {
            // Filled earlier while the output was taken.
            if (!items.get(FILLED_OUT).isEmpty()) return false;
            items.set(FILLED_OUT, stack);
            items.set(EMPTY_IN, ItemStack.EMPTY);
            return true;
        }
        if (room == 0 || basinServings == 0) return false;
        if (DistillerWater.whole(stack)) {
            if (basinServings < room || !items.get(FILLED_OUT).isEmpty()) return false;
            items.set(FILLED_OUT, DistillerWater.filled(stack, room));
            stack.shrink(1);
            basinServings -= room;
            return true;
        }
        int drawn = Math.min(room, basinServings);
        ItemStack skin = DistillerWater.filled(stack, drawn);
        basinServings -= drawn;
        boolean full = DistillerWater.room(skin) == 0;
        if (full && items.get(FILLED_OUT).isEmpty()) {
            items.set(FILLED_OUT, skin);
            items.set(EMPTY_IN, ItemStack.EMPTY);
        } else {
            items.set(EMPTY_IN, skin);
        }
        return true;
    }

    /** Burns one fuel item, leaving what is left of it, an empty bucket, in its slot. */
    private boolean lightFuel() {
        ItemStack fuel = items.get(FUEL);
        int burn = Loader.burnTime(this, fuel);
        if (burn <= 0) return false;
        ItemStack remainder = Vanilla.craftingRemainder(fuel);
        fuel.shrink(1);
        if (fuel.isEmpty() && !remainder.isEmpty()) items.set(FUEL, remainder);
        burnLeft = burn;
        burnTotal = burn;
        return true;
    }

    /** One serving from the boiler to the basin. */
    private void distilOne() {
        progress = 0;
        if (WaterQuality.SALT.equals(boilerQuality())) saltServings = Math.min(saltServings + 1, SALT_SERVINGS);
        boilerServings--;
        if (boilerServings == 0) boilerQuality = 0;
        basinServings++;
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
    public int[] getSlotsForFace(Direction side) {
        if (side == Direction.UP) return TOP;
        return side == Direction.DOWN ? BOTTOM : SIDES;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        return canPlaceItem(slot, stack);
    }

    /** From below: what the machine made, and what it has spent in the slots it took water and fuel from. */
    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (side != Direction.DOWN) return false;
        if (slot == WATER_IN) return DistillerWater.held(stack) == 0;
        if (slot == FUEL) return Loader.burnTime(this, stack) == 0;
        return slot == FILLED_OUT || slot == SALT_OUT;
    }

    /** What each slot takes: water to pour, fuel to burn, a container to fill; nothing into the outputs. */
    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        switch (slot) {
            case WATER_IN:
                return DistillerWater.held(stack) > 0;
            case FUEL:
                return Loader.burnTime(this, stack) > 0;
            case EMPTY_IN:
                return DistillerWater.room(stack) > 0;
            default:
                return false;
        }
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
