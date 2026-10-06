package com.thirstwastaken2.platform;

import com.google.gson.JsonObject;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.config.ThirstConfig;
import net.minecraft.resources.Identifier;
import net.minecraft.util.GsonHelper;
import net.minecraftforge.common.crafting.conditions.ICondition;
import net.minecraftforge.common.crafting.conditions.IConditionSerializer;

/**
 * {@code thirstwastaken2:config_enabled}: holds while the config's switch {@code setting} is on. Datagen
 * writes it as a Fabric condition, and this node's build rewrites it to this one.
 */
public record ConfigEnabledCondition(String setting) implements ICondition {
    private static final Identifier ID = ThirstWasTaken2.id("config_enabled");

    public static final IConditionSerializer<ConfigEnabledCondition> SERIALIZER = new IConditionSerializer<>() {
        @Override
        public void write(JsonObject json, ConfigEnabledCondition value) {
            json.addProperty("setting", value.setting());
        }

        @Override
        public ConfigEnabledCondition read(JsonObject json) {
            return new ConfigEnabledCondition(GsonHelper.getAsString(json, "setting"));
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
        return ThirstConfig.get().isSettingEnabled(setting);
    }
}
