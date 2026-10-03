package com.thirstwastaken2.hearthandharvestforge.mixin;

import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hearth and Harvest's {@code minecraft:salt_from_bottle} boils any potion into two salt in the Cooking
 * Pot. A fresh water bottle matched both it and the pot's purifying recipe, and recipe order decided which
 * one cooked; in game it was salt for a Pure bottle. Now only sea water boils down to salt: a water
 * container in the pot that is not salty fails this recipe. On 1.21.1 the same is a data file that
 * replaces the recipe; on Forge 47 the mod's own file wins over a replacement, ordering or not.
 *
 * <p>{@code matches(RecipeWrapper, Level)} is Farmer's Delight's own method, which the vanilla bridge
 * calls, so it keeps its name in a production jar. Named by string, since nothing compiles against it.
 */
@Mixin(targets = "vectorwing.farmersdelight.common.crafting.CookingPotRecipe")
abstract class SaltFromSeaWaterMixin {
    private static final Identifier THIRST$SALT_FROM_BOTTLE = Identifier.withDefaultNamespace("salt_from_bottle");

    @Inject(method = "matches(Lnet/minecraftforge/items/wrapper/RecipeWrapper;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true)
    private void thirst$onlySeaWater(RecipeWrapper input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (!THIRST$SALT_FROM_BOTTLE.equals(((Recipe<?>) (Object) this).getId())) return;
        // The first six are the ingredients; the rest are the container, the meal and the output.
        for (int slot = 0; slot < Math.min(6, input.getContainerSize()); slot++) {
            ItemStack stack = input.getItem(slot);
            if (WaterPurity.isWaterContainer(stack) && !WaterPurity.isSalty(stack)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }
}
