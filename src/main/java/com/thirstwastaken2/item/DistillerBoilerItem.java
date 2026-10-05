package com.thirstwastaken2.item;

import com.thirstwastaken2.block.DistillerPartBlock;
import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * The distiller boiler's item. Placed on the top of a brick firebox it does not stack a second block
 * on it but sets itself into the firebox: the two become one block, the distiller's boiler half before
 * its pipe, facing as the firebox faces. Anywhere else it places the lone boiler.
 */
public final class DistillerBoilerItem extends BlockItem {
    public DistillerBoilerItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockState firebox = level.getBlockState(pos);
        Player player = context.getPlayer();
        if (context.getClickedFace() != Direction.UP || !firebox.is(ThirstBlocks.BRICK_FIREBOX)
                || player != null && !player.mayUseItemAt(pos, Direction.UP, context.getItemInHand())) {
            return super.useOn(context);
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        BlockState merged = ThirstBlocks.COPPER_DISTILLER.unpiped(firebox.getValue(DistillerPartBlock.FACING));
        level.setBlock(pos, merged, Block.UPDATE_ALL);
        SoundType sound = merged.getSoundType();
        level.playSound(null, pos, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(player, GameEvent.BLOCK_PLACE, pos);
        if (player == null || !Vanilla.hasInfiniteMaterials(player)) context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }
}
