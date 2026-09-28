package com.thirstwastaken2.datagen.legacy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.platform.ItemEnabledCondition;
import com.thirstwastaken2.platform.Vanilla;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.triggers.RecipeUnlockedTrigger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.FinishedRecipe;
import net.minecraft.data.recipes.RecipeBuilder;
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Every recipe the mod ships on Minecraft 1.20.1, and the recipe book unlocks that go with them: the
 * same recipes, names and unlocks {@code ThirstRecipeProvider} writes on every later version, which is
 * where their reasons are given. The build compiles this one in its place, because the two share no
 * shape: 1.20.1 writes recipes through {@link FinishedRecipe}, has no components, and cannot give a
 * vanilla recipe result a tag.
 *
 * <p>So a stack's water is a tag here, {@code thirstwastaken2: {purity, salty, servings}} as
 * {@code ItemWaterData} keeps it, matched by Fabric's partial {@code fabric:nbt} ingredient. A recipe
 * that hands out water is one of {@code NbtRecipes}' types, whose result carries that tag. The Cooking
 * Pot recipes are left out: Farmer's Delight is not built against on 1.20.1 yet.
 */
public final class LegacyRecipeProvider extends FabricRecipeProvider {
    /** The grade boiling cannot improve on, so the grade with no recipe of its own. */
    static final int PURIFIED = 3;
    /** Input grade to output grade: a two grade bump, capped. Index is the input grade. */
    private static final int[] PURIFY_TABLE = {2, 3, 3};
    // Fabric's convention tags on 1.20.1 still name the material first, and have no iron nuggets.
    private static final TagKey<Item> COPPER_INGOTS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "copper_ingots"));
    private static final TagKey<Item> IRON_INGOTS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "iron_ingots"));
    private static final float PURIFY_EXPERIENCE = 0.35F;
    private static final String TAG = "thirstwastaken2";

    public LegacyRecipeProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public String getName() {
        return "ThirstWasTaken2 Recipes";
    }

    /** One purifiable container, as {@code ThirstRecipeProvider.Container}. */
    private record Container(String name, Item item, boolean potion, boolean bowl) {
        static final List<Container> ALL = List.of(
                new Container("bottle", Items.POTION, true, false),
                new Container("bowl", ThirstItems.TERRACOTTA_WATER_BOWL, false, true),
                new Container("bucket", Items.WATER_BUCKET, false, false));
    }

    /** Smelting, smoking and campfire cooking, and how long each takes. */
    private enum Heat {
        SMELTING("smelting", 200),
        SMOKING("smoking", 100),
        CAMPFIRE("campfire", 600);

        private final String suffix;
        private final int time;

        Heat(String suffix, int time) {
            this.suffix = suffix;
            this.time = time;
        }

        /** The {@code NbtRecipes} type of this heat. */
        Identifier type() {
            return ThirstWasTaken2.id(this == CAMPFIRE ? "campfire_cooking" : suffix);
        }
    }

    @Override
    public void buildRecipes(Consumer<FinishedRecipe> output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ThirstItems.CLAY_BOWL, 4)
                .pattern("C C")
                .pattern(" C ")
                .define('C', Items.CLAY_BALL)
                .unlockedBy("has_clay_ball", has(Items.CLAY_BALL))
                .save(enabled(output, ThirstItems.CLAY_BOWL), id("clay_bowl"));

        SimpleCookingRecipeBuilder.smelting(Ingredient.of(ThirstItems.CLAY_BOWL), RecipeCategory.MISC,
                        ThirstItems.TERRACOTTA_BOWL, 0.1F, 200)
                .unlockedBy("has_clay_bowl", has(ThirstItems.CLAY_BOWL))
                .save(enabled(output, ThirstItems.TERRACOTTA_BOWL), id("terracotta_bowl_from_smelting"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ThirstItems.WATERSKIN)
                .pattern(" S ")
                .pattern("L L")
                .pattern(" L ")
                .define('S', Items.STRING)
                .define('L', Items.LEATHER)
                .unlockedBy("has_leather", has(Items.LEATHER))
                .save(enabled(output, ThirstItems.WATERSKIN), id("waterskin"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ThirstItems.COPPER_HANGING_POT)
                .pattern("SKS")
                .pattern("C C")
                .pattern("CCC")
                .define('S', Items.STICK)
                .define('K', Items.CHAIN)
                .define('C', COPPER_INGOTS)
                .unlockedBy("has_copper_ingot", has(COPPER_INGOTS))
                .save(enabled(output, ThirstItems.COPPER_HANGING_POT), id("copper_hanging_pot"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ThirstItems.IRON_HANGING_POT)
                .pattern("SKS")
                .pattern("I I")
                .pattern("III")
                .define('S', Items.STICK)
                .define('K', Items.CHAIN)
                .define('I', IRON_INGOTS)
                .unlockedBy("has_iron_ingot", has(IRON_INGOTS))
                .save(enabled(output, ThirstItems.IRON_HANGING_POT), id("iron_hanging_pot"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ThirstItems.COPPER_CANTEEN)
                .pattern(" L ")
                .pattern("C C")
                .pattern("CCC")
                .define('L', Items.LEATHER)
                .define('C', COPPER_INGOTS)
                .unlockedBy("has_copper_ingot", has(COPPER_INGOTS))
                .save(enabled(output, ThirstItems.COPPER_CANTEEN), id("copper_canteen"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ThirstItems.IRON_FLASK)
                .pattern(" N ")
                .pattern("I I")
                .pattern("III")
                .define('N', Items.IRON_NUGGET)
                .define('I', IRON_INGOTS)
                .unlockedBy("has_iron_ingot", has(IRON_INGOTS))
                .save(enabled(output, ThirstItems.IRON_FLASK), id("iron_flask"));

        // A bucket of fresh water poured into a fired bowl, graded 2.
        Identifier bowlRecipe = id("terracotta_water_bowl");
        JsonArray ingredients = new JsonArray();
        ingredients.add(item(ThirstItems.TERRACOTTA_BOWL));
        ingredients.add(nbtIngredient(Items.WATER_BUCKET, water(null, null, false)));
        JsonObject bowl = new JsonObject();
        bowl.addProperty("category", "misc");
        bowl.add("ingredients", ingredients);
        bowl.add("result", result(ThirstItems.TERRACOTTA_WATER_BOWL, bowlTag(2)));
        Map<String, ItemLike> bowlUnlocks = new LinkedHashMap<>();
        bowlUnlocks.put("has_terracotta_bowl", ThirstItems.TERRACOTTA_BOWL);
        enabled(output, ThirstItems.TERRACOTTA_WATER_BOWL).accept(new Written(bowlRecipe,
                ThirstWasTaken2.id("crafting_shapeless"), bowl,
                unlock(bowlRecipe, List.of(bowlRecipe), bowlUnlocks), bowlRecipe.withPrefix("recipes/misc/")));

        Container.ALL.forEach(container -> purifyRecipes(output, container));
        flaskPurifyRecipes(output);
    }

    private void purifyRecipes(Consumer<FinishedRecipe> output, Container container) {
        List<Identifier> names = new ArrayList<>();
        for (int purity = 0; purity < PURIFIED; purity++) {
            for (Heat heat : Heat.values()) names.add(id(purifyName(container, purity, heat)));
        }
        Map<String, ItemLike> unlocks = new LinkedHashMap<>();
        if (container.potion()) {
            unlocks.put("has_glass_bottle", Items.GLASS_BOTTLE);
            unlocks.put("has_potion", Items.POTION);
        } else if (container.bowl()) {
            unlocks.put("has_terracotta_bowl", ThirstItems.TERRACOTTA_BOWL);
            unlocks.put("has_terracotta_water_bowl", ThirstItems.TERRACOTTA_WATER_BOWL);
        } else {
            unlocks.put("has_bucket", Items.BUCKET);
            unlocks.put("has_water_bucket", Items.WATER_BUCKET);
        }
        Advancement.Builder unlock = unlock(names.get(0), names, unlocks);
        Identifier unlockId = ThirstWasTaken2.id("recipes/misc/purify_water_" + container.name());
        // Only the bowl is the mod's own; bottles and buckets are purified whatever the config says.
        Consumer<FinishedRecipe> purified = container.bowl() ? enabled(output, container.item()) : output;

        boolean first = true;
        for (int purity = 0; purity < PURIFIED; purity++) {
            int to = PURIFY_TABLE[purity];
            CompoundTag in = container.bowl() ? water(null, purity, false) : potion(container, water(null, purity, false));
            CompoundTag out = container.bowl() ? bowlTag(to) : potion(container, water(null, to, false));
            for (Heat heat : Heat.values()) {
                purified.accept(cooking(id(purifyName(container, purity, heat)), heat,
                        nbtIngredient(container.item(), in), result(container.item(), out),
                        first ? unlock : null, unlockId));
                first = false;
            }
        }
    }

    private void flaskPurifyRecipes(Consumer<FinishedRecipe> output) {
        List<Identifier> names = new ArrayList<>();
        for (int servings = 1; servings <= WaterskinItem.MAX_CAPACITY; servings++) {
            for (int purity = 0; purity < PURIFIED; purity++) names.add(id(flaskPurifyName(servings, purity)));
        }
        Map<String, ItemLike> unlocks = new LinkedHashMap<>();
        unlocks.put("has_iron_flask", ThirstItems.IRON_FLASK);
        Advancement.Builder unlock = unlock(names.get(0), names, unlocks);
        Identifier unlockId = ThirstWasTaken2.id("recipes/misc/purify_water_iron_flask");
        Consumer<FinishedRecipe> flasks = enabled(output, ThirstItems.IRON_FLASK);

        boolean first = true;
        for (int servings = 1; servings <= WaterskinItem.MAX_CAPACITY; servings++) {
            for (int purity = 0; purity < PURIFIED; purity++) {
                flasks.accept(cooking(id(flaskPurifyName(servings, purity)), Heat.SMELTING,
                        nbtIngredient(ThirstItems.IRON_FLASK, water(servings, purity, false)),
                        result(ThirstItems.IRON_FLASK, water(servings, PURIFY_TABLE[purity], false)),
                        first ? unlock : null, unlockId));
                first = false;
            }
        }
    }

    private static FinishedRecipe cooking(Identifier id, Heat heat, JsonObject ingredient, JsonObject result,
                                          Advancement.Builder unlock, Identifier unlockId) {
        JsonObject json = new JsonObject();
        json.addProperty("category", "misc");
        json.add("ingredient", ingredient);
        json.add("result", result);
        json.addProperty("experience", PURIFY_EXPERIENCE);
        json.addProperty("cookingtime", heat.time);
        return new Written(id, heat.type(), json, unlock, unlockId);
    }

    /**
     * One unlock for a family of recipes: knowing the first of {@code names} or holding any of
     * {@code items} is enough, and it hands out all of them at once.
     */
    private static Advancement.Builder unlock(Identifier representative, List<Identifier> names,
                                              Map<String, ItemLike> items) {
        AdvancementRewards.Builder rewards = new AdvancementRewards.Builder();
        names.forEach(rewards::addRecipe);
        Advancement.Builder builder = Advancement.Builder.recipeAdvancement()
                .parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT)
                .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(representative))
                .rewards(rewards)
                .requirements(RequirementsStrategy.OR);
        items.forEach((name, item) -> builder.addCriterion(name, (CriterionTriggerInstance) has(item)));
        return builder;
    }

    /** {@code output}, with everything written through it loading only while the config leaves {@code item} on. */
    private Consumer<FinishedRecipe> enabled(Consumer<FinishedRecipe> output, Item item) {
        return withConditions(output, new ConditionJsonProvider[]{new ItemEnabledCondition(Vanilla.itemId(item))});
    }

    /** The water compound as {@code ItemWaterData} writes it; a null part is left out. */
    private static CompoundTag water(Integer servings, Integer purity, boolean salty) {
        CompoundTag data = new CompoundTag();
        if (servings != null) data.putInt("servings", servings);
        if (purity != null) data.putInt("purity", purity);
        data.putBoolean("salty", salty);
        CompoundTag tag = new CompoundTag();
        tag.put(TAG, data);
        return tag;
    }

    /** A water bottle is a potion of water besides. */
    private static CompoundTag potion(Container container, CompoundTag tag) {
        if (container.potion()) tag.putString("Potion", "minecraft:water");
        return tag;
    }

    /** A filled bowl's grade, and the custom model data its sprite dispatches on, as {@code WaterPurity.setQuality} writes them. */
    private static CompoundTag bowlTag(int purity) {
        CompoundTag tag = water(null, purity, false);
        tag.putInt("CustomModelData", purity);
        return tag;
    }

    private static JsonObject item(ItemLike item) {
        JsonObject json = new JsonObject();
        json.addProperty("item", BuiltInRegistries.ITEM.getKey(item.asItem()).toString());
        return json;
    }

    /**
     * Fabric's partial NBT ingredient. Written here rather than through its own serializer, which goes
     * through JSON numbers and so reads {@code salty:0b} back as an int that no stack's byte equals.
     */
    private static JsonObject nbtIngredient(Item base, CompoundTag nbt) {
        JsonObject json = new JsonObject();
        json.addProperty("fabric:type", "fabric:nbt");
        json.add("base", item(base));
        json.addProperty("nbt", nbt.toString());
        json.addProperty("strict", false);
        return json;
    }

    private static JsonObject result(Item item, CompoundTag nbt) {
        JsonObject json = item(item);
        json.addProperty("nbt", nbt.toString());
        return json;
    }

    private static Identifier id(String name) {
        return ThirstWasTaken2.id(name);
    }

    static String purifyName(Container container, int purity, Heat heat) {
        return "purify_water_" + container.name() + "_" + purity + "_" + heat.suffix;
    }

    /** The iron flask's furnace recipe for {@code servings} of water at {@code purity}. */
    static String flaskPurifyName(int servings, int purity) {
        return "purify_water_iron_flask_" + servings + "_" + purity + "_smelting";
    }

    /** A recipe whose JSON is written out here, and the unlock that goes with it, if any. */
    private record Written(Identifier id, Identifier type, JsonObject json, Advancement.Builder unlock,
                           Identifier unlockId) implements FinishedRecipe {
        @Override
        public void serializeRecipeData(JsonObject out) {
            json.entrySet().forEach(entry -> out.add(entry.getKey(), entry.getValue()));
        }

        @Override
        public JsonObject serializeRecipe() {
            JsonObject out = new JsonObject();
            out.addProperty("type", type.toString());
            serializeRecipeData(out);
            return out;
        }

        @Override
        public Identifier getId() {
            return id;
        }

        @Override
        public RecipeSerializer<?> getType() {
            return BuiltInRegistries.RECIPE_SERIALIZER.get(type);
        }

        @Override
        public JsonObject serializeAdvancement() {
            return unlock == null ? null : unlock.serializeToJson();
        }

        @Override
        public Identifier getAdvancementId() {
            return unlock == null ? null : unlockId;
        }
    }
}
