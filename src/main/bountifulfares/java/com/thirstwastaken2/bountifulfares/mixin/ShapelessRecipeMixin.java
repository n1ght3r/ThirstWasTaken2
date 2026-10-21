package com.thirstwastaken2.bountifulfares.mixin;

import com.thirstwastaken2.bountifulfares.BountifulFaresRecipes;
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
 * Sea water is not the water a Bountiful Fares recipe asks for. Its six teas are shapeless crafting, tea
 * leaves or a flower and a water cup, matched by item alone, so a cup of sea water made a cup of tea. Any
 * fresh grade still matches: tea is a drink with a value of its own, as Croptopia's is.
 *
 * <p>Matched by result, since {@code matches} does not know its id: any shapeless recipe making an item
 * of the mod's, a data pack's included. Only water containers carry salt, so a recipe with no water in it
 * is never refused. {@link ShapedRecipeMixin} is the same for the Sponge Cake's water buckets.
 */
@Mixin(ShapelessRecipe.class)
abstract class ShapelessRecipeMixin {
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
