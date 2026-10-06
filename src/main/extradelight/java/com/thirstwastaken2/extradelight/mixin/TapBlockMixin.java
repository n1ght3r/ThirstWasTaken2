package com.thirstwastaken2.extradelight.mixin;

import com.lance5057.extradelight.blocks.TapBlock;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thirstwastaken2.extradelight.ExtraDelightWater;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * The Tap's water is Murky, from its endless tank ({@link WellFluidCapabilityMixin}), and a bucket
 * filled at it carries that grade ({@link FluidUtilMixin}). A glass bottle is built from a plain water
 * stack of its own, not drawn from the tank, so that stack is Murky too.
 */
@Mixin(TapBlock.class)
abstract class TapBlockMixin {
    @ModifyArg(method = "useItemOn", at = @At(value = "INVOKE",
            target = "Lcom/lance5057/extradelight/util/BottleFluidRegistry;getBottleFromFluid(Lnet/neoforged/neoforge/fluids/FluidStack;)Lnet/minecraft/world/item/ItemStack;"))
    private FluidStack thirst$standingBottle(FluidStack water) {
        return ExtraDelightWater.stamped(water, ExtraDelightWater.TAP);
    }

    @WrapMethod(method = "useItemOn")
    private ItemInteractionResult thirst$graded(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                Player player, InteractionHand hand, BlockHitResult hit,
                                                Operation<ItemInteractionResult> original) {
        return ExtraDelightWater.graded(() -> original.call(stack, state, level, pos, player, hand, hit));
    }
}
