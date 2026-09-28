package com.thirstwastaken2.platform;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Default data for the mod's items on Minecraft 1.20.1, which has no default components: the tag a new
 * stack of an item starts with, as a filled bowl starts out as grade 3 water. {@link ItemWaterData} and
 * {@link Vanilla} add to it where later versions add a default component, {@link Vanilla#registerItem}
 * moves it from the properties onto the item once the item exists, and {@code ItemStackMixin} copies it
 * onto every stack made from the item. On every later version nothing ever calls in here.
 */
public final class DefaultData {
    private static final Map<Item.Properties, CompoundTag> PENDING = new IdentityHashMap<>();
    private static final Map<Item, CompoundTag> DEFAULTS = new IdentityHashMap<>();

    private DefaultData() { }

    /** Adds to the tag the stacks of the item built from {@code properties} start with. */
    static Item.Properties add(Item.Properties properties, Consumer<CompoundTag> writer) {
        writer.accept(PENDING.computeIfAbsent(properties, ignored -> new CompoundTag()));
        return properties;
    }

    /** Moves what {@link #add} gathered for {@code properties} onto {@code item}. */
    static void itemBuilt(Item.Properties properties, Item item) {
        CompoundTag tag = PENDING.remove(properties);
        if (tag != null) DEFAULTS.put(item, tag);
    }

    /** Gives a stack that has just been made its item's default tag. Called by {@code ItemStackMixin} on 1.20.1. */
    public static void apply(ItemStack stack) {
        //? if <1.20.5 {
        /*if (DEFAULTS.isEmpty()) return;
        CompoundTag tag = DEFAULTS.get(stack.getItem());
        if (tag != null && !stack.hasTag()) stack.setTag(tag.copy());
        *///?}
    }
}
