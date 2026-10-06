package com.thirstwastaken2.extradelight.mixin;

import com.thirstwastaken2.extradelight.ExtraDelightWater;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Extra Delight's Vinegar Pot and Yeast Pot are crafted with a water bottle or bucket, matched on the item
 * alone, so a sea water one served too. Sea water crafts no item of Extra Delight's now, as it crafts
 * none of Croptopia's. Croptopia's own guard cannot be used here: it only loads with Croptopia.
 */
@Mixin(ShapelessRecipe.class)
abstract class ShapelessRecipeMixin {
    @Shadow @Final ItemStack result;

    /** Whether this recipe makes an item of Extra Delight's, worked out on its first match: a recipe never changes. */
    @Unique
    private Boolean thirst$makesExtraDelight;

    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("RETURN"), cancellable = true)
    private void thirst$seaWaterIsNotWater(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ() || !thirst$isExtraDelight()) return;
        for (ItemStack stack : input.items()) {
            if (ExtraDelightWater.isSeaWater(stack)) {
                cir.setReturnValue(false);
                return;
            }
        }
    }

    @Unique
    private boolean thirst$isExtraDelight() {
        if (thirst$makesExtraDelight == null) {
            thirst$makesExtraDelight = BuiltInRegistries.ITEM.getKey(result.getItem()).getNamespace().equals("extradelight");
        }
        return thirst$makesExtraDelight;
    }
}
