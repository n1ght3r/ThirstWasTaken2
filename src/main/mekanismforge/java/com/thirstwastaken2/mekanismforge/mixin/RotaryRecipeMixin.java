package com.thirstwastaken2.mekanismforge.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.thirstwastaken2.forge.WaterFluids;
import com.thirstwastaken2.purity.WaterQuality;
import mekanism.api.recipes.RotaryRecipe;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Water condensed from vapour in the Rotary Condensentrator is distilled: Pure, whatever went in, sea
 * water included. Without this it came out plain and read as the default grade. Every rotary recipe that
 * condenses into water comes through here, another mod's included; the recipe hands out a copy, so
 * stamping it touches nothing the recipe keeps.
 */
@Mixin(value = RotaryRecipe.class, remap = false)
abstract class RotaryRecipeMixin {
    @ModifyReturnValue(method = "getFluidOutput", at = @At("RETURN"))
    private FluidStack thirst$distilled(FluidStack condensed) {
        return WaterFluids.isWater(condensed) ? WaterFluids.stamp(condensed, WaterQuality.fresh(3)) : condensed;
    }
}
