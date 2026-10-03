package com.thirstwastaken2.hearthandharvest.mixin;

import alabaster.hearthandharvest.common.block.entity.TroughBlockEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.hearthandharvest.HearthWater;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Rain filling a Trough is rainwater, the grade a cauldron left in the rain gets, rather than water with
 * no grade at all. With rain collection switched off it is Hearth and Harvest's plain water again. A
 * trough already holding water of another grade takes no rain, as a tank takes no second grade.
 */
@Mixin(TroughBlockEntity.class)
abstract class TroughBlockEntityMixin {
    @WrapOperation(method = "serverTick", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/fluids/capability/templates/FluidTank;fill(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I"))
    private static int thirst$rainwater(FluidTank tank, FluidStack water, IFluidHandler.FluidAction action,
                                        Operation<Integer> original) {
        if (!ThirstConfig.get().enableRainCollection) return original.call(tank, water, action);
        return original.call(tank, HearthWater.stamped(water, WaterQuality.fresh(WaterPurity.rainwaterPurity())), action);
    }
}
