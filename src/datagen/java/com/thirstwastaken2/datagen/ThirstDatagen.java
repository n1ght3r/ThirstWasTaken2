package com.thirstwastaken2.datagen;

import net.fabricmc.fabric.api.datagen.v1.DataGeneratorEntrypoint;
import net.fabricmc.fabric.api.datagen.v1.FabricDataGenerator;

/**
 * Writes every datapack and asset JSON the mod ships into
 * {@code src/main/generated/<minecraft version>}, which {@code build.gradle.kts} adds as a resource
 * root of {@code main}.
 *
 * <p>This mod is {@code thirstwastaken2-datagen}, but everything it writes belongs to
 * {@code thirstwastaken2}, which is what {@link #getEffectiveModId()} says.
 */
public final class ThirstDatagen implements DataGeneratorEntrypoint {
    @Override
    public String getEffectiveModId() {
        return "thirstwastaken2";
    }

    @Override
    public void onInitializeDataGenerator(FabricDataGenerator generator) {
        FabricDataGenerator.Pack pack = generator.createPack();

        // 1.20.1 has its own recipe and advancement providers in src/datagen/legacy, and no Cooking Pot
        // recipes yet; see build.gradle.kts.
        //? if >=1.20.5 {
        pack.addProvider(ThirstRecipeProvider::new);
        pack.addProvider(FarmersDelightRecipeProvider::new);
        pack.addProvider(ThirstAdvancementProvider::new);
        //?} else {
        /*pack.addProvider(com.thirstwastaken2.datagen.legacy.LegacyRecipeProvider::new);
        pack.addProvider(com.thirstwastaken2.datagen.legacy.LegacyAdvancementProvider::new);
        *///?}
        pack.addProvider(ThirstDamageTypeProvider::new);
        pack.addProvider(ThirstDamageTypeTagProvider::new);
        pack.addProvider(ThirstBiomeTagProvider::new);
        pack.addProvider(ThirstFluidTagProvider::new);
        pack.addProvider(ThirstItemTagProvider::new);
        pack.addProvider(ThirstBlockLootProvider::new);
        pack.addProvider(ThirstModelProvider::new);
        // Item model definitions arrived in 1.21.4, and the provider for them with it.
        //? if >=1.21.4
        pack.addProvider(ThirstItemModelDefinitionProvider::new);
    }
}
