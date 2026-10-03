package com.thirstwastaken2.hearthandharvest.mixin;

import alabaster.hearthandharvest.common.fluid.HHFluidHandling;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.datafixers.util.Pair;
import com.thirstwastaken2.hearthandharvest.HearthWater;
import com.thirstwastaken2.hearthandharvest.StampingTank;
import com.thirstwastaken2.neoforge.WaterContainerFluids;
import com.thirstwastaken2.neoforge.WaterFluids;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;

/**
 * The one place every Hearth and Harvest tank a player clicks pours and draws through: the Sink, the
 * Jug, the Trough, the Stomping Basin, and the Sprinkler through {@code SprinklerBlockMixin}. It knew a
 * water bottle by its potion alone, poured it in as plain water, and drew plain bottles back out, so a
 * Dirty or salty bottle came back Clean. Now the water keeps its grade both ways, and the tank's own
 * component comparison keeps two grades apart, as Create's and Sophisticated's tanks do.
 */
@Mixin(HHFluidHandling.class)
abstract class FluidHandlingMixin {
    /**
     * A stamped container is handed over unstamped, so that the bottle test and NeoForge's bucket wrapper
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
        if (!WaterFluids.isWater(poured.getFirst())) return poured;
        return Pair.of(HearthWater.stamped(poured.getFirst(), quality), poured.getSecond());
    }

    /** What is drawn from a tank of water is stamped with that water's grade, Clean if it has none. */
    @WrapMethod(method = "testFilling")
    private static Pair<Integer, ItemStack> thirst$drawWithGrade(ItemStack input, IFluidHandler tank,
                                                                Operation<Pair<Integer, ItemStack>> original) {
        Pair<Integer, ItemStack> drawn = original.call(input, tank);
        if (drawn.getFirst() <= 0 || tank.getTanks() == 0) return drawn;
        FluidStack held = tank.getFluidInTank(0);
        if (!WaterFluids.isWater(held)) return drawn;
        return Pair.of(drawn.getFirst(), WaterFluids.stampContainer(drawn.getSecond(), WaterFluids.quality(held)));
    }
}
