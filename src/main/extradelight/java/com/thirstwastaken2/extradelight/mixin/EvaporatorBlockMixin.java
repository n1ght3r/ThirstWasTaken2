package com.thirstwastaken2.extradelight.mixin;

import com.lance5057.extradelight.workstations.evaporator.EvaporatorBlock;
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
import org.spongepowered.asm.mixin.Mixin;

/**
 * A bucket of sea water poured into the Evaporator stays sea water, so it can make salt, and fresh water
 * stays fresh, so it does not; see {@link EvaporatorRecipeMixin} and {@link FluidUtilMixin}.
 */
@Mixin(EvaporatorBlock.class)
abstract class EvaporatorBlockMixin {
    @WrapMethod(method = "useItemOn")
    private ItemInteractionResult thirst$graded(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                Player player, InteractionHand hand, BlockHitResult hit,
                                                Operation<ItemInteractionResult> original) {
        return ExtraDelightWater.graded(() -> original.call(stack, state, level, pos, player, hand, hit));
    }
}
