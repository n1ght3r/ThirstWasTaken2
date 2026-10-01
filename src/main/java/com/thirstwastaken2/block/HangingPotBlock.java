package com.thirstwastaken2.block;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.platform.SupportedBlock;
import com.thirstwastaken2.platform.Vanilla;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * A hanging pot, copper or iron, adapted from Dehydration's campfire cauldron (Globox1997, GPL-3.0).
 *
 * <p>It holds {@link #CAPACITY} servings of water, a bucket's worth like a cauldron, since the pot is no
 * bigger than one, and keeps their quality the way a cauldron does, in {@link WaterPurity#BLOCK_PURITY}.
 * It hangs a block above the floor, from a stand whose legs reach down past the block between, which is
 * where its campfire goes; {@code HangingPotItem} places it there. It stands over that block empty, and
 * only boils once a lit campfire is in it; taking the campfire away leaves it standing, and putting
 * anything else solid there knocks it off. The pot and its
 * stand never change shape, and the pot is in its own block, so its shape is where it is drawn.
 * Boiling takes {@link #secondsPerServing} for each
 * serving in the pot, the way a furnace takes its time per item, and leaves fresh water pure in one go.
 * That time is the one thing the two pots differ in: copper carries heat better, so it boils faster. Salt water is not boiled: taking the salt out is
 * distillation, which is a separate idea on the roadmap.
 *
 * <p>Boiling runs on scheduled ticks rather than a block entity. {@link #BOIL} counts the steps done,
 * {@link #STEPS_PER_SERVING} for every serving, and every change of state schedules the next step
 * through {@link #onPlace}, so a pot that has nothing to boil costs nothing. A step that finds the fire
 * out does nothing and schedules nothing; lighting the fire again reaches the pot through
 * {@link #supportChanged}, which picks the count up where it stopped. Pouring more water in keeps what
 * is done and only adds the new servings' steps, and water that is already pure counts as boiled, so a
 * bottle topped up into a finished pot takes one serving's time, not the whole pot's.
 */
public final class HangingPotBlock extends SupportedBlock {
    /** Servings the pot holds: a bottle or a bowl is one, a bucket three. */
    public static final int CAPACITY = 3;
    public static final int BUCKET = 3;
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, CAPACITY);
    /**
     * Whether a campfire is below. Nothing in the mod reads it; it is kept up to date for a resource pack
     * that draws the pot differently over a fire, and it was in the blockstate before.
     */
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
    /** The axis the frame's crossbar runs along, across the placing player's view. */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    /** Scheduled ticks a serving takes to boil; more of them just means a finer count. */
    public static final int STEPS_PER_SERVING = 4;
    public static final IntegerProperty BOIL = IntegerProperty.create("boil", 0, CAPACITY * STEPS_PER_SERVING - 1);

    private static final VoxelShape POT = Block.box(3.0, 0.0, 3.0, 13.0, 6.0, 13.0);
    private static final VoxelShape FRAME_ALONG_Z = Shapes.or(POT,
            Block.box(7.0, 0.0, 0.0, 9.0, 16.0, 1.0),
            Block.box(7.0, 0.0, 15.0, 9.0, 16.0, 16.0),
            Block.box(7.0, 14.0, 1.0, 9.0, 15.0, 15.0));
    private static final VoxelShape FRAME_ALONG_X = Shapes.or(POT,
            Block.box(0.0, 0.0, 7.0, 1.0, 16.0, 9.0),
            Block.box(15.0, 0.0, 7.0, 16.0, 16.0, 9.0),
            Block.box(1.0, 14.0, 7.0, 15.0, 15.0, 9.0));
    /** Vanilla's chance that rain adds a layer to a cauldron on one of its precipitation ticks. */
    private static final float RAIN_FILL_CHANCE = 0.05F;
    private static final int BLOCK_UPDATE_FLAGS = 3;

    private final IntSupplier secondsPerServing;

    /** A pot whose servings each take {@code secondsPerServing}, read from the config at each step, to boil. */
    public HangingPotBlock(Properties properties, IntSupplier secondsPerServing) {
        super(properties, copy -> new HangingPotBlock(copy, secondsPerServing));
        this.secondsPerServing = secondsPerServing;
        registerDefaultState(stateDefinition.any()
                .setValue(LEVEL, 0)
                .setValue(WaterPurity.BLOCK_PURITY, WaterPurity.BLOCK_UNSET)
                .setValue(BOIL, 0)
                .setValue(HANGING, false)
                .setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, WaterPurity.BLOCK_PURITY, BOIL, HANGING, AXIS);
    }

    /** The water in the pot, or {@code null} when it is empty. */
    public static WaterQuality quality(BlockState state) {
        if (state.getValue(LEVEL) == 0) return null;
        WaterQuality stored = WaterPurity.storedQuality(state);
        return stored != null ? stored : WaterQuality.fresh(ThirstConfig.get().defaultPurity);
    }

    /** {@code state} holding {@code level} servings of {@code quality}, with nothing boiled yet. */
    public static BlockState withWater(BlockState state, int level, WaterQuality quality) {
        int stored = level == 0 ? WaterPurity.BLOCK_UNSET : WaterPurity.storedValue(quality);
        return state.setValue(LEVEL, level).setValue(WaterPurity.BLOCK_PURITY, stored).setValue(BOIL, 0);
    }

    /**
     * {@code state} with {@code servings} more of {@code poured}, mixed the way a cauldron mixes: the worse
     * grade wins. What has boiled so far stays boiled, and water that is already pure, in the pot or
     * poured in, counts as boiled, so only the rest adds to the time left.
     */
    public static BlockState withPoured(BlockState state, int servings, WaterQuality poured) {
        WaterQuality held = quality(state);
        int level = state.getValue(LEVEL);
        int boiled = isPure(held) ? boilSteps(level) : state.getValue(BOIL);
        if (isPure(poured)) boiled += boilSteps(servings);

        WaterQuality mixed = held == null ? poured : WaterQuality.worse(held, poured);
        BlockState result = withWater(state, level + servings, mixed);
        // Below what the mix needs whenever it still needs boiling: only pure into pure reaches it.
        return needsBoiling(result) ? result.setValue(BOIL, boiled) : result;
    }

    /**
     * {@code state} with {@code servings} fewer, keeping how far the rest has boiled. The water drawn
     * takes its share of what was left to do, but never all of it, so the step after finishes the rest.
     */
    public static BlockState withLess(BlockState state, int servings) {
        int level = state.getValue(LEVEL) - servings;
        if (level <= 0) return withWater(state, 0, null);
        return state.setValue(LEVEL, level)
                .setValue(BOIL, Math.min(state.getValue(BOIL), boilSteps(level) - 1));
    }

    /** The steps {@code servings} take to boil from nothing. */
    public static int boilSteps(int servings) {
        return servings * STEPS_PER_SERVING;
    }

    private static boolean isPure(WaterQuality quality) {
        return quality instanceof WaterQuality.Fresh fresh && fresh.purity() == WaterPurity.MAX;
    }

    /** How high, in pixels from the bottom of the block, the water in a pot holding {@code servings} stands. */
    public static double surfaceHeight(int servings) {
        return 2.5 + (servings - 1) * 1.5;
    }

    /** Whether there is fresh water in the pot that is not pure yet. */
    public static boolean needsBoiling(BlockState state) {
        return quality(state) instanceof WaterQuality.Fresh fresh && fresh.purity() < WaterPurity.MAX;
    }

    /** Whether {@code below} is a burning campfire, soul campfires included. */
    public static boolean isHeat(BlockState below) {
        return CampfireBlock.isLitCampfire(below);
    }

    /** Seconds each serving in this pot takes to boil, from the config. */
    public int secondsPerServing() {
        return secondsPerServing.getAsInt();
    }

    private int stepTicks() {
        return Math.max(1, secondsPerServing() * 20 / STEPS_PER_SERVING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState below = context.getLevel().getBlockState(context.getClickedPos().below());
        return defaultBlockState()
                .setValue(HANGING, below.is(BlockTags.CAMPFIRES))
                .setValue(AXIS, context.getHorizontalDirection().getClockWise().getAxis());
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos below = pos.below();
        BlockState under = level.getBlockState(below);
        if (under.is(BlockTags.CAMPFIRES)) return true;
        // Otherwise the block its campfire goes in has to stay clear, grass and flowers aside, with the floor
        // under it for the legs. Anything solid put there would bury the legs, so it knocks the pot off
        // instead, the way a torch drops when its wall goes.
        return under.getCollisionShape(level, below).isEmpty() && canSupportCenter(level, below.below(), Direction.UP);
    }

    @Override
    protected BlockState supportChanged(BlockState state, LevelReader level, BlockPos pos, BlockState below,
                                        IntConsumer scheduleTick) {
        if (needsBoiling(state) && isHeat(below)) scheduleTick.accept(stepTicks());
        return state.setValue(HANGING, below.is(BlockTags.CAMPFIRES));
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState old, boolean movedByPiston) {
        if (needsBoiling(state) && isHeat(level.getBlockState(pos.below()))) {
            level.scheduleTick(pos, this, stepTicks());
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!needsBoiling(state)) {
            if (state.getValue(BOIL) != 0) level.setBlock(pos, state.setValue(BOIL, 0), BLOCK_UPDATE_FLAGS);
            return;
        }
        // Paused: lighting the fire again schedules the next step through supportChanged.
        if (!isHeat(level.getBlockState(pos.below()))) return;

        int stage = state.getValue(BOIL) + 1;
        if (stage < boilSteps(state.getValue(LEVEL))) {
            // onPlace schedules the step after this one.
            level.setBlock(pos, state.setValue(BOIL, stage), BLOCK_UPDATE_FLAGS);
            return;
        }
        level.setBlock(pos, withWater(state, state.getValue(LEVEL), WaterQuality.fresh(WaterPurity.MAX)),
                BLOCK_UPDATE_FLAGS);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6F, 1.4F);
    }

    /** Rain tops the pot up like a cauldron, a serving at a time, graded as rainwater. */
    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        int servings = state.getValue(LEVEL);
        if (precipitation != Biome.Precipitation.RAIN || servings >= CAPACITY
                || !ThirstConfig.get().enableRainCollection
                || level.getRandom().nextFloat() >= RAIN_FILL_CHANCE) {
            return;
        }
        WaterQuality rain = WaterQuality.fresh(WaterPurity.rainwaterPurity());
        level.setBlockAndUpdate(pos, withPoured(state, 1, rain));
        level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
    }

    /** Bubbles while water sits over a fire, and a wisp of steam now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int servings = state.getValue(LEVEL);
        if (servings == 0 || !isHeat(level.getBlockState(pos.below()))) return;

        double surface = pos.getY() + surfaceHeight(servings) / 16.0;
        double x = pos.getX() + 0.3 + random.nextDouble() * 0.4;
        double z = pos.getZ() + 0.3 + random.nextDouble() * 0.4;
        level.addParticle(ParticleTypes.BUBBLE_POP, x, surface, z, 0.0, 0.02, 0.0);
        if (random.nextInt(4) == 0) {
            level.addParticle(Vanilla.steamParticle(), x, surface + 0.1, z, 0.0, 0.03, 0.0);
        }
        if (random.nextInt(10) == 0) {
            level.playLocalSound(pos.getX() + 0.5, surface, pos.getZ() + 0.5, SoundEvents.BUBBLE_COLUMN_BUBBLE_POP,
                    SoundSource.BLOCKS, 0.4F, 0.9F + random.nextFloat() * 0.2F, false);
        }
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(AXIS) == Direction.Axis.Z ? FRAME_ALONG_Z : FRAME_ALONG_X;
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return switch (rotation) {
            case CLOCKWISE_90, COUNTERCLOCKWISE_90 -> state.setValue(AXIS,
                    state.getValue(AXIS) == Direction.Axis.X ? Direction.Axis.Z : Direction.Axis.X);
            default -> state;
        };
    }

    @Override
    public boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }
}
