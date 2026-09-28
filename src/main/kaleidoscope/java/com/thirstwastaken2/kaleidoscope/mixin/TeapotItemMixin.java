package com.thirstwastaken2.kaleidoscope.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.item.TeapotItem;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Share;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import com.thirstwastaken2.kaleidoscope.BrewedWaterQuality;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BucketPickup;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * An empty teapot held in the hand scoops water straight out of the world, the way a bucket does, and
 * writes only the fluid id into the item. The water is sampled where it lay, as a bucket's is, sea
 * water included, which the teapot keeps salty and brews nothing from. Without this a teapot dipped in
 * the sea and emptied into a bucket turned sea water fresh.
 */
@Mixin(value = TeapotItem.class, remap = false)
abstract class TeapotItemMixin {
    // Sampled before the call: it takes the source block away.
    // Item.use and pickupBlock are Minecraft's, so remapped; pickupBlock took the Player itself on 1.21.1.
    //? if >1.21.1 {
    @WrapOperation(method = "use", remap = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack thirst$sampleScooped(BucketPickup pickup, LivingEntity user, LevelAccessor level, BlockPos pos,
                                          BlockState state, Operation<ItemStack> original,
                                          @Share("scooped") LocalRef<WaterQuality> scooped) {
        return BrewedWaterQuality.scoop(user, level, pos, state, scooped::set, () -> original.call(pickup, user, level, pos, state));
    }
    //?} elif >=1.20.5 {
    /*@WrapOperation(method = "use", remap = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack thirst$sampleScooped(BucketPickup pickup, net.minecraft.world.entity.player.Player user, LevelAccessor level, BlockPos pos,
                                          BlockState state, Operation<ItemStack> original,
                                          @Share("scooped") LocalRef<WaterQuality> scooped) {
        return BrewedWaterQuality.scoop(user, level, pos, state, scooped::set, () -> original.call(pickup, user, level, pos, state));
    }
    *///?} else {
    /*@WrapOperation(method = "use", remap = true, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/BucketPickup;pickupBlock(Lnet/minecraft/world/level/LevelAccessor;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack thirst$sampleScooped(BucketPickup pickup, LevelAccessor level, BlockPos pos, BlockState state,
                                          Operation<ItemStack> original, @com.llamalad7.mixinextras.sugar.Local(argsOnly = true) net.minecraft.world.entity.player.Player user,
                                          @Share("scooped") LocalRef<WaterQuality> scooped) {
        return BrewedWaterQuality.scoop(user, level, pos, state, scooped::set, () -> original.call(pickup, level, pos, state));
    }
    *///?}

    // How the scooped water reaches the teapot. From 1.20.5 through fillFluid, the mod's own, matched by
    // name alone so its descriptor needs no remapping. The 1.20.1 Forge build fills through a fluid
    // capability and never calls it, so on 1.20.1 the teapot use hands back is stamped, on both loaders.
    //? if >=1.20.5 {
    @WrapOperation(method = "use", remap = true, at = @At(value = "INVOKE", remap = false,
            target = "Lcom/github/ysbbbbbb/kaleidoscopecookery/item/TeapotItem;fillFluid"))
    private boolean thirst$stampScooped(ItemStack teapot, Fluid fluid, LivingEntity user, Operation<Boolean> original,
                                        @Share("scooped") LocalRef<WaterQuality> scooped) {
        boolean filled = original.call(teapot, fluid, user);
        if (filled) BrewedWaterQuality.stampItem(teapot, scooped.get());
        return filled;
    }
    //?} else {
    /*@com.llamalad7.mixinextras.injector.ModifyReturnValue(method = "use", at = @At("RETURN"), remap = true)
    private net.minecraft.world.InteractionResultHolder<ItemStack> thirst$stampScooped(
            net.minecraft.world.InteractionResultHolder<ItemStack> result, @Share("scooped") LocalRef<WaterQuality> scooped) {
        return BrewedWaterQuality.stamped(result, result.getObject(), scooped.get());
    }
    *///?}
}
