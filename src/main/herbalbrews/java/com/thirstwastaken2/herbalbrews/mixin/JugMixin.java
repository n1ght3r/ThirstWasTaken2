package com.thirstwastaken2.herbalbrews.mixin;

import com.thirstwastaken2.data.ThirstManager;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.satisfy.herbalbrews.core.blocks.JugBlock;
import net.satisfy.herbalbrews.core.blocks.entity.JugBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A placed Jug holds up to three teas or coffees, and an empty hand drinks them all at once: their
 * effects, then the jug is empty. They went through the block, not the item, so they restored no
 * thirst. Now each one is drunk as the item it was, through the call a tea drunk from the hand goes
 * through, just before the jug forgets them: three teas from a jug are three teas.
 */
@Mixin(JugBlock.class)
abstract class JugMixin {
    // clearDrinks is HerbalBrews' own and takes no Minecraft type, so it is not remapped. The block
    // returns on the client before it gets here.
    @Inject(method = "useItemOn", at = @At(value = "INVOKE", remap = false,
            target = "Lnet/satisfy/herbalbrews/core/blocks/entity/JugBlockEntity;clearDrinks()V"))
    private void thirst$drinkEachCup(ItemStack held, BlockState state, Level level, BlockPos pos, Player player,
                                     InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<ItemInteractionResult> cir) {
        if (!(level.getBlockEntity(pos) instanceof JugBlockEntity jug)) return;
        for (ItemStack cup : jug.getDrinks()) ThirstManager.drinkItem(player, cup);
    }
}
