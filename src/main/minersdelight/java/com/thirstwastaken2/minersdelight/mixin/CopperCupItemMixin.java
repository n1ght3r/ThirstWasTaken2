package com.thirstwastaken2.minersdelight.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.sammy.minersdelight.content.item.CopperCupItem;
import com.thirstwastaken2.purity.FillCapture;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * An empty copper cup scoops a source block the way a bucket does, through {@code pickupBlock}, and
 * trades the plain bucket it gets for a water cup, so the cup came out ungraded and poured into a
 * cauldron as Clean water, sea water included. The water is sampled where it lay, as core's
 * {@code BucketItemMixin} samples a bucket's.
 *
 * <p>{@code use} is the same shape on both builds, and so are the two calls hooked here; only
 * {@code pickupBlock} differs, so this does not slice from it as the bucket mixin does. The emptying
 * branch hands back an empty cup, which is not a water container, so the stamp passes it by.
 */
@Mixin(CopperCupItem.class)
abstract class CopperCupItemMixin {
    @Inject(method = "use", at = @At("HEAD"))
    private void thirst$clearCapture(Level level, Player player, InteractionHand hand,
                                     CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        FillCapture.clear();
    }

    @ModifyExpressionValue(method = "use", at = @At(value = "INVOKE",
            target = "Lcom/sammy/minersdelight/content/item/CopperCupItem;getPlayerPOVHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private BlockHitResult thirst$capturePurity(BlockHitResult hit, Level level) {
        return FillCapture.capture(level, hit);
    }

    @ModifyArg(method = "use", index = 2, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemUtils;createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack thirst$addPurity(ItemStack filled) {
        if (WaterPurity.isWaterContainer(filled)) return FillCapture.stamp(filled);
        FillCapture.clear();
        return filled;
    }
}
