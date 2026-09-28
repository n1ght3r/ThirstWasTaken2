package com.thirstwastaken2.farmersdelight;

import com.thirstwastaken2.farmersdelight.platform.PotRecipes;
import net.fabricmc.fabric.api.recipe.v1.ingredient.FabricIngredient;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.refabricated.inventory.RecipeWrapper;

import java.util.List;

/**
 * The second half of Refabricated's Cooking Pot match. Its own {@code matches} counts items by id
 * through {@code StackedContents}, which never asks a Fabric custom ingredient about the stack's tag or
 * components. So a recipe whose ingredient is {@code fabric:nbt} or {@code fabric:components}, such as
 * this mod's purification recipes, matched any bottle: salt water, or a Potion of Healing, boiled into
 * Pure water. The original mod on NeoForge and Forge tests every ingredient and never did.
 *
 * <p>Only a recipe with an ingredient that needs testing is looked at again, so every recipe of plain
 * items and tags matches exactly as Refabricated decides. A recipe does not know its own id from 1.21.2,
 * so this asks the ingredients rather than the mod the recipe came from.
 */
public final class CookingPotMatching {
    /** The pot's ingredient slots, the first six of its inventory, as Refabricated counts them. */
    private static final int SLOTS = 6;

    private CookingPotMatching() { }

    /**
     * Whether each filled ingredient slot passes a different one of the recipe's ingredients, once
     * Refabricated has already found the right items in the right numbers.
     */
    public static boolean honoursIngredients(CookingPotRecipe recipe, RecipeWrapper input) {
        List<Ingredient> ingredients = PotRecipes.ingredients(recipe);
        return !anyTested(ingredients) || assign(input, ingredients, 0, 0);
    }

    private static boolean anyTested(List<Ingredient> ingredients) {
        for (Ingredient ingredient : ingredients) {
            if (((FabricIngredient) (Object) ingredient).requiresTesting()) return true;
        }
        return false;
    }

    /** Six slots at most, so trying every pairing allocates nothing and costs next to nothing. */
    private static boolean assign(RecipeWrapper input, List<Ingredient> ingredients, int slot, int used) {
        if (slot == SLOTS) return true;
        ItemStack stack = input.getItem(slot);
        if (stack.isEmpty()) return assign(input, ingredients, slot + 1, used);
        for (int i = 0; i < ingredients.size(); i++) {
            if ((used & 1 << i) == 0 && ingredients.get(i).test(stack)
                    && assign(input, ingredients, slot + 1, used | 1 << i)) {
                return true;
            }
        }
        return false;
    }
}
