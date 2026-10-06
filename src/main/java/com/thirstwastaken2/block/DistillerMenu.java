package com.thirstwastaken2.block;

import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The copper distiller's menu: its five slots above the player's inventory, and the numbers its gauges
 * draw, sent from the machine as data slots. Laid out like a furnace's, left to right as the water goes:
 * water in and fuel under it, the fuel left between them, the boiler's gauge, the arrow, the basin's
 * gauge, an empty container in over the filled one out, and the salt beside it. The positions are the texture's; see
 * {@code tools/distiller/generate_distiller_gui.py}.
 *
 * <p>On the client it holds a plain container and data of its own, which the server fills in. Its slots
 * still refuse what the machine would, so a click is never shown taking what the server then puts back.
 * The salt slot is there only while the distiller makes salt.
 */
public final class DistillerMenu extends AbstractContainerMenu {
    public static final int WIDTH = 176;
    public static final int HEIGHT = 166;
    /** Where the player's inventory starts, below the machine. */
    private static final int INVENTORY_Y = 84;

    // The data slots, in DistillerBlockEntity's data.
    public static final int BURN_LEFT = 0;
    public static final int BURN_TOTAL = 1;
    public static final int PROGRESS = 2;
    public static final int SERVING = 3;
    public static final int BOILER = 4;
    /** The boiler's quality as {@code WaterPurity.storedValue} keeps it, 0 when empty. */
    public static final int BOILER_QUALITY = 5;
    public static final int BASIN = 6;
    /** 1 when the tub holds its coolant. */
    public static final int COOLED = 7;
    /** 1 when the distiller makes salt, so its slot shows. */
    public static final int SALT = 8;
    /** Servings each tank holds, the server's config, which the client's may not match. */
    public static final int TANK = 9;
    public static final int DATA = 10;

    private static final int MACHINE_SLOTS = DistillerBlockEntity.SLOTS;
    private static final int INVENTORY_END = MACHINE_SLOTS + 27;
    private static final int HOTBAR_END = INVENTORY_END + 9;

    private final Container container;
    private final ContainerData data;
    private final Level level;

    /** The client's menu, filled in by the server. */
    public DistillerMenu(int id, Inventory inventory) {
        this(id, inventory, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(DATA));
    }

    DistillerMenu(int id, Inventory inventory, Container container, ContainerData data) {
        super(ThirstMenus.COPPER_DISTILLER, id);
        checkContainerSize(container, MACHINE_SLOTS);
        checkContainerDataCount(data, DATA);
        this.container = container;
        this.data = data;
        this.level = inventory.player.level();
        container.startOpen(inventory.player);

        addSlot(new MachineSlot(container, level, DistillerBlockEntity.WATER_IN, 20, 17));
        addSlot(new MachineSlot(container, level, DistillerBlockEntity.FUEL, 20, 53));
        addSlot(new MachineSlot(container, level, DistillerBlockEntity.EMPTY_IN, 126, 17));
        addSlot(new MachineSlot(container, level, DistillerBlockEntity.FILLED_OUT, 126, 53));
        addSlot(new MachineSlot(container, level, DistillerBlockEntity.SALT_OUT, 150, 53) {
            @Override
            public boolean isActive() {
                return makesSalt();
            }
        });
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(inventory, col + row * 9 + 9, 8 + col * 18, INVENTORY_Y + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(inventory, col, 8 + col * 18, INVENTORY_Y + 58));
        }
        addDataSlots(data);
    }

    public int burnLeft() {
        return data.get(BURN_LEFT);
    }

    public int burnTotal() {
        return data.get(BURN_TOTAL);
    }

    public int progress() {
        return data.get(PROGRESS);
    }

    /** Ticks a serving takes. */
    public int servingTicks() {
        return Math.max(1, data.get(SERVING));
    }

    public int boilerServings() {
        return data.get(BOILER);
    }

    /** What the boiler holds, or {@code null} when it is empty. */
    public WaterQuality boilerQuality() {
        int stored = data.get(BOILER_QUALITY);
        if (stored == 0 || boilerServings() == 0) return null;
        return stored == WaterPurity.BLOCK_SALT ? WaterQuality.SALT : WaterQuality.fresh(stored - 1);
    }

    public int basinServings() {
        return data.get(BASIN);
    }

    /** Servings each tank holds. */
    public int tank() {
        return Math.max(1, data.get(TANK));
    }

    public boolean cooled() {
        return data.get(COOLED) != 0;
    }

    public boolean makesSalt() {
        return data.get(SALT) != 0;
    }

    @Override
    public boolean stillValid(Player player) {
        return container.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        container.stopOpen(player);
    }

    /**
     * Shift-click: out of the machine into the inventory, hotbar last; out of the inventory into the slot
     * that takes it, water, fuel or an empty container, or else between the inventory and the hotbar.
     */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack original = stack.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(stack, MACHINE_SLOTS, HOTBAR_END, true)) return ItemStack.EMPTY;
        } else if (DistillerBlockEntity.accepts(level, DistillerBlockEntity.WATER_IN, stack)) {
            if (!moveItemStackTo(stack, DistillerBlockEntity.WATER_IN, DistillerBlockEntity.WATER_IN + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (DistillerBlockEntity.accepts(level, DistillerBlockEntity.FUEL, stack)) {
            if (!moveItemStackTo(stack, DistillerBlockEntity.FUEL, DistillerBlockEntity.FUEL + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (DistillerBlockEntity.accepts(level, DistillerBlockEntity.EMPTY_IN, stack)) {
            if (!moveItemStackTo(stack, DistillerBlockEntity.EMPTY_IN, DistillerBlockEntity.EMPTY_IN + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else if (index < INVENTORY_END) {
            if (!moveItemStackTo(stack, INVENTORY_END, HOTBAR_END, false)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(stack, MACHINE_SLOTS, INVENTORY_END, false)) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.set(ItemStack.EMPTY);
        else slot.setChanged();
        if (stack.getCount() == original.getCount()) return ItemStack.EMPTY;
        slot.onTake(player, stack);
        return original;
    }

    /** One of the machine's slots, taking only what the machine would. */
    private static class MachineSlot extends Slot {
        private final Level level;

        MachineSlot(Container container, Level level, int index, int x, int y) {
            super(container, index, x, y);
            this.level = level;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return DistillerBlockEntity.accepts(level, getContainerSlot(), stack);
        }

        /** The water slot pours one container at a time; water bowls stack. */
        @Override
        public int getMaxStackSize() {
            return getContainerSlot() == DistillerBlockEntity.WATER_IN ? 1 : super.getMaxStackSize();
        }
    }
}
