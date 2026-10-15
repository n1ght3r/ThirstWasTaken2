package com.thirstwastaken2.hearthandharvest.mixin;

import alabaster.hearthandharvest.common.block.entity.SprinklerBlockEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.hearthandharvest.HearthWater;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * From Hearth and Harvest 1.4.0 the Sprinkler fills itself, from rain and from a water source beside or
 * below it, as plain water, which a bottle then drew back out Clean. Rain is rainwater, as in the Trough;
 * a source is sampled where it lies, as the Jug samples one. A sprinkler holding another grade takes
 * neither, as a tank takes no second grade.
 */
@Mixin(SprinklerBlockEntity.class)
abstract class SprinklerBlockEntityMixin {
    @WrapOperation(method = "refill", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/neoforged/neoforge/fluids/capability/templates/FluidTank;fill(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I"))
    private int thirst$rainwater(FluidTank tank, FluidStack water, IFluidHandler.FluidAction action,
                                 Operation<Integer> original) {
        if (!ThirstConfig.get().enableRainCollection) return original.call(tank, water, action);
        return original.call(tank, HearthWater.stamped(water, WaterQuality.fresh(WaterPurity.rainwaterQuality())), action);
    }

    @WrapOperation(method = "refill", at = @At(value = "INVOKE", ordinal = 1,
            target = "Lnet/neoforged/neoforge/fluids/capability/templates/FluidTank;fill(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I"))
    private int thirst$sampled(FluidTank tank, FluidStack water, IFluidHandler.FluidAction action,
                               Operation<Integer> original, @Local(argsOnly = true) Level level,
                               @Local(argsOnly = true) BlockPos pos, @Local Direction direction) {
        return original.call(tank, HearthWater.stamped(water, WaterPurity.sampleAt(level, pos.relative(direction))), action);
    }
}
