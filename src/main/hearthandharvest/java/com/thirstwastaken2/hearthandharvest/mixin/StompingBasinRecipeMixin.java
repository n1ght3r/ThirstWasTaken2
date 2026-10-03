package com.thirstwastaken2.hearthandharvest.mixin;

import alabaster.hearthandharvest.common.crafting.StompingBasinRecipe;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.thirstwastaken2.hearthandharvest.HearthWater;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The one stomping recipe that gives water, a wet sponge squeezed into a bucket's worth. A sponge keeps
 * no grade, so one soaked in the sea gave fresh Clean water. What it gives is
 * {@link HearthWater#STANDING}, Murky: a sponge is not a filter. Every other result, juice, is left alone.
 */
@Mixin(StompingBasinRecipe.class)
abstract class StompingBasinRecipeMixin {
    @ModifyReturnValue(method = "getResultFluid", at = @At("RETURN"))
    private FluidStack thirst$wrungOut(FluidStack result) {
        return HearthWater.stamped(result, HearthWater.STANDING);
    }
}
