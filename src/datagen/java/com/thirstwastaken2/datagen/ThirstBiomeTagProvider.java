package com.thirstwastaken2.datagen;

import com.thirstwastaken2.ThirstWasTaken2;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;

import java.util.concurrent.CompletableFuture;

/**
 * Biomes whose water sits still long enough to go bad. {@code WaterPurity.sampleAt} reads this as
 * the worst baseline a fresh sample can start from, which is why it is a tag: a modded swamp should
 * be able to join it without touching the mod.
 *
 * <p>Terralith's two wetlands join it too, as optional entries so the tag loads without Terralith:
 * Orchid Swamp and Ice Marsh are swamps in all but their tags, and without this their water graded
 * cleaner than a vanilla swamp's.
 */
public final class ThirstBiomeTagProvider extends FabricTagsProvider<Biome> {
    private static final TagKey<Biome> STAGNANT_WATER =
            TagKey.create(Registries.BIOME, ThirstWasTaken2.id("stagnant_water"));
    private static final ResourceKey<Biome> TERRALITH_ORCHID_SWAMP = terralith("orchid_swamp");
    private static final ResourceKey<Biome> TERRALITH_ICE_MARSH = terralith("ice_marsh");

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
                .add(Biomes.MANGROVE_SWAMP)
                .addOptional(TERRALITH_ORCHID_SWAMP)
                .addOptional(TERRALITH_ICE_MARSH);
    }

    private static ResourceKey<Biome> terralith(String path) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("terralith", path));
    }

    @Override
    public String getName() {
        return "ThirstWasTaken2 Biome Tags";
    }
}
