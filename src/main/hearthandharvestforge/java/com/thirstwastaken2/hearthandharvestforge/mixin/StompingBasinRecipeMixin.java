package com.thirstwastaken2.hearthandharvestforge.mixin;

import alabaster.hearthandharvest.common.crafting.StompingBasinRecipe;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.thirstwastaken2.hearthandharvestforge.HearthWater;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The one stomping recipe that gives water, a wet sponge squeezed into a bucket's worth, gives
 * {@link HearthWater#STANDING}, Murky: a sponge is not a filter. Every other result, juice, is left
 * alone. As on 1.21.1.
 */
@Mixin(value = StompingBasinRecipe.class, remap = false)
abstract class StompingBasinRecipeMixin {
    @ModifyReturnValue(method = "getResultFluid", at = @At("RETURN"))
    private FluidStack thirst$wrungOut(FluidStack result) {
        return HearthWater.stamped(result, HearthWater.STANDING);
    }
}
