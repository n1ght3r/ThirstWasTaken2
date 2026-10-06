package com.thirstwastaken2.extradelight.mixin;

import com.lance5057.extradelight.workstations.vat.recipes.VatRecipe;
import com.lance5057.extradelight.workstations.vat.recipes.VatRecipeWrapper;
import com.thirstwastaken2.extradelight.ExtraDelightWater;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Vat matched its water by fluid alone, so sea water pickled and fermented as well as fresh. As in
 * Brewin' and Chewin's keg and Cultural Delights' vat, sea water makes nothing now; the Vat keeps it and
 * hands it back salty. Fresh water of any grade still works, since what comes out is its own food.
 */
@Mixin(VatRecipe.class)
abstract class VatRecipeMixin {
    @Inject(method = "matches(Lcom/lance5057/extradelight/workstations/vat/recipes/VatRecipeWrapper;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true)
    private void thirst$noSeaWater(VatRecipeWrapper input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (ExtraDelightWater.holdsSeaWater(input.getTank().getAsList())) cir.setReturnValue(false);
    }
}
