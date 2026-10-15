package com.thirstwastaken2.immersiveengineering.mixin;

import blusunrize.immersiveengineering.common.blocks.metal.FluidPumpBlockEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.thirstwastaken2.neoforge.SampledWater;
import com.thirstwastaken2.neoforge.WaterFluids;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The Fluid Pump makes a plain bucket of water for every source it takes, which reads as the default
 * grade, so a pump in the sea filled its tank with Clean water. Each bucket is stamped with the grade of
 * the block it came from, on both of the pump's paths: a pool it drains block by block, and an infinite
 * pool it draws from without taking the block.
 *
 * <p>The pump's tank compares components, so a pump holding one grade waits on a block of another until
 * its tank has emptied into its outputs, as it does for any other fluid, rather than mixing them.
 */
@Mixin(value = FluidPumpBlockEntity.class, remap = false)
abstract class FluidPumpMixin {
    @Unique
    private final SampledWater thirst$source = new SampledWater();

    /** Sampled before the call, which picks the block up when it executes. */
    @WrapOperation(method = "tickServer",
            at = @At(value = "INVOKE", target = "Lblusunrize/immersiveengineering/common/util/Utils;drainFluidBlock(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/neoforged/neoforge/fluids/FluidStack;"))
    private FluidStack thirst$stampDrained(Level level, BlockPos pos, FluidAction action, Operation<FluidStack> original) {
        WaterQuality quality = thirst$source.at(level, pos);
        return WaterFluids.stampIfKnown(original.call(level, pos, action), quality);
    }

    @ModifyExpressionValue(method = "gatherInfiniteFluidFromWorld",
            at = @At(value = "NEW", target = "net/neoforged/neoforge/fluids/FluidStack"))
    private FluidStack thirst$stampGathered(FluidStack gathered, @Local(argsOnly = true) Direction from) {
        if (!WaterFluids.isWater(gathered)) return gathered;
        BlockEntity pump = (BlockEntity) (Object) this;
        Level level = pump.getLevel();
        if (level == null) return gathered;
        return WaterFluids.stampIfKnown(gathered, thirst$source.at(level, pump.getBlockPos().relative(from)));
    }
}
