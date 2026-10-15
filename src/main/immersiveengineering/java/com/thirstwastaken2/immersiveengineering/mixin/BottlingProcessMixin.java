package com.thirstwastaken2.immersiveengineering.mixin;

import blusunrize.immersiveengineering.common.blocks.multiblocks.logic.bottling_machine.BottlingProcess;
import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.thirstwastaken2.neoforge.WaterFluids;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.templates.FluidTank;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

import java.util.ArrayList;
import java.util.List;

/**
 * A glass bottle the Bottling Machine fills by recipe comes out a plain water bottle, which reads as the
 * default grade, so a machine fed sea water made Clean bottles. Each water container a recipe hands out
 * is stamped with the grade of the water it spent.
 *
 * <p>The water is spent partway through the process and the bottle handed out at its end, so the grade
 * is kept from the drain. A process loaded from a save after its drain has lost it and reads the tank
 * instead. A container the machine fills through its fluid capability, a bucket or a waterskin, takes no
 * recipe and is stamped by that capability.
 */
@Mixin(value = BottlingProcess.class, remap = false)
abstract class BottlingProcessMixin {
    @Shadow
    @Final
    private boolean isFilling;

    @Shadow
    @Final
    private FluidTank tank;

    @Unique
    private WaterQuality thirst$spent;

    @ModifyExpressionValue(method = "doProcessTick",
            at = @At(value = "INVOKE", target = "Lnet/neoforged/neoforge/fluids/capability/templates/FluidTank;drain(ILnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/neoforged/neoforge/fluids/FluidStack;"))
    private FluidStack thirst$keepSpent(FluidStack drained) {
        thirst$spent = WaterFluids.isWater(drained) ? WaterFluids.quality(drained) : null;
        return drained;
    }

    /** By full descriptor, so the bridge the compiler adds for the generic override is left alone. */
    @ModifyReturnValue(method = "getRecipeItemOutputs(Lnet/minecraft/world/level/Level;Lblusunrize/immersiveengineering/common/blocks/multiblocks/process/ProcessContext$ProcessContextInWorld;)Ljava/util/List;",
            at = @At("RETURN"))
    private List<ItemStack> thirst$stampBottled(List<ItemStack> outputs) {
        if (isFilling || outputs == null) return outputs;
        List<ItemStack> stamped = null;
        for (int i = 0; i < outputs.size(); i++) {
            ItemStack output = outputs.get(i);
            if (!WaterPurity.isWaterContainer(output)) continue;
            WaterQuality quality = thirst$spent;
            if (quality == null) {
                FluidStack inTank = tank.getFluid();
                if (!WaterFluids.isWater(inTank)) return outputs;
                quality = WaterFluids.quality(inTank);
            }
            if (stamped == null) stamped = new ArrayList<>(outputs);
            // A copy, since a recipe may hand out its own stack.
            stamped.set(i, WaterFluids.stampContainer(output.copy(), quality));
        }
        return stamped == null ? outputs : stamped;
    }
}
