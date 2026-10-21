package com.thirstwastaken2.meadow.mixin;

import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.satisfy.meadow.core.block.entity.CookingCauldronBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Meadow's Cooking Pot fills with "water" from any water bucket, wooden water bucket or water bottle, by
 * its {@code meadow/cooking_liquid} data, and keeps only the amount, so sea water cooked as fresh water
 * does. Sea water is refused: the click is spent and the water stays in the hand. Any fresh grade still
 * fills it, since what comes out is cooked food, as in Farm & Charm's Cooking Pot. Only water containers
 * carry salt, so nothing else the pot is used with is touched.
 */
@Mixin(CookingCauldronBlockEntity.class)
abstract class CookingCauldronBlockEntityMixin {
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true, remap = false)
    private void thirst$noSeaWater(Level level, Player player, InteractionHand hand, ItemStack stack,
                                   CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (WaterPurity.isSalty(stack)) cir.setReturnValue(ItemInteractionResult.CONSUME);
    }
}
