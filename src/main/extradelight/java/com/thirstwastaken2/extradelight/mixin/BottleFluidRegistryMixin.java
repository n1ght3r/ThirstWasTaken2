package com.thirstwastaken2.extradelight.mixin;

import com.lance5057.extradelight.util.BottleFluidRegistry;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.thirstwastaken2.extradelight.ExtraDelightWater;
import com.thirstwastaken2.neoforge.WaterFluids;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Every bottle Extra Delight pours into a tank or draws from one goes through its registry, which knew a
 * water bottle by its potion alone and gave back a plain one: a Dirty or salty bottle went into a Jar,
 * a Keg or the Vat as plain water and came back Clean. The grade now crosses both ways, and the tanks,
 * which compare components, keep it and refuse a second grade.
 */
@Mixin(BottleFluidRegistry.class)
abstract class BottleFluidRegistryMixin {
    /**
     * A copy: the registry hands out the first stack of an ingredient, which the ingredient caches, so
     * stamping it in place would stamp every later bottle too.
     */
    @ModifyReturnValue(method = "getFluidFromBottle", at = @At("RETURN"))
    private static FluidStack thirst$bottleIntoTank(FluidStack fluid, ItemStack bottle) {
        if (!WaterPurity.isWaterContainer(bottle)) return fluid;
        return ExtraDelightWater.stamped(fluid, WaterPurity.quality(bottle));
    }

    /** Water with no grade fills the bottle as {@code defaultQuality}, as from any tank. */
    @ModifyReturnValue(method = "getBottleFromFluid", at = @At("RETURN"))
    private static ItemStack thirst$tankIntoBottle(ItemStack bottle, FluidStack fluid) {
        if (!WaterFluids.isWater(fluid)) return bottle;
        return WaterFluids.stampContainer(bottle, WaterFluids.quality(fluid));
    }
}
