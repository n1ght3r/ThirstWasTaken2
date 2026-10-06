package com.thirstwastaken2.extradelight.mixin;

import com.lance5057.extradelight.workstations.chiller.ChillerBlockEntity;
import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.thirstwastaken2.extradelight.ExtraDelightWater;
import com.thirstwastaken2.neoforge.WaterFluids;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Chiller's drip tray fills with the ice melting while it chills. That is meltwater, Clean by
 * default, rather than water with no grade; and a bucket or bottle drawn from the tray, which the
 * Chiller builds itself, gets the grade of the water in it.
 */
@Mixin(ChillerBlockEntity.class)
abstract class ChillerBlockEntityMixin {
    @ModifyArg(method = "tick", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/fluids/capability/templates/FluidTank;fill(Lnet/neoforged/neoforge/fluids/FluidStack;Lnet/neoforged/neoforge/fluids/capability/IFluidHandler$FluidAction;)I"))
    private static FluidStack thirst$meltwater(FluidStack water) {
        return ExtraDelightWater.stamped(water, ExtraDelightWater.meltwater());
    }

    /** Read before the tray is drained: a full bucket empties it. */
    @Inject(method = "drainDripTray", at = @At("HEAD"))
    private static void thirst$trayWater(ChillerBlockEntity chiller, CallbackInfo ci,
                                         @Share("tray") LocalRef<WaterQuality> tray) {
        FluidStack held = chiller.getDripTray().getFluid();
        tray.set(WaterFluids.isWater(held) ? WaterFluids.quality(held) : null);
    }

    @ModifyArg(method = "drainDripTray", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/items/ItemStackHandler;setStackInSlot(ILnet/minecraft/world/item/ItemStack;)V"),
            index = 1)
    private static ItemStack thirst$bucket(ItemStack bucket, @Share("tray") LocalRef<WaterQuality> tray) {
        return WaterFluids.stampContainer(bucket, tray.get());
    }

    @ModifyArg(method = "drainDripTray", at = @At(value = "INVOKE",
            target = "Lcom/lance5057/extradelight/util/BlockEntityUtils$Inventory;dropItemInWorld(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"),
            index = 0)
    private static ItemStack thirst$bottle(ItemStack bottle, @Share("tray") LocalRef<WaterQuality> tray) {
        return WaterFluids.stampContainer(bottle, tray.get());
    }
}
