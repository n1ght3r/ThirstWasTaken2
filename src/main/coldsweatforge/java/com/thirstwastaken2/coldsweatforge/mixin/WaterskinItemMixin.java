package com.thirstwastaken2.coldsweatforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.momosoftworks.coldsweat.common.item.WaterskinItem;
import com.thirstwastaken2.coldsweatforge.WaterskinWater;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cold Sweat's empty waterskin fills with the grade of the water it drew: a source in the world, a
 * cauldron, or a tank. See {@link WaterskinWater} for why the last two are noted before the fill.
 *
 * <p>{@code useOn} is Minecraft's and remapped through the refmap; the lambda, {@code getFilledItem} and
 * Forge's fluid handler are not Minecraft's and keep their names under SRG, so those are {@code remap = false}.
 */
@Mixin(WaterskinItem.class)
abstract class WaterskinItemMixin {
    @Inject(method = "useOn", at = @At("HEAD"))
    private void thirst$noteCauldron(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        Level level = context.getLevel();
        BlockState state = level.getBlockState(context.getClickedPos());
        if (!level.isClientSide() && state.is(Blocks.WATER_CAULDRON)) WaterskinWater.fromCauldron(state);
    }

    @Inject(method = "useOn", at = @At("RETURN"))
    private void thirst$forgetNote(UseOnContext context, CallbackInfoReturnable<InteractionResult> cir) {
        WaterskinWater.clear();
    }

    // The tank branch of useOn, a lambda handed the block entity's fluid capability.
    @WrapOperation(method = "lambda$useOn$0", remap = false, at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/fluids/capability/IFluidHandler;drain(Lnet/minecraftforge/fluids/FluidStack;Lnet/minecraftforge/fluids/capability/IFluidHandler$FluidAction;)Lnet/minecraftforge/fluids/FluidStack;"))
    private static FluidStack thirst$noteTank(IFluidHandler handler, FluidStack request, IFluidHandler.FluidAction action,
                                              Operation<FluidStack> original) {
        FluidStack drained = original.call(handler, request, action);
        WaterskinWater.fromFluid(drained);
        return drained;
    }

    @Inject(method = "getFilledItem", remap = false, at = @At("RETURN"))
    private static void thirst$stampFilled(ItemStack empty, Level level, BlockPos pos, CallbackInfoReturnable<ItemStack> cir) {
        WaterskinWater.stamp(cir.getReturnValue(), level, pos);
    }
}
