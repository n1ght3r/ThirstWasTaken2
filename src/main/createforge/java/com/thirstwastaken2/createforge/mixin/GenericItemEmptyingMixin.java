package com.thirstwastaken2.createforge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.fluids.transfer.GenericItemEmptying;
import com.thirstwastaken2.forge.WaterFluids;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.createmod.catnip.data.Pair;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Bottles and buckets poured into an Item Drain, or by hand into a Create tank. The quality is read
 * before emptying, which shrinks the stack it would be read from.
 */
@Mixin(value = GenericItemEmptying.class, remap = false)
abstract class GenericItemEmptyingMixin {
    @WrapMethod(method = "emptyItem")
    private static Pair<FluidStack, ItemStack> thirst$stampEmptied(Level level, ItemStack stack, boolean simulate,
                                                                   Operation<Pair<FluidStack, ItemStack>> original) {
        WaterQuality quality = WaterPurity.isWaterContainer(stack) ? WaterPurity.quality(stack) : null;
        Pair<FluidStack, ItemStack> result = original.call(level, stack, simulate);
        if (quality == null || !WaterFluids.isWater(result.getFirst())) return result;
        // A copy, because an emptying recipe hands out its own result stack.
        return Pair.of(WaterFluids.stamp(result.getFirst().copy(), quality), result.getSecond());
    }
}
