package com.thirstwastaken2.datagen;

import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.purity.WaterPurity;
import net.fabricmc.fabric.api.client.datagen.v1.provider.FabricModelProvider;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.client.data.models.BlockModelGenerators;
import net.minecraft.client.data.models.ItemModelGenerators;
import net.minecraft.client.data.models.model.ModelLocationUtils;
import net.minecraft.client.data.models.model.ModelTemplates;
import net.minecraft.client.data.models.model.TextureMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
//? if >=1.21.4 {
import net.minecraft.client.data.models.model.ItemModelUtils;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.RangeSelectItemModel;
import net.minecraft.client.renderer.item.properties.numeric.CustomModelDataProperty;
//?}

import java.util.ArrayList;
import java.util.List;

/**
 * The item models and the model definitions that select between them.
 *
 * <p>Two items dispatch on {@code minecraft:custom_model_data} rather than showing one sprite. The
 * waterskin reads index 0, how many servings are left, which {@code WaterskinItem.setServings}
 * writes; the filled bowl reads index 1, the water's grade, which {@code WaterPurity.setQuality} and
 * the purification recipes write. Both are mirrored from a real component, never a separate source
 * of truth.
 *
 * <p>From 1.21.4 that dispatch is a range select in the item's model definition. Before it there are
 * no definitions: the item's own model carries an override per value, and custom model data is one
 * integer. Both versions select the same sprite for the same stack.
 *
 * <p>Sea water in a vanilla bottle or bucket keeps vanilla's sprite on every version; only the bowl,
 * which is the mod's own, has a salty one.
 */
public final class ThirstModelProvider extends FabricModelProvider {
    /** The dispatch value sea water gets, one past the last fresh grade. */
    private static final int BOWL_SALT = WaterPurity.MAX + 1;

    public ThirstModelProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateBlockStateModels(BlockModelGenerators generators) {
        // The cauldron's stored quality is a property on vanilla's block and needs no model of its own,
        // because the water level already has one. The hanging pot is the mod's only block.
        HangingPotModels.generate(generators);
    }

    // Comments inside the version blocks below stay line comments: a disabled branch is itself one
    // block comment, and a nested one would end it early.
    //? if >=1.21.4 {
    @Override
    public void generateItemModels(ItemModelGenerators generators) {
        generators.generateFlatItem(ThirstItems.CLAY_BOWL, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ThirstItems.TERRACOTTA_BOWL, ModelTemplates.FLAT_ITEM);
        // Rigid vessels keep one sprite whatever they hold; the item bar shows how full they are.
        generators.generateFlatItem(ThirstItems.COPPER_CANTEEN, ModelTemplates.FLAT_ITEM);
        generators.generateFlatItem(ThirstItems.IRON_FLASK, ModelTemplates.FLAT_ITEM);
        HangingPotModels.item(generators);

        // The plain filled-bowl sprite. Nothing selects it, because every grade has one of its own,
        // but it is what a resource pack that does not know about grades falls back to.
        generators.createFlatItemModel(ThirstItems.TERRACOTTA_WATER_BOWL, ModelTemplates.FLAT_ITEM);

        generators.itemModelOutput.accept(ThirstItems.TERRACOTTA_WATER_BOWL,
                dispatch(generators, ThirstItems.TERRACOTTA_WATER_BOWL, ThirstItems.BOWL_MODEL_INDEX, bowlVariants()));
        generators.itemModelOutput.accept(ThirstItems.WATERSKIN,
                dispatch(generators, ThirstItems.WATERSKIN, ThirstItems.WATERSKIN_MODEL_INDEX, waterskinVariants()));
    }

    // Generates one flat model per suffix and returns a range dispatch over them, with the first
    // suffix as the fallback and the rest reached at thresholds 1, 2, 3 and so on.
    //
    // The index is custom_model_data's, not the entry's: the thresholds are the values the item
    // actually carries, so a bowl of grade 2 selects the third sprite by holding a 2.
    private static ItemModel.Unbaked dispatch(
            ItemModelGenerators generators, Item item, int index, List<String> suffixes) {
        ItemModel.Unbaked fallback = null;
        List<RangeSelectItemModel.Entry> entries = new ArrayList<>();

        for (int variant = 0; variant < suffixes.size(); variant++) {
            Identifier model = generators.createFlatItemModel(
                    item, suffixes.get(variant), ModelTemplates.FLAT_ITEM);
            if (variant == 0) {
                fallback = ItemModelUtils.plainModel(model);
            } else {
                entries.add(ItemModelUtils.override(ItemModelUtils.plainModel(model), variant));
            }
        }

        return ItemModelUtils.rangeSelect(new CustomModelDataProperty(index), fallback, entries);
    }
    //?} else {
    /*@Override
    public void generateItemModels(ItemModelGenerators generators) {
        flat(generators, ThirstItems.CLAY_BOWL);
        flat(generators, ThirstItems.TERRACOTTA_BOWL);
        flat(generators, ThirstItems.COPPER_CANTEEN);
        flat(generators, ThirstItems.IRON_FLASK);
        HangingPotModels.item(generators);
        overrides(generators, ThirstItems.TERRACOTTA_WATER_BOWL, bowlVariants());
        overrides(generators, ThirstItems.WATERSKIN, waterskinVariants());
    }

    private static void flat(ItemModelGenerators generators, Item item) {
        ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item),
                TextureMapping.layer0(item), generators.output);
    }

    // One flat model per suffix, and the item's own model on top of them. It draws the first suffix
    // itself, as the fallback, and overrides to the rest at custom_model_data 1, 2, 3 and so on. The
    // game takes the last override whose value the stack reaches, so they are listed in rising order.
    private static void overrides(ItemModelGenerators generators, Item item, List<String> suffixes) {
        com.google.gson.JsonArray overrides = new com.google.gson.JsonArray();
        for (int variant = 0; variant < suffixes.size(); variant++) {
            Identifier model = ModelLocationUtils.getModelLocation(item, suffixes.get(variant));
            ModelTemplates.FLAT_ITEM.create(model, TextureMapping.layer0(model), generators.output);
            if (variant == 0) continue;

            com.google.gson.JsonObject predicate = new com.google.gson.JsonObject();
            predicate.addProperty("custom_model_data", variant);
            com.google.gson.JsonObject override = new com.google.gson.JsonObject();
            override.add("predicate", predicate);
            override.addProperty("model", model.toString());
            overrides.add(override);
        }

        Identifier fallback = ModelLocationUtils.getModelLocation(item, suffixes.get(0));
        ModelTemplates.FLAT_ITEM.create(ModelLocationUtils.getModelLocation(item), TextureMapping.layer0(fallback),
                generators.output, (id, textures) -> {
                    com.google.gson.JsonObject json = ModelTemplates.FLAT_ITEM.createBaseTemplate(id, textures);
                    json.add("overrides", overrides);
                    return json;
                });
    }
    *///?}

    /** The filled bowl: one sprite per fresh grade, then one for sea water. */
    private static List<String> bowlVariants() {
        List<String> suffixes = new ArrayList<>();
        for (int purity = WaterPurity.MIN; purity <= WaterPurity.MAX; purity++) {
            suffixes.add("_purity_" + purity);
        }
        suffixes.add("_salty");
        return List.copyOf(suffixes);
    }

    /** The waterskin: empty, then one sprite per serving it still holds. */
    private static List<String> waterskinVariants() {
        List<String> suffixes = new ArrayList<>();
        for (int servings = 0; servings <= 3; servings++) {
            suffixes.add("_" + servings);
        }
        return List.copyOf(suffixes);
    }

    @Override
    public String getName() {
        return "ThirstWasTaken2 Models";
    }
}
