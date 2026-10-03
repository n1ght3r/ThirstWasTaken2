package com.thirstwastaken2.datagen;

import com.thirstwastaken2.ThirstWasTaken2;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;

import java.util.concurrent.CompletableFuture;

/**
 * Water that {@code WaterPurity.sampleAt} grades Pure wherever it lies, ahead of the biome and the sea.
 *
 * <p>Spelunkery's Spring Water joins it as optional entries, so the tag loads without Spelunkery: it
 * pools deep in mountains and heals whoever bathes in it, which reads as mineral spring water. See
 * docs/dev/integration/world/SPELUNKERY-INTEGRATION.md.
 */
public final class ThirstFluidTagProvider extends FabricTagsProvider<Fluid> {
    private static final TagKey<Fluid> PURE_WATER =
            TagKey.create(Registries.FLUID, ThirstWasTaken2.id("pure_water"));

    public ThirstFluidTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Registries.FLUID, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Fabric API's tag builder was called getOrCreateTagBuilder on 1.21.1.
        //? if >1.21.1 {
        builder(PURE_WATER)
        //?} else
        //getOrCreateTagBuilder(PURE_WATER)
                .addOptional(spelunkery("spring_water"))
                .addOptional(spelunkery("flowing_spring_water"));
    }

    private static ResourceKey<Fluid> spelunkery(String path) {
        return ResourceKey.create(Registries.FLUID, Identifier.fromNamespaceAndPath("spelunkery", path));
    }

    @Override
    public String getName() {
        return "ThirstWasTaken2 Fluid Tags";
    }
}
