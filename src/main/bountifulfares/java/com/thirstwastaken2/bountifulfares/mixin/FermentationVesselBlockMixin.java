package com.thirstwastaken2.bountifulfares.mixin;

import com.thirstwastaken2.purity.WaterPurity;
import net.hecco.bountifulfares.definition.block.custom.FermentationVesselBlock;
import net.hecco.bountifulfares.definition.block.enums.FermentationStage;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The Fermentation Vessel takes any water bottle, bucket or cup into an empty vessel by its item tag and
 * keeps only that it holds water, so sea water fermented ciders and wine as fresh water does. Sea water
 * is refused there: the click is spent and the water stays in the hand. Any fresh grade is taken, since a
 * fermented drink is safe whatever water went in, as in Brewin' and Chewin's keg. Only an empty vessel
 * takes water, so the other stages, adding fruit and taking the drink out, are left alone.
 */
@Mixin(FermentationVesselBlock.class)
abstract class FermentationVesselBlockMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void thirst$noSeaWater(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                   InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (state.getValue(FermentationVesselBlock.FERMENTATION_STAGE) == FermentationStage.EMPTY
                && WaterPurity.isSalty(stack)) {
            cir.setReturnValue(ItemInteractionResult.CONSUME);
        }
    }
}
