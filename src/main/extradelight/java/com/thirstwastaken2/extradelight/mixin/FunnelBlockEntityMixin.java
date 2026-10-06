package com.thirstwastaken2.extradelight.mixin;

import com.lance5057.extradelight.blocks.funnel.FunnelBlockEntity;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.thirstwastaken2.extradelight.ExtraDelightWater;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A Funnel takes up a water source above it, removing the block: water collected from the world, so it
 * is graded where it lay, like a bucket filled there. Water it drains from a tank above keeps the grade
 * that tank gave it.
 */
@Mixin(FunnelBlockEntity.class)
abstract class FunnelBlockEntityMixin {
    @WrapOperation(method = "pullFluidIn", at = @At(value = "INVOKE", ordinal = 0,
            target = "Lnet/neoforged/neoforge/fluids/capability/templates/FluidTank;fill(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I"))
    private static int thirst$sampled(FluidTank tank, FluidStack fluid, IFluidHandler.FluidAction action,
                                      Operation<Integer> original,
                                      @Local(argsOnly = true) Level level, @Local(argsOnly = true) BlockPos pos) {
        if (level.isClientSide) return original.call(tank, fluid, action);
        return original.call(tank, ExtraDelightWater.stamped(fluid, WaterPurity.sampleAt(level, pos.above())), action);
    }
}
