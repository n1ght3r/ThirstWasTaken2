package com.thirstwastaken2.kaleidoscopetavern.mixin;

import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.mixology.ShakerBlockEntity;
import com.thirstwastaken2.kaleidoscopetavern.TavernWater;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * The shaker refuses a sea-water bottle: {@code #cocktail_ingredient_white} holds any potion, so any water
 * bottle counts as an ingredient, and a cocktail made with sea water would come out as safe as any other.
 * Refused the way the mod refuses a drink below its brew level, before the bottle is taken.
 */
@Mixin(value = ShakerBlockEntity.class, remap = false)
abstract class ShakerBlockEntityMixin {
    @Inject(method = "addIngredient", at = @At("HEAD"), cancellable = true)
    private void thirst$noSeaWater(ItemStack stack, LivingEntity user, CallbackInfoReturnable<Boolean> cir) {
        if (TavernWater.shakerRefuses(stack, user)) cir.setReturnValue(false);
    }
}
