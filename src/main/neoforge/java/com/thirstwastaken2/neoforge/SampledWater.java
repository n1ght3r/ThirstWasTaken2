package com.thirstwastaken2.neoforge;

import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The quality of the water one pump draws, remembered for a few seconds: a Create pump or hose
 * pulley, or a Sophisticated Pump upgrade.
 *
 * <p>Collecting water is where the mod samples it, and a pipe collects every tick it pulls. The
 * neighbourhood scan behind {@link WaterPurity#sampleAt} is kept off that tick path by reusing its
 * answer for {@link #RESAMPLE_TICKS}; a cauldron's stored quality is only a blockstate read, so it is
 * never reused, and a block with no water in it is not sampled at all.
 *
 * <p>It holds a position and a quality, nothing that keeps a level or a chunk alive.
 */
public final class SampledWater {
    public static final int RESAMPLE_TICKS = 100;

    private WaterQuality quality;
    private BlockPos pos;
    private long sampledAt;

    /** What water drawn from {@code pos} is, or {@code null} when there is no water there. */
    public WaterQuality at(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        WaterQuality stored = WaterPurity.storedQuality(state);
        if (stored != null) return stored;
        if (!state.getFluidState().is(FluidTags.WATER)) return null;

        long now = level.getGameTime();
        if (quality == null || !pos.equals(this.pos) || now - sampledAt >= RESAMPLE_TICKS) {
            quality = WaterPurity.sampleAt(level, pos);
            this.pos = pos.immutable();
            sampledAt = now;
        }
        return quality;
    }
}
