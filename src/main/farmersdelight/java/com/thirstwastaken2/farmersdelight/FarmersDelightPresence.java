package com.thirstwastaken2.farmersdelight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Whether Farmer's Delight Refabricated is installed with the Cooking Pot classes the mixin targets.
 *
 * <p>Names no Farmer's Delight class and no Minecraft class, so the mixin plugin can ask before anything
 * the mixin targets is loaded: both answers are resource lookups, which never load a class.
 */
public final class FarmersDelightPresence {
    private static final String RECIPE = "vectorwing/farmersdelight/common/crafting/CookingPotRecipe.class";
    private static final String WRAPPER = "vectorwing/farmersdelight/refabricated/inventory/RecipeWrapper.class";

    private static final Logger LOGGER = LoggerFactory.getLogger("thirstwastaken2");

    private static volatile Boolean present;

    private FarmersDelightPresence() { }

    public static boolean isPresent() {
        Boolean known = present;
        if (known == null) {
            boolean recipe = has(RECIPE);
            known = recipe && has(WRAPPER);
            if (recipe && !known) {
                LOGGER.warn("Farmer's Delight is installed but is not a Refabricated build ThirstWasTaken2 knows; "
                        + "its Cooking Pot may boil salt water into Pure water");
            }
            present = known;
        }
        return known;
    }

    private static boolean has(String resource) {
        return FarmersDelightPresence.class.getClassLoader().getResource(resource) != null;
    }
}
