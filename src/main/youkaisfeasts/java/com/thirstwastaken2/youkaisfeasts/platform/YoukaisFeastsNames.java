package com.thirstwastaken2.youkaisfeasts.platform;

/** The names in Youkais' Feasts that differ between its 1.21.1 and 1.20.1 builds. */
public final class YoukaisFeastsNames {
    /**
     * What the Kettle and the Fermentation Tank are used through. 1.21.1 overrides vanilla's
     * {@code useItemOn}; 1.20.1 is built on L2 Modular Blocks, whose {@code onClick} is the mod's own name
     * and is never remapped.
     */
    //? if >=1.20.5 {
    public static final String USE_METHOD = "useItemOn";
    //?} else {
    /*public static final String USE_METHOD = "onClick";
    *///?}

    private YoukaisFeastsNames() { }
}
