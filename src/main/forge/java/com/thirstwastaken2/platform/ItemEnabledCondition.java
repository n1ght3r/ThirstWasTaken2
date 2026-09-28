package com.thirstwastaken2.platform;

import com.google.gson.JsonObject;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.config.ThirstConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

/**
 * {@code thirstwastaken2:item_enabled}: holds while the config has not switched off {@code item}, one of
 * the mod's own items. Datagen writes it as a Fabric condition, and this node's build rewrites it to
 * this one (see {@code forgeConditions} in {@code build.forge.gradle.kts}).
 */
public record ItemEnabledCondition(Identifier item) implements ICondition {
    private static final Identifier ID = ThirstWasTaken2.id("item_enabled");

    public static final IConditionSerializer<ItemEnabledCondition> SERIALIZER = new IConditionSerializer<>() {
        @Override
        public void write(JsonObject json, ItemEnabledCondition value) {
            json.addProperty("item", value.item().toString());
        }

        @Override
        public ItemEnabledCondition read(JsonObject json) {
            return new ItemEnabledCondition(Identifier.parse(GsonHelper.getAsString(json, "item")));
        }

        @Override
        public Identifier getID() {
            return ID;
        }
    };

    @Override
    public Identifier getID() {
        return ID;
    }

    @Override
    public boolean test(IContext context) {
        return ThirstConfig.get().isItemEnabled(item.toString());
    }
}
