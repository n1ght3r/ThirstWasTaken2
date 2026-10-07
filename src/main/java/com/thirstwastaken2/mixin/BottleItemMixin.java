package com.thirstwastaken2.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.thirstwastaken2.purity.FillCapture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(BottleItem.class)
abstract class BottleItemMixin {
    @Inject(method = "use", at = @At("HEAD"))
    private void thirst$clearCapture(Level level, Player player, InteractionHand hand,
                                     CallbackInfoReturnable<InteractionResult> cir) {
        FillCapture.clear();
    }

    // Diverges from vanilla and upstream: a bottle fills from flowing water too, as the bowl and the
    // waterskin already do. The ray stops at the first water block, source or not, and vanilla's own
    // water tag check that follows accepts both; a bottle takes nothing from the world either way.
    @ModifyArg(method = "use", index = 2, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/BottleItem;getPlayerPOVHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private ClipContext.Fluid thirst$anyWater(ClipContext.Fluid mode) {
        return ClipContext.Fluid.ANY;
    }

    @ModifyExpressionValue(method = "use", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/BottleItem;getPlayerPOVHitResult(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/ClipContext$Fluid;)Lnet/minecraft/world/phys/BlockHitResult;"))
    private BlockHitResult thirst$capturePurity(BlockHitResult hit, Level level) {
        return FillCapture.capture(level, hit);
    }

    @ModifyArg(method = "use", index = 2, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/BottleItem;turnBottleIntoItem(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack thirst$addPurity(ItemStack filled) {
        return FillCapture.stamp(filled);
    }
}
