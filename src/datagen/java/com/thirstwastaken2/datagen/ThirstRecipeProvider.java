package com.thirstwastaken2.datagen;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.platform.ThirstComponents;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricRecipeProvider;
import net.fabricmc.fabric.api.recipe.v1.ingredient.DefaultCustomIngredients;
import net.fabricmc.fabric.api.resource.conditions.v1.ResourceCondition;
import com.thirstwastaken2.platform.ItemEnabledCondition;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementRewards;
import net.minecraft.advancements.triggers.RecipeUnlockedTrigger;
import net.minecraft.core.HolderGetter;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.recipes.RecipeBuilder;
//? if >=26.3 {
import net.minecraft.data.worldgen.BootstrapContext;
//?}
import net.minecraft.data.recipes.RecipeCategory;
import net.minecraft.data.recipes.RecipeOutput;
import net.minecraft.data.recipes.RecipeProvider;
import net.minecraft.data.recipes.ShapedRecipeBuilder;
import net.minecraft.data.recipes.ShapelessRecipeBuilder;
import net.minecraft.data.recipes.SimpleCookingRecipeBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
//? if >=26.1 {
import net.minecraft.world.item.ItemStackTemplate;
//?} else
//import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.CampfireCookingRecipe;
import net.minecraft.world.item.crafting.CookingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmeltingRecipe;
import net.minecraft.world.item.crafting.SmokingRecipe;
import net.minecraft.world.level.ItemLike;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiFunction;

/**
 * Every recipe the mod ships, and the recipe book unlocks that go with them.
 *
 * <p>Most of them are purification recipes, and they are one shape rather than dozens of decisions:
 * for each container, each input grade below the cap and each heat source, boiling bumps the water
 * two grades and stops at {@link #PURIFIED}. The iron flask adds one smelting recipe per fill level.
 * Reading them out of {@link #PURIFY_TABLE} is the point of generating them — the JSON files they
 * replace had to be kept consistent by hand.
 *
 * <p>Salt water carries no {@code water_purity} at all, so an ingredient that demands one already
 * excludes it. Demanding {@code water_salty: false} as well is belt and braces, and it is also what
 * forces anything that hands out water to stamp both components, loot included.
 *
 * <p>Every recipe that makes one of the mod's own items, or fills or cleans one, carries a
 * {@code thirstwastaken2:item_enabled} condition naming it, so a pack that switches the item off in the
 * config loses those recipes and their unlocks. The bottle and bucket recipes carry none.
 */
public final class ThirstRecipeProvider extends FabricRecipeProvider {
    /** The grade boiling cannot improve on, so the grade with no recipe of its own. */
    static final int PURIFIED = 3;

    /** Input grade to output grade: a two grade bump, capped. Index is the input grade. */
    private static final int[] PURIFY_TABLE = { 2, 3, 3 };

    /** Any mod's copper, through the convention tag both loaders fill. */
    private static final TagKey<Item> COPPER_INGOTS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/copper"));
    private static final TagKey<Item> IRON_INGOTS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "ingots/iron"));
    private static final TagKey<Item> IRON_NUGGETS =
            TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath("c", "nuggets/iron"));
    // 1.21.9 renamed the chain to the iron chain when it added copper ones.
    //? if >=1.21.9 {
    private static final Item CHAIN = Items.IRON_CHAIN;
    //?} else
    //private static final Item CHAIN = Items.CHAIN;

    static final float PURIFY_EXPERIENCE = 0.35F;
    private static final int SMELTING_TIME = 200;
    /** Half a furnace's time, as a smoker cooks food, which also suits Sophisticated's Smoking upgrade. */
    private static final int SMOKING_TIME = 100;
    private static final int CAMPFIRE_TIME = 600;

    public ThirstRecipeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries);
    }

    @Override
    public String getName() {
        return "ThirstWasTaken2 Recipes";
    }

    // 1.21.2 moved building the recipes into a separate RecipeProvider the Fabric provider creates.
    // Before it the Fabric provider builds them itself, and Recipes is only the helper it calls. 26.3
    // made recipes and their unlock advancements registries the provider bootstraps, so it is handed a
    // context for each rather than one output to write files through.
    //? if >=26.3 {
    @Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries,
                                                  BootstrapContext<Recipe<?>> recipes,
                                                  BootstrapContext<Advancement> advancements) {
        return new Recipes(registries, recipes, advancements, this::withConditions);
    }
    //?} elif >=1.21.2 {
    /*@Override
    protected RecipeProvider createRecipeProvider(HolderLookup.Provider registries, RecipeOutput output) {
        return new Recipes(registries, output, this::withConditions);
    }
    *///?} else {
    /*@Override
    public void buildRecipes(RecipeOutput output) {
        new Recipes(output, this::withConditions).buildRecipes();
    }
    *///?}

    /**
     * {@link #withConditions}, which {@code Recipes} cannot call itself: it is a static class, and from
     * 1.21.2 its output is the vanilla provider's, which only exists once it is constructed.
     */
    interface Conditions extends BiFunction<RecipeOutput, ResourceCondition[], RecipeOutput> { }

    /** The condition that loads a recipe only while the config leaves {@code item}, one of the mod's own, switched on. */
    static ResourceCondition itemEnabled(Item item) {
        return new ItemEnabledCondition(Vanilla.itemId(item));
    }

    /** One purifiable container: what holds the water, and what the recipes call it. */
    record Container(String name, Item item, boolean potion, boolean bowl) {
        static final Container BOTTLE = new Container("bottle", Items.POTION, true, false);
        static final Container BOWL = new Container("bowl", ThirstItems.TERRACOTTA_WATER_BOWL, false, true);
        static final Container BUCKET = new Container("bucket", Items.WATER_BUCKET, false, false);

        static final List<Container> ALL = List.of(BOTTLE, BOWL, BUCKET);
    }

    //? if >=26.3 {
    static final class Recipes extends RecipeProvider {
        private final HolderGetter<Item> items;
        private final Conditions conditions;
        /** The recipes this provider is about to register, for the criteria that name one. */
        private final HolderGetter<Recipe<?>> registered;

        private Recipes(HolderLookup.Provider registries, BootstrapContext<Recipe<?>> recipes,
                        BootstrapContext<Advancement> advancements, Conditions conditions) {
            super(recipes, advancements);
            this.items = registries.lookupOrThrow(Registries.ITEM);
            this.registered = recipes.lookup(Registries.RECIPE);
            this.conditions = conditions;
        }

        private ShapedRecipeBuilder shaped(ItemLike result, int count) {
            return ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, result, count);
        }
    //?} elif >=1.21.2 {
    /*static final class Recipes extends RecipeProvider {
        private final HolderGetter<Item> items;
        private final Conditions conditions;

        private Recipes(HolderLookup.Provider registries, RecipeOutput output, Conditions conditions) {
            super(registries, output);
            this.items = registries.lookupOrThrow(Registries.ITEM);
            this.conditions = conditions;
        }

        private ShapedRecipeBuilder shaped(ItemLike result, int count) {
            return ShapedRecipeBuilder.shaped(items, RecipeCategory.MISC, result, count);
        }
    *///?} else {
    /*static final class Recipes {
        private final RecipeOutput output;
        private final Conditions conditions;

        private Recipes(RecipeOutput output, Conditions conditions) {
            this.output = output;
            this.conditions = conditions;
        }

        private ShapedRecipeBuilder shaped(ItemLike result, int count) {
            return ShapedRecipeBuilder.shaped(RecipeCategory.MISC, result, count);
        }
    *///?}

        //? if >=1.21.2
        @Override
        public void buildRecipes() {
            shaped(ThirstItems.CLAY_BOWL, 4)
                    .pattern("C C")
                    .pattern(" C ")
                    .define('C', Items.CLAY_BALL)
                    .unlockedBy("has_clay_ball", has(Items.CLAY_BALL))
                    .save(enabled(ThirstItems.CLAY_BOWL), recipe("clay_bowl"));

            //? if >=26.1 {
            SimpleCookingRecipeBuilder.smelting(Ingredient.of(ThirstItems.CLAY_BOWL), RecipeCategory.MISC,
                            CookingBookCategory.MISC, ThirstItems.TERRACOTTA_BOWL, 0.1F, SMELTING_TIME)
            //?} else {
            /*SimpleCookingRecipeBuilder.smelting(Ingredient.of(ThirstItems.CLAY_BOWL), RecipeCategory.MISC,
                            ThirstItems.TERRACOTTA_BOWL, 0.1F, SMELTING_TIME)
            *///?}
                    .unlockedBy("has_clay_bowl", has(ThirstItems.CLAY_BOWL))
                    .save(enabled(ThirstItems.TERRACOTTA_BOWL), recipe("terracotta_bowl_from_smelting"));

            shaped(ThirstItems.WATERSKIN, 1)
                    .pattern(" S ")
                    .pattern("L L")
                    .pattern(" L ")
                    .define('S', Items.STRING)
                    .define('L', Items.LEATHER)
                    .unlockedBy("has_leather", has(Items.LEATHER))
                    .save(enabled(ThirstItems.WATERSKIN), recipe("waterskin"));

            // The pot costs a campfire's worth of commitment on top of its own copper, which is what its
            // capacity and one-pass boil pay back. An iron chain for now: 1.21.1 has no copper one.
            shaped(ThirstItems.COPPER_HANGING_POT, 1)
                    .pattern("SKS")
                    .pattern("C C")
                    .pattern("CCC")
                    .define('S', Items.STICK)
                    .define('K', CHAIN)
                    .define('C', COPPER_INGOTS)
                    .unlockedBy("has_copper_ingot", has(COPPER_INGOTS))
                    .save(enabled(ThirstItems.COPPER_HANGING_POT), recipe("copper_hanging_pot"));

            // The same pot in iron, for a world short on copper. Its top row keeps it clear of the
            // cauldron's recipe.
            shaped(ThirstItems.IRON_HANGING_POT, 1)
                    .pattern("SKS")
                    .pattern("I I")
                    .pattern("III")
                    .define('S', Items.STICK)
                    .define('K', CHAIN)
                    .define('I', IRON_INGOTS)
                    .unlockedBy("has_iron_ingot", has(IRON_INGOTS))
                    .save(enabled(ThirstItems.IRON_HANGING_POT), recipe("iron_hanging_pot"));

            // The canteen and the flask share the waterskin's shape, a U with one thing on top, so a
            // player who knows one can guess the others. Their top rows keep them clear of the pots,
            // the cauldron and the bucket. Leather is the canteen's strap, the nugget the flask's cap.
            shaped(ThirstItems.COPPER_CANTEEN, 1)
                    .pattern(" L ")
                    .pattern("C C")
                    .pattern("CCC")
                    .define('L', Items.LEATHER)
                    .define('C', COPPER_INGOTS)
                    .unlockedBy("has_copper_ingot", has(COPPER_INGOTS))
                    .save(enabled(ThirstItems.COPPER_CANTEEN), recipe("copper_canteen"));

            shaped(ThirstItems.IRON_FLASK, 1)
                    .pattern(" N ")
                    .pattern("I I")
                    .pattern("III")
                    .define('N', IRON_NUGGETS)
                    .define('I', IRON_INGOTS)
                    .unlockedBy("has_iron_ingot", has(IRON_INGOTS))
                    .save(enabled(ThirstItems.IRON_FLASK), recipe("iron_flask"));

            // A bucket of fresh water poured into a fired bowl. The result is graded 2 rather than
            // sampled, because the bucket's own grade is gone by the time a recipe sees it.
            Ingredient freshWaterBucket = DefaultCustomIngredients.components(
                    Ingredient.of(Items.WATER_BUCKET),
                    DataComponentPatch.builder()
                            .set(ThirstComponents.WATER_SALTY, false)
                            .build());
            //? if >=1.21.2 {
            ShapelessRecipeBuilder.shapeless(items, RecipeCategory.MISC, bowlResult(2))
                    .requires(ThirstItems.TERRACOTTA_BOWL)
                    .requires(freshWaterBucket)
                    .unlockedBy("has_terracotta_bowl", has(ThirstItems.TERRACOTTA_BOWL))
                    .save(enabled(ThirstItems.TERRACOTTA_WATER_BOWL), recipe("terracotta_water_bowl"));
            //?} else {
            /*// 1.21.1's builder cannot give its result components, so the recipe and its unlock are
            // written out the way the builder itself would write them.
            var bowlRecipe = recipe("terracotta_water_bowl");
            RecipeOutput bowlOutput = enabled(ThirstItems.TERRACOTTA_WATER_BOWL);
            bowlOutput.accept(bowlRecipe,
                    new net.minecraft.world.item.crafting.ShapelessRecipe("",
                            net.minecraft.world.item.crafting.CraftingBookCategory.MISC, bowlResult(2),
                            net.minecraft.core.NonNullList.of(Ingredient.EMPTY,
                                    Ingredient.of(ThirstItems.TERRACOTTA_BOWL), freshWaterBucket)),
                    bowlOutput.advancement()
                            .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(bowlRecipe))
                            .rewards(AdvancementRewards.Builder.recipe(bowlRecipe))
                            .requirements(AdvancementRequirements.Strategy.OR)
                            .addCriterion("has_terracotta_bowl", has(ThirstItems.TERRACOTTA_BOWL))
                            .build(bowlRecipe.withPrefix("recipes/misc/")));
            *///?}

            Container.ALL.forEach(this::purifyRecipes);
            flaskPurifyRecipes();
        }

        /**
         * The recipe output, with everything written through it, recipes and unlocks alike, loading only
         * while the config leaves {@code item} switched on.
         */
        private RecipeOutput enabled(Item item) {
            return conditions.apply(output, new ResourceCondition[]{itemEnabled(item)});
        }

        /**
         * The iron flask in a furnace: one recipe per fill level and grade below the cap, because an
         * ingredient matches exact component values and the result has to keep the servings. Smelting
         * only; the copper canteen has none, which is what sets the two apart. No campfire recipe
         * either: vanilla puts anything with one into the campfire's slots, which would swallow the
         * flask's own boil on a campfire.
         */
        private void flaskPurifyRecipes() {
            List<String> names = new java.util.ArrayList<>();
            for (int servings = 1; servings <= WaterskinItem.MAX_CAPACITY; servings++) {
                for (int purity = 0; purity < PURIFIED; purity++) names.add(flaskPurifyName(servings, purity));
            }
            java.util.SequencedMap<String, ItemLike> unlocks = new java.util.LinkedHashMap<>();
            unlocks.put("has_iron_flask", ThirstItems.IRON_FLASK);
            AdvancementHolder unlock = purifyUnlock("iron_flask", names, unlocks);
            RecipeOutput flasks = enabled(ThirstItems.IRON_FLASK);

            boolean first = true;
            for (int servings = 1; servings <= WaterskinItem.MAX_CAPACITY; servings++) {
                for (int purity = 0; purity < PURIFIED; purity++) {
                    var key = recipe(flaskPurifyName(servings, purity));
                    flasks.accept(key, Heat.SMELTING.create(flaskIngredient(servings, purity),
                            flaskResult(servings, PURIFY_TABLE[purity])), first ? unlock : null);
                    first = false;
                }
            }
        }

        static Ingredient flaskIngredient(int servings, int purity) {
            return DefaultCustomIngredients.components(Ingredient.of(ThirstItems.IRON_FLASK),
                    flaskComponents(servings, purity));
        }

        //? if >=26.1 {
        static ItemStackTemplate flaskResult(int servings, int purity) {
            return new ItemStackTemplate(ThirstItems.IRON_FLASK, flaskComponents(servings, purity));
        }
        //?} else {
        /*static ItemStack flaskResult(int servings, int purity) {
            ItemStack stack = new ItemStack(ThirstItems.IRON_FLASK);
            stack.applyComponents(flaskComponents(servings, purity));
            return stack;
        }
        *///?}

        private static DataComponentPatch flaskComponents(int servings, int purity) {
            return DataComponentPatch.builder()
                    .set(ThirstComponents.WATER_SERVINGS, servings)
                    .set(ThirstComponents.WATER_PURITY, purity)
                    .set(ThirstComponents.WATER_SALTY, false)
                    .build();
        }

        /**
         * The six purification recipes for one container, plus the single recipe book unlock that
         * covers all six. Writing one unlock per recipe would put six identical entries in the
         * recipe book.
         */
        private void purifyRecipes(Container container) {
            AdvancementHolder unlock = purifyUnlock(container);
            // Only the bowl is the mod's own; bottles and buckets are purified whatever the config says.
            RecipeOutput purified = container.bowl() ? enabled(container.item()) : output;

            boolean first = true;
            for (int purity = 0; purity < PURIFIED; purity++) {
                Ingredient ingredient = purifyIngredient(container, purity);
                var result = purifyResult(container, PURIFY_TABLE[purity]);

                for (Heat heat : Heat.values()) {
                    var key = recipe(purifyName(container, purity, heat));
                    // The unlock is one file shared by all six, so only the first accept writes it.
                    purified.accept(key, heat.create(ingredient, result), first ? unlock : null);
                    first = false;
                }
            }
        }

        /**
         * The recipe book unlock for one container: holding either the empty or the filled container
         * is enough, and so is already knowing the recipe.
         */
        private AdvancementHolder purifyUnlock(Container container) {
            List<String> names = new java.util.ArrayList<>();
            for (int purity = 0; purity < PURIFIED; purity++) {
                for (Heat heat : Heat.values()) names.add(purifyName(container, purity, heat));
            }
            return purifyUnlock(container.name(), names, purifyUnlockItems(container));
        }

        /**
         * One unlock for a family of purification recipes: the first of {@code names} stands for the
         * family, and the unlock hands out all of them at once.
         */
        private AdvancementHolder purifyUnlock(String family, List<String> names,
                                               java.util.SequencedMap<String, ItemLike> items) {
            var representative = recipe(names.get(0));
            Advancement.Builder builder = rootedRecipeAdvancement()
                    // 26.3 made recipes a registry, so the criterion names a holder rather than a key.
                    //? if >=26.3 {
                    .addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(registered.getOrThrow(representative)))
                    //?} else
                    //.addCriterion("has_the_recipe", RecipeUnlockedTrigger.unlocked(representative))
                    .rewards(purifyRewards(names))
                    .requirements(AdvancementRequirements.Strategy.OR);
            items.forEach((name, item) -> builder.addCriterion(name, has(item)));
            return builder.build(ThirstWasTaken2.id("recipes/misc/purify_water_" + family));
        }

        /**
         * A recipe advancement hanging off the recipe root.
         *
         * <p>{@code parent(Identifier)} is deprecated for removal, but the root is only published
         * as an identifier, and the overload that survives wants an {@link AdvancementHolder} that
         * nothing here can produce. Vanilla's own recipe builders make the same call, so this is
         * kept in one place until the root is exposed as a holder.
         */
        @SuppressWarnings("removal")
        private static Advancement.Builder rootedRecipeAdvancement() {
            return Advancement.Builder.recipeAdvancement().parent(RecipeBuilder.ROOT_RECIPE_ADVANCEMENT);
        }

        /** Every recipe in the family, so the recipe book learns the whole family at once. */
        private static AdvancementRewards purifyRewards(List<String> names) {
            AdvancementRewards.Builder rewards = new AdvancementRewards.Builder();
            names.forEach(name -> rewards.addRecipe(recipe(name)));
            return rewards.build();
        }

        /** The empty container and the filled one, named the way the criterion is. */
        private static java.util.SequencedMap<String, ItemLike> purifyUnlockItems(Container container) {
            java.util.SequencedMap<String, ItemLike> unlocks = new java.util.LinkedHashMap<>();
            if (container == Container.BOTTLE) {
                unlocks.put("has_glass_bottle", Items.GLASS_BOTTLE);
                unlocks.put("has_potion", Items.POTION);
            } else if (container == Container.BOWL) {
                unlocks.put("has_terracotta_bowl", ThirstItems.TERRACOTTA_BOWL);
                unlocks.put("has_terracotta_water_bowl", ThirstItems.TERRACOTTA_WATER_BOWL);
            } else {
                unlocks.put("has_bucket", Items.BUCKET);
                unlocks.put("has_water_bucket", Items.WATER_BUCKET);
            }
            return unlocks;
        }

        private static String purifyName(Container container, int purity, Heat heat) {
            return "purify_water_" + container.name() + "_" + purity + "_" + heat.suffix;
        }

        static Ingredient purifyIngredient(Container container, int purity) {
            DataComponentPatch.Builder patch = DataComponentPatch.builder();
            if (container.potion()) {
                patch.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
            }
            patch.set(ThirstComponents.WATER_PURITY, purity);
            patch.set(ThirstComponents.WATER_SALTY, false);
            return DefaultCustomIngredients.components(Ingredient.of(container.item()), patch.build());
        }

        //? if >=26.1 {
        static ItemStackTemplate purifyResult(Container container, int purity) {
        //?} else
        //static ItemStack purifyResult(Container container, int purity) {
            if (container.bowl()) return bowlResult(purity);

            DataComponentPatch.Builder patch = DataComponentPatch.builder();
            if (container.potion()) {
                patch.set(DataComponents.POTION_CONTENTS, new PotionContents(Potions.WATER));
            }
            patch.set(ThirstComponents.WATER_PURITY, purity);
            patch.set(ThirstComponents.WATER_SALTY, false);
            //? if >=26.1 {
            return new ItemStackTemplate(container.item(), patch.build());
            //?} else {
            /*ItemStack stack = new ItemStack(container.item());
            stack.applyComponents(patch.build());
            return stack;
            *///?}
        }

        /**
         * A filled bowl carries its grade twice: once as the component the game reads, and once as
         * custom model data index 1, so the sprite is right without the client having to look the
         * grade up per frame. {@code WaterPurity.setQuality} writes the same pair at runtime.
         */
        /*
         * Before 26.1 a recipe result is a live ItemStack, whose components serialize as the delta
         * from the item's own defaults. The filled bowl already defaults to grade 3, fresh and
         * custom model data [0, 3], so a result that sets exactly those writes nothing and a result
         * for grade 2 writes no `water_salty`. The stack the furnace hands out is the same either
         * way, because what is missing from the file is what the item supplies anyway; only the
         * 1.21.11 files read shorter than the rest. `ItemStackTemplate` is what fixed this.
         */
        //? if >=26.1 {
        private static ItemStackTemplate bowlResult(int purity) {
            return new ItemStackTemplate(ThirstItems.TERRACOTTA_WATER_BOWL, bowlComponents(purity));
        }
        //?} else {
        /*private static ItemStack bowlResult(int purity) {
            ItemStack stack = new ItemStack(ThirstItems.TERRACOTTA_WATER_BOWL);
            stack.applyComponents(bowlComponents(purity));
            return stack;
        }
        *///?}

        private static DataComponentPatch bowlComponents(int purity) {
            return DataComponentPatch.builder()
                    .set(ThirstComponents.WATER_PURITY, purity)
                    .set(ThirstComponents.WATER_SALTY, false)
                    .set(DataComponents.CUSTOM_MODEL_DATA, Vanilla.modelSelector(ThirstItems.BOWL_MODEL_INDEX, purity))
                    .build();
        }
    }

    /** Smelting, smoking and campfire cooking differ only in how long they take. */
    private enum Heat {
        SMELTING("smelting", SMELTING_TIME),
        SMOKING("smoking", SMOKING_TIME),
        CAMPFIRE("campfire", CAMPFIRE_TIME);

        private final String suffix;
        private final int time;

        Heat(String suffix, int time) {
            this.suffix = suffix;
            this.time = time;
        }

        //? if >=26.1 {
        AbstractCookingRecipe create(Ingredient ingredient, ItemStackTemplate result) {
            Recipe.CommonInfo common = new Recipe.CommonInfo(true);
            AbstractCookingRecipe.CookingBookInfo book =
                    new AbstractCookingRecipe.CookingBookInfo(CookingBookCategory.MISC, "");
            return switch (this) {
                case SMELTING -> new SmeltingRecipe(common, book, ingredient, result, PURIFY_EXPERIENCE, time);
                case SMOKING -> new SmokingRecipe(common, book, ingredient, result, PURIFY_EXPERIENCE, time);
                case CAMPFIRE -> new CampfireCookingRecipe(common, book, ingredient, result, PURIFY_EXPERIENCE, time);
            };
        }
        //?} else {
        /*AbstractCookingRecipe create(Ingredient ingredient, ItemStack result) {
            return switch (this) {
                case SMELTING -> new SmeltingRecipe("", CookingBookCategory.MISC, ingredient, result, PURIFY_EXPERIENCE, time);
                case SMOKING -> new SmokingRecipe("", CookingBookCategory.MISC, ingredient, result, PURIFY_EXPERIENCE, time);
                case CAMPFIRE -> new CampfireCookingRecipe("", CookingBookCategory.MISC, ingredient, result, PURIFY_EXPERIENCE, time);
            };
        }
        *///?}
    }

    //? if >=26.3 {
    /**
     * The recipe registry, as much of it as a criterion naming one of this mod's recipes needs.
     *
     * <p>26.3 made recipes a registry, and the criteria that name one name a holder rather than a key.
     * A holder is only written out against the registry that vouches for it, and the recipes here are
     * registered into a set of this generator's own that no other provider is handed — so the
     * advancement provider and the Cooking Pot provider answer the recipe registry with this instead,
     * which hands out a reference for any key it is asked for and vouches for the ones it made. The id
     * is all a criterion writes either way, and the game, which loads every recipe file into the real
     * registry, resolves it there.
     */
    static final class RecipeKeys implements net.minecraft.core.HolderGetter<Recipe<?>> {
        @Override
        public java.util.Optional<net.minecraft.core.Holder.Reference<Recipe<?>>> get(ResourceKey<Recipe<?>> key) {
            return java.util.Optional.of(net.minecraft.core.Holder.Reference.createStandAlone(this, key));
        }

        @Override
        public java.util.Optional<net.minecraft.core.HolderSet.Named<Recipe<?>>> get(TagKey<Recipe<?>> tag) {
            return java.util.Optional.empty();
        }

        /** Serialization ops answering the recipe registry with this lookup and every other with {@code registries}. */
        com.mojang.serialization.DynamicOps<com.google.gson.JsonElement> ops(HolderLookup.Provider registries) {
            return net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,
                    new net.minecraft.resources.RegistryOps.RegistryInfoLookup() {
                        @SuppressWarnings("unchecked")
                        @Override
                        public <T> java.util.Optional<net.minecraft.core.HolderGetter<T>> lookup(
                                ResourceKey<? extends net.minecraft.core.Registry<? extends T>> registry) {
                            if (Registries.RECIPE.equals(registry)) {
                                return java.util.Optional.of((net.minecraft.core.HolderGetter<T>) RecipeKeys.this);
                            }
                            return registries.lookup(registry).map(lookup -> lookup);
                        }
                    });
        }
    }

    /** The holder for {@code key} that {@code ops}, built by {@link RecipeKeys#ops}, can write out. */
    static net.minecraft.core.Holder<Recipe<?>> recipeHolder(com.mojang.serialization.DynamicOps<?> ops,
                                                             ResourceKey<Recipe<?>> key) {
        return ((net.minecraft.resources.RegistryOps<?>) ops).getter(Registries.RECIPE).orElseThrow().getOrThrow(key);
    }
    //?}

    /** The iron flask's furnace recipe for {@code servings} of water at {@code purity}. */
    static String flaskPurifyName(int servings, int purity) {
        return "purify_water_iron_flask_" + servings + "_" + purity + "_smelting";
    }

    // Recipes are registry entries with keys from 1.21.2; before it a recipe is known by its id alone.
    //? if >=1.21.2 {
    static ResourceKey<Recipe<?>> recipe(String name) {
        return ResourceKey.create(Registries.RECIPE, ThirstWasTaken2.id(name));
    }
    //?} else {
    /*static Identifier recipe(String name) {
        return ThirstWasTaken2.id(name);
    }
    *///?}
}
