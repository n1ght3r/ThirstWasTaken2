package com.thirstwastaken2.bountifulfares.mixin;

import com.thirstwastaken2.bountifulfares.BountifulFaresRecipes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * {@link ShapelessRecipeMixin}, for the mod's shaped recipes: the Sponge Cake takes three water buckets,
 * and sea water bakes none. One mixin cannot target both: their {@code result} fields are two fields.
 */
@Mixin(ShapedRecipe.class)
abstract class ShapedRecipeMixin {
    @Shadow @Final ItemStack result;

    /** Whether this recipe makes an item of the mod's, worked out on its first match: a recipe never changes. */
    @Unique
    private Boolean thirst$makesBountifulFares;

    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("RETURN"), cancellable = true)
    private void thirst$bountifulFaresSeaWater(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> cir) {
        if (!cir.getReturnValueZ()) return;
        if (thirst$makesBountifulFares == null) thirst$makesBountifulFares = BountifulFaresRecipes.makesBountifulFares(result);
        if (thirst$makesBountifulFares && BountifulFaresRecipes.holdsSeaWater(input)) cir.setReturnValue(false);
    }
}
