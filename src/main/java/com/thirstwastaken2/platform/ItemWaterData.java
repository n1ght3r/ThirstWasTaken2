package com.thirstwastaken2.platform;

import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;

/**
 * The water an item stack carries: servings, grade and salt. The only place that knows how they are
 * stored: as the {@link ThirstComponents} data components on every version that has data components,
 * and on 1.20.1, which has none, as a {@code thirstwastaken2} compound in the stack's tag holding
 * {@code servings}, {@code purity} and {@code salty}. Callers read and write through here.
 *
 * <p>Plain storage, no rules: which items hold water, what a missing grade means and which sprite goes
 * with which water are {@code WaterPurity}'s and {@code WaterskinItem}'s. Reads allocate nothing, since
 * tooltips call them every frame.
 */
public final class ItemWaterData {
    //? if <1.20.5 {
    /*private static final String TAG = "thirstwastaken2";
    private static final String SERVINGS = "servings";
    private static final String PURITY = "purity";
    private static final String SALTY = "salty";
    *///?}

    private ItemWaterData() { }

    /** Registers the storage, once, with the other registrations {@code ThirstWasTaken2.initialize} makes. */
    public static void register() {
        //? if >=1.20.5 {
        Loader.onRegister(net.minecraft.core.registries.Registries.DATA_COMPONENT_TYPE, ThirstComponents::register);
        //?} else {
        /*// A tag in a recipe result needs recipe types of the mod's own before 1.20.5; see NbtRecipes.
        Loader.onRegister(net.minecraft.core.registries.Registries.RECIPE_SERIALIZER, NbtRecipes::register);
        *///?}
    }

    /** Servings in a waterskin, canteen or flask, 0 when it records none. */
    public static int servings(ItemStack stack) {
        //? if >=1.20.5 {
        return stack.getOrDefault(ThirstComponents.WATER_SERVINGS, 0);
        //?} else {
        /*net.minecraft.nbt.CompoundTag data = data(stack);
        return data != null ? data.getInt(SERVINGS) : 0;
        *///?}
    }

    public static void setServings(ItemStack stack, int servings) {
        //? if >=1.20.5 {
        stack.set(ThirstComponents.WATER_SERVINGS, servings);
        //?} else {
        /*writable(stack).putInt(SERVINGS, servings);
        *///?}
    }

    /** The grade of the fresh water the stack records, or {@code null} when it records none. */
    public static Integer grade(ItemStack stack) {
        //? if >=1.20.5 {
        return stack.get(ThirstComponents.WATER_PURITY);
        //?} else {
        /*net.minecraft.nbt.CompoundTag data = data(stack);
        return data != null && data.contains(PURITY, net.minecraft.nbt.Tag.TAG_ANY_NUMERIC) ? data.getInt(PURITY) : null;
        *///?}
    }

    public static boolean hasGrade(ItemStack stack) {
        //? if >=1.20.5 {
        return stack.has(ThirstComponents.WATER_PURITY);
        //?} else {
        /*return grade(stack) != null;
        *///?}
    }

    /** Whether the stack records sea water. */
    public static boolean salty(ItemStack stack) {
        //? if >=1.20.5 {
        return stack.getOrDefault(ThirstComponents.WATER_SALTY, false);
        //?} else {
        /*net.minecraft.nbt.CompoundTag data = data(stack);
        return data != null && data.getBoolean(SALTY);
        *///?}
    }

    /**
     * Records fresh water of {@code grade}. Salt is written as {@code false} rather than left out: the
     * purification recipes match on it, and a container that left it out could never be cooked.
     */
    public static void setFresh(ItemStack stack, int grade) {
        //? if >=1.20.5 {
        stack.set(ThirstComponents.WATER_PURITY, grade);
        stack.set(ThirstComponents.WATER_SALTY, false);
        //?} else {
        /*net.minecraft.nbt.CompoundTag data = writable(stack);
        data.putInt(PURITY, grade);
        data.putBoolean(SALTY, false);
        *///?}
    }

    /** Records sea water, with no grade at all, so no recipe matching on a grade can match it. */
    public static void setSalty(ItemStack stack) {
        //? if >=1.20.5 {
        stack.remove(ThirstComponents.WATER_PURITY);
        stack.set(ThirstComponents.WATER_SALTY, true);
        //?} else {
        /*net.minecraft.nbt.CompoundTag data = writable(stack);
        data.remove(PURITY);
        data.putBoolean(SALTY, true);
        *///?}
    }

    /** Takes the grade and the salt off, leaving the servings. */
    public static void clearQuality(ItemStack stack) {
        //? if >=1.20.5 {
        stack.remove(ThirstComponents.WATER_PURITY);
        stack.remove(ThirstComponents.WATER_SALTY);
        //?} else {
        /*net.minecraft.nbt.CompoundTag data = data(stack);
        if (data == null) return;
        data.remove(PURITY);
        data.remove(SALTY);
        // An empty compound left behind would keep the stack from matching, or stacking with, one that
        // never held water, as a stack with neither component does from 1.20.5.
        if (data.isEmpty()) stack.removeTagKey(TAG);
        *///?}
    }

    /** Item properties whose stacks start out as fresh water of {@code grade}. See {@link #setFresh}. */
    public static Item.Properties freshByDefault(Item.Properties properties, int grade) {
        //? if >=1.20.5 {
        return properties.component(ThirstComponents.WATER_PURITY, grade)
                .component(ThirstComponents.WATER_SALTY, false);
        //?} else {
        /*return DefaultData.add(properties, tag -> {
            net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
            data.putInt(PURITY, grade);
            data.putBoolean(SALTY, false);
            tag.put(TAG, data);
        });
        *///?}
    }

    /** Item properties whose stacks start out empty, recording no servings. */
    public static Item.Properties emptyByDefault(Item.Properties properties) {
        //? if >=1.20.5 {
        return properties.component(ThirstComponents.WATER_SERVINGS, 0);
        //?} else {
        /*// No servings recorded already reads as none.
        return properties;
        *///?}
    }

    /** Adds loot functions that stamp the item as fresh water of {@code grade}. See {@link #setFresh}. */
    public static void stampFreshLoot(FunctionUserBuilder<?> entry, int grade) {
        //? if >=1.20.5 {
        entry.apply(net.minecraft.world.level.storage.loot.functions.SetComponentsFunction.setComponent(
                ThirstComponents.WATER_PURITY, grade));
        entry.apply(net.minecraft.world.level.storage.loot.functions.SetComponentsFunction.setComponent(
                ThirstComponents.WATER_SALTY, false));
        //?} else {
        /*net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
        data.putInt(PURITY, grade);
        data.putBoolean(SALTY, false);
        tag.put(TAG, data);
        entry.apply(net.minecraft.world.level.storage.loot.functions.SetNbtFunction.setTag(tag));
        *///?}
    }

    //? if <1.20.5 {
    /*// The stack's own compound, or null when it has none. Reads through here allocate nothing.
    private static net.minecraft.nbt.CompoundTag data(ItemStack stack) {
        net.minecraft.nbt.CompoundTag tag = stack.getTag();
        return tag != null && tag.contains(TAG, net.minecraft.nbt.Tag.TAG_COMPOUND) ? tag.getCompound(TAG) : null;
    }

    // The stack's own compound, made when it has none.
    private static net.minecraft.nbt.CompoundTag writable(ItemStack stack) {
        return stack.getOrCreateTagElement(TAG);
    }
    *///?}
}
