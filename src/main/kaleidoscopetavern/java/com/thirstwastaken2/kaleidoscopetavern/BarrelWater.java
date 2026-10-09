package com.thirstwastaken2.kaleidoscopetavern;

import com.thirstwastaken2.purity.WaterQuality;

/**
 * Kaleidoscope Tavern's barrel, through its mixin: the grade of the water it holds. How the Jade reader
 * reaches it, naming no class of the mod's, so the reader can ask whether or not the mod is installed.
 */
public interface BarrelWater {
    /**
     * The grade of the water in the barrel now, or {@code null} when it holds none, holds another fluid,
     * is brewing, or was filled before the integration existed.
     */
    WaterQuality thirst$heldWater();
}
