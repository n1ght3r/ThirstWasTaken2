package com.thirstwastaken2.hearthandharvestforge.mixin;

import alabaster.hearthandharvest.common.fluid.HHFluidHandling;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.datafixers.util.Pair;
import com.thirstwastaken2.forge.WaterContainerFluids;
import com.thirstwastaken2.forge.WaterFluids;
import com.thirstwastaken2.hearthandharvestforge.HearthWater;
import com.thirstwastaken2.hearthandharvestforge.StampingTank;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;

/**
 * The one place every Hearth and Harvest tank a player clicks pours and draws through, from 1.4.1 on
 * 1.20.1: the Sink, the placed Jug, the Trough, the Stomping Basin, and the Sprinkler through
 * {@code SprinklerBlockMixin}. It knew a water bottle by its potion alone, poured it in as plain water,
 * and drew plain bottles back out, so a Dirty or salty bottle came back Clean. Now the water keeps its
 * grade both ways, and the tank's own NBT comparison keeps two grades apart. As on 1.21.1.
 */
@Mixin(value = HHFluidHandling.class, remap = false)
abstract class FluidHandlingMixin {
    /**
     * A stamped container is handed over unstamped, so that the bottle test and Forge's bucket wrapper
     * still know it, against a tank that sees the grade it is about to receive; the water that comes back
     * is stamped with it. The mod's own waterskin, canteens and bowls carry the grade on their capability
     * already and go through as they are.
     */
    @WrapMethod(method = "testEmptying")
    private static Pair<FluidStack, ItemStack> thirst$pourWithGrade(ItemStack input, IFluidHandler tank,
                                                                   Operation<Pair<FluidStack, ItemStack>> original) {
        if (WaterContainerFluids.handles(input) || !WaterPurity.isWaterContainer(input)) return original.call(input, tank);
        WaterQuality quality = WaterPurity.quality(input);
        Pair<FluidStack, ItemStack> poured = original.call(WaterPurity.unstamped(input), StampingTank.of(tank, quality));
        if (poured == null || !WaterFluids.isWater(poured.getFirst())) return poured;
        return Pair.of(HearthWater.stamped(poured.getFirst(), quality), poured.getSecond());
    }

    /** What is drawn from a tank of water is stamped with that water's grade, Clean if it has none. */
    @WrapMethod(method = "testFilling")
    private static Pair<Integer, ItemStack> thirst$drawWithGrade(ItemStack input, IFluidHandler tank,
                                                                Operation<Pair<Integer, ItemStack>> original) {
        Pair<Integer, ItemStack> drawn = original.call(input, tank);
        if (drawn == null || drawn.getFirst() <= 0 || tank.getTanks() == 0) return drawn;
        FluidStack held = tank.getFluidInTank(0);
        if (!WaterFluids.isWater(held)) return drawn;
        return Pair.of(drawn.getFirst(), WaterFluids.stampContainer(drawn.getSecond(), WaterFluids.quality(held)));
    }
}
