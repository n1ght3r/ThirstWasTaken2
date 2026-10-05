package com.thirstwastaken2.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.IntConsumer;

/**
 * A block that has to react when the block it stands on changes. Vanilla calls that {@code updateShape},
 * and 1.21.2 reordered its parameters and moved tick scheduling onto an argument of its own, so the
 * override lives here once and hands the mod's blocks one signature for every version.
 *
 * <p>It also carries the block codec, which is the other override a mod block used to owe vanilla and
 * which 26.3 removed along with the whole codec. {@code copy} is what rebuilds the block from properties
 * alone; from 26.3 nothing asks for it.
 *
 * <p>A block made of two halves hears about its other half the same way, through {@link #sideChanged},
 * and about a player breaking it through {@link #beforePlayerBreaks}, whose override lost its void
 * return in 1.20.2.
 *
 * <p>A block whose block entity is a container spills what it holds when broken, which vanilla leaves to
 * each block before 1.21.5 and does itself from then on.
 *
 * <p>A class rather than a method for the same reason as {@link DrinkItem}: what differs is an override.
 */
public abstract class SupportedBlock extends Block {
    //? if >=1.20.5 <26.3 {
    /*private final com.mojang.serialization.MapCodec<? extends Block> codec;
    *///?}

    protected SupportedBlock(Properties properties,
                             java.util.function.Function<Properties, ? extends SupportedBlock> copy) {
        super(properties);
        //? if >=1.20.5 <26.3 {
        /*this.codec = simpleCodec(copy::apply);
        *///?}
    }

    //? if >=1.20.5 <26.3 {
    /*@Override
    protected com.mojang.serialization.MapCodec<? extends Block> codec() {
        return codec;
    }
    *///?}

    // Before 1.20.5 whether mobs path through a block is also asked with its level and position. The
    // one block here answers without them, so the older form hands over to the newer one, which says
    // no unless the block says otherwise.
    //? if <1.20.5 {
    /*@Override
    public boolean isPathfindable(BlockState state, net.minecraft.world.level.BlockGetter level, BlockPos pos,
                                  net.minecraft.world.level.pathfinder.PathComputationType type) {
        return isPathfindable(state, type);
    }

    protected boolean isPathfindable(BlockState state, net.minecraft.world.level.pathfinder.PathComputationType type) {
        return false;
    }
    *///?}

    /**
     * The block below {@code pos} is now {@code below}. Returns the state this block should take, which
     * may be the same one.
     *
     * @param scheduleTick schedules this block's {@code tick} that many ticks from now
     */
    protected abstract BlockState supportChanged(BlockState state, LevelReader level, BlockPos pos, BlockState below,
                                                 IntConsumer scheduleTick);

    /**
     * The block beside or above {@code pos}, toward {@code direction}, is now {@code neighbor}. Returns the
     * state this block should take; air breaks it, drops included. Most blocks ignore it.
     */
    protected BlockState sideChanged(BlockState state, Direction direction, BlockState neighbor) {
        return state;
    }

    /** A player is about to break the block at {@code pos}, on both sides, before it is removed. */
    protected void beforePlayerBreaks(Level level, BlockPos pos, BlockState state, Player player) { }

    //? if >=1.20.2 {
    @Override
    public BlockState playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        beforePlayerBreaks(level, pos, state, player);
        return super.playerWillDestroy(level, pos, state, player);
    }
    //?} else {
    /*@Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player) {
        beforePlayerBreaks(level, pos, state, player);
        super.playerWillDestroy(level, pos, state, player);
    }
    *///?}

    // A block entity that is a container spills what it holds when its block goes. From 1.21.5 the
    // block entity does that itself, in preRemoveSideEffects; before it the block's onRemove does, as
    // a chest's or a furnace's does, and only when the block itself is replaced, not a state of it.
    //? if >=1.20.5 <1.21.5 {
    /*@Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        dropContents(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, moved);
    }
    *///?}
    //? if <1.20.5 {
    /*@Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        dropContents(state, level, pos, newState);
        super.onRemove(state, level, pos, newState, moved);
    }
    *///?}
    //? if <1.21.5 {
    /*private static void dropContents(BlockState state, Level level, BlockPos pos, BlockState newState) {
        if (!state.is(newState.getBlock())
                && level.getBlockEntity(pos) instanceof net.minecraft.world.Container container) {
            net.minecraft.world.Containers.dropContents(level, pos, container);
        }
    }
    *///?}

    private BlockState neighborChanged(BlockState state, LevelReader level, BlockPos pos, Direction direction,
                                       BlockState neighbor, IntConsumer scheduleTick) {
        if (direction != Direction.DOWN) return sideChanged(state, direction, neighbor);
        // updateOrDestroy breaks a block that turns into air here, drops included.
        if (!state.canSurvive(level, pos)) return Blocks.AIR.defaultBlockState();
        return supportChanged(state, level, pos, neighbor, scheduleTick);
    }

    //? if >=1.21.2 {
    @Override
    protected BlockState updateShape(BlockState state, LevelReader level,
                                     net.minecraft.world.level.ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighbor,
                                     net.minecraft.util.RandomSource random) {
        return neighborChanged(state, level, pos, direction, neighbor,
                delay -> ticks.scheduleTick(pos, this, delay));
    }
    //?} else {
    /*@Override
    public BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                     net.minecraft.world.level.LevelAccessor level, BlockPos pos,
                                     BlockPos neighborPos) {
        return neighborChanged(state, level, pos, direction, neighbor,
                delay -> level.scheduleTick(pos, this, delay));
    }
    *///?}
}
