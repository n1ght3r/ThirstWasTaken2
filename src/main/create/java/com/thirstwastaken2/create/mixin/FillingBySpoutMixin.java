package com.thirstwastaken2.create.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.fluids.spout.FillingBySpout;
import com.thirstwastaken2.neoforge.WaterFluids;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Items a Spout fills by recipe, such as Miner's Delight's copper cup, which a `create:filling` recipe
 * turns into a water cup. The Spout tries its recipes before {@code GenericItemFilling}, so these never
 * reached {@code GenericItemFillingMixin}, and a cup filled from a tank of sea water came out Clean. Only
 * a result that is a water container is stamped; a copy, in case a recipe hands out its own stack.
 */
@Mixin(value = FillingBySpout.class, remap = false)
abstract class FillingBySpoutMixin {
    @WrapMethod(method = "fillItem")
    private static ItemStack thirst$stampFilled(Level level, int requiredAmount, ItemStack stack,
                                                FluidStack availableFluid, Operation<ItemStack> original) {
        WaterQuality quality = WaterFluids.isWater(availableFluid) ? WaterFluids.quality(availableFluid) : null;
        ItemStack filled = original.call(level, requiredAmount, stack, availableFluid);
        return quality == null ? filled : WaterFluids.stampContainer(filled.copy(), quality);
    }
}
