package com.thirstwastaken2.platform;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.config.ThirstConfig;
import net.minecraft.resources.Identifier;

/**
 * {@code thirstwastaken2:item_enabled}: holds while the config has not switched off {@code item}, one of
 * the mod's own items. Datagen puts it on every recipe that makes one of them, so a pack that switches
 * an item off loses its recipes on the next data load. The NeoForge build translates it to its own
 * condition of the same id.
 *
 * <p>Here rather than beside the Fabric entrypoint because Fabric API rewrote its conditions for
 * 1.20.5, typed conditions with codecs replacing a predicate over the JSON, and the parameter of
 * {@code test} changed again in 1.21.2. A fork belongs in {@code platform/}. Both forms read and write
 * the same JSON.
 */
//? if >=1.21.2 {
public record ItemEnabledCondition(Identifier item)
        implements net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition {
    public static final com.mojang.serialization.MapCodec<ItemEnabledCondition> CODEC =
            com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC.fieldOf("item").forGetter(ItemEnabledCondition::item)
            ).apply(instance, ItemEnabledCondition::new));
    public static final net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType<ItemEnabledCondition> TYPE =
            net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType.create(
                    ThirstWasTaken2.id("item_enabled"), CODEC);

    static void register() {
        net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions.register(TYPE);
    }

    @Override
    public net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(net.minecraft.resources.RegistryOps.RegistryInfoLookup registryInfo) {
        return ThirstConfig.get().isItemEnabled(item.toString());
    }
}
//?} elif >=1.20.5 {
/*public record ItemEnabledCondition(Identifier item)
        implements net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition {
    public static final com.mojang.serialization.MapCodec<ItemEnabledCondition> CODEC =
            com.mojang.serialization.codecs.RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Identifier.CODEC.fieldOf("item").forGetter(ItemEnabledCondition::item)
            ).apply(instance, ItemEnabledCondition::new));
    public static final net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType<ItemEnabledCondition> TYPE =
            net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType.create(
                    ThirstWasTaken2.id("item_enabled"), CODEC);

    static void register() {
        net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions.register(TYPE);
    }

    @Override
    public net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditionType<?> getType() {
        return TYPE;
    }

    @Override
    public boolean test(net.minecraft.core.HolderLookup.Provider registryLookup) {
        return ThirstConfig.get().isItemEnabled(item.toString());
    }
}
*///?} else {
/*public record ItemEnabledCondition(Identifier item)
        implements net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider {
    private static final Identifier ID = ThirstWasTaken2.id("item_enabled");

    static void register() {
        net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions.register(ID, json ->
                ThirstConfig.get().isItemEnabled(net.minecraft.util.GsonHelper.getAsString(json, "item")));
    }

    @Override
    public Identifier getConditionId() {
        return ID;
    }

    @Override
    public void writeParameters(com.google.gson.JsonObject json) {
        json.addProperty("item", item.toString());
    }
}
*///?}
