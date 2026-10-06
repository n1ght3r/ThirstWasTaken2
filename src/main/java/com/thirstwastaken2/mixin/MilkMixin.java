package com.thirstwastaken2.mixin;

import com.thirstwastaken2.effect.UpsetStomach;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Milk does not cure Upset Stomach. Around the call that clears a milk drinker's effects, the illness is
 * noted and given back; see {@code UpsetStomach.beforeMilk}. From 1.21.2 that call is the milk bucket's
 * clear-all-effects consume effect, before it the bucket's own finish. NeoForge and Forge clear by cure
 * inside the same call, so one hook serves every loader.
 */
//? if >=1.21.2 {
@Mixin(net.minecraft.world.item.consume_effects.ClearAllStatusEffectsConsumeEffect.class)
//?} else
//@Mixin(net.minecraft.world.item.MilkBucketItem.class)
abstract class MilkMixin {
    //? if >=1.21.2 {
    @Inject(method = "apply", at = @At("HEAD"))
    private void thirst$noteIllness(Level level, ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        UpsetStomach.beforeMilk(entity);
    }

    @Inject(method = "apply", at = @At("RETURN"))
    private void thirst$keepIllness(Level level, ItemStack stack, LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        UpsetStomach.afterMilk(entity);
    }
    //?} else {
    /*@Inject(method = "finishUsingItem", at = @At("HEAD"))
    private void thirst$noteIllness(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        UpsetStomach.beforeMilk(entity);
    }

    @Inject(method = "finishUsingItem", at = @At("RETURN"))
    private void thirst$keepIllness(ItemStack stack, Level level, LivingEntity entity, CallbackInfoReturnable<ItemStack> cir) {
        UpsetStomach.afterMilk(entity);
    }
    *///?}
}
