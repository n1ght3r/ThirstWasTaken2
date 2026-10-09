package com.thirstwastaken2.kaleidoscopetavern.mixin;

import com.github.ysbbbbbb.kaleidoscopetavern.game.tap.impl.WaterCauldronTapBehavior;
import com.thirstwastaken2.kaleidoscopetavern.TavernWater;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A tap on a water cauldron fills the cauldron or the empty bottle below with the source cauldron's
 * grade. The mod does not drain the source, an endless tap by design; this only makes it copy the right
 * water. At every return: the method returns from the middle once it has filled a cauldron.
 */
@Mixin(value = WaterCauldronTapBehavior.class, remap = false)
abstract class WaterCauldronTapMixin {
    @Inject(method = "onEndExtract", at = @At("RETURN"))
    private void thirst$copyGrade(Level level, BlockPos tapPos, BlockState tapState, BlockState sourceState,
                                  BlockState destinationState, CallbackInfo ci) {
        TavernWater.tappedCauldron(level, tapPos, sourceState, destinationState);
    }
}
