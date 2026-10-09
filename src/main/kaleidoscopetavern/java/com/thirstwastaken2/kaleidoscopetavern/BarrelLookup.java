package com.thirstwastaken2.kaleidoscopetavern;

import com.github.ysbbbbbb.kaleidoscopetavern.block.brew.BarrelBlock;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The barrel's water from any of its eight blocks, through the one block entity they share. For the Jade
 * reader, which names no class of the mod's and so hands over through this static call, made only once
 * the gate has said the mod is installed.
 */
public final class BarrelLookup {
    private BarrelLookup() { }

    /** The grade of the water in the barrel {@code state} is part of, or {@code null} for any other block. */
    public static WaterQuality heldWater(Level level, BlockPos pos, BlockState state) {
        if (!(state.getBlock() instanceof BarrelBlock)) return null;
        return BarrelBlock.getBarrelEntity(level, pos, state) instanceof BarrelWater barrel ? barrel.thirst$heldWater() : null;
    }
}
