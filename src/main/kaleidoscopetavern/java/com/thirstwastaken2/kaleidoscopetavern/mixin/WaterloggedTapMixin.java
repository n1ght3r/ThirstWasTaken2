package com.thirstwastaken2.kaleidoscopetavern.mixin;

import com.github.ysbbbbbb.kaleidoscopetavern.game.tap.impl.WaterloggedBehavior;
import com.thirstwastaken2.kaleidoscopetavern.TavernWater;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A tap on any waterlogged block, the mod's fallback when no behaviour is registered for the block, draws
 * the world's water: it is sampled where it lies, so a slab in the sea fills a salty cauldron and one in a
 * swamp a Murky or Dirty one. At every return, as in {@link WaterCauldronTapMixin}.
 */
@Mixin(value = WaterloggedBehavior.class, remap = false)
abstract class WaterloggedTapMixin {
    @Inject(method = "onEndExtract", at = @At("RETURN"))
    private void thirst$sampleWater(Level level, BlockPos tapPos, BlockState tapState, BlockState sourceState,
                                    BlockState destinationState, CallbackInfo ci) {
        TavernWater.tappedWorld(level, tapPos, tapState, destinationState);
    }
}
