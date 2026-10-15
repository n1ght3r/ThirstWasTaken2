package com.thirstwastaken2.immersiveengineering.mixin;

import blusunrize.immersiveengineering.common.fluids.IEItemFluidHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thirstwastaken2.neoforge.WaterFluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The Jerrycan, and every other Immersive Engineering item that holds fluid, tops itself up with any
 * water whatever its grade and keeps the components of what it already held, so Murky water poured into
 * a can of Pure came out Pure. Two waters now top up only when their grades match, as the item already
 * asks when it is empty. Other fluids compare as before.
 *
 * <p>NeoForge only: on Forge 1.20.1 the same check is {@code isFluidEqual}, which reads the tag.
 */
@Mixin(value = IEItemFluidHandler.class, remap = false)
abstract class ItemFluidHandlerMixin {
    @WrapOperation(method = "fill",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/fluids/FluidStack;isSameFluid(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/FluidStack;)Z"))
    private boolean thirst$sameGrade(FluidStack contained, FluidStack resource, Operation<Boolean> original) {
        if (!original.call(contained, resource)) return false;
        if (!WaterFluids.isWater(contained)) return true;
        return WaterFluids.quality(contained).equals(WaterFluids.quality(resource));
    }
}
