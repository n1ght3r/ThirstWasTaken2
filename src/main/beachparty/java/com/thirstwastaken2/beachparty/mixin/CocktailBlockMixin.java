package com.thirstwastaken2.beachparty.mixin;

import com.thirstwastaken2.api.ThirstApi;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.satisfy.beachparty.core.block.CocktailBlock;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A cocktail placed from one glass is sipped three times with an empty hand, each sip an effect, before
 * the glass breaks. The sips went through the block, so they restored no thirst. Now each restores a
 * third of what the glass restores drunk from the hand, rounded, at least one point of thirst: three
 * sips are about one glass, so placing it is a way to share it, not to triple it.
 */
@Mixin(CocktailBlock.class)
abstract class CocktailBlockMixin {
    // The one setBlock in the method lowers the stage after a sip, on the server only. The glass breaking
    // on the fourth click goes through destroyBlock and is no sip.
    @Inject(method = "useWithoutItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/Level;setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;I)Z"))
    private void thirst$sip(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit,
                            CallbackInfoReturnable<InteractionResult> cir) {
        int[] glass = ThirstApi.thirstValues(state.getBlock().asItem());
        if (glass == null) return;
        // A placed glass is three sips.
        ThirstApi.drink(player, Math.max(1, Math.round(glass[0] / 3.0F)), Math.round(glass[1] / 3.0F));
    }
}
