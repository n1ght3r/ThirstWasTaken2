package com.thirstwastaken2.createforge.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.simibubi.create.content.fluids.transfer.GenericItemFilling;
import com.thirstwastaken2.item.WaterContainers;
import com.thirstwastaken2.forge.WaterFluids;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraftforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;

/**
 * Bottles and buckets filled by a Spout, or by hand from a Create tank. The quality is read before the
 * fill runs, because the fill spends the fluid and a spent stack reads as empty.
 */
@Mixin(value = GenericItemFilling.class, remap = false)
abstract class GenericItemFillingMixin {
    @WrapMethod(method = "fillItem")
    private static ItemStack thirst$stampFilled(Level level, int requiredAmount, ItemStack stack,
                                                FluidStack availableFluid, Operation<ItemStack> original) {
        WaterQuality quality = WaterFluids.isWater(availableFluid) ? WaterFluids.quality(availableFluid) : null;
        return WaterFluids.stampContainer(original.call(level, requiredAmount, stack, availableFluid), quality);
    }

    /**
     * Asks about one of a stack, as {@code fillItem} already fills one. Create otherwise asks the whole
     * stack's fluid capability, and an item fluid handler refuses a stack of more than one, as a bucket's
     * does, so a Spout over a stack of terracotta bowls filled up and never poured. Only this mod's
     * containers, so another mod's stackable item keeps whatever Create gives it.
     */
    @WrapMethod(method = "getRequiredAmountForItem")
    private static int thirst$askAboutOne(Level level, ItemStack stack, FluidStack availableFluid,
                                          Operation<Integer> original) {
        boolean stacked = stack.getCount() > 1 && WaterContainers.handles(stack);
        return original.call(level, stacked ? stack.copyWithCount(1) : stack, availableFluid);
    }
}
