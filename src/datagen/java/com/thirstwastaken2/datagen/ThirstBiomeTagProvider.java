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
 * Two biome tags {@code WaterPurity.sampleAt} reads.
 *
 * <p>{@code stagnant_water}: biomes whose water sits still long enough to go bad. {@code WaterPurity.sampleAt} reads this as
 * the worst baseline a fresh sample can start from, which is why it is a tag: a modded swamp should
 * be able to join it without touching the mod.
 *
 * <p>Terralith's two wetlands join it too, as optional entries so the tag loads without Terralith:
 * Orchid Swamp and Ice Marsh are swamps in all but their tags, and without this their water graded
 * cleaner than a vanilla swamp's. No Man's Land's Bog, Bayou and Dark Swamp join for the same reason,
 * and its Blackwater River, which it tags both river and swamp: a dark, slow swamp river, which this tag
 * wins over {@code minecraft:is_river}.
 *
 * <p>{@code sea_water}: biomes whose water is the sea's, beside {@code minecraft:is_ocean} and
 * {@code minecraft:is_beach}. A tag of our own, because adding a biome to {@code is_beach} would move
 * buried treasure and whatever else other mods hang on that tag. No Man's Land's Mud Beach replaces part
 * of a beach yet is not in {@code is_beach}, so without this the sea at its edge read fresh.
 */
public final class ThirstBiomeTagProvider extends FabricTagsProvider<Biome> {
    private static final TagKey<Biome> STAGNANT_WATER =
            TagKey.create(Registries.BIOME, ThirstWasTaken2.id("stagnant_water"));
    private static final ResourceKey<Biome> TERRALITH_ORCHID_SWAMP = terralith("orchid_swamp");
    private static final ResourceKey<Biome> TERRALITH_ICE_MARSH = terralith("ice_marsh");
    private static final TagKey<Biome> SEA_WATER =
            TagKey.create(Registries.BIOME, ThirstWasTaken2.id("sea_water"));

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
                .addOptional(TERRALITH_ICE_MARSH)
                .addOptional(noMansLand("bog"))
                .addOptional(noMansLand("bayou"))
                .addOptional(noMansLand("dark_swamp"))
                .addOptional(noMansLand("blackwater_river"));
        //? if >1.21.1 {
        builder(SEA_WATER)
        //?} else
        //getOrCreateTagBuilder(SEA_WATER)
                .addOptional(noMansLand("mud_beach"));
    }

    private static ResourceKey<Biome> terralith(String path) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("terralith", path));
    }

    private static ResourceKey<Biome> noMansLand(String path) {
        return ResourceKey.create(Registries.BIOME, Identifier.fromNamespaceAndPath("nomansland", path));
    }

    @Override
    public String getName() {
        return "ThirstWasTaken2 Biome Tags";
    }
}
