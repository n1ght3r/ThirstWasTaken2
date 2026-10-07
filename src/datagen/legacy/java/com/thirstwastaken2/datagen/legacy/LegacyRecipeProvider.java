package com.thirstwastaken2.datagen.legacy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.compat.FarmersDelight;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.platform.ConfigEnabledCondition;
import com.thirstwastaken2.platform.ItemEnabledCondition;
import com.thirstwastaken2.platform.Vanilla;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.ConditionJsonProvider;
import net.fabricmc.fabric.api.resource.conditions.v1.DefaultResourceConditions;
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
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
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
 * Pot recipes are Farmer's Delight's own type, whose result is read with its tag on both loaders.
 */
public final class LegacyRecipeProvider extends FabricRecipeProvider {
    /** What every boiling recipe makes, and the first grade with no recipe of its own: Clean. */
    static final int BOILED = com.thirstwastaken2.purity.WaterPurity.BOILED;
    // Fabric's convention tags on 1.20.1 still name the material first, and have no iron nuggets.
    private static final TagKey<Item> COPPER_INGOTS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "copper_ingots"));
    private static final TagKey<Item> IRON_INGOTS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "iron_ingots"));
    private static final float PURIFY_EXPERIENCE = 0.0F;
    /** The switch every furnace and smoker water recipe loads under. */
    private static final String FURNACE_BOILING = "enableFurnaceBoiling";
    private static final String TAG = "thirstwastaken2";

    public LegacyRecipeProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public String getName() {
        return "ThirstWasTaken2 Recipes";
    }

    /** One purifiable container, as {@code ThirstRecipeProvider.Container}. */
    private record Container(String name, Item item, boolean potion, boolean bowl, int smeltingTicks) {
        static final List<Container> ALL = List.of(
                new Container("bottle", Items.POTION, true, false, 160),
                new Container("bowl", ThirstItems.TERRACOTTA_WATER_BOWL, false, true, 160),
                new Container("bucket", Items.WATER_BUCKET, false, false, 480));
    }

    /** Smelting and smoking; a smoker takes half a furnace's time. No campfire, as on every later version. */
    private enum Heat {
        SMELTING("smelting"),
        SMOKING("smoking");

        private final String suffix;

        Heat(String suffix) {
            this.suffix = suffix;
        }

        int ticks(int smeltingTicks) {
            return this == SMOKING ? smeltingTicks / 2 : smeltingTicks;
        }

        /** The {@code NbtRecipes} type of this heat. */
        Identifier type() {
            return ThirstWasTaken2.id(suffix);
        }
    }

    @Override
    public void buildRecipes(Consumer<FinishedRecipe> output) {
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ThirstItems.CLAY_BOWL, 3)
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
                .pattern(" L ")
                .pattern("L L")
                .pattern(" L ")
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
                .pattern(" S ")
                .pattern("C C")
                .pattern(" C ")
                .define('S', Items.STRING)
                .define('C', COPPER_INGOTS)
                .unlockedBy("has_copper_ingot", has(COPPER_INGOTS))
                .save(enabled(output, ThirstItems.COPPER_CANTEEN), id("copper_canteen"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ThirstItems.IRON_FLASK)
                .pattern(" N ")
                .pattern("I I")
                .pattern(" I ")
                .define('N', Items.IRON_NUGGET)
                .define('I', IRON_INGOTS)
                .unlockedBy("has_iron_ingot", has(IRON_INGOTS))
                .save(enabled(output, ThirstItems.IRON_FLASK), id("iron_flask"));

        // The distiller and its four parts, as in ThirstRecipeProvider.
        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ThirstItems.COPPER_PIPE, 4)
                .pattern("CCC")
                .define('C', COPPER_INGOTS)
                .unlockedBy("has_copper_ingot", has(COPPER_INGOTS))
                .save(enabled(output, ThirstItems.COPPER_PIPE), id("copper_pipe"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ThirstItems.DISTILLER_BOILER)
                .pattern("CPC")
                .pattern("CIC")
                .pattern("CCC")
                .define('P', ThirstItems.COPPER_PIPE)
                .define('C', COPPER_INGOTS)
                .define('I', IRON_INGOTS)
                .unlockedBy("has_copper_pipe", has(ThirstItems.COPPER_PIPE))
                .save(enabled(output, ThirstItems.DISTILLER_BOILER), id("distiller_boiler"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ThirstItems.COOLING_TUB)
                .pattern("PPP")
                .pattern("PBP")
                .pattern(" U ")
                .define('P', ThirstItems.COPPER_PIPE)
                .define('B', Items.BARREL)
                .define('U', Items.CAULDRON)
                .unlockedBy("has_copper_pipe", has(ThirstItems.COPPER_PIPE))
                .save(enabled(output, ThirstItems.COOLING_TUB), id("cooling_tub"));

        ShapedRecipeBuilder.shaped(RecipeCategory.MISC, ThirstItems.BRICK_FIREBOX)
                .pattern("SSS")
                .pattern("XFX")
                .pattern("XXX")
                .define('S', Items.SMOOTH_STONE_SLAB)
                .define('X', Items.BRICKS)
                .define('F', Items.CAMPFIRE)
                .unlockedBy("has_copper_pipe", has(ThirstItems.COPPER_PIPE))
                .save(enabled(output, ThirstItems.BRICK_FIREBOX), id("brick_firebox"));

        ShapelessRecipeBuilder.shapeless(RecipeCategory.MISC, ThirstItems.COPPER_DISTILLER)
                .requires(ThirstItems.BRICK_FIREBOX)
                .requires(ThirstItems.DISTILLER_BOILER)
                .requires(ThirstItems.COOLING_TUB)
                .requires(ThirstItems.COPPER_PIPE)
                .unlockedBy("has_distiller_boiler", has(ThirstItems.DISTILLER_BOILER))
                .save(enabled(output, ThirstItems.COPPER_DISTILLER), id("copper_distiller"));

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
        vesselPurifyRecipes(output, ThirstItems.COPPER_CANTEEN);
        vesselPurifyRecipes(output, ThirstItems.IRON_FLASK);
        Container.ALL.stream().filter(container -> container.potion() || container.bowl())
                .forEach(container -> cookingPotRecipe(output, container));
    }

    /**
     * Boiling water in the Farmer's Delight Cooking Pot, as {@code FarmersDelightRecipeProvider} writes it
     * on later versions: any fresh grade below Clean comes out Clean, and the files load only alongside Farmer's
     * Delight. No container is named; a 1.20.1 potion has no crafting remainder, so the pot serves the
     * bottle straight into its output slot, as it does the bowl.
     */
    private void cookingPotRecipe(Consumer<FinishedRecipe> output, Container container) {
        Identifier name = id("cooking_pot_purify_water_" + container.name());
        JsonArray grades = new JsonArray();
        for (int purity = 0; purity < BOILED; purity++) {
            CompoundTag in = container.bowl() ? water(null, purity, false) : potion(container, water(null, purity, false));
            grades.add(nbtIngredient(container.item(), in));
        }
        JsonObject anyGrade = new JsonObject();
        anyGrade.addProperty("fabric:type", "fabric:any");
        anyGrade.add("ingredients", grades);
        JsonArray ingredients = new JsonArray();
        ingredients.add(anyGrade);

        JsonObject json = new JsonObject();
        json.addProperty("recipe_book_tab", "drinks");
        json.add("ingredients", ingredients);
        json.add("result", result(container.item(),
                container.bowl() ? bowlTag(BOILED) : potion(container, water(null, BOILED, false))));
        json.addProperty("experience", PURIFY_EXPERIENCE);
        json.addProperty("cookingtime", 200);

        Map<String, ItemLike> unlocks = new LinkedHashMap<>();
        unlocks.put("has_water", container.item());
        List<ConditionJsonProvider> conditions = new ArrayList<>();
        conditions.add(DefaultResourceConditions.allModsLoaded(FarmersDelight.MOD_ID));
        if (container.bowl()) conditions.add(new ItemEnabledCondition(Vanilla.itemId(container.item())));
        withConditions(output, conditions.toArray(new ConditionJsonProvider[0])).accept(new Written(name,
                Identifier.fromNamespaceAndPath(FarmersDelight.MOD_ID, "cooking"), json,
                unlock(name, List.of(name), unlocks), name.withPrefix("recipes/misc/")));
    }

    private void purifyRecipes(Consumer<FinishedRecipe> output, Container container) {
        List<Identifier> names = new ArrayList<>();
        for (int purity = 0; purity < BOILED; purity++) {
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
        // Only the bowl is the mod's own; bottles and buckets are purified whatever its switch says.
        Consumer<FinishedRecipe> purified = furnace(output, container.bowl() ? container.item() : null);

        boolean first = true;
        for (int purity = 0; purity < BOILED; purity++) {
            int to = BOILED;
            CompoundTag in = container.bowl() ? water(null, purity, false) : potion(container, water(null, purity, false));
            CompoundTag out = container.bowl() ? bowlTag(to) : potion(container, water(null, to, false));
            for (Heat heat : Heat.values()) {
                purified.accept(cooking(id(purifyName(container, purity, heat)), heat,
                        nbtIngredient(container.item(), in), result(container.item(), out),
                        heat.ticks(container.smeltingTicks()), purifyGroup(container.name()), first ? unlock : null, unlockId));
                first = false;
            }
        }
    }

    /** A copper canteen or iron flask in a furnace, one recipe per grade matching any fill, as on later versions. */
    private void vesselPurifyRecipes(Consumer<FinishedRecipe> output, Item vessel) {
        String path = Vanilla.itemId(vessel).getPath();
        List<Identifier> names = new ArrayList<>();
        for (int purity = 0; purity < BOILED; purity++) names.add(id(vesselPurifyName(vessel, purity)));
        Map<String, ItemLike> unlocks = new LinkedHashMap<>();
        unlocks.put("has_" + path, vessel);
        Advancement.Builder unlock = unlock(names.get(0), names, unlocks);
        Identifier unlockId = ThirstWasTaken2.id("recipes/misc/purify_water_" + path);
        Consumer<FinishedRecipe> vessels = furnace(output, vessel);

        ThirstConfig defaults = new ThirstConfig();
        int ticks = vessel == ThirstItems.COPPER_CANTEEN
                ? defaults.copperCanteenCapacity * defaults.copperCanteenBoilSeconds * 20
                : defaults.ironFlaskCapacity * defaults.ironFlaskBoilSeconds * 20;
        for (int purity = 0; purity < BOILED; purity++) {
            vessels.accept(cooking(id(vesselPurifyName(vessel, purity)), Heat.SMELTING,
                    nbtIngredient(vessel, water(null, purity, false)), result(vessel, water(null, BOILED, false)),
                    ticks, purifyGroup(path), purity == 0 ? unlock : null, unlockId));
        }
    }

    private static FinishedRecipe cooking(Identifier id, Heat heat, JsonObject ingredient, JsonObject result,
                                          int ticks, String group, Advancement.Builder unlock, Identifier unlockId) {
        JsonObject json = new JsonObject();
        json.addProperty("group", group);
        json.addProperty("category", "misc");
        json.add("ingredient", ingredient);
        json.add("result", result);
        json.addProperty("experience", PURIFY_EXPERIENCE);
        json.addProperty("cookingtime", ticks);
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

    /** {@code output} for furnace and smoker water, under {@code enableFurnaceBoiling} and {@code item}'s switch when not null. */
    private Consumer<FinishedRecipe> furnace(Consumer<FinishedRecipe> output, Item item) {
        ConditionJsonProvider boiling = new ConfigEnabledCondition(FURNACE_BOILING);
        return withConditions(output, item == null
                ? new ConditionJsonProvider[]{boiling}
                : new ConditionJsonProvider[]{new ItemEnabledCondition(Vanilla.itemId(item)), boiling});
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

    /** The recipe book group one family shares, so its grades show as one button, as on later versions. */
    static String purifyGroup(String family) {
        return ThirstWasTaken2.MOD_ID + ":purify_water_" + family;
    }

    static String purifyName(Container container, int purity, Heat heat) {
        return "purify_water_" + container.name() + "_" + purity + "_" + heat.suffix;
    }

    /** A copper canteen's or iron flask's furnace recipe for water at {@code purity}, any fill. */
    static String vesselPurifyName(Item vessel, int purity) {
        return "purify_water_" + Vanilla.itemId(vessel).getPath() + "_" + purity + "_smelting";
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
