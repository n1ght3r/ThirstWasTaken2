package com.thirstwastaken2.coldsweatforge;

import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

/**
 * What Cold Sweat's heat does to water, the same rules as NeoForge's copy: the Boiler raises what sits
 * in it a grade at a time, and a campfire boils Cold Sweat's waterskin as it boils a bottle. Salt water
 * stays salt everywhere: boiling does not take the salt out.
 */
public final class BoiledWater {
    /** The Boiler's water slots; 0 is its fuel. */
    private static final int FIRST_SLOT = 1;
    private static final int LAST_SLOT = 9;
    /** What a campfire makes of Dirty water, as the bottle's campfire recipe. Anything better comes out Pure. */
    private static final int CAMPFIRE_FROM_DIRTY = 2;

    private BoiledWater() { }

    /**
     * Whether the Boiler takes {@code stack} into a water slot, besides what its tag already lets in:
     * anything holding water this mod grades, a bottle, a bucket, a bowl, a canteen, a waterskin.
     */
    public static boolean accepts(ItemStack stack) {
        return WaterPurity.isWaterContainer(stack);
    }

    /** Whether anything in the Boiler's water slots can still be raised, which keeps the Boiler burning. */
    public static boolean hasImpureWater(Container boiler) {
        for (int slot = FIRST_SLOT; slot <= LAST_SLOT; slot++) {
            if (raisable(boiler.getItem(slot))) return true;
        }
        return false;
    }

    /**
     * Raises every container in the Boiler's water slots by one grade, on the Boiler's own purifying
     * beat. Returns whether anything changed, which is what the Boiler's own purifying reports too.
     */
    public static boolean purify(Container boiler) {
        boolean changed = false;
        for (int slot = FIRST_SLOT; slot <= LAST_SLOT; slot++) {
            ItemStack stack = boiler.getItem(slot);
            if (!raisable(stack)) continue;
            WaterPurity.purify(stack, 1);
            changed = true;
        }
        if (changed) boiler.setChanged();
        return changed;
    }

    /** The grade a campfire leaves water at, the bottle's campfire recipes as a rule. */
    public static WaterQuality campfire(WaterQuality quality) {
        if (!(quality instanceof WaterQuality.Fresh fresh)) return quality;
        return WaterQuality.fresh(fresh.purity() == WaterPurity.MIN ? CAMPFIRE_FROM_DIRTY : WaterPurity.MAX);
    }

    private static boolean raisable(ItemStack stack) {
        return WaterPurity.isWaterContainer(stack)
                && WaterPurity.quality(stack) instanceof WaterQuality.Fresh fresh && fresh.purity() < WaterPurity.MAX;
    }
}
