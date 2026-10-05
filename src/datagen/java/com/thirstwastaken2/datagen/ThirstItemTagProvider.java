package com.thirstwastaken2.datagen;

import com.thirstwastaken2.block.DistillerSalt;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;

import java.util.concurrent.CompletableFuture;

/**
 * The salt the copper distiller may leave behind, {@link DistillerSalt#TAG}. The mod adds none, so every
 * entry is another mod's and optional, and the tag loads empty without them. From the jars this repo
 * builds against: Expanded Delight, Spelunkery, Cook's Collection (Cultural Delights) and Hearth and
 * Harvest put their salt in {@code c:dusts/salt}; Croptopia, Spelunkery and Cook's Collection in
 * {@code c:salt} and {@code c:salts}. Hearth and Harvest on 1.20.1 tags its salt nowhere, so it is named
 * itself. 1.20.1 Forge mods spell the convention {@code forge:}, so that node takes those too.
 */
public final class ThirstItemTagProvider extends FabricTagsProvider<Item> {
    public ThirstItemTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Registries.ITEM, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        // Fabric API's tag builder was called getOrCreateTagBuilder on 1.21.1.
        //? if >1.21.1 {
        builder(DistillerSalt.TAG)
        //?} else
        //getOrCreateTagBuilder(DistillerSalt.TAG)
                .addOptionalTag(tag("c", "dusts/salt"))
                .addOptionalTag(tag("c", "salt"))
                .addOptionalTag(tag("c", "salts"))
                //? if <1.20.5 {
                /*.addOptionalTag(tag("forge", "dusts/salt"))
                .addOptionalTag(tag("forge", "salt"))
                *///?}
                .addOptional(ResourceKey.create(Registries.ITEM,
                        Identifier.fromNamespaceAndPath("hearthandharvest", "salt")));
    }

    private static TagKey<Item> tag(String namespace, String path) {
        return TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(namespace, path));
    }

    @Override
    public String getName() {
        return "ThirstWasTaken2 Item Tags";
    }
}
