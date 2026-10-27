package com.thirstwastaken2.immersiveengineering.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.thirstwastaken2.immersiveengineering.ImmersiveEngineeringWater;
import com.thirstwastaken2.neoforge.WaterQualityFluidHandler;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/**
 * NeoForge's bucket wrapper takes and gives plain water, so a bucket filled from a barrel or a Fluid Tank
 * of Murky water came out with no grade, and so Clean, and a Murky bucket poured in went in plain. Inside
 * one of Immersive Engineering's own transfers ({@link FluidUtilsMixin}), and only there, a container's
 * handler carries the grade across. Every {@code FluidUtil} helper finds the container's handler through
 * this one method.
 */
@Mixin(FluidUtil.class)
abstract class NeoForgeFluidUtilMixin {
    @ModifyReturnValue(method = "getFluidHandler(Lnet/minecraft/world/item/ItemStack;)Ljava/util/Optional;", at = @At("RETURN"))
    private static Optional<IFluidHandlerItem> thirst$graded(Optional<IFluidHandlerItem> handler, ItemStack container) {
        if (!ImmersiveEngineeringWater.inTransfer()) return handler;
        return handler.map(WaterQualityFluidHandler::of);
    }
}
