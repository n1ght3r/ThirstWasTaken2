package com.thirstwastaken2.item;

import com.thirstwastaken2.block.CoolingTubBlock;
import com.thirstwastaken2.block.DistillerBlock;
import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * The copper pipe, the swan neck that finishes a distiller built in the world. Used on a boiler set on
 * its firebox, or on the cooling tub, while the tub stands to the boiler's right as seen from the front
 * and faces the same way, it is spent and the two become a whole copper distiller, the same block a
 * crafted one is. The tub keeps its coolant. Anything else it is used on, nothing happens.
 */
public final class CopperPipeItem extends Item {
    public CopperPipeItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos clicked = context.getClickedPos();
        BlockState state = level.getBlockState(clicked);
        BlockPos boiler;
        BlockPos tub;
        if (state.is(ThirstBlocks.COPPER_DISTILLER) && !DistillerBlock.isWhole(state)) {
            boiler = clicked;
            tub = clicked.relative(DistillerBlock.towardOtherHalf(state));
        } else if (state.is(ThirstBlocks.COOLING_TUB)) {
            // The way from a tub half to its boiler, which is where a boiler would have to stand.
            boiler = clicked.relative(state.getValue(CoolingTubBlock.FACING).getClockWise());
            tub = clicked;
        } else {
            return InteractionResult.PASS;
        }
        BlockState boilerState = level.getBlockState(boiler);
        BlockState tubState = level.getBlockState(tub);
        if (!boilerState.is(ThirstBlocks.COPPER_DISTILLER) || DistillerBlock.isWhole(boilerState)
                || !tubState.is(ThirstBlocks.COOLING_TUB)) {
            return InteractionResult.PASS;
        }
        Direction facing = boilerState.getValue(DistillerBlock.FACING);
        Player player = context.getPlayer();
        if (tubState.getValue(CoolingTubBlock.FACING) != facing
                || player != null && !player.mayUseItemAt(clicked, context.getClickedFace(), context.getItemInHand())) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) return InteractionResult.SUCCESS;

        // The boiler first: the tub turning into the other half is what it then checks itself against.
        BlockState whole = boilerState.setValue(DistillerBlock.PIPED, true);
        level.setBlock(boiler, whole, Block.UPDATE_ALL);
        level.setBlock(tub, whole.setValue(DistillerBlock.PART, DistillerBlock.Part.TUB)
                .setValue(DistillerBlock.COOLED, tubState.getValue(CoolingTubBlock.COOLED)), Block.UPDATE_ALL);
        SoundType sound = whole.getSoundType();
        level.playSound(null, clicked, sound.getPlaceSound(), SoundSource.BLOCKS,
                (sound.getVolume() + 1.0F) / 2.0F, sound.getPitch() * 0.8F);
        level.gameEvent(player, GameEvent.BLOCK_CHANGE, boiler);
        if (player == null || !Vanilla.hasInfiniteMaterials(player)) context.getItemInHand().shrink(1);
        return InteractionResult.SUCCESS_SERVER;
    }
}
