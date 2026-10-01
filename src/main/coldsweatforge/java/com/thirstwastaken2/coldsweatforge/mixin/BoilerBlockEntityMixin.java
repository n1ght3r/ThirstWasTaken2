package com.thirstwastaken2.coldsweatforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.momosoftworks.coldsweat.common.blockentity.BoilerBlockEntity;
import com.momosoftworks.coldsweat.config.ConfigSettings;
import com.thirstwastaken2.coldsweatforge.BoiledWater;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The Boiler purifies this mod's water, as Cold Sweat has it purify the other Thirst Was Taken port's:
 * it takes any graded water container into its water slots, and on the beat Cold Sweat's own purifying
 * runs, raises each by one grade up to Pure. Cold Sweat's branch for that port asks for its mod id, which
 * is not this mod's, so it never runs here and the two cannot both raise a stack.
 *
 * <p>{@code canPlaceItemThroughFace} and {@code ItemStack.is} are Minecraft's and remapped; the tick, the
 * item check and the field are Cold Sweat's own, so those are {@code remap = false}.
 */
@Mixin(BoilerBlockEntity.class)
abstract class BoilerBlockEntityMixin {
    @Shadow(remap = false)
    protected boolean hasDrinkables;

    // A hopper or pipe from above; the menu's slot is BoilerSlotMixin.
    @WrapOperation(method = "canPlaceItemThroughFace", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean thirst$acceptWater(ItemStack stack, TagKey<Item> tag, Operation<Boolean> original) {
        return original.call(stack, tag) || BoiledWater.accepts(stack);
    }

    @Inject(method = "tick(Lnet/minecraft/world/level/Level;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;)V",
            at = @At("TAIL"), remap = false)
    private void thirst$purify(Level level, BlockState state, BlockPos pos, CallbackInfo ci) {
        BoilerBlockEntity boiler = (BoilerBlockEntity) (Object) this;
        if (level.isClientSide() || boiler.getFuel() <= 0) return;
        // Cold Sweat's own interval, arithmetic and all: every 200 ticks at the default temperature rate.
        if (boiler.ticksExisted % (200 / Math.max(1, ConfigSettings.TEMP_RATE.get())) != 0) return;
        if (BoiledWater.purify(boiler)) hasDrinkables = true;
    }

    // Keeps the Boiler lit, and burning fuel, while it has water to raise, as its own drinkables do.
    @Inject(method = "checkForItems", at = @At("TAIL"), remap = false)
    private void thirst$noticeWater(CallbackInfo ci) {
        if (BoiledWater.hasImpureWater((BoilerBlockEntity) (Object) this)) hasDrinkables = true;
    }
}
