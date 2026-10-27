package com.thirstwastaken2.immersiveengineeringforge.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.thirstwastaken2.forge.WaterQualityFluidHandler;
import com.thirstwastaken2.immersiveengineeringforge.ImmersiveEngineeringForgeWater;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidUtil;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Forge's bucket wrapper takes and gives plain water, so a bucket filled from a barrel or a Fluid Tank of
 * Murky water came out with no grade, and so Clean, and a Murky bucket poured in went in plain. Inside
 * one of Immersive Engineering's own transfers ({@link FluidUtilsMixin}), and only there, a container's
 * handler carries the grade across. Every {@code FluidUtil} helper finds the container's handler through
 * this one method.
 */
@Mixin(value = FluidUtil.class, remap = false)
abstract class ForgeFluidUtilMixin {
    @ModifyReturnValue(method = "getFluidHandler(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraftforge/common/util/LazyOptional;", at = @At("RETURN"))
    private static LazyOptional<IFluidHandlerItem> thirst$graded(LazyOptional<IFluidHandlerItem> handler, ItemStack container) {
        if (!ImmersiveEngineeringForgeWater.inTransfer()) return handler;
        return handler.lazyMap(WaterQualityFluidHandler::of);
    }
}
