package com.thirstwastaken2.youkaisfeasts.mixin;

import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.Potions;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A bottle of sea water is not the water bottle a recipe asks for. Youkais' Feasts' Bamboo Mizuyokan
 * cooks in Farmer's Delight's Cooking Pot from L2 Core's potion ingredient, which reads only the bottle's
 * potion, so without this the sea went into it like any water. Fresh water of any grade still counts.
 *
 * <p>The same mixin as Fruits Delight's, which nests the same L2 Core: each integration applies its own
 * only when its mod is installed, so either mod alone is covered, and with both the second check finds
 * nothing left to refuse. It also reaches any other L2 mod's water bottle recipe, where sea water should
 * not count either. Named by string, since L2 Core is not on the compile classpath; the record's one field
 * is a Minecraft type.
 */
// L2 Library on 1.20.1, where the field is a plain Potion and a bridge test(Object) sits beside
// test(ItemStack), so the target names its descriptor. Neither name is Minecraft's, so neither is remapped.
//? if >=1.20.5 {
@Mixin(targets = "dev.xkmc.l2core.serial.ingredients.PotionIngredient")
abstract class WaterBottleIngredientMixin {
    @Shadow
    @Final
    private Holder<Potion> potion;

    @Inject(method = "test", at = @At("HEAD"), cancellable = true)
    private void thirst$seaWaterIsNotAWaterBottle(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (potion.value() == Potions.WATER.value() && WaterPurity.isSalty(stack)) cir.setReturnValue(false);
    }
}
//?} else {
/*@Mixin(targets = "dev.xkmc.l2library.serial.ingredients.PotionIngredient", remap = false)
abstract class WaterBottleIngredientMixin {
    @Shadow
    public Potion potion;

    @Inject(method = "test(Lnet/minecraft/world/item/ItemStack;)Z", at = @At("HEAD"), cancellable = true)
    private void thirst$seaWaterIsNotAWaterBottle(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        if (potion == Potions.WATER && WaterPurity.isSalty(stack)) cir.setReturnValue(false);
    }
}
*///?}
