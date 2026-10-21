package com.thirstwastaken2.hearthandharvest.mixin;

import alabaster.hearthandharvest.common.block.entity.KegBlockEntity;
import alabaster.hearthandharvest.common.crafting.KegRecipe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thirstwastaken2.hearthandharvest.HearthWater;
import com.thirstwastaken2.hearthandharvest.StampingTank;
import com.thirstwastaken2.neoforge.WaterContainerFluids;
import com.thirstwastaken2.neoforge.WaterFluids;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Hearth and Harvest 1.4.0's Keg pours and draws through its container slot with NeoForge's
 * {@code FluidUtil}, not {@code HHFluidHandling}, so a Dirty bucket went in as plain water and came back
 * out Clean. The same two wraps as {@link FluidHandlingMixin}: a container pours in with its grade, and
 * what is drawn from a tank of water is stamped with that water's grade. Its recipes match water by
 * fluid alone, so stamped water still ferments; sea water only into brine.
 */
@Mixin(KegBlockEntity.class)
abstract class KegBlockEntityMixin {
    @WrapOperation(method = "transfer", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/fluids/FluidUtil;tryEmptyContainer(Lnet/minecraft/world/item/ItemStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;ILnet/minecraft/world/entity/player/Player;Z)Lnet/neoforged/neoforge/fluids/FluidActionResult;"))
    private FluidActionResult thirst$pourWithGrade(ItemStack input, IFluidHandler tank, int max, Player player,
                                                   boolean execute, Operation<FluidActionResult> original) {
        if (WaterContainerFluids.handles(input) || !WaterPurity.isWaterContainer(input)) {
            return original.call(input, tank, max, player, execute);
        }
        return original.call(WaterPurity.unstamped(input), StampingTank.of(tank, WaterPurity.quality(input)), max, player, execute);
    }

    @WrapOperation(method = "transfer", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/fluids/FluidUtil;tryFillContainer(Lnet/minecraft/world/item/ItemStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler;ILnet/minecraft/world/entity/player/Player;Z)Lnet/neoforged/neoforge/fluids/FluidActionResult;"))
    private FluidActionResult thirst$drawWithGrade(ItemStack container, IFluidHandler tank, int max, Player player,
                                                   boolean execute, Operation<FluidActionResult> original) {
        // Read before the call: drawing the last of it empties the tank.
        FluidStack held = tank.getTanks() == 0 ? FluidStack.EMPTY : tank.getFluidInTank(0).copy();
        FluidActionResult drawn = original.call(container, tank, max, player, execute);
        if (!drawn.isSuccess() || !WaterFluids.isWater(held)) return drawn;
        WaterQuality quality = WaterFluids.quality(held);
        return new FluidActionResult(WaterFluids.stampContainer(drawn.getResult(), quality));
    }

    /**
     * Mead, beer, moonshine and root beer ferment from water matched by fluid alone, so sea water fermented
     * into a drink. As in the Cask, it ferments nothing that is drunk now; brine, salt water anyway, still
     * comes from it.
     */
    @WrapOperation(method = "findRecipe", at = @At(value = "INVOKE",
            target = "Lalabaster/hearthandharvest/common/crafting/KegRecipe;matchesFluid(Lnet/neoforged/neoforge/fluids/FluidStack;)Z"))
    private boolean thirst$noDrinkFromSeaWater(KegRecipe recipe, FluidStack input, Operation<Boolean> original) {
        return original.call(recipe, input) && HearthWater.fermentsFrom(input, recipe.getResultFluid());
    }
}
