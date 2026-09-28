package com.thirstwastaken2.gametest.platform;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;

/**
 * A data file's load conditions, read the way Fabric API reads them as the file loads: through its own
 * codec, which only knows a condition type that was registered. The NeoForge copy reads that loader's
 * spelling, which its build translated the file to.
 */
public final class LoadConditions {
    private LoadConditions() { }

    /** Whether {@code json} carries load conditions at all. */
    public static boolean present(JsonObject json) {
        return json.has(ResourceConditions.CONDITIONS_KEY);
    }

    /** Whether the game would load {@code json} now; true when it carries no conditions. */
    public static boolean hold(JsonObject json) {
        JsonElement conditions = json.get(ResourceConditions.CONDITIONS_KEY);
        if (conditions == null) return true;
        // Fabric API rewrote its conditions for 1.20.5; before it they are tested straight off the JSON.
        //? if >=1.20.5 {
        return net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition.CONDITION_CODEC
                .parse(com.mojang.serialization.JsonOps.INSTANCE, conditions).getOrThrow().test(null);
        //?} else {
        /*return ResourceConditions.objectMatchesConditions(json);
        *///?}
    }
}
