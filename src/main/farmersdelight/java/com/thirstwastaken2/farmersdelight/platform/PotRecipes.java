package com.thirstwastaken2.farmersdelight.platform;

import net.minecraft.world.item.crafting.Ingredient;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;

import java.util.List;

/** The Cooking Pot recipe calls whose shape differs between Refabricated's builds. */
public final class PotRecipes {
    private PotRecipes() { }

    /** A recipe's ingredients. 1.21.2 took {@code getIngredients} off recipes, and Refabricated added {@code input}. */
    public static List<Ingredient> ingredients(CookingPotRecipe recipe) {
        //? if >=1.21.11 {
        return recipe.input();
        //?} else
        //return recipe.getIngredients();
    }
}
