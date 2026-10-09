package com.thirstwastaken2.block;

import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterContainers;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Which containers the distiller takes water from and fills with it, in servings: a bucket is three, a
 * bottle and a terracotta bowl one, and a waterskin, canteen or flask what it holds or has room for.
 * Another mod's container of plain water, a {@link WaterPurity#vessel}, holds what that says: a Miner's
 * Delight cup a bucket's three, a Cold Sweat waterskin one. A bucket or a vessel moves its servings at
 * once; a carried container as many as it can. What the distiller fills is always Pure.
 */
final class DistillerWater {
    /** The distiller's water, whatever went in. */
    static final WaterQuality PURE = WaterQuality.fresh(WaterPurity.MAX);
    static final int BUCKET = 3;

    private DistillerWater() { }

    /** The servings of water {@code stack} holds that the distiller can take, 0 for anything else. */
    static int held(ItemStack stack) {
        if (stack.is(Items.WATER_BUCKET)) return BUCKET;
        if (stack.is(Items.POTION) && WaterPurity.isWaterContainer(stack)) return 1;
        if (stack.is(ThirstItems.TERRACOTTA_WATER_BOWL)) return 1;
        WaterPurity.Vessel vessel = WaterPurity.vessel(stack.getItem());
        if (vessel != null) return WaterPurity.isWaterContainer(stack) ? vessel.servings() : 0;
        return WaterskinItem.is(stack) ? WaterskinItem.servings(stack) : 0;
    }

    /** How many servings an empty or part-filled {@code stack} can take, 0 for anything else. */
    static int room(ItemStack stack) {
        if (stack.is(Items.BUCKET)) return BUCKET;
        if (stack.is(Items.GLASS_BOTTLE) || stack.is(ThirstItems.TERRACOTTA_BOWL)) return 1;
        WaterPurity.Vessel vessel = WaterPurity.vessel(stack.getItem());
        if (vessel != null) return WaterPurity.isWaterContainer(stack) ? 0 : vessel.servings();
        return WaterskinItem.is(stack) ? WaterskinItem.capacity(stack) - WaterskinItem.servings(stack) : 0;
    }

    /** Whether {@code stack} gives or takes its water all at once, as a bucket or a bottle does. */
    static boolean whole(ItemStack stack) {
        return !WaterskinItem.is(stack);
    }

    /** One of {@code stack} with {@code servings} of its water poured out. */
    static ItemStack emptied(ItemStack stack, int servings) {
        if (stack.is(Items.WATER_BUCKET)) return new ItemStack(Items.BUCKET);
        if (stack.is(Items.POTION)) return new ItemStack(Items.GLASS_BOTTLE);
        if (stack.is(ThirstItems.TERRACOTTA_WATER_BOWL)) return new ItemStack(ThirstItems.TERRACOTTA_BOWL);
        WaterPurity.Vessel vessel = WaterPurity.vessel(stack.getItem());
        if (vessel != null) return new ItemStack(vessel.swap());
        ItemStack skin = stack.copyWithCount(1);
        WaterskinItem.removeWater(skin, servings);
        return skin;
    }

    /** One of {@code stack} given {@code servings} of Pure water. */
    static ItemStack filled(ItemStack stack, int servings) {
        if (stack.is(Items.BUCKET)) return WaterPurity.setQuality(new ItemStack(Items.WATER_BUCKET), PURE);
        if (stack.is(Items.GLASS_BOTTLE)) return WaterPurity.setQuality(Vanilla.waterBottle(), PURE);
        if (stack.is(ThirstItems.TERRACOTTA_BOWL)) return WaterContainers.holding(stack, PURE, 1);
        WaterPurity.Vessel vessel = WaterPurity.vessel(stack.getItem());
        if (vessel != null) return WaterPurity.setQuality(new ItemStack(vessel.swap()), PURE);
        ItemStack skin = stack.copyWithCount(1);
        WaterskinItem.addWater(skin, PURE, servings);
        return skin;
    }

    /** The sound of pouring {@code stack} out. */
    static SoundEvent pourSound(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET) || stack.is(ThirstItems.TERRACOTTA_WATER_BOWL) || held(stack) >= BUCKET
                ? SoundEvents.BUCKET_EMPTY : SoundEvents.BOTTLE_EMPTY;
    }

    /** The sound of filling {@code stack}. */
    static SoundEvent fillSound(ItemStack stack) {
        return stack.is(Items.BUCKET) || stack.is(ThirstItems.TERRACOTTA_BOWL) || !WaterskinItem.is(stack) && room(stack) >= BUCKET
                ? SoundEvents.BUCKET_FILL : SoundEvents.BOTTLE_FILL;
    }
}
