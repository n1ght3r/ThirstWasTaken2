package com.thirstwastaken2.create;

import com.thirstwastaken2.neoforge.SampledWater;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Map;
import java.util.WeakHashMap;

/**
 * The quality of the water at every open pipe end Create Pipes n Physics probes, by position.
 *
 * <p>{@link SampledWater} remembers one position, which suits a pipe; the addon's engine probes every
 * open end of a network each tick from one static method, so this keeps one answer per position for the
 * same {@link SampledWater#RESAMPLE_TICKS}, and drops those older than that. A cauldron's quality is a
 * blockstate read and is never kept, as in {@link SampledWater}. Server thread only, like the engine.
 */
public final class IntakeSamples {
    private static final Map<Level, Samples> BY_LEVEL = new WeakHashMap<>();

    private IntakeSamples() { }

    /** What water drawn from {@code pos} is, or {@code null} when there is no water there. */
    public static WaterQuality at(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        WaterQuality stored = WaterPurity.storedQuality(state);
        if (stored != null) return stored;
        if (!state.getFluidState().is(FluidTags.WATER)) return null;

        long now = level.getGameTime();
        Samples samples = BY_LEVEL.computeIfAbsent(level, key -> new Samples());
        if (now - samples.prunedAt >= SampledWater.RESAMPLE_TICKS || now < samples.prunedAt) {
            samples.values().removeIf(sample -> now - sample.sampledAt >= SampledWater.RESAMPLE_TICKS);
            samples.prunedAt = now;
        }
        long key = pos.asLong();
        Sample sample = samples.get(key);
        if (sample == null || now - sample.sampledAt >= SampledWater.RESAMPLE_TICKS) {
            sample = new Sample(WaterPurity.sampleAt(level, pos), now);
            samples.put(key, sample);
        }
        return sample.quality;
    }

    private record Sample(WaterQuality quality, long sampledAt) { }

    private static final class Samples extends Long2ObjectOpenHashMap<Sample> {
        private long prunedAt;
    }
}
