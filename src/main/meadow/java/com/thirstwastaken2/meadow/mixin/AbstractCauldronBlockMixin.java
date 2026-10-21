package com.thirstwastaken2.meadow.mixin;

import com.thirstwastaken2.meadow.WoodenCauldron;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Meadow's wooden cauldrons are standing water (see {@link WoodenCauldron}). Every use of a cauldron goes
 * through {@code AbstractCauldronBlock.useItemOn}, which hands the item to the block's interaction map,
 * and Meadow's three wooden cauldrons are the ones with Meadow's maps. Sea water poured into any of them
 * is refused, the click spent and the water kept in the hand. While the wooden water cauldron is used,
 * what Meadow's interactions hand back through {@code ItemUtils.createFilledResult} is stamped Murky by
 * {@code ItemUtilsMixin}; the empty bucket a pour hands back is no water container and is left alone.
 * Vanilla's cauldrons have vanilla's maps and are not touched.
 */
@Mixin(AbstractCauldronBlock.class)
abstract class AbstractCauldronBlockMixin {
    @Shadow
    @Final
    protected CauldronInteraction.InteractionMap interactions;

    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void thirst$standingWater(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                      InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (!WoodenCauldron.isWooden(interactions)) return;
        if (WaterPurity.isSalty(stack)) {
            cir.setReturnValue(ItemInteractionResult.CONSUME);
            return;
        }
        if (!level.isClientSide() && WoodenCauldron.holdsWater(interactions)) WoodenCauldron.startDrawing();
    }

    @Inject(method = "useItemOn", at = @At("RETURN"))
    private void thirst$doneDrawing(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                    InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<ItemInteractionResult> cir) {
        WoodenCauldron.stopDrawing();
    }
}
