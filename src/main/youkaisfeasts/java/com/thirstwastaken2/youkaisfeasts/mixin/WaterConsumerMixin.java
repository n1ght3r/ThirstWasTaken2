package com.thirstwastaken2.youkaisfeasts.mixin;

import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A bottle or bucket of sea water does not start a soup. The Small Iron Pot, the Short Iron Pot and the
 * Stockpot each turn into their cooking pot when used with water, and ask only this whether the stack is
 * water: a water bucket, a water potion, or a fluid container that drains plain water. The soups restore
 * thirst, so without this the sea made a safe one. Fresh water of any grade still counts: the soup is its
 * own item, and boiled.
 *
 * <p>Named by string, since nothing compiles against the mod. The method is the mod's own on both
 * builds, so nothing is remapped.
 */
@Mixin(targets = "dev.xkmc.youkaishomecoming.util.WaterConsumer", remap = false)
abstract class WaterConsumerMixin {
    @Inject(method = "isWaterContainer", at = @At("HEAD"), cancellable = true)
    private static void thirst$seaWaterIsNotPotWater(ItemStack stack, int minAmount, CallbackInfoReturnable<Boolean> cir) {
        if (WaterPurity.isSalty(stack)) cir.setReturnValue(false);
    }
}
