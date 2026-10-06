package com.thirstwastaken2.farmandcharm;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.material.FluidState;
import net.satisfy.farm_and_charm.core.block.TimberWellBlock;

/**
 * The grades Farm & Charm's water blocks hand out. None of them can keep one: the well and the trough
 * hold a level, the sink a filled flag. So the grade is decided as the water comes out, from where it
 * came from rather than from what was poured in, which also means nothing gets better by going through.
 *
 * <p>Loaded only from the mixins, which only apply with Farm & Charm installed.
 */
public final class StandingWater {
    /**
     * What a Water Trough and a kitchen sink give: Murky. Water left standing in an open trough or a basin
     * is not clean, and the sink fills from nothing, so an endless tap of Clean water would make
     * purifying pointless. One constant, so that Farm & Charm and Candlelight agree.
     */
    public static final WaterQuality STANDING = WaterQuality.fresh(1);

    private StandingWater() { }

    /** Whether {@code stack} is a water bucket holding sea water, which the trough and the sink refuse. */
    public static boolean isSaltyBucket(ItemStack stack) {
        return stack.is(Items.WATER_BUCKET) && WaterPurity.isSalty(stack);
    }

    /** Stamps {@code stack} with {@link #STANDING} if it is a water container, and hands it back. */
    public static ItemStack standing(ItemStack stack) {
        if (WaterPurity.isWaterContainer(stack)) WaterPurity.setQuality(stack, STANDING);
        return stack;
    }

    /**
     * The water a bucket draws from the Timber Well at {@code pos}: the groundwater the well pumps from,
     * sampled where it lies, so a beach well gives sea water and a swamp well Dirty water. With no
     * groundwater below, the well was filled by rain, and gives rain's grade. Null leaves the bucket
     * unstamped, which is {@code defaultQuality}: rain collection switched off.
     *
     * <p>If both groundwater and rain filled it, the groundwater decides, since the blockstate cannot say
     * which level came from where. Called on the server, when a bucket is filled, never on a tick.
     */
    public static WaterQuality wellWater(Level level, BlockPos pos, BlockState state) {
        BlockPos source = groundwater(level, pos, state);
        if (source != null) return WaterPurity.sampleAt(level, source);
        if (!ThirstConfig.get().enableRainCollection) return null;
        return WaterQuality.fresh(WaterPurity.rainwaterQuality());
    }

    /**
     * The groundwater source the well finds, scanning the box {@code TimberWellBlock.hasGroundwater}
     * scans, in its order, so the first source it would accept is the one sampled.
     */
    private static BlockPos groundwater(Level level, BlockPos pos, BlockState state) {
        Direction facing = state.getValue(BlockStateProperties.HORIZONTAL_FACING);
        BlockPos foot = switch (state.getValue(TimberWellBlock.PART).name()) {
            case "HEAD" -> pos.relative(facing.getOpposite());
            case "TOP" -> pos.below().relative(facing.getOpposite());
            default -> pos;
        };
        BlockPos head = foot.relative(facing);
        int minX = Math.min(foot.getX(), head.getX()) - 1;
        int maxX = Math.max(foot.getX(), head.getX()) + 1;
        int minZ = Math.min(foot.getZ(), head.getZ()) - 1;
        int maxZ = Math.max(foot.getZ(), head.getZ()) + 1;
        int depth = TimberWellBlock.GROUNDWATER_DEPTH;
        for (BlockPos check : BlockPos.betweenClosed(minX, foot.getY() - depth, minZ, maxX, foot.getY() - 1, maxZ)) {
            FluidState fluid = level.getFluidState(check);
            if (fluid.is(FluidTags.WATER) && fluid.isSource()) return check.immutable();
        }
        return null;
    }
}
