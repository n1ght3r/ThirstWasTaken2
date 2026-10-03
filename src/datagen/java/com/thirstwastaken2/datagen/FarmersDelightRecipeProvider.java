package com.thirstwastaken2.datagen;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.compat.FarmersDelight;
import com.thirstwastaken2.datagen.ThirstRecipeProvider.Container;
import com.thirstwastaken2.platform.ThirstComponents;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceConditions;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.triggers.InventoryChangeTrigger;
import net.minecraft.advancements.triggers.RecipeUnlockedTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
//? if >=26.1 {
import net.minecraft.world.item.ItemStackTemplate;
//?} else
//import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Boiling water in the Farmer's Delight Cooking Pot, one recipe per container.
 *
 * <p>Nothing of Farmer's Delight is on the datagen classpath, so these cannot go through a recipe
 * builder the way {@link ThirstRecipeProvider}'s do. The JSON is assembled here instead, from the same
 * ingredient and result that provider uses, encoded with vanilla's own codecs so each Minecraft version
 * gets its own component format. Every file carries a {@code fabric:all_mods_loaded} condition, so
 * without Farmer's Delight the game skips them rather than failing to parse an unknown recipe type.
 *
 * <p>The pot is the better tool: any fresh grade comes out purified in one go, where the furnace and
 * campfire bump it by two. No container is named, so the pot falls back to the result's crafting
 * remainder, the same as for its own drinks. The bowl has none and goes straight to the output slot.
 * From 1.21.2 a potion's remainder is a glass bottle: the pot hands the bottle back when it starts and
 * wants one in its container slot to serve the water, like Farmer's Delight's milk and hot cocoa.
 */
public final class FarmersDelightRecipeProvider implements DataProvider {
    private static final String COOKING = FarmersDelight.MOD_ID + ":cooking";
    /** The Cooking Pot's own default, the same as a furnace. */
    private static final int COOKING_TIME = 200;
    private static final List<Container> CONTAINERS = List.of(Container.BOTTLE, Container.BOWL);
    private static final String HEARTH_AND_HARVEST = "hearthandharvest";
    // Hearth and Harvest has no build past 1.21.1, so later versions write no salt recipe for it.
    //? if <1.21.2 {
    /*private static final boolean HEARTH_AND_HARVEST_BUILDS = true;
    *///?} else {
    private static final boolean HEARTH_AND_HARVEST_BUILDS = false;
    //?}

    private final PackOutput.PathProvider recipes;
    private final PackOutput.PathProvider advancements;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public FarmersDelightRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.recipes = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipe");
        this.advancements = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancement");
        this.registries = registries;
    }

    @Override
    public String getName() {
        return "ThirstWasTaken2 Farmer's Delight Recipes";
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registries.thenCompose(lookup -> {
            // 26.3 only: the recipe these two unlocks name is written as JSON rather than registered,
            // so the recipe registry is answered by ThirstRecipeProvider.RecipeKeys instead.
            //? if >=26.3 {
            DynamicOps<JsonElement> ops = new ThirstRecipeProvider.RecipeKeys().ops(lookup);
            //?} else
            //DynamicOps<JsonElement> ops = lookup.createSerializationContext(JsonOps.INSTANCE);
            List<CompletableFuture<?>> writes = new ArrayList<>();
            for (Container container : CONTAINERS) {
                String name = name(container);
                writes.add(DataProvider.saveStable(cache, recipe(container, ops),
                        recipes.json(ThirstWasTaken2.id(name))));
                writes.add(DataProvider.saveStable(cache, unlock(container, ops),
                        advancements.json(ThirstWasTaken2.id("recipes/misc/" + name))));
            }
            if (HEARTH_AND_HARVEST_BUILDS) {
                writes.add(DataProvider.saveStable(cache, hearthAndHarvestSalt(ops),
                        recipes.json(Identifier.withDefaultNamespace("salt_from_bottle"))));
            }
            return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
        });
    }

    private static String name(Container container) {
        return "cooking_pot_purify_water_" + container.name();
    }

    private static JsonObject recipe(Container container, DynamicOps<JsonElement> ops) {
        List<Ingredient> grades = new ArrayList<>();
        for (int purity = 0; purity < ThirstRecipeProvider.PURIFIED; purity++) {
            grades.add(ThirstRecipeProvider.Recipes.purifyIngredient(container, purity));
        }
        Ingredient anyGrade = DefaultCustomIngredients.any(grades.toArray(Ingredient[]::new));

        JsonObject json = conditional(container, ops);
        json.addProperty("type", COOKING);
        json.addProperty("recipe_book_tab", "drinks");
        JsonArray ingredients = new JsonArray();
        ingredients.add(encode(INGREDIENT_CODEC, anyGrade, ops));
        json.add("ingredients", ingredients);
        json.add("result", encode(RESULT_CODEC,
                ThirstRecipeProvider.Recipes.purifyResult(container, ThirstRecipeProvider.PURIFIED), ops));
        json.addProperty("experience", ThirstRecipeProvider.PURIFY_EXPERIENCE);
        json.addProperty("cookingtime", COOKING_TIME);
        return json;
    }

    /**
     * Hearth and Harvest's salt from a bottle in the Cooking Pot, replaced at its own id so that only sea
     * water boils down to salt. Its own matched any water bottle by its potion alone, so a fresh bottle
     * matched both it and the purifying recipe above, and recipe order decided which one cooked. This mod
     * loads after Hearth and Harvest (its row in the integration table says so), so this file wins. Its
     * bucket recipe, which nothing of ours matches, is left alone. The result is the mod's item, which
     * the datagen registries do not know, so it is written by hand in 1.21.1's shape.
     */
    private static JsonObject hearthAndHarvestSalt(DynamicOps<JsonElement> ops) {
        DataComponentPatch.Builder seaWater = DataComponentPatch.builder();
        seaWater.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
        seaWater.set(ThirstComponents.WATER_SALTY, true);
        Ingredient ingredient = DefaultCustomIngredients.components(Ingredient.of(Items.POTION), seaWater.build());

        JsonObject json = new JsonObject();
        json.add(ResourceConditions.CONDITIONS_KEY, encode(ResourceCondition.LIST_CODEC,
                List.of(ResourceConditions.allModsLoaded(FarmersDelight.MOD_ID, HEARTH_AND_HARVEST)), ops));
        json.addProperty("type", COOKING);
        JsonArray ingredients = new JsonArray();
        ingredients.add(encode(INGREDIENT_CODEC, ingredient, ops));
        json.add("ingredients", ingredients);
        JsonObject salt = new JsonObject();
        salt.addProperty("count", 2);
        salt.addProperty("id", HEARTH_AND_HARVEST + ":salt");
        json.add("result", salt);
        json.addProperty("experience", 0.35);
        json.addProperty("cookingtime", 100);
        return json;
    }

    /** Holding the filled container, or already knowing the recipe, puts it in the pot's recipe book. */
    @SuppressWarnings("removal")
    private static JsonObject unlock(Container container, DynamicOps<JsonElement> ops) {
        var key = ThirstRecipeProvider.recipe(name(container));
        Advancement advancement = Advancement.Builder.recipeAdvancement()
                .parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
                // 26.3 made recipes a registry, so the criterion names a holder rather than a key.
                //? if >=26.3 {
                .addCriterion("has_the_recipe",
                        RecipeUnlockedTrigger.unlocked(ThirstRecipeProvider.recipeHolder(ops, key)))
                //?} else
                //.addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(key))
                .addCriterion("has_water", InventoryChangeTrigger.TriggerInstance.hasItems(container.item()))
                .rewards(AdvancementRewards.Builder.recipe(key))
                .requirements(AdvancementRequirements.Strategy.OR)
                .build(ThirstWasTaken2.id("recipes/misc/" + name(container)))
                .value();

        JsonObject json = conditional(container, ops);
        encode(Advancement.CODEC, advancement, ops).getAsJsonObject().entrySet()
                .forEach(entry -> json.add(entry.getKey(), entry.getValue()));
        return json;
    }

    /**
     * A JSON object that only loads alongside Farmer's Delight, and for the bowl only while the config
     * leaves the bowls switched on, as the bowl's other recipes do.
     */
    private static JsonObject conditional(Container container, DynamicOps<JsonElement> ops) {
        List<ResourceCondition> conditions = new ArrayList<>();
        conditions.add(ResourceConditions.allModsLoaded(FarmersDelight.MOD_ID));
        if (container.bowl()) conditions.add(ThirstRecipeProvider.itemEnabled(container.item()));
        JsonObject json = new JsonObject();
        json.add(ResourceConditions.CONDITIONS_KEY, encode(ResourceCondition.LIST_CODEC, conditions, ops));
        return json;
    }

    private static <T> JsonElement encode(Codec<T> codec, T value, DynamicOps<JsonElement> ops) {
        return codec.encodeStart(ops, value).getOrThrow();
    }

    // 1.21.2 made every Ingredient non-empty, and 26.1 replaced the ItemStack result with a template.
    //? if >=1.21.2 {
    private static final Codec<Ingredient> INGREDIENT_CODEC = Ingredient.CODEC;
    //?} else
    //private static final Codec<Ingredient> INGREDIENT_CODEC = Ingredient.CODEC_NONEMPTY;
    //? if >=26.1 {
    private static final Codec<ItemStackTemplate> RESULT_CODEC = ItemStackTemplate.CODEC;
    //?} else
    //private static final Codec<ItemStack> RESULT_CODEC = ItemStack.CODEC;
}
