package com.thirstwastaken2.config;

/**
 * Whether scooping water from the world with a bottle, bowl, waterskin, canteen or flask takes the
 * source block away, as a bucket does. The config file stores the name, so renaming a value resets it
 * to the default.
 */
public enum WaterSourceUse {
    /** Only in a world whose water does not refill itself: the waterSourceConversion game rule is off. */
    AUTO,
    /** Always, for a pack whose finite water comes from a mod rather than the game rule. */
    ON,
    /** Never: a lake has no levels to lower, and water is drawn from it as from an endless source. */
    OFF
}
