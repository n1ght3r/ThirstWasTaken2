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
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
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
 * <p>It holds {@link #capacity()} servings, three for copper and six for iron by default, from the
 * config, and keeps their quality the way a cauldron does, in {@link WaterPurity#BLOCK_PURITY}. How many
 * servings, and how far they have boiled, are its {@link HangingPotBlockEntity}'s; the blockstate's
 * {@link #LEVEL} only says how full it looks, in {@link #FILLS} steps, so a pot of 64 servings needs no
 * more blockstates than one of three. It hangs a block above the floor, from a stand whose legs reach
 * down past the block between, which is where its campfire goes; {@code HangingPotItem} places it there.
 * It stands over that block empty, and only boils once a lit campfire is in it; taking the campfire away
 * leaves it standing, and putting anything else solid there knocks it off. The pot and its stand never
 * change shape, and the pot is in its own block, so its shape is where it is drawn.
 *
 * <p>Boiling takes {@link #secondsPerServing} for each serving in the pot, the way a furnace takes its
 * time per item, and leaves fresh water Clean in one go, never Pure: heat stops at Clean
 * ({@link WaterPurity#boil}). That time is one of the things the two pots differ in: copper carries heat
 * better, so it boils faster. Salt water is not boiled: taking the salt out is the distiller's work.
 *
 * <p>Boiling runs on scheduled ticks, not a block entity ticker. Each step counts one of
 * {@link #STEPS_PER_SERVING} per serving and schedules the next while there is more to do, so a pot that
 * has nothing to boil costs nothing. A step that finds the fire out schedules nothing; lighting the fire
 * again reaches the pot through {@link #supportChanged}, which picks the count up where it stopped.
 * Pouring more water in keeps what is done and only adds the new servings' steps, and water that is
 * already Clean or Pure counts as boiled, so a bottle topped up into a finished pot takes one serving's
 * time, not the whole pot's.
 */
public final class HangingPotBlock extends SupportedBlock implements EntityBlock {
    /** Servings a bucket is, whatever the pot holds. */
    public static final int BUCKET = 3;
    /** How many steps of fullness the model draws. */
    public static final int FILLS = 3;
    /**
     * How full the pot looks: 0 empty, then a third, two thirds and full, rounded up. Named {@code level}
     * as before: it counted servings then, when every pot held three, so an old save reads back as the
     * same water through {@link HangingPotBlockEntity#servings}.
     */
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, FILLS);
    /**
     * Whether a campfire is below. Nothing in the mod reads it; it is kept up to date for a resource pack
     * that draws the pot differently over a fire, and it was in the blockstate before.
     */
    public static final BooleanProperty HANGING = BlockStateProperties.HANGING;
    /** The axis the frame's crossbar runs along, across the placing player's view. */
    public static final EnumProperty<Direction.Axis> AXIS = BlockStateProperties.HORIZONTAL_AXIS;
    /** Scheduled ticks a serving takes to boil; more of them just means a finer count. */
    public static final int STEPS_PER_SERVING = 4;

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
    private final IntSupplier capacity;

    /**
     * A pot holding {@code capacity} servings, each taking {@code secondsPerServing} to boil, both read
     * from the config whenever they are needed.
     */
    public HangingPotBlock(Properties properties, IntSupplier secondsPerServing, IntSupplier capacity) {
        super(properties, copy -> new HangingPotBlock(copy, secondsPerServing, capacity));
        this.secondsPerServing = secondsPerServing;
        this.capacity = capacity;
        registerDefaultState(stateDefinition.any()
                .setValue(LEVEL, 0)
                .setValue(WaterPurity.BLOCK_PURITY, WaterPurity.BLOCK_UNSET)
                .setValue(HANGING, false)
                .setValue(AXIS, Direction.Axis.X));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LEVEL, WaterPurity.BLOCK_PURITY, HANGING, AXIS);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new HangingPotBlockEntity(pos, state);
    }

    /** Servings this pot holds when full, from the config. */
    public int capacity() {
        return Math.max(1, capacity.getAsInt());
    }

    /** Seconds each serving in this pot takes to boil, from the config. */
    public int secondsPerServing() {
        return secondsPerServing.getAsInt();
    }

    // ---- what a pot holds ----------------------------------------------------

    /** The water in the pot at {@code state}, or {@code null} when it is empty. */
    public static WaterQuality quality(BlockState state) {
        if (state.getValue(LEVEL) == 0) return null;
        WaterQuality stored = WaterPurity.storedQuality(state);
        return stored != null ? stored : WaterQuality.fresh(ThirstConfig.get().defaultQuality);
    }

    /** Servings in the pot at {@code pos}, or 0 when there is none. */
    public static int servings(BlockGetter level, BlockPos pos) {
        HangingPotBlockEntity pot = pot(level, pos);
        return pot == null ? 0 : pot.servings();
    }

    /** Servings that still fit in the pot at {@code pos}. A pot over a lowered capacity takes none. */
    public static int room(BlockGetter level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof HangingPotBlock block)) return 0;
        return Math.max(0, block.capacity() - servings(level, pos));
    }

    /** Boiling steps done in the pot at {@code pos}. */
    public static int boiled(BlockGetter level, BlockPos pos) {
        HangingPotBlockEntity pot = pot(level, pos);
        return pot == null ? 0 : pot.boiled();
    }

    /**
     * Pours {@code servings} of {@code poured} into the pot at {@code pos}, mixed the way a cauldron mixes:
     * the worse grade wins. What has boiled so far stays boiled, and water that is already Clean or Pure,
     * in the pot or poured in, counts as boiled, so only the rest adds to the time left. The caller has
     * checked there is room.
     */
    public static void pour(Level level, BlockPos pos, int servings, WaterQuality poured) {
        BlockState state = level.getBlockState(pos);
        HangingPotBlockEntity pot = pot(level, pos);
        if (pot == null || servings <= 0) return;
        int held = pot.servings();
        WaterQuality heldQuality = quality(state);
        int boiled = isBoiled(heldQuality) ? boilSteps(held) : pot.boiled();
        if (isBoiled(poured)) boiled += boilSteps(servings);
        WaterQuality mixed = held == 0 ? poured : WaterQuality.worse(heldQuality, poured);
        write(level, pos, state, pot, held + servings, mixed, WaterPurity.boils(mixed) ? boiled : 0);
    }

    /**
     * Takes {@code servings} out of the pot at {@code pos}, keeping how far the rest has boiled. The water
     * drawn takes its share of what was left to do, but never all of it, so the step after finishes the
     * rest. The caller has checked there is enough.
     */
    public static void draw(Level level, BlockPos pos, int servings) {
        BlockState state = level.getBlockState(pos);
        HangingPotBlockEntity pot = pot(level, pos);
        if (pot == null || servings <= 0) return;
        int left = pot.servings() - servings;
        if (left <= 0) {
            write(level, pos, state, pot, 0, null, 0);
            return;
        }
        write(level, pos, state, pot, left, quality(state), Math.min(pot.boiled(), boilSteps(left) - 1));
    }

    /** Sets the pot at {@code pos} to hold {@code servings} of {@code quality}, with nothing boiled yet. */
    public static void setWater(Level level, BlockPos pos, int servings, WaterQuality quality) {
        HangingPotBlockEntity pot = pot(level, pos);
        if (pot == null) return;
        write(level, pos, level.getBlockState(pos), pot, servings, servings == 0 ? null : quality, 0);
    }

    private static void write(Level level, BlockPos pos, BlockState state, HangingPotBlockEntity pot,
                              int servings, WaterQuality quality, int boiled) {
        pot.setWater(servings, boiled);
        int stored = servings == 0 || quality == null ? WaterPurity.BLOCK_UNSET : WaterPurity.storedValue(quality);
        BlockState next = state.setValue(LEVEL, fill(servings, ((HangingPotBlock) state.getBlock()).capacity()))
                .setValue(WaterPurity.BLOCK_PURITY, stored);
        if (next != state) level.setBlock(pos, next, BLOCK_UPDATE_FLAGS);
        ((HangingPotBlock) state.getBlock()).scheduleBoil(level, pos, next);
    }

    /**
     * How full {@code servings} out of {@code capacity} look, in {@link #FILLS} steps: any water at all
     * shows, and a pot holding more than a lowered capacity looks full.
     */
    public static int fill(int servings, int capacity) {
        if (servings <= 0) return 0;
        return Math.max(1, (FILLS * Math.min(servings, capacity) + capacity - 1) / capacity);
    }

    private static HangingPotBlockEntity pot(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof HangingPotBlockEntity pot ? pot : null;
    }

    /** The steps {@code servings} take to boil from nothing. */
    public static int boilSteps(int servings) {
        return servings * STEPS_PER_SERVING;
    }

    /** Whether heat has nothing left to do to {@code quality}: Clean or Pure. */
    private static boolean isBoiled(WaterQuality quality) {
        return quality instanceof WaterQuality.Fresh && !WaterPurity.boils(quality);
    }

    /** How high, in pixels from the bottom of the block, the water in a pot that looks {@code fill} full stands. */
    public static double surfaceHeight(int fill) {
        return 2.5 + (fill - 1) * 1.5;
    }

    /** Whether there is fresh water in the pot that is not Clean yet. */
    public static boolean needsBoiling(BlockState state) {
        return WaterPurity.boils(quality(state));
    }

    /** Whether {@code below} is a burning campfire, soul campfires included. */
    public static boolean isHeat(BlockState below) {
        return CampfireBlock.isLitCampfire(below);
    }

    private int stepTicks() {
        return Math.max(1, secondsPerServing() * 20 / STEPS_PER_SERVING);
    }

    /** Schedules the next boiling step when there is something to boil over a lit fire. */
    private void scheduleBoil(Level level, BlockPos pos, BlockState state) {
        if (needsBoiling(state) && isHeat(level.getBlockState(pos.below()))) {
            level.scheduleTick(pos, this, stepTicks());
        }
    }

    // ---- the block ------------------------------------------------------------

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
        scheduleBoil(level, pos, state);
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        HangingPotBlockEntity pot = pot(level, pos);
        if (pot == null) return;
        if (!needsBoiling(state)) {
            if (pot.boiled() != 0) pot.setBoiled(0);
            return;
        }
        // Paused: lighting the fire again schedules the next step through supportChanged.
        if (!isHeat(level.getBlockState(pos.below()))) return;

        int step = pot.boiled() + 1;
        if (step < boilSteps(pot.servings())) {
            pot.setBoiled(step);
            level.scheduleTick(pos, this, stepTicks());
            return;
        }
        write(level, pos, state, pot, pot.servings(), WaterPurity.boil(quality(state)), 0);
        level.playSound(null, pos, SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 0.6F, 1.4F);
    }

    /** Rain tops the pot up like a cauldron, a serving at a time, graded as rainwater. */
    @Override
    public void handlePrecipitation(BlockState state, Level level, BlockPos pos, Biome.Precipitation precipitation) {
        if (precipitation != Biome.Precipitation.RAIN || room(level, pos) <= 0
                || !ThirstConfig.get().enableRainCollection
                || level.getRandom().nextFloat() >= RAIN_FILL_CHANCE) {
            return;
        }
        pour(level, pos, 1, WaterQuality.fresh(WaterPurity.rainwaterQuality()));
        level.gameEvent(null, GameEvent.BLOCK_CHANGE, pos);
    }

    /** Bubbles while water sits over a fire, and a wisp of steam now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int fill = state.getValue(LEVEL);
        if (fill == 0 || !isHeat(level.getBlockState(pos.below()))) return;

        double surface = pos.getY() + surfaceHeight(fill) / 16.0;
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
