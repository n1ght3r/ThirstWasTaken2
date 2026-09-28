package com.thirstwastaken2.datagen;

// Item model definitions, assets/<namespace>/items/, arrived in 1.21.4. Before it no stack could have
// been pointed at one, so there is nothing for this provider to write, and on those versions the file
// holds no class at all. Comments inside the block stay line comments: a
// disabled branch is itself one block comment.
//? if >=1.21.4 {
import com.thirstwastaken2.ThirstWasTaken2;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.color.item.Potion;
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.item.ClientItem;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

// The two item model definitions that belong to no item of the mod's own, kept only for old saves.
//
// Up to 1.4 sea water pointed the minecraft:item_model component of vanilla's water bottle and water
// bucket at these ids, so a bottle or bucket filled back then still carries it. Sea water no longer
// changes their sprite, so every node looks the same, 1.21.1 and 1.20.1 included, which never could;
// the tooltip says Salty instead. Without these files such a stack would draw as a missing model, so
// each draws exactly what vanilla's own definition draws: the potion model tinted by its contents, and
// the water bucket.
public final class ThirstItemModelDefinitionProvider implements DataProvider {
    private static final Identifier POTION_MODEL = Identifier.withDefaultNamespace("item/potion");
    private static final Identifier WATER_BUCKET_MODEL = Identifier.withDefaultNamespace("item/water_bucket");

    private final PackOutput.PathProvider definitions;

    public ThirstItemModelDefinitionProvider(FabricPackOutput output) {
        this.definitions = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "items");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput writer) {
        Map<Identifier, ClientItem> items = new LinkedHashMap<>();
        items.put(ThirstWasTaken2.id("salt_water_bottle"),
                definition(ItemModelUtils.tintedModel(POTION_MODEL, new Potion())));
        items.put(ThirstWasTaken2.id("salt_water_bucket"),
                definition(ItemModelUtils.plainModel(WATER_BUCKET_MODEL)));

        return DataProvider.saveAll(writer, ClientItem.CODEC, definitions, items);
    }

    private static ClientItem definition(ItemModel.Unbaked model) {
        return new ClientItem(model, ClientItem.Properties.DEFAULT);
    }

    @Override
    public String getName() {
        return "ThirstWasTaken2 Item Model Definitions";
    }
}
//?}
