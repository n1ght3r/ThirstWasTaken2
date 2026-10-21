package com.thirstwastaken2.hearthandharvestforge.mixin;

import alabaster.hearthandharvest.common.block.BasinBlock;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thirstwastaken2.hearthandharvestforge.HearthWater;
import com.thirstwastaken2.hearthandharvestforge.StampingTank;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Sink ({@code basin}), which refills itself by a bottle every three seconds from nothing and handed
 * that out as Clean. Its water is now always {@link HearthWater#STANDING}, Murky, whether it filled
 * itself or was poured in, and sea water is refused. As on 1.21.1; there {@code useItemOn} is vanilla's,
 * here {@code hhUseItemOn} is the mod's own, which {@code use} calls first.
 */
@Mixin(BasinBlock.class)
abstract class SinkBlockMixin {
    @Inject(method = "hhUseItemOn", at = @At("HEAD"), cancellable = true, remap = false)
    private void thirst$noSeaWater(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                   InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (WaterPurity.isSalty(stack)) cir.setReturnValue(InteractionResult.CONSUME);
    }

    @WrapOperation(method = "hhUseItemOn", remap = false, at = @At(value = "INVOKE",
            target = "Lalabaster/hearthandharvest/common/fluid/HHFluidHandling;useOnTank(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraftforge/fluids/capability/IFluidHandler;Ljava/lang/Runnable;)Lnet/minecraft/world/InteractionResult;"))
    private InteractionResult thirst$standingWater(Level level, BlockPos pos, Player player, InteractionHand hand,
                                                   IFluidHandler tank, Runnable onChanged,
                                                   Operation<InteractionResult> original) {
        if (tank instanceof FluidTank held) HearthWater.settle(held, HearthWater.STANDING);
        return original.call(level, pos, player, hand, StampingTank.sink(tank), onChanged);
    }

    /** Settled first, so that water saved unstamped before this does not refuse the fill for ever. */
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/minecraftforge/fluids/capability/templates/FluidTank;fill(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)I"))
    private int thirst$fillsStanding(FluidTank tank, FluidStack water, IFluidHandler.FluidAction action,
                                     Operation<Integer> original) {
        HearthWater.settle(tank, HearthWater.STANDING);
        return original.call(tank, HearthWater.stamped(water, HearthWater.STANDING), action);
    }
}
