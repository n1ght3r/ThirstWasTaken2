package com.thirstwastaken2.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.thirstwastaken2.item.WaterskinItem;
import net.minecraft.world.level.block.entity.AbstractFurnaceBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A canteen or flask takes as long in a furnace as its servings do, its per-serving boil time each, the
 * way a stack of bowls takes one bowl's time each: four drinks in a canteen take as long whatever its
 * capacity. The recipe's own time is the vessel's default fill, for anything that cooks without this.
 * Recomputed from the input rather than scaled, so it does not matter that 26.3 asks through two methods
 * of this name, one calling the other.
 */
@Mixin(AbstractFurnaceBlockEntity.class)
abstract class FurnaceMixin {
    @Inject(method = "getTotalCookTime", at = @At("RETURN"), cancellable = true)
    private static void thirst$timePerServing(CallbackInfoReturnable<Integer> cir,
                                              @Local(argsOnly = true) AbstractFurnaceBlockEntity furnace) {
        int ticks = WaterskinItem.furnaceTicks(furnace.getItem(0));
        if (ticks > 0) cir.setReturnValue(ticks);
    }
}
