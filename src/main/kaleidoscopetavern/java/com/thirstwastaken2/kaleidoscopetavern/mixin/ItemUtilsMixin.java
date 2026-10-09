package com.thirstwastaken2.kaleidoscopetavern.mixin;

import com.github.ysbbbbbb.kaleidoscopetavern.util.ItemUtils;
import com.thirstwastaken2.kaleidoscopetavern.ReturnedWater;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Where the barrel hands a filled container back. Refabricated builds that bucket from nothing; the official
 * mod hands back the container its fluid capability filled. Stamped only while a remove call is running,
 * see {@link ReturnedWater}; every other item passing through is left alone. Stamped through
 * {@code WaterPurity.setQuality}, so fresh water gets {@code water_salty: false}.
 */
@Mixin(value = ItemUtils.class, remap = false)
abstract class ItemUtilsMixin {
    // By name alone: both overloads take the stack. The official mod's short one calls the long one, where a
    // second stamp changes nothing; Refabricated's two are separate, and its barrel calls the short one.
    @ModifyVariable(method = "getItemToLivingEntity", at = @At("HEAD"), argsOnly = true)
    private static ItemStack thirst$stampReturnedWater(ItemStack stack) {
        return ReturnedWater.stamp(stack);
    }
}
