package com.thirstwastaken2.nomansland.mixin;

import com.farcr.nomansland.common.block.cauldrons.MilkCauldron;
import com.thirstwastaken2.api.ThirstApi;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A milk bucket poured into a cauldron is four levels, and each is sipped with an empty hand, which takes
 * off one milk-curable effect. The sips went through the block, so they restored no thirst. Now each
 * restores a quarter of what a milk bucket restores, rounded, at least one point of thirst: four sips are
 * about one bucket, so the cauldron is a way to share milk, not to multiply it.
 */
@Mixin(MilkCauldron.class)
abstract class MilkCauldronMixin {
    // Every sip ends in the one lowerFillLevel of the method; the last level empties the cauldron through
    // it too. Bottling with Farmer's Delight goes through useItemOn and is no sip. The client runs this as
    // well, where ThirstApi.drink does nothing.
    @Inject(method = "useWithoutItem", at = @At(value = "INVOKE",
            target = "Lcom/farcr/nomansland/common/block/cauldrons/MilkCauldron;lowerFillLevel(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"))
    private void thirst$sip(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
                            CallbackInfoReturnable<InteractionResult> cir) {
        int[] bucket = ThirstApi.thirstValues(Items.MILK_BUCKET);
        if (bucket == null) return;
        // A milk bucket is four sips.
        ThirstApi.drink(player, Math.max(1, Math.round(bucket[0] / 4.0F)), Math.round(bucket[1] / 4.0F));
    }
}
