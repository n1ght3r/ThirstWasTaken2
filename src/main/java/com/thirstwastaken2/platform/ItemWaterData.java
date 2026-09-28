package com.thirstwastaken2.platform;

import com.thirstwastaken2.purity.ThirstComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.functions.FunctionUserBuilder;
import net.minecraft.world.level.storage.loot.functions.SetComponentsFunction;

/**
 * The water an item stack carries: servings, grade and salt. The only place that knows how they are
 * stored, which is as the {@link ThirstComponents} data components on every version that has data
 * components. Callers read and write through here, so a version that keeps them in the stack's tag
 * instead changes this class and nothing else.
 *
 * <p>Plain storage, no rules: which items hold water, what a missing grade means and which sprite goes
 * with which water are {@code WaterPurity}'s and {@code WaterskinItem}'s. Reads allocate nothing, since
 * tooltips call them every frame.
 */
public final class ItemWaterData {
    private ItemWaterData() { }

    /** Registers the storage, once, with the other registrations {@code ThirstWasTaken2.initialize} makes. */
    public static void register() {
        Loader.onRegister(Registries.DATA_COMPONENT_TYPE, ThirstComponents::register);
    }

    /** Servings in a waterskin, canteen or flask, 0 when it records none. */
    public static int servings(ItemStack stack) {
        return stack.getOrDefault(ThirstComponents.WATER_SERVINGS, 0);
    }

    public static void setServings(ItemStack stack, int servings) {
        stack.set(ThirstComponents.WATER_SERVINGS, servings);
    }

    /** The grade of the fresh water the stack records, or {@code null} when it records none. */
    public static Integer grade(ItemStack stack) {
        return stack.get(ThirstComponents.WATER_PURITY);
    }

    public static boolean hasGrade(ItemStack stack) {
        return stack.has(ThirstComponents.WATER_PURITY);
    }

    /** Whether the stack records sea water. */
    public static boolean salty(ItemStack stack) {
        return stack.getOrDefault(ThirstComponents.WATER_SALTY, false);
    }

    /**
     * Records fresh water of {@code grade}. Salt is written as {@code false} rather than left out: the
     * purification recipes match on it, and a container that left it out could never be cooked.
     */
    public static void setFresh(ItemStack stack, int grade) {
        stack.set(ThirstComponents.WATER_PURITY, grade);
        stack.set(ThirstComponents.WATER_SALTY, false);
    }

    /** Records sea water, with no grade at all, so no recipe matching on a grade can match it. */
    public static void setSalty(ItemStack stack) {
        stack.remove(ThirstComponents.WATER_PURITY);
        stack.set(ThirstComponents.WATER_SALTY, true);
    }

    /** Takes the grade and the salt off, leaving the servings. */
    public static void clearQuality(ItemStack stack) {
        stack.remove(ThirstComponents.WATER_PURITY);
        stack.remove(ThirstComponents.WATER_SALTY);
    }

    /** Item properties whose stacks start out as fresh water of {@code grade}. See {@link #setFresh}. */
    public static Item.Properties freshByDefault(Item.Properties properties, int grade) {
        return properties.component(ThirstComponents.WATER_PURITY, grade)
                .component(ThirstComponents.WATER_SALTY, false);
    }

    /** Item properties whose stacks start out empty, recording no servings. */
    public static Item.Properties emptyByDefault(Item.Properties properties) {
        return properties.component(ThirstComponents.WATER_SERVINGS, 0);
    }

    /** Adds loot functions that stamp the item as fresh water of {@code grade}. See {@link #setFresh}. */
    public static void stampFreshLoot(FunctionUserBuilder<?> entry, int grade) {
        entry.apply(SetComponentsFunction.setComponent(ThirstComponents.WATER_PURITY, grade));
        entry.apply(SetComponentsFunction.setComponent(ThirstComponents.WATER_SALTY, false));
    }
}
