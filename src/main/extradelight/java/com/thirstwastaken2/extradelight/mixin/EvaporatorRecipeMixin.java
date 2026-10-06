package com.thirstwastaken2.extradelight.mixin;

import com.lance5057.extradelight.workstations.evaporator.recipes.EvaporatorRecipe;
import com.lance5057.extradelight.workstations.evaporator.recipes.EvaporatorRecipeWrapper;
import com.thirstwastaken2.extradelight.ExtraDelightWater;
import com.thirstwastaken2.neoforge.WaterFluids;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Evaporator dried any water into salt, which made salt out of a river. Every other salt this mod
 * allows comes from the sea (Hearth and Harvest's pot, Spelunkery's furnace, the Copper Distiller), so
 * now only sea water evaporates; fresh water stands in it untouched. Its recipes for other fluids are
 * left alone.
 */
@Mixin(EvaporatorRecipe.class)
abstract class EvaporatorRecipeMixin {
    @Inject(method = "matches(Lcom/lance5057/extradelight/workstations/evaporator/recipes/EvaporatorRecipeWrapper;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true)
    private void thirst$onlySeaWater(EvaporatorRecipeWrapper input, Level level, CallbackInfoReturnable<Boolean> cir) {
        FluidStack held = input.getTank().getFluid();
        if (WaterFluids.isWater(held) && !ExtraDelightWater.isSeaWater(held)) cir.setReturnValue(false);
    }
}
