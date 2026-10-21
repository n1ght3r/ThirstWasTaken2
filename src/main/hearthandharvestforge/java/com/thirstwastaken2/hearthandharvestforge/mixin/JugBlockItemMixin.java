package com.thirstwastaken2.hearthandharvestforge.mixin;

import alabaster.hearthandharvest.common.item.JugBlockItem;
import com.llamalad7.mixinextras.sugar.Local;
import com.thirstwastaken2.hearthandharvestforge.HearthWater;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * A Jug used on a source block scoops a bucket of it into the jug's tank, made from the fluid's type
 * alone, so ocean water came in as plain water and poured back out fresh. It is sampled where it lies
 * now, like any water collected from the world. As on 1.21.1; 1.4.1 brought the jug item to 1.20.1.
 */
@Mixin(JugBlockItem.class)
abstract class JugBlockItemMixin {
    @ModifyVariable(method = "use", at = @At("STORE"))
    private FluidStack thirst$sampled(FluidStack incoming, Level level, Player player, InteractionHand hand,
                                      @Local BlockPos pos) {
        if (level.isClientSide()) return incoming;
        return HearthWater.stamped(incoming, WaterPurity.sampleAt(level, pos));
    }
}
