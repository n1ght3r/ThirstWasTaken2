package com.thirstwastaken2.hearthandharvestforge.mixin;

import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.Level;
import net.minecraftforge.items.wrapper.RecipeWrapper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Only sea water boils down to salt in the Cooking Pot. On 1.21.1 the same is a data file that replaces
 * the recipe; on Forge 47 the mod's own file wins over a replacement, ordering or not.
 *
 * <p>1.0.12c's {@code minecraft:salt_from_bottle} took any potion, so a fresh water bottle matched both it
 * and the pot's purifying recipe, and in game a Pure bottle came out as salt. A water container in the pot
 * that is not salty fails it now; a potion that is not water still makes salt, as Hearth and Harvest
 * meant.
 *
 * <p>1.4.1's {@code hearthandharvest:cooking/salt_from_bottle} takes a water bottle by exact NBT, so a
 * plain one matched it and the purifying recipe both, and a sea water bottle, whose stamp is NBT, matched
 * nothing. A fresh one fails it too now, and a sea water bottle alone among the ingredients matches.
 *
 * <p>{@code matches(RecipeWrapper, Level)} is Farmer's Delight's own method, which the vanilla bridge
 * calls, so it keeps its name in a production jar. Named by string, since nothing compiles against it.
 */
@Mixin(targets = "vectorwing.farmersdelight.common.crafting.CookingPotRecipe")
abstract class SaltFromSeaWaterMixin {
    private static final Identifier THIRST$SALT_FROM_BOTTLE = Identifier.withDefaultNamespace("salt_from_bottle");
    private static final Identifier THIRST$COOKING_SALT_FROM_BOTTLE =
            Identifier.fromNamespaceAndPath("hearthandharvest", "cooking/salt_from_bottle");
    /** The first six slots are the ingredients; the rest are the container, the meal and the output. */
    private static final int THIRST$INGREDIENT_SLOTS = 6;

    @Inject(method = "matches(Lnet/minecraftforge/items/wrapper/RecipeWrapper;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true, remap = false)
    private void thirst$onlySeaWater(RecipeWrapper input, Level level, CallbackInfoReturnable<Boolean> cir) {
        Identifier id = ((Recipe<?>) (Object) this).getId();
        boolean exactNbt = THIRST$COOKING_SALT_FROM_BOTTLE.equals(id);
        if (!exactNbt && !THIRST$SALT_FROM_BOTTLE.equals(id)) return;
        int slots = Math.min(THIRST$INGREDIENT_SLOTS, input.getContainerSize());
        ItemStack only = ItemStack.EMPTY;
        int filled = 0;
        for (int slot = 0; slot < slots; slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.isEmpty()) continue;
            if (WaterPurity.isWaterContainer(stack) && !WaterPurity.isSalty(stack)) {
                cir.setReturnValue(false);
                return;
            }
            only = stack;
            filled++;
        }
        if (exactNbt && filled == 1 && only.is(Items.POTION) && WaterPurity.isSalty(only)) cir.setReturnValue(true);
    }
}
