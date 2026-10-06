package com.thirstwastaken2.mixin;

import com.thirstwastaken2.item.WaterskinItem;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A canteen or flask boiled in a furnace keeps the water it went in with. Its recipe matches any number
 * of servings, one recipe a grade rather than one for every fill, so the result the recipe hands out
 * cannot say how many: this copies them over from what went in, see {@code WaterskinItem.keepServings}.
 * On the recipe itself, so a furnace and anything else that cooks through the recipe agree. From 1.21.2
 * cooking recipes are single item recipes, and from 26.1 assembling takes no registries; 1.20.1 hands the
 * recipe the furnace itself as the input.
 */
//? if >=1.21.2 {
@Mixin(net.minecraft.world.item.crafting.SingleItemRecipe.class)
//?} else
//@Mixin(net.minecraft.world.item.crafting.AbstractCookingRecipe.class)
abstract class CookingRecipeMixin {
    //? if >=26.1 {
    @Inject(method = "assemble(Lnet/minecraft/world/item/crafting/SingleRecipeInput;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"))
    private void thirst$keepServings(net.minecraft.world.item.crafting.SingleRecipeInput input,
                                     CallbackInfoReturnable<ItemStack> cir) {
        WaterskinItem.keepServings(input.item(), cir.getReturnValue());
    }
    //?} elif >=1.20.5 {
    /*@Inject(method = "assemble(Lnet/minecraft/world/item/crafting/SingleRecipeInput;Lnet/minecraft/core/HolderLookup$Provider;)Lnet/minecraft/world/item/ItemStack;",
            at = @At("RETURN"))
    private void thirst$keepServings(net.minecraft.world.item.crafting.SingleRecipeInput input,
                                     net.minecraft.core.HolderLookup.Provider registries, CallbackInfoReturnable<ItemStack> cir) {
        WaterskinItem.keepServings(input.item(), cir.getReturnValue());
    }
    *///?} else {
    /*@Inject(method = "assemble", at = @At("RETURN"))
    private void thirst$keepServings(net.minecraft.world.Container input, net.minecraft.core.RegistryAccess registries,
                                     CallbackInfoReturnable<ItemStack> cir) {
        WaterskinItem.keepServings(input.getItem(0), cir.getReturnValue());
    }
    *///?}
}
