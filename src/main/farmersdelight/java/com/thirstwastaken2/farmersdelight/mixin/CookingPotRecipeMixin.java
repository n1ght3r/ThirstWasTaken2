package com.thirstwastaken2.farmersdelight.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.thirstwastaken2.farmersdelight.CookingPotMatching;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import vectorwing.farmersdelight.common.crafting.CookingPotRecipe;
import vectorwing.farmersdelight.refabricated.inventory.RecipeWrapper;

/** Refabricated's Cooking Pot match, made to ask the recipe's own ingredients; see {@link CookingPotMatching}. */
@Mixin(value = CookingPotRecipe.class, remap = false)
abstract class CookingPotRecipeMixin {
    // From 26.1 the class also has a bridge taking any RecipeInput, so the target names its descriptor,
    // which needs no remapping there. Before it there is one matches, and its descriptor is in
    // intermediary names, so the name alone is given.
    //? if >=26.1 {
    @ModifyReturnValue(method = "matches(Lvectorwing/farmersdelight/refabricated/inventory/RecipeWrapper;Lnet/minecraft/world/level/Level;)Z", at = @At("RETURN"))
    //?} else
    //@ModifyReturnValue(method = "matches", at = @At("RETURN"))
    private boolean thirst$testIngredients(boolean matched, @Local(argsOnly = true) RecipeWrapper input) {
        return matched && CookingPotMatching.honoursIngredients((CookingPotRecipe) (Object) this, input);
    }
}
