package com.thirstwastaken2.extradelight.mixin;

import com.lance5057.extradelight.workstations.meltingpot.MeltingPotBlockEntity;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.thirstwastaken2.extradelight.ExtraDelightWater;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Ice melted in the Melting Pot is meltwater, as in the Chiller's drip tray; see {@link ChillerBlockEntityMixin}. */
@Mixin(MeltingPotBlockEntity.class)
abstract class MeltingPotBlockEntityMixin {
    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/fluids/FluidStack;copy()Lnet/neoforged/neoforge/fluids/FluidStack;"))
    private static FluidStack thirst$meltwater(FluidStack melted) {
        return ExtraDelightWater.stamped(melted, ExtraDelightWater.meltwater());
    }
}
