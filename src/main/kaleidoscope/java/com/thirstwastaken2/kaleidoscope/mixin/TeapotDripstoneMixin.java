package com.thirstwastaken2.kaleidoscope.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.TeapotBlockEntity;
import com.thirstwastaken2.kaleidoscope.BrewedWater;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Water dripping into a teapot from pointed dripstone is graded as it is for a cauldron. A class of its
 * own because {@code receiveDripstoneFluid} is only in the 1.21.1 builds, so the plugin skips this
 * class where the teapot has no such method, rather than making the teapot's other hooks optional.
 */
@Mixin(value = TeapotBlockEntity.class, remap = false)
abstract class TeapotDripstoneMixin {
    @Inject(method = "receiveDripstoneFluid", at = @At("RETURN"))
    private void thirst$gradeDripstone(Fluid fluid, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        ((BrewedWater) this).thirst$holdWater(fluid == Fluids.WATER ? WaterQuality.fresh(WaterPurity.dripstoneQuality()) : null);
    }
}
