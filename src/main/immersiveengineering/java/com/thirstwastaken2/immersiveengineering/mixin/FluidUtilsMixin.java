package com.thirstwastaken2.immersiveengineering.mixin;

import blusunrize.immersiveengineering.api.fluid.FluidUtils;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thirstwastaken2.immersiveengineering.ImmersiveEngineeringWater;
import com.thirstwastaken2.neoforge.WaterQualityFluidHandler;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A bucket or other container keeps the grade of the water it draws from or pours into Immersive
 * Engineering's tanks. A click on a barrel, the Fluid Tank or the Refinery goes through
 * {@code interactWithFluidHandler}, and a machine's container slots through {@code fillFluidContainer}
 * and {@code tryEmptyContainer}. The first two reach the container's handler through NeoForge's
 * {@code FluidUtil} ({@link NeoForgeFluidUtilMixin}); the last asks the stack for it directly.
 */
@Mixin(value = FluidUtils.class, remap = false)
abstract class FluidUtilsMixin {
    @WrapMethod(method = "interactWithFluidHandler")
    private static boolean thirst$graded(Player player, InteractionHand hand, IFluidHandler handler,
            Operation<Boolean> original) {
        return ImmersiveEngineeringWater.graded(() -> original.call(player, hand, handler));
    }

    @WrapMethod(method = "fillFluidContainer(Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;)Lnet/minecraft/world/item/ItemStack;")
    private static ItemStack thirst$gradedFill(IFluidHandler handler, ItemStack in, ItemStack out, Player player,
            Operation<ItemStack> original) {
        return ImmersiveEngineeringWater.graded(() -> original.call(handler, in, out, player));
    }

    @WrapOperation(method = "tryEmptyContainer",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;getCapability(Lnet/neoforged/neoforge/capabilities/ItemCapability;)Ljava/lang/Object;"))
    private static Object thirst$gradedEmpty(ItemStack container, ItemCapability<?, ?> capability, Operation<Object> original) {
        Object handler = original.call(container, capability);
        return handler instanceof IFluidHandlerItem item ? WaterQualityFluidHandler.of(item) : handler;
    }
}
