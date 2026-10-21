package com.thirstwastaken2.meadow.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.thirstwastaken2.purity.FillCapture;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.satisfy.meadow.core.item.WoodenBucket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Meadow's wooden bucket is an {@code Item} of its own, not a {@code BucketItem}, so core's bucket mixin
 * never saw it, and a wooden water bucket scooped from the sea came back fresh. The water is sampled
 * where it lay, as core's {@code BucketItemMixin} samples a bucket's, through the same capture.
 *
 * <p>{@code use} builds one {@code ItemStack}, the wooden water bucket the filling branch hands back; the
 * emptying branch builds its empty bucket in {@code getEmptySuccessItem}, so no slice is needed.
 */
@Mixin(WoodenBucket.class)
abstract class WoodenBucketMixin {
    @Inject(method = "use", at = @At("HEAD"))
    private void thirst$clearCapture(Level level, Player player, InteractionHand hand,
                                     CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        FillCapture.clear();
    }

    @ModifyExpressionValue(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/satisfy/meadow/core/item/WoodenBucket;getPlayerPOVHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private BlockHitResult thirst$capturePurity(BlockHitResult hit, Level level) {
        return FillCapture.capture(level, hit);
    }

    @ModifyExpressionValue(method = "use", at = @At(value = "NEW", target = "net/minecraft/world/item/ItemStack"))
    private ItemStack thirst$addPurity(ItemStack filled) {
        if (WaterPurity.isWaterContainer(filled)) return FillCapture.stamp(filled);
        FillCapture.clear();
        return filled;
    }
}
