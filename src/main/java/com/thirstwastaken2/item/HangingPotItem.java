package com.thirstwastaken2.item;

import com.thirstwastaken2.block.HangingPotBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;

/**
 * A hanging pot's item. A pot hangs a block above the floor, its stand's legs reaching down past the block
 * between, which is where its campfire goes. Placed on a campfire it goes right on top; placed on a plain
 * floor it goes a block up, leaving that block free, so a campfire can be put under it afterwards.
 */
public final class HangingPotItem extends BlockItem {
    public HangingPotItem(HangingPotBlock block, Properties properties) {
        super(block, properties);
    }

    /** Where the pot goes instead of where the player clicked, or {@code null} when the block above is taken. */
    @Override
    public BlockPlaceContext updatePlacementContext(BlockPlaceContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        BlockPos below = pos.below();
        if (level.getBlockState(below).is(BlockTags.CAMPFIRES)
                || !Block.canSupportCenter(level, below, Direction.UP)) {
            return context;
        }
        BlockPos above = pos.above();
        if (!level.getBlockState(above).canBeReplaced()) return null;
        return BlockPlaceContext.at(context, above, context.getClickedFace());
    }
}
