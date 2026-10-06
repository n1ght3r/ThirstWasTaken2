package com.thirstwastaken2.extradelight.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.thirstwastaken2.extradelight.ExtraDelightWater;
import com.thirstwastaken2.neoforge.WaterQualityFluidHandler;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.Optional;

/**
 * NeoForge's bucket wrapper takes and gives plain water, so a Dirty bucket poured into a Jar or a Keg
 * went in as plain water, and a bucket filled from one came out with no grade. Inside one of Extra
 * Delight's own transfers, and only there, a container's handler carries the grade across: water leaving
 * it is stamped with the container's grade, and the container it turns into with the water entering it.
 * Every {@code FluidUtil} helper finds the container's handler through this one method.
 */
@Mixin(FluidUtil.class)
abstract class FluidUtilMixin {
    @ModifyReturnValue(method = "getFluidHandler(Lnet/minecraft/world/item/ItemStack;)Ljava/util/Optional;", at = @At("RETURN"))
    private static Optional<IFluidHandlerItem> thirst$graded(Optional<IFluidHandlerItem> handler, ItemStack container) {
        if (!ExtraDelightWater.inTransfer()) return handler;
        return handler.map(WaterQualityFluidHandler::of);
    }
}
