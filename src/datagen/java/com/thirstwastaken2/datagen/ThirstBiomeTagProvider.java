package com.thirstwastaken2.datagen;

import com.thirstwastaken2.ThirstWasTaken2;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.concurrent.CompletableFuture;

/**
 * Biomes whose water sits still long enough to go bad. {@code WaterPurity.sampleAt} reads this as
 * the worst baseline a fresh sample can start from, which is why it is a tag: a modded swamp should
 * be able to join it without touching the mod.
 */
public final class ThirstBiomeTagProvider extends FabricTagsProvider<Biome> {
    private static final TagKey<Biome> STAGNANT_WATER =
            TagKey.create(Registries.BIOME, ThirstWasTaken2.id("stagnant_water"));

    public ThirstBiomeTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Registries.BIOME, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Fabric API's tag builder was called getOrCreateTagBuilder on 1.21.1.
        //? if >1.21.1 {
        builder(STAGNANT_WATER)
        //?} else
        //getOrCreateTagBuilder(STAGNANT_WATER)
                .add(Biomes.SWAMP)
                .add(Biomes.MANGROVE_SWAMP);
    }

    @Override
    public String getName() {
        return "ThirstWasTaken2 Biome Tags";
    }
}
