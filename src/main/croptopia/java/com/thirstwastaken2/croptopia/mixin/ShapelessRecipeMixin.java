package com.thirstwastaken2.croptopia.mixin;

import com.thirstwastaken2.croptopia.platform.CroptopiaVanilla;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
//? if >=26.1 {
import net.minecraft.world.item.ItemStackTemplate;
//?} else {
/*import net.minecraft.world.item.ItemStack;
*///?}
//? if >=1.21 {
import net.minecraft.world.item.crafting.CraftingInput;
//?} else {
/*import net.minecraft.world.inventory.CraftingContainer;
*///?}

/** {@link ShapedRecipeMixin}, for Croptopia's shapeless recipes: sea water crafts none of them. */
@Mixin(ShapelessRecipe.class)
abstract class ShapelessRecipeMixin {
    //? if >=26.1 {
    @Shadow @Final private ItemStackTemplate result;
    //?} else {
    /*@Shadow @Final ItemStack result;
    *///?}

    /** Whether this recipe makes an item of Croptopia's, worked out on its first match: a recipe never changes. */
    @Unique
    private Boolean thirst$makesCroptopia;

    //? if >=1.21 {
    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("RETURN"), cancellable = true)
    private void thirst$seaWaterIsNotWater(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && thirst$isCroptopia() && CroptopiaVanilla.holdsSeaWater(input)) cir.setReturnValue(false);
    }
    //?} else {
    /*@Inject(method = "matches(Lnet/minecraft/world/inventory/CraftingContainer;Lnet/minecraft/world/level/Level;)Z",
            at = @At("RETURN"), cancellable = true)
    private void thirst$seaWaterIsNotWater(CraftingContainer input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValueZ() && thirst$isCroptopia() && CroptopiaVanilla.holdsSeaWater(input)) cir.setReturnValue(false);
    }
    *///?}

    @Unique
    private boolean thirst$isCroptopia() {
        if (thirst$makesCroptopia == null) {
            thirst$makesCroptopia = Vanilla.itemId(CroptopiaVanilla.resultItem(result)).getNamespace().equals("croptopia");
        }
        return thirst$makesCroptopia;
    }
}
