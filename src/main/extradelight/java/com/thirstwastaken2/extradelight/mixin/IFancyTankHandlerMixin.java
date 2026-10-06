package com.thirstwastaken2.extradelight.mixin;

import com.lance5057.extradelight.workstations.IFancyTankHandler;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.thirstwastaken2.neoforge.WaterFluids;
import com.thirstwastaken2.neoforge.WaterQualityFluidHandler;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.ItemCapability;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.capability.IFluidHandlerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The fluid slots of the Vat, the Mixing Bowl and the Chiller, which all share these two methods. A
 * bucket or other container in the input slot pours its water in with its grade, and one in the output
 * slot comes out with the grade of the water it took. Bottles go through the bottle registry, see
 * {@link BottleFluidRegistryMixin}.
 */
@Mixin(IFancyTankHandler.class)
interface IFancyTankHandlerMixin {
    @WrapOperation(method = {"fillInternal", "drainInternal"}, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;getCapability(Lnet/neoforged/neoforge/capabilities/ItemCapability;)Ljava/lang/Object;"))
    private Object thirst$graded(ItemStack container, ItemCapability<?, ?> capability, Operation<Object> original) {
        Object handler = original.call(container, capability);
        return handler instanceof IFluidHandlerItem item ? WaterQualityFluidHandler.of(item) : handler;
    }

    /** Read before the tank is drained: the last bucket empties it. */
    @Inject(method = "drainInternal", at = @At("HEAD"))
    private void thirst$tankWater(BlockEntity blockEntity, CallbackInfo ci, @Share("tank") LocalRef<WaterQuality> tank) {
        FluidStack held = ((IFancyTankHandler<?>) this).getFluidTank().getFluid();
        tank.set(WaterFluids.isWater(held) ? WaterFluids.quality(held) : null);
    }

    /**
     * The bucket branch builds each bucket from the fluid's type, plain; it gets the grade of the water
     * drained into it. Every other container this drops is stamped already, with the same grade.
     */
    @ModifyArg(method = "drainInternal", at = @At(value = "INVOKE",
            target = "Lcom/lance5057/extradelight/util/BlockEntityUtils$Inventory;dropItemInWorld(Lnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)V"),
            index = 0)
    private ItemStack thirst$gradedBucket(ItemStack dropped, @Share("tank") LocalRef<WaterQuality> tank) {
        return WaterFluids.stampContainer(dropped, tank.get());
    }
}
