package com.thirstwastaken2.kaleidoscopetavern.mixin;

import com.github.ysbbbbbb.kaleidoscopetavern.event.VanillaBottlePlaceEvent;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.thirstwastaken2.kaleidoscopetavern.TavernWater;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * A water bottle shift-placed as a block keeps its grade beside it; see {@link TavernWater#placed}. The
 * official mod places it from a NeoForge or Forge event and Refabricated from a {@code UseBlockCallback},
 * two methods of the same name and different parameters, so this matches the name alone and reads the
 * bottle from the one {@code ItemStack} local both keep. The bottle is read before it is shrunk.
 */
@Mixin(value = VanillaBottlePlaceEvent.class, remap = false)
abstract class BottlePlaceMixin {
    @WrapOperation(method = "onRightClickBlock", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private static boolean thirst$keepGrade(Level level, BlockPos pos, BlockState state, int flags,
                                            Operation<Boolean> original, @Local ItemStack bottle) {
        boolean placed = original.call(level, pos, state, flags);
        if (placed) TavernWater.placed(level, pos, state, bottle);
        return placed;
    }
}
