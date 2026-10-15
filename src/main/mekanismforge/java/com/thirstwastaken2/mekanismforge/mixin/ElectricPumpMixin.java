package com.thirstwastaken2.mekanismforge.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.thirstwastaken2.forge.SampledWater;
import com.thirstwastaken2.forge.WaterFluids;
import com.thirstwastaken2.purity.WaterQuality;
import mekanism.common.tile.machine.TileEntityElectricPump;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The Electric Pump makes a plain bucket of water for every source it takes, which reads as the default
 * grade, so a pump in the sea filled its tank with Clean water. Each bucket is stamped with the grade of
 * the block it came from.
 *
 * <p>The pump builds its stack twice for one block, before and after picking it up, and the second time
 * the block is already gone, so the first sample is kept for the second. The pump compares a stack with
 * its last one, components included, so a pump holding one grade leaves blocks of another in the world
 * rather than mixing them.
 */
@Mixin(value = TileEntityElectricPump.class, remap = false)
abstract class ElectricPumpMixin {
    @Unique
    private final SampledWater thirst$source = new SampledWater();

    /** The last block sampled and its grade, for the second stack built once it is picked up. */
    @Unique
    private BlockPos thirst$sampledPos;

    @Unique
    private WaterQuality thirst$sampled;

    @ModifyExpressionValue(method = "suck(Lnet/minecraft/core/BlockPos;ZZ)Z",
            at = @At(value = "INVOKE", target = "Lmekanism/common/tile/machine/TileEntityElectricPump;getOutput(Lnet/minecraft/world/level/material/Fluid;Z)Lnet/minecraftforge/fluids/FluidStack;"))
    private FluidStack thirst$stampPumped(FluidStack pumped, @Local(argsOnly = true) BlockPos pos) {
        if (!WaterFluids.isWater(pumped)) return pumped;
        Level level = ((BlockEntity) (Object) this).getLevel();
        WaterQuality quality = level == null ? null : thirst$source.at(level, pos);
        if (quality != null) {
            thirst$sampledPos = pos.immutable();
            thirst$sampled = quality;
        } else if (pos.equals(thirst$sampledPos)) {
            quality = thirst$sampled;
        }
        return WaterFluids.stampIfKnown(pumped, quality);
    }
}
