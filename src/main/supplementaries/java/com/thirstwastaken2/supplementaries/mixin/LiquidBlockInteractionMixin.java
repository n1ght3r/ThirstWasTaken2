package com.thirstwastaken2.supplementaries.mixin;

import com.thirstwastaken2.supplementaries.SampledWater;
import net.mehvahdjukaar.moonlight.api.fluids.FluidOffer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A faucet over a water source block. Supplementaries offers plain water, which a tank then reads as
 * {@code defaultQuality}, so a faucet over a swamp filled a jar with Clean water where a bottle filled
 * by hand in the same pool comes out Dirty. The water is sampled where it lies instead, which is what
 * every other way of collecting it already does.
 *
 * <p>The sample is cached, because a faucet asks on a tick; see {@link SampledWater}. The cache hangs
 * off this behaviour, which Supplementaries builds again on every data pack reload, so nothing here
 * outlives a world.
 */
@Mixin(targets = "net.mehvahdjukaar.supplementaries.common.block.faucet.LiquidBlockInteraction",
        remap = false)
abstract class LiquidBlockInteractionMixin {
    @Unique
    private SampledWater thirst$sampled;

    @Inject(method = "getProvidedFluid", at = @At("RETURN"))
    private void thirst$sampleWorldWater(Level level, BlockPos pos, Direction dir, FluidState source,
                                          CallbackInfoReturnable<FluidOffer> cir) {
        if (thirst$sampled == null) thirst$sampled = new SampledWater();
        thirst$sampled.stamp(cir.getReturnValue(), level, pos);
    }
}
