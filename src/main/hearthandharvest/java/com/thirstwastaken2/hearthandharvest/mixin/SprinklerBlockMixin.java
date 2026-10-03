package com.thirstwastaken2.hearthandharvest.mixin;

import alabaster.hearthandharvest.common.block.SprinklerBlock;
import alabaster.hearthandharvest.common.block.entity.SprinklerBlockEntity;
import alabaster.hearthandharvest.common.fluid.HHFluidHandling;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The Sprinkler is the one Hearth and Harvest tank filled through NeoForge's {@code FluidUtil} rather
 * than the mod's own {@code useOnTank}. Its bucket wrapper pours and draws plain water, so a Dirty or
 * salty bucket came back out Clean. It goes through {@code useOnTank} now, which
 * {@code FluidHandlingMixin} makes keep the grade, like the mod's other tanks. A glass bottle draws from
 * it too, as from those.
 */
@Mixin(SprinklerBlock.class)
abstract class SprinklerBlockMixin {
    @WrapOperation(method = "useItemOn", at = @At(value = "INVOKE",
            target = "Lnet/neoforged/neoforge/fluids/FluidUtil;interactWithFluidHandler(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;)Z"))
    private boolean thirst$keepGrade(Player player, InteractionHand hand, Level level, BlockPos pos, Direction side,
                                     Operation<Boolean> original) {
        if (!(level.getBlockEntity(pos) instanceof SprinklerBlockEntity sprinkler)) {
            return original.call(player, hand, level, pos, side);
        }
        return HHFluidHandling.useOnTank(level, pos, player, hand, sprinkler.getFluidTank(), null)
                != ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }
}
