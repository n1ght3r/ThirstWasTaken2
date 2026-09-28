package com.thirstwastaken2.datagen;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementRequirements;
import net.minecraft.advancements.AdvancementType;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.advancements.triggers.Criterion;
import net.minecraft.advancements.triggers.ImpossibleTrigger;
import net.minecraft.advancements.triggers.PlayerTrigger;
import net.minecraft.advancements.triggers.RecipeCraftedTrigger;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.registries.Registries;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

/**
 * The mod's own advancement tab.
 *
 * <p>Every advancement here except the root and {@code boil_water} has a single
 * {@code minecraft:impossible} criterion and is granted by id from
 * {@code com.thirstwastaken2.advancement.ThirstAdvancements}, so the ids and the criterion name have
 * to match that class. {@code AdvancementGameTest} is what catches it when they stop matching.
 *
 * <p>{@code boil_water} is the exception: a furnace or a smoker credits the player who takes the
 * result, so vanilla's {@code recipe_crafted} trigger can see it. The nine campfire recipes cannot be
 * in it, because a campfire has no player to credit.
 */
public final class ThirstAdvancementProvider implements DataProvider {
    /** The criterion name {@code ThirstAdvancements} awards by. */
    private static final String CRITERION = "thirst";

    private final PackOutput.PathProvider advancements;
    private final CompletableFuture<HolderLookup.Provider> registries;

    public ThirstAdvancementProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        this.advancements = output.createPathProvider(PackOutput.Target.DATA_PACK, "advancement");
        this.registries = registries;
    }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        return registries.thenCompose(lookup -> {
            // 26.3 only: the criteria in boil_water name recipes this provider's registry set does not
            // hold, so the recipe registry is answered by ThirstRecipeProvider.RecipeKeys instead.
            //? if >=26.3 {
            DynamicOps<JsonElement> ops = new ThirstRecipeProvider.RecipeKeys().ops(lookup);
            //?} else
            //DynamicOps<JsonElement> ops = lookup.createSerializationContext(JsonOps.INSTANCE);
            List<CompletableFuture<?>> writes = new ArrayList<>();
            generate(ops, advancement -> writes.add(DataProvider.saveStable(cache,
                    Advancement.CODEC.encodeStart(ops, advancement.value()).getOrThrow(),
                    advancements.json(advancement.id()))));
            return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
        });
    }

    private void generate(DynamicOps<JsonElement> ops, Consumer<AdvancementHolder> consumer) {
        AdvancementHolder root = builder()
                // 26.3 split `display` in two, and the root is the only caller that still passes a
                // background. This cannot be a replacement: `display` is also what a child calls on
                // 26.3, so switching the active project would reverse the rule onto those too.
                //? if >=26.3 {
                .rootDisplay(
                //?} else {
                /*.display(
                *///?}
                        ThirstItems.WATERSKIN,
                        title("root"),
                        description("root"),
                        rootBackground(),
                        AdvancementType.TASK,
                        false,
                        false,
                        false)
                // The root is the one advancement nothing grants by hand: everyone gets it on their
                // first tick, which is what makes the tab appear at all.
                .addCriterion(CRITERION, new Criterion<>(CriteriaTriggers.TICK,
                        new PlayerTrigger.TriggerInstance(java.util.Optional.empty())))
                .build(ThirstWasTaken2.id("root"));
        consumer.accept(root);

        AdvancementHolder firstDrink = awarded(consumer, root, "first_drink",
                ThirstItems.TERRACOTTA_WATER_BOWL, AdvancementType.TASK);

        AdvancementHolder dirtyWater = awarded(consumer, firstDrink, "dirty_water",
                Items.MUD, AdvancementType.TASK);

        AdvancementHolder boilWater = boilWater(ops, dirtyWater);
        consumer.accept(boilWater);

        awarded(consumer, boilWater, "purified_water", Items.GLASS_BOTTLE, AdvancementType.TASK);
        awarded(consumer, firstDrink, "sea_water", Items.KELP, AdvancementType.GOAL);
        awarded(consumer, firstDrink, "nether_drink", Items.MAGMA_BLOCK, AdvancementType.GOAL);
    }

    /** One advancement the drinking code grants by id, so its only criterion is impossible. */
    private static AdvancementHolder awarded(
            Consumer<AdvancementHolder> consumer,
            AdvancementHolder parent,
            String name,
            Item icon,
            AdvancementType type) {
        AdvancementHolder advancement = childDisplay(builder().parent(parent), icon, name, type)
                .addCriterion(CRITERION, new Criterion<>(CriteriaTriggers.IMPOSSIBLE,
                        new ImpossibleTrigger.TriggerInstance()))
                .build(ThirstWasTaken2.id(name));
        consumer.accept(advancement);
        return advancement;
    }

    /**
     * Boiling any of the nine smelting or nine smoking recipes, or any of the iron flask's eighteen, as
     * an OR. Vanilla sees these without help, because a furnace and a smoker credit the player who
     * takes the result, which is why this is also the parent of {@code purified_water} rather than a
     * sibling.
     */
    private static AdvancementHolder boilWater(DynamicOps<JsonElement> ops, AdvancementHolder parent) {
        Advancement.Builder builder =
                childDisplay(builder().parent(parent), Items.FURNACE, "boil_water", AdvancementType.TASK);

        for (String container : List.of("bottle", "bowl", "bucket")) {
            for (int purity = 0; purity < 3; purity++) {
                for (String heat : List.of("smelting", "smoking")) {
                    crafted(builder, ops, container + "_" + purity + "_" + heat,
                            ThirstWasTaken2.id("purify_water_" + container + "_" + purity + "_" + heat));
                }
            }
        }
        for (int servings = 1; servings <= WaterskinItem.MAX_CAPACITY; servings++) {
            for (int purity = 0; purity < 3; purity++) {
                crafted(builder, ops, "iron_flask_" + servings + "_" + purity + "_smelting",
                        ThirstWasTaken2.id(ThirstRecipeProvider.flaskPurifyName(servings, purity)));
            }
        }

        // Any one of them is enough, so one requirements list holding all of them.
        return builder
                .requirements(AdvancementRequirements.Strategy.OR)
                .build(ThirstWasTaken2.id("boil_water"));
    }

    /** A criterion met by crafting the recipe {@code id}. */
    private static void crafted(Advancement.Builder builder, DynamicOps<JsonElement> ops, String name, Identifier id) {
        // Recipes are registry entries with keys from 1.21.2, and from 26.3 a registry of their own, so
        // the trigger names the holders rather than one key.
        //? if >=26.3 {
        HolderSet<Recipe<?>> key = HolderSet.direct(
                ThirstRecipeProvider.recipeHolder(ops, ResourceKey.create(Registries.RECIPE, id)));
        //?} elif >=1.21.2 {
        /*ResourceKey<Recipe<?>> key = ResourceKey.create(Registries.RECIPE, id);
        *///?} else {
        /*Identifier key = id;
        *///?}
        builder.addCriterion(name, new Criterion<>(CriteriaTriggers.RECIPE_CRAFTED,
                new RecipeCraftedTrigger.TriggerInstance(java.util.Optional.empty(), key, List.of())));
    }

    /**
     * The icon, title, description and type of an advancement below the root. 26.3 split the tab
     * background off into {@code rootDisplay}, so a child no longer passes one at all.
     */
    private static Advancement.Builder childDisplay(Advancement.Builder builder, Item icon, String name,
                                                    AdvancementType type) {
        //? if >=26.3 {
        return builder.display(icon, title(name), description(name), type, true, true, false);
        //?} else
        //return builder.display(icon, title(name), description(name), null, type, true, true, false);
    }

    /**
     * A builder with the telemetry flag left off, which {@code Advancement.Builder.advancement()}
     * would set. Vanilla only ever reads that flag for advancements in the {@code minecraft}
     * namespace, so setting it on ours would write a line into every file that nothing can act on.
     */
    private static Advancement.Builder builder() {
        return new Advancement.Builder();
    }

    /**
     * The tab's background. Later releases name it by texture id, which the game resolves under
     * {@code textures/}; 1.21.1 takes the texture's full path.
     */
    private static Identifier rootBackground() {
        //? if >1.21.1 {
        return Identifier.withDefaultNamespace("block/terracotta");
        //?} else
        //return Identifier.withDefaultNamespace("textures/block/terracotta.png");
    }

    private static Component title(String name) {
        return Component.translatable("advancements.thirstwastaken2." + name + ".title");
    }

    private static Component description(String name) {
        return Component.translatable("advancements.thirstwastaken2." + name + ".description");
    }

    @Override
    public String getName() {
        return "ThirstWasTaken2 Advancements";
    }
}
