package com.thirstwastaken2.kaleidoscope;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.state.BlockState;

import java.util.function.Consumer;
import java.util.function.ObjIntConsumer;
import java.util.function.Supplier;

/**
 * The only place that reads a grade off what goes into a stockpot or a teapot, and reads and writes it on
 * the block entity and on a teapot item. Both blocks keep only a fluid id of their own, so without this
 * a Dirty bucket poured in came back out at {@code defaultQuality} and sea water came back out fresh.
 */
public final class BrewedWaterQuality {
    /** One int, {@link WaterPurity#storedValue}: 1-4 for the four grades, 5 for sea water, absent for none. */
    private static final String KEY = ThirstWasTaken2.MOD_ID + ":water_quality";

    /** The fluid id both blocks keep for water: the stockpot's water soup base and the teapot's tea fluid. */
    private static final Identifier WATER = Identifier.withDefaultNamespace("water");

    private BrewedWaterQuality() { }

    /** Whether a fluid id one of the blocks keeps is water. */
    public static boolean isWater(Identifier fluidId) {
        return WATER.equals(fluidId);
    }

    /** The grade of the water in {@code stack}, or {@code null} when it is not a water container. Read it before the call that spends it. */
    public static WaterQuality of(ItemStack stack) {
        return WaterPurity.isWaterContainer(stack) ? WaterPurity.quality(stack) : null;
    }

    /** The grade of the water at {@code pos}, sampled as a bucket filled there is, or {@code null} when there is no water. */
    public static WaterQuality sample(LevelAccessor level, BlockPos pos, BlockState state) {
        if (!(level instanceof Level world) || !state.getFluidState().is(FluidTags.WATER)) return null;
        return WaterPurity.sampleAt(world, pos);
    }

    /**
     * An empty teapot item about to scoop the water at {@code pos} out of the world: hands the water's
     * grade to {@code scooped}, sea water included, and lets {@code pickup} take it. Sampled before the
     * pickup, which takes the source block away.
     */
    public static ItemStack scoop(LivingEntity user, LevelAccessor level, BlockPos pos, BlockState state,
                                  Consumer<WaterQuality> scooped, Supplier<ItemStack> pickup) {
        scooped.accept(sample(level, pos, state));
        return pickup.get();
    }

    /** Whether a block holds sea water: {@code held} is its {@code thirst$heldWater}, null for none. */
    public static boolean isSalt(WaterQuality held) {
        return held != null && held.salty();
    }

    /** Adds {@code quality} to the block entity data a teapot item carries, which the teapot loads once placed. */
    public static void stampItem(ItemStack teapot, WaterQuality quality) {
        if (quality != null) Vanilla.putBlockEntityInt(teapot, KEY, WaterPurity.storedValue(quality));
    }

    /** {@link #stampItem}, handing {@code result} back, for a {@code @ModifyReturnValue} on what a teapot's use returns. */
    public static <T> T stamped(T result, ItemStack teapot, WaterQuality quality) {
        stampItem(teapot, quality);
        return result;
    }

    /**
     * Writes {@code quality} through {@code putInt}, a {@code CompoundTag}'s on 1.21.1 or a
     * {@code ValueOutput}'s from 1.21.6, so the mixins' version forks differ only in their signature.
     */
    public static void save(ObjIntConsumer<String> putInt, WaterQuality quality) {
        if (quality != null) putInt.accept(KEY, WaterPurity.storedValue(quality));
    }

    /** {@link #save}, handing {@code data} back, for a {@code @ModifyArg} on what a teapot item is given. */
    public static <T> T saved(T data, ObjIntConsumer<String> putInt, WaterQuality quality) {
        save(putInt, quality);
        return data;
    }

    /**
     * What {@link #save} wrote, or {@code null}: a block filled before the integration existed has no
     * grade to keep. {@code getIntOr} is a {@code ValueInput}'s, or {@code Vanilla.getInt} over a tag.
     */
    public static WaterQuality load(IntReader getIntOr) {
        int stored = getIntOr.read(KEY, WaterPurity.BLOCK_UNSET);
        if (stored == WaterPurity.BLOCK_SALT) return WaterQuality.SALT;
        return stored > WaterPurity.BLOCK_UNSET ? WaterQuality.fresh(stored - 1) : null;
    }

    /** An int under a key, or the fallback when there is none. */
    @FunctionalInterface
    public interface IntReader {
        int read(String key, int fallback);
    }
}
