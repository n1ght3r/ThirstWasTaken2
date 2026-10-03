package com.thirstwastaken2.croptopia.mixin;

import com.thirstwastaken2.croptopia.platform.CroptopiaVanilla;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.world.item.crafting.ShapedRecipe;
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

/**
 * Sea water is not the water a Croptopia recipe asks for, as Farm & Charm's and Fruits Delight's make
 * nothing from it. Croptopia's recipes are plain crafting: its water bottle is crafted from
 * {@code minecraft:water_bucket}, and its tea, steamed rice, soups and dough take {@code #c:water_bottles},
 * which holds the water bucket too, both matched by item alone, so a bucket of sea water made sixteen water
 * bottles or a cup of tea. Fresh water of any grade still matches: what a recipe makes is its own item,
 * as tea is, and Croptopia's water bottle carries no grade at all.
 *
 * <p>Matched by result rather than by recipe id, since {@code matches} does not know its id: any shaped or
 * shapeless recipe making an item of Croptopia's, a data pack's included. Only water containers carry
 * salt, so a recipe with no water in it is never refused.
 *
 * <p>{@link ShapelessRecipeMixin} is the same for shapeless recipes. One mixin cannot target both: their
 * {@code result} fields are two fields, with different names under Forge 1.20.1's SRG.
 */
@Mixin(ShapedRecipe.class)
abstract class ShapedRecipeMixin {
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
