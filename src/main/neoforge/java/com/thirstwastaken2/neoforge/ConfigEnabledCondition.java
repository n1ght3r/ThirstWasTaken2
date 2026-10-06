package com.thirstwastaken2.neoforge;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.thirstwastaken2.config.ThirstConfig;
import net.neoforged.neoforge.common.conditions.ICondition;

/**
 * {@code thirstwastaken2:config_enabled}: holds while the config's switch {@code setting} is on. Datagen
 * writes it as a Fabric condition, and this node's build rewrites it to this one.
 */
public record ConfigEnabledCondition(String setting) implements ICondition {
    public static final MapCodec<ConfigEnabledCondition> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.fieldOf("setting").forGetter(ConfigEnabledCondition::setting)
    ).apply(instance, ConfigEnabledCondition::new));

    @Override
    public boolean test(IContext context) {
        return ThirstConfig.get().isSettingEnabled(setting);
    }

    @Override
    public MapCodec<? extends ICondition> codec() {
        return CODEC;
    }
}
