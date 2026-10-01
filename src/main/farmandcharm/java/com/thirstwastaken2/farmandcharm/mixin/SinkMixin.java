package com.thirstwastaken2.farmandcharm.mixin;

import com.thirstwastaken2.farmandcharm.StandingWater;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.satisfy.farm_and_charm.core.block.SinkBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Farm & Charm's sink, which Farm & Charm leaves unregistered and Candlelight registers as its eleven
 * kitchen sinks. It fills from nothing, with an empty hand or a glass bottle, and handed out plain water
 * bottles and buckets, which read as Clean: an endless tap that made purifying pointless. What it gives
 * is now {@link StandingWater#STANDING}, Murky, as a trough's is. It stays an endless tap, as its authors
 * meant, but its water still wants boiling. Sea water poured in is refused, since it would come out fresh.
 */
@Mixin(SinkBlock.class)
abstract class SinkMixin {
    @Inject(method = "useItemOn", at = @At("HEAD"), cancellable = true)
    private void thirst$noSeaWater(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                   InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (StandingWater.isSaltyBucket(stack)) cir.setReturnValue(ItemInteractionResult.CONSUME);
    }

    // Every result goes to the player through addItem: an empty bucket or bottle after filling, left alone,
    // and the water bucket or bottle drawn.
    @ModifyArg(method = "useItemOn", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/entity/player/Player;addItem(Lnet/minecraft/world/item/ItemStack;)Z"))
    private ItemStack thirst$standingWater(ItemStack drawn) {
        return StandingWater.standing(drawn);
    }
}
