package com.thirstwastaken2.gametest.platform;

import com.google.gson.JsonObject;
import net.minecraftforge.common.crafting.CraftingHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;

/**
 * A data file's load conditions, read the way Forge reads them as the file loads: through its own
 * serializers, which only know a condition type that was registered. The file is the one the build
 * translated from Fabric's spelling, so this also checks that translation.
 */
public final class LoadConditions {
    private static final String KEY = "conditions";

    private LoadConditions() { }

    /** Whether {@code json} carries load conditions at all. */
    public static boolean present(JsonObject json) {
        return json.has(KEY);
    }

    /** Whether the game would load {@code json} now; true when it carries no conditions. */
    public static boolean hold(JsonObject json) {
        return CraftingHelper.processConditions(json, KEY, ICondition.IContext.EMPTY);
    }
}
