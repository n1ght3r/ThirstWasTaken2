package com.thirstwastaken2.bountifulfares.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.thirstwastaken2.purity.FillCapture;
import net.hecco.bountifulfares.definition.item.custom.CupItem;
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
 * An empty cup scoops water the way a glass bottle does, with its own {@code use}: a ray to the first
 * water source, then {@code turnCupIntoItem} trades the cup for a water cup. Core's {@code BottleItemMixin}
 * never sees it, so the cup came out ungraded and a cup of sea water read as Clean. The water is sampled
 * where it lay, through the same {@link FillCapture}: clear at the head, capture on the ray, stamp the
 * water cup handed to {@code turnCupIntoItem}, which is the only thing {@code use} ever fills.
 */
@Mixin(CupItem.class)
abstract class CupItemMixin {
    @Inject(method = "use", at = @At("HEAD"))
    private void thirst$clearCapture(Level level, Player player, InteractionHand hand,
                                     CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
        FillCapture.clear();
    }

    @ModifyExpressionValue(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/hecco/bountifulfares/definition/item/custom/CupItem;getPlayerPOVHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private BlockHitResult thirst$capturePurity(BlockHitResult hit, Level level) {
        return FillCapture.capture(level, hit);
    }

    @ModifyArg(method = "use", index = 2, at = @At(value = "INVOKE",
            target = "Lnet/hecco/bountifulfares/definition/item/custom/CupItem;turnCupIntoItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack thirst$addPurity(ItemStack filled) {
        return FillCapture.stamp(filled);
    }
}
