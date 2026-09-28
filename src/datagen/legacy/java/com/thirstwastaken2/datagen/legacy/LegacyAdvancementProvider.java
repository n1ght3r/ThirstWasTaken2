package com.thirstwastaken2.datagen.legacy;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.item.WaterskinItem;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricAdvancementProvider;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.FrameType;
import net.minecraft.advancements.RequirementsStrategy;
import net.minecraft.advancements.critereon.ImpossibleTrigger;
import net.minecraft.advancements.critereon.PlayerTrigger;
import net.minecraft.advancements.critereon.RecipeCraftedTrigger;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

import java.util.List;
import java.util.function.Consumer;

/**
 * The mod's own advancement tab on Minecraft 1.20.1: the same advancements, criteria and ids
 * {@code ThirstAdvancementProvider} writes on every later version, which is where their reasons are
 * given. The build compiles this one in its place, because 1.20.1 builds an advancement around the
 * advancement itself rather than a holder, with its own display and criterion types.
 */
public final class LegacyAdvancementProvider extends FabricAdvancementProvider {
    /** The criterion name {@code ThirstAdvancements} awards by. */
    private static final String CRITERION = "thirst";

    public LegacyAdvancementProvider(FabricPackOutput output) {
        super(output);
    }

    @Override
    public void generateAdvancement(Consumer<Advancement> consumer) {
        // A recipe advancement builder is the one without the telemetry flag, which vanilla only reads
        // for its own namespace.
        Advancement root = Advancement.Builder.recipeAdvancement()
                .display(ThirstItems.WATERSKIN, title("root"), description("root"),
                        Identifier.withDefaultNamespace("textures/block/terracotta.png"), FrameType.TASK,
                        false, false, false)
                .addCriterion(CRITERION, PlayerTrigger.TriggerInstance.tick())
                .save(consumer, ThirstWasTaken2.id("root").toString());

        Advancement firstDrink = awarded(consumer, root, "first_drink", ThirstItems.TERRACOTTA_WATER_BOWL, FrameType.TASK);
        Advancement dirtyWater = awarded(consumer, firstDrink, "dirty_water", Items.MUD, FrameType.TASK);
        Advancement boilWater = boilWater(consumer, dirtyWater);
        awarded(consumer, boilWater, "purified_water", Items.GLASS_BOTTLE, FrameType.TASK);
        awarded(consumer, firstDrink, "sea_water", Items.KELP, FrameType.GOAL);
        awarded(consumer, firstDrink, "nether_drink", Items.MAGMA_BLOCK, FrameType.GOAL);
    }

    private static Advancement awarded(Consumer<Advancement> consumer, Advancement parent, String name, Item icon,
                                       FrameType frame) {
        return child(parent, icon, name, frame)
                .addCriterion(CRITERION, new ImpossibleTrigger.TriggerInstance())
                .save(consumer, ThirstWasTaken2.id(name).toString());
    }

    private static Advancement boilWater(Consumer<Advancement> consumer, Advancement parent) {
        Advancement.Builder builder = child(parent, Items.FURNACE, "boil_water", FrameType.TASK);
        for (String container : List.of("bottle", "bowl", "bucket")) {
            for (int purity = 0; purity < 3; purity++) {
                for (String heat : List.of("smelting", "smoking")) {
                    builder.addCriterion(container + "_" + purity + "_" + heat, RecipeCraftedTrigger.TriggerInstance
                            .craftedItem(ThirstWasTaken2.id("purify_water_" + container + "_" + purity + "_" + heat)));
                }
            }
        }
        for (int servings = 1; servings <= WaterskinItem.MAX_CAPACITY; servings++) {
            for (int purity = 0; purity < 3; purity++) {
                builder.addCriterion("iron_flask_" + servings + "_" + purity + "_smelting",
                        RecipeCraftedTrigger.TriggerInstance.craftedItem(
                                ThirstWasTaken2.id(LegacyRecipeProvider.flaskPurifyName(servings, purity))));
            }
        }
        return builder.requirements(RequirementsStrategy.OR).save(consumer, ThirstWasTaken2.id("boil_water").toString());
    }

    private static Advancement.Builder child(Advancement parent, Item icon, String name, FrameType frame) {
        return Advancement.Builder.recipeAdvancement()
                .parent(parent)
                .display(icon, title(name), description(name), null, frame, true, true, false);
    }

    private static Component title(String name) {
        return Component.translatable("advancements.thirstwastaken2." + name + ".title");
    }

    private static Component description(String name) {
        return Component.translatable("advancements.thirstwastaken2." + name + ".description");
    }
}
