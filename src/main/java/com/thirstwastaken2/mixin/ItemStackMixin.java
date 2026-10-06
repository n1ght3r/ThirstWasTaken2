package com.thirstwastaken2.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.tooltip.ThirstTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.function.Consumer;

@Mixin(ItemStack.class)
abstract class ItemStackMixin {
    // Before 1.21.2 use hands back the resulting stack along with the result.
    //? if >=1.21.2 {
    @Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void thirst$preventDrinkingWhenFull(Level level, Player player,
                                                 InteractionHand hand,
                                                 CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (WaterPurity.isPlainWaterDrink(stack) && !ThirstManager.canDrinkWater(player, stack)) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
    //?} else {
    /*@Inject(method = "use", at = @At("HEAD"), cancellable = true)
    private void thirst$preventDrinkingWhenFull(Level level, Player player, InteractionHand hand,
            CallbackInfoReturnable<net.minecraft.world.InteractionResultHolder<ItemStack>> cir) {
        ItemStack stack = (ItemStack) (Object) this;
        if (WaterPurity.isPlainWaterDrink(stack) && !ThirstManager.canDrinkWater(player, stack)) {
            cir.setReturnValue(net.minecraft.world.InteractionResultHolder.fail(stack));
        }
    }
    *///?}

    @Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void thirst$onFinishUsing(Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        if (level.isClientSide() || !(entity instanceof Player player)) return;
        ThirstManager.drinkItem(player, (ItemStack) (Object) this);
    }

    // 1.21.5 gave the item's own lines a method of their own. Before it they are the item's hover text,
    // added inside getTooltipLines, and the mod's lines follow that call instead.
    //? if >=1.21.5 {
    @Inject(method = "addDetailsToTooltip", at = @At("TAIL"))
    private void thirst$addPurityTooltip(Item.TooltipContext context,
                                         net.minecraft.world.item.component.TooltipDisplay display, Player player,
                                         TooltipFlag flag, Consumer<Component> tooltip, CallbackInfo ci) {
        ThirstTooltip.appendTo((ItemStack) (Object) this, tooltip);
    }
    //?} elif >=1.20.5 {
    /*@WrapOperation(method = "getTooltipLines", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/Item$TooltipContext;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V"))
    private void thirst$addPurityTooltip(Item item, ItemStack stack, Item.TooltipContext context,
                                         List<Component> lines, TooltipFlag flag, Operation<Void> original) {
        original.call(item, stack, context, lines, flag);
        ThirstTooltip.appendTo(stack, lines::add);
    }
    *///?} else {
    /*// Before 1.20.5 the hover text is handed the level rather than a tooltip context.
    @WrapOperation(method = "getTooltipLines", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/Item;appendHoverText(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Ljava/util/List;Lnet/minecraft/world/item/TooltipFlag;)V"))
    private void thirst$addPurityTooltip(Item item, ItemStack stack, Level level,
                                         List<Component> lines, TooltipFlag flag, Operation<Void> original) {
        original.call(item, stack, level, lines, flag);
        ThirstTooltip.appendTo(stack, lines::add);
    }
    *///?}

    // 1.20.1 has no default components, so a new stack is handed its item's default tag instead.
    //? if <1.20.5 {
    /*@Inject(method = "<init>(Lnet/minecraft/world/level/ItemLike;I)V", at = @At("TAIL"))
    private void thirst$applyDefaultData(net.minecraft.world.level.ItemLike item, int count, CallbackInfo ci) {
        com.thirstwastaken2.platform.DefaultData.apply((ItemStack) (Object) this);
    }
    *///?}
}
