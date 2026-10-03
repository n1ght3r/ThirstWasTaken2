package com.thirstwastaken2.hearthandharvest.mixin;

import alabaster.hearthandharvest.common.block.entity.CaskBlockEntity;
import alabaster.hearthandharvest.common.crafting.CaskRecipe;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.neoforged.neoforge.items.wrapper.RecipeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.Optional;

/**
 * Mead, moonshine and root beer age in a Cask from a water bottle matched on its potion alone, so a
 * bottle of sea water aged into a drink. As in Brewin' and Chewin's keg and Cultural Delights' vat, sea
 * water ages nothing now: the cask finds no recipe, and the bottle waits in its slot. Fresh water of any
 * grade still ages, since what comes out is its own drink.
 */
@Mixin(CaskBlockEntity.class)
abstract class CaskBlockEntityMixin {
    @Inject(method = "getMatchingRecipe", at = @At("HEAD"), cancellable = true)
    private void thirst$noSeaWater(RecipeWrapper inventory, CallbackInfoReturnable<Optional<RecipeHolder<CaskRecipe>>> cir) {
        for (int slot = 0; slot < CaskBlockEntity.MEAL_DISPLAY_SLOT; slot++) {
            if (WaterPurity.isSalty(inventory.getItem(slot))) {
                cir.setReturnValue(Optional.empty());
                return;
            }
        }
    }
}
