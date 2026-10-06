package com.thirstwastaken2.extradelight.mixin;

import com.lance5057.extradelight.workstations.mixingbowl.recipes.MixingBowlRecipe;
import com.lance5057.extradelight.workstations.mixingbowl.recipes.MixingBowlRecipeWrapper;
import com.thirstwastaken2.extradelight.ExtraDelightWater;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Lemonade, limeade, orangeade, dough and sweets were stirred from sea water as readily as from fresh.
 * Now the Mixing Bowl mixes nothing while it holds sea water; see {@link VatRecipeMixin}.
 */
@Mixin(MixingBowlRecipe.class)
abstract class MixingBowlRecipeMixin {
    @Inject(method = "matches(Lcom/lance5057/extradelight/workstations/mixingbowl/recipes/MixingBowlRecipeWrapper;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true)
    private void thirst$noSeaWater(MixingBowlRecipeWrapper input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (ExtraDelightWater.holdsSeaWater(input.getTank().getAsList())) cir.setReturnValue(false);
    }
}
