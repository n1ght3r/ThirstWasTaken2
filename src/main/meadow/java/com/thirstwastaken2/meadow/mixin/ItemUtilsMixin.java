package com.thirstwastaken2.meadow.mixin;

import com.thirstwastaken2.meadow.WoodenCauldron;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Every draw from Meadow's wooden water cauldron, by a glass bottle, a bucket or a wooden bucket, hands
 * its result through {@code createFilledResult} from one of Meadow's interaction lambdas. Stamping here,
 * only while {@code AbstractCauldronBlockMixin} says a wooden water cauldron is in use, reaches all of
 * them without naming a lambda. The three-argument overload calls this one. Outside such a use it does
 * nothing.
 */
@Mixin(ItemUtils.class)
abstract class ItemUtilsMixin {
    @ModifyVariable(method = "createFilledResult(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;Z)Lnet/minecraft/world/item/ItemStack;",
            at = @At("HEAD"), argsOnly = true, ordinal = 1)
    private static ItemStack thirst$standingWater(ItemStack filled) {
        return WoodenCauldron.stampDrawn(filled);
    }
}
