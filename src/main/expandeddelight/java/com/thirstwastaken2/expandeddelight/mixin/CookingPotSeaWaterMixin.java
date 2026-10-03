package com.thirstwastaken2.expandeddelight.mixin;

import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A Cooking Pot recipe takes no sea water. Expanded Delight cooks Asparagus Soup and Cinnamon Apples
 * from a water bucket, and its ingredient matches the item alone, so a bucket of sea water would cook
 * into a soup that restores thirst, as it would through the keg or the vat.
 *
 * <p>Refused for every Cooking Pot recipe, not only Expanded Delight's: none should take sea water, and
 * the pot's own purification recipes already refuse it by their ingredients. The one exception is a
 * recipe that asks for sea water itself, an ingredient that takes the salty stack and would not take
 * the same stack fresh: the salt boiled from a sea water bottle that replaces Hearth and Harvest's (see
 * {@code src/main/hearthandharvest}). Fresh water of any grade is unchanged. The pot asks this
 * {@code matches} with its six input slots, and the {@code RecipeInput} overload only forwards here.
 * Named by string, since nothing compiles against Farmer's Delight.
 */
@Mixin(targets = "vectorwing.farmersdelight.common.crafting.CookingPotRecipe")
abstract class CookingPotSeaWaterMixin {
    @Inject(method = "matches(Lnet/neoforged/neoforge/items/wrapper/RecipeWrapper;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true)
    private void thirst$noSeaWaterInThePot(RecipeWrapper input, Level level, CallbackInfoReturnable<Boolean> cir) {
        // The first six are the ingredients; the rest are the container, the meal and the output.
        for (int slot = 0; slot < Math.min(6, input.size()); slot++) {
            ItemStack stack = input.getItem(slot);
            if (WaterPurity.isSalty(stack) && !thirst$asksForSeaWater(stack)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }

    private boolean thirst$asksForSeaWater(ItemStack salty) {
        ItemStack fresh = WaterPurity.setQuality(salty.copy(), WaterQuality.fresh(WaterPurity.MAX));
        for (Ingredient ingredient : ((Recipe<?>) (Object) this).getIngredients()) {
            if (ingredient.test(salty) && !ingredient.test(fresh)) return true;
        }
        return false;
    }
}
