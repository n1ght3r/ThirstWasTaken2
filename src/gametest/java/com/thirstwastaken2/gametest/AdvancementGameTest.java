package com.thirstwastaken2.gametest;

import com.thirstwastaken2.ThirstWasTaken2;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.crafting.Recipe;

import java.util.List;

/**
 * The advancements are a datapack, and the compiler has no opinion about those. Nothing fails to
 * build when an id is misspelled, a parent names an advancement that was renamed, or a reward names
 * a recipe that does not exist: the game drops it and says nothing.
 *
 * <p>{@code ThirstAdvancements} awards its advancements by id, so an id that no longer resolves is
 * exactly the kind of silent nothing these tests exist to catch.
 */
public final class AdvancementGameTest {
    /** Every advancement on the mod's own tab, root first. */
    private static final List<String> TAB = List.of(
            "root", "first_drink", "dirty_water", "boil_water", "purified_water", "sea_water",
            "nether_drink");

    /** Every recipe-book unlock, named after the advancement rather than the recipe it rewards. */
    private static final List<String> RECIPE_UNLOCKS = List.of(
            "clay_bowl", "terracotta_bowl_from_smelting", "terracotta_water_bowl", "waterskin",
            "purify_water_bottle", "purify_water_bowl", "purify_water_bucket");

    /**
     * Every advancement on the tab loaded, and one tab means one root with everything else hanging off
     * it. A child whose parent is missing is dropped along with everything under it.
     */
    @GameTest
    public void everyAdvancementHangsOffTheRoot(GameTestHelper helper) {
        for (String name : TAB) {
            var holder = advancement(helper, ThirstWasTaken2.id(name));
            TestFixtures.check(helper, holder != null, "advancement " + name + " did not load");
            boolean shouldBeRoot = name.equals("root");
            TestFixtures.check(helper, isRoot(holder) == shouldBeRoot,
                    shouldBeRoot ? "the root advancement should have no parent"
                            : name + " should hang off another advancement of this mod");
        }
        helper.succeed();
    }

    /** The whole point of the recipe advancements: they hand out recipes the game actually has. */
    @GameTest
    public void recipeAdvancementsRewardRecipesThatExist(GameTestHelper helper) {
        MinecraftServer server = helper.getLevel().getServer();
        for (String name : RECIPE_UNLOCKS) {
            var holder = advancement(helper, ThirstWasTaken2.id("recipes/misc/" + name));
            TestFixtures.check(helper, holder != null, "recipe advancement " + name + " did not load");
            if (holder == null) continue;

            var rewards = rewardedRecipes(holder);
            TestFixtures.check(helper, !rewards.isEmpty(), name + " should unlock at least one recipe");
            for (var recipe : rewards) {
                TestFixtures.check(helper, server.getRecipeManager().byKey(recipe).isPresent(),
                        "recipe " + recipeId(recipe) + ", unlocked by " + name + ", does not exist");
            }
        }
        helper.succeed();
    }

    /** Recipes are registry entries with keys from 1.21.2; before it a recipe is known by its id alone. */
    //? if >=1.21.2 {
    private static Identifier recipeId(ResourceKey<Recipe<?>> recipe) {
        return recipe.identifier();
    }
    //?} else {
    /*private static Identifier recipeId(Identifier recipe) {
        return recipe;
    }
    *///?}

    /**
     * The Cooking Pot recipes and their unlocks name a recipe type only Farmer's Delight registers, so
     * they carry a load condition. The test server runs without it, and must skip them rather than
     * fail on them. The purification unlock beside them is the control that the lookup works at all.
     */
    @GameTest
    public void cookingPotFilesAreSkippedWithoutFarmersDelight(GameTestHelper helper) {
        TestFixtures.check(helper, advancement(helper, ThirstWasTaken2.id("recipes/misc/purify_water_bottle")) != null,
                "the furnace purification unlock should load, or the check below proves nothing");
        for (String container : List.of("bottle", "bowl")) {
            String name = "cooking_pot_purify_water_" + container;
            TestFixtures.check(helper, advancement(helper, ThirstWasTaken2.id("recipes/misc/" + name)) == null,
                    "the unlock for " + name + " should be skipped without Farmer's Delight");
        }
        helper.succeed();
    }

    // An advancement by id, whether it is the root, and the recipes it rewards. 1.20.2 put an advancement
    // behind a holder that carries its id, and 1.21.2 named the recipes by registry key.
    //? if >=1.21.2 {
    private static net.minecraft.advancements.AdvancementHolder advancement(GameTestHelper helper, Identifier id) {
        return helper.getLevel().getServer().getAdvancements().get(id);
    }

    private static boolean isRoot(net.minecraft.advancements.AdvancementHolder holder) {
        return holder.value().isRoot();
    }

    private static List<ResourceKey<Recipe<?>>> rewardedRecipes(net.minecraft.advancements.AdvancementHolder holder) {
        return holder.value().rewards().recipes();
    }
    //?} elif >=1.20.5 {
    /*private static net.minecraft.advancements.AdvancementHolder advancement(GameTestHelper helper, Identifier id) {
        return helper.getLevel().getServer().getAdvancements().get(id);
    }

    private static boolean isRoot(net.minecraft.advancements.AdvancementHolder holder) {
        return holder.value().isRoot();
    }

    private static List<Identifier> rewardedRecipes(net.minecraft.advancements.AdvancementHolder holder) {
        return holder.value().rewards().recipes();
    }
    *///?} else {
    /*private static net.minecraft.advancements.Advancement advancement(GameTestHelper helper, Identifier id) {
        return helper.getLevel().getServer().getAdvancements().getAdvancement(id);
    }

    private static boolean isRoot(net.minecraft.advancements.Advancement advancement) {
        return advancement.getParent() == null;
    }

    private static List<Identifier> rewardedRecipes(net.minecraft.advancements.Advancement advancement) {
        return List.of(advancement.getRewards().getRecipes());
    }
    *///?}
}
