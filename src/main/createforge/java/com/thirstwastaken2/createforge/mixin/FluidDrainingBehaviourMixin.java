package com.thirstwastaken2.createforge.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.simibubi.create.content.fluids.transfer.FluidDrainingBehaviour;
import com.thirstwastaken2.forge.SampledWater;
import com.thirstwastaken2.forge.WaterFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/** A Hose Pulley drawing from a body of water, graded where the hose ends as the original mod did. */
@Mixin(value = FluidDrainingBehaviour.class, remap = false)
abstract class FluidDrainingBehaviourMixin {
    @Unique
    private final SampledWater thirst$source = new SampledWater();

    @ModifyReturnValue(method = "getDrainableFluid", at = @At("RETURN"))
    private FluidStack thirst$stampDrawn(FluidStack drawn, BlockPos rootPos) {
        Level level = ((FluidDrainingBehaviour) (Object) this).getWorld();
        return WaterFluids.isWater(drawn) && level != null ? WaterFluids.stampIfKnown(drawn, thirst$source.at(level, rootPos)) : drawn;
    }
}
