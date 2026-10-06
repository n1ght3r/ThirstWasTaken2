package com.thirstwastaken2.platform;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.config.ThirstConfig;
import net.minecraft.resources.Identifier;

/**
 * {@code thirstwastaken2:config_enabled}: holds while the config's switch {@code setting} is on, see
 * {@code ThirstConfig.isSettingEnabled}. Datagen puts it on the furnace and smoker water recipes, so
 * {@code enableFurnaceBoiling} off leaves them unloaded on the next data load. The NeoForge and Forge
 * builds translate it to their own condition of the same id. Forked like {@link ItemEnabledCondition}.
 */
//? if >=1.21.2 {
public record ConfigEnabledCondition(String setting)
        implements net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition {
    public static final com.mojang.serialization.MapCodec<ConfigEnabledCondition> CODEC =
            com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(
                    com.mojang.serialization.Codec.STRING.fieldOf("setting").forGetter(ConfigEnabledCondition::setting)
            ).apply(instance, ConfigEnabledCondition::new));
    public static final net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType<ConfigEnabledCondition> TYPE =
            net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType.create(
                    ThirstWasTaken2.id("config_enabled"), CODEC);

    static void register() {
        net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions.register(TYPE);
    }

    @Override
    public net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(net.minecraft.resources.RegistryOps.RegistryInfoLookup registryInfo) {
        return ThirstConfig.get().isSettingEnabled(setting);
    }
}
//?} elif >=1.20.5 {
/*public record ConfigEnabledCondition(String setting)
        implements net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition {
    public static final com.mojang.serialization.MapCodec<ConfigEnabledCondition> CODEC =
            com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(
                    com.mojang.serialization.Codec.STRING.fieldOf("setting").forGetter(ConfigEnabledCondition::setting)
            ).apply(instance, ConfigEnabledCondition::new));
    public static final net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType<ConfigEnabledCondition> TYPE =
            net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType.create(
                    ThirstWasTaken2.id("config_enabled"), CODEC);

    static void register() {
        net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions.register(TYPE);
    }

    @Override
    public net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(net.minecraft.core.HolderLookup.Provider registryLookup) {
        return ThirstConfig.get().isSettingEnabled(setting);
    }
}
*///?} else {
/*public record ConfigEnabledCondition(String setting)
        implements net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider {
    private static final Identifier ID = ThirstWasTaken2.id("config_enabled");

    static void register() {
        net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions.register(ID, json ->
                ThirstConfig.get().isSettingEnabled(net.minecraft.util.GsonHelper.getAsString(json, "setting")));
    }

    @Override
    public Identifier getConditionId() {
        return ID;
    }

    @Override
    public void writeParameters(com.google.gson.JsonObject json) {
        json.addProperty("setting", setting);
    }
}
*///?}
