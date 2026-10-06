package com.thirstwastaken2.extradelight.mixin;

import com.lance5057.extradelight.capabilities.WellFluidCapability;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.thirstwastaken2.extradelight.ExtraDelightWater;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The endless water behind the Tap and the Sink Cabinet, its only two users, is Murky, as every other
 * mod's endless sink is. They stay endless. {@code drain(FluidStack, FluidAction)} goes through
 * {@code drain(int, FluidAction)}, so the two below cover every way out, pipes included.
 */
@Mixin(WellFluidCapability.class)
abstract class WellFluidCapabilityMixin {
    @ModifyReturnValue(method = "getFluid", at = @At("RETURN"))
    private FluidStack thirst$standing(FluidStack fluid) {
        return ExtraDelightWater.stamped(fluid, ExtraDelightWater.TAP);
    }

    @ModifyReturnValue(method = "drain(ILnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/neoforged/neoforge/fluids/FluidStack;",
            at = @At("RETURN"))
    private FluidStack thirst$drawnStanding(FluidStack fluid) {
        return ExtraDelightWater.stamped(fluid, ExtraDelightWater.TAP);
    }
}
