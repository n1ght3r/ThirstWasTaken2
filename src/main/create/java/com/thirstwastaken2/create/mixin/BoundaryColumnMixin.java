package com.thirstwastaken2.create.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.thirstwastaken2.create.IntakeSamples;
import com.thirstwastaken2.neoforge.WaterFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Create Pipes n Physics plans each tick from what an open pipe end could drink, a plain
 * {@code FluidStack} it builds itself, then drains through Create's {@code removeFluidFromSpace}, which
 * {@link OpenEndedPipeMixin} stamps, and moves nothing when the two differ. So no water reached a pump
 * from the world. Stamping the probe with what the drain will carry makes them agree.
 *
 * <p>Only the first probe, at the pipe's own mouth, and only when the mouth is in the world it stands in:
 * on a Sable sub-level the addon drains itself, unstamped, and the probe has to stay plain to match it.
 * The addon is not on the compile classpath, hence {@code @Pseudo}, and {@code PipesPresence} has checked
 * both methods are there before this applies; {@code require = 0} lets a moved call site fail quietly.
 */
@Pseudo
@Mixin(targets = "de.devin.pipesnphysics.engine.boundary.BoundaryColumn", remap = false)
abstract class BoundaryColumnMixin {
    @Shadow
    private static BlockPos worldOutputPos(Level level, BlockPos pos) {
        throw new AssertionError();
    }

    @ModifyExpressionValue(method = "intakeFluid", require = 0, at = @At(value = "INVOKE", ordinal = 0,
            target = "Lde/devin/pipesnphysics/engine/boundary/BoundaryColumn;drinkableSource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ZZ)Lnet/neoforged/neoforge/fluids/FluidStack;"))
    private static FluidStack thirst$stampProbe(FluidStack probe, @Local(argsOnly = true) Level level,
                                                @Local(argsOnly = true) BlockPos pos) {
        if (!WaterFluids.isWater(probe) || !worldOutputPos(level, pos).equals(pos)) return probe;
        return WaterFluids.stampIfKnown(probe, IntakeSamples.at(level, pos));
    }
}
