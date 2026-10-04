package com.thirstwastaken2.block;

import com.thirstwastaken2.platform.SupportedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.IntConsumer;

/**
 * The copper distiller, two blocks wide, placed and broken whole the way a bed is.
 *
 * <p>The {@link Part#BOILER} half is where the player clicked: a brick firebox under a copper boiler,
 * with a chimney behind. The {@link Part#TUB} half stands to its right as seen from the front, a cooling
 * tub with a tap and a basin. {@link #FACING} is the way the front looks, toward the player who placed
 * it. Each half watches the other through {@link #sideChanged} and breaks when it goes, so only the
 * boiler half's loot drops the item, whichever half was mined.
 *
 * <p>Distilling is not built yet: for now the block is the machine's look, its fire always burning.
 * The models are written by {@code tools/distiller/generate_distiller_model.py}.
 */
public final class DistillerBlock extends SupportedBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);

    /** The halves' outlines facing north, in each half's own pixels; the models' boxes, simplified. */
    private static final double[][] BOILER_BOXES = {
            { 0.5, 0, 1.5, 15.5, 7, 15.5 },     // firebox and ledge
            { 3, 7, 2, 13, 17, 12 },            // boiler
            { 5.5, 17, 4.5, 10.5, 23.5, 9.5 },  // neck and helmet
            { 10.5, 0, 11.5, 15.5, 18.5, 16 },  // chimney
            { 0, 21, 6, 5.5, 22.5, 7.5 },       // swan neck
    };
    private static final double[][] TUB_BOXES = {
            { 1, 0, 4, 13, 12, 15 },            // tub
            { 8, 21, 6, 16, 22.5, 7.5 },        // swan neck
            { 8, 10.5, 6, 9.5, 21, 7.5 },       // pipe down into the tub
            { 6.5, 5, 1.5, 8.5, 9, 4 },         // tap
            { 5, 0, 0, 10, 4, 4 },              // basin
    };
    /** Shapes by part, then by {@link Direction#get2DDataValue()}. */
    private static final VoxelShape[][] SHAPES = new VoxelShape[2][4];
    /** The chimney's mouth, facing north, in the boiler half's pixels. */
    private static final double[] CHIMNEY_TOP = { 13, 18.5, 13.75 };

    static {
        for (Direction facing : Direction.Plane.HORIZONTAL) {
            SHAPES[Part.BOILER.ordinal()][facing.get2DDataValue()] = shape(BOILER_BOXES, facing);
            SHAPES[Part.TUB.ordinal()][facing.get2DDataValue()] = shape(TUB_BOXES, facing);
        }
    }

    public DistillerBlock(Properties properties) {
        super(properties, DistillerBlock::new);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PART, Part.BOILER));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    /** The way from {@code state}'s half to the other one. */
    public static Direction towardOtherHalf(BlockState state) {
        Direction facing = state.getValue(FACING);
        return state.getValue(PART) == Part.BOILER ? facing.getCounterClockWise() : facing.getClockWise();
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Direction facing = context.getHorizontalDirection().getOpposite();
        BlockState state = defaultBlockState().setValue(FACING, facing);
        BlockPos tub = context.getClickedPos().relative(towardOtherHalf(state));
        Level level = context.getLevel();
        if (!level.getBlockState(tub).canBeReplaced(context) || !level.getWorldBorder().isWithinBounds(tub)) {
            return null;
        }
        return state;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide()) return;
        level.setBlock(pos.relative(towardOtherHalf(state)), state.setValue(PART, Part.TUB), Block.UPDATE_ALL);
    }

    @Override
    protected BlockState supportChanged(BlockState state, LevelReader level, BlockPos pos, BlockState below,
                                        IntConsumer scheduleTick) {
        return state;
    }

    /** Breaks when the other half is no longer there, as a bed does. */
    @Override
    protected BlockState sideChanged(BlockState state, Direction direction, BlockState neighbor) {
        if (direction != towardOtherHalf(state)) return state;
        boolean whole = neighbor.is(this) && neighbor.getValue(PART) != state.getValue(PART)
                && neighbor.getValue(FACING) == state.getValue(FACING);
        return whole ? state : Blocks.AIR.defaultBlockState();
    }

    /**
     * Mining the tub half in creative takes the boiler half without its drop, as a bed does. Otherwise
     * the boiler half, breaking through {@link #sideChanged} because the tub went, would drop the item a
     * creative player never spent. Asks the abilities rather than {@code isCreative}, which vanilla's
     * gametest mock player answers yes to in every game mode.
     */
    @Override
    protected void beforePlayerBreaks(Level level, BlockPos pos, BlockState state, Player player) {
        if (level.isClientSide() || !player.getAbilities().instabuild || state.getValue(PART) != Part.TUB) return;
        BlockPos boiler = pos.relative(towardOtherHalf(state));
        BlockState other = level.getBlockState(boiler);
        if (other.is(this) && other.getValue(PART) == Part.BOILER) level.destroyBlock(boiler, false, player);
    }

    /** Smoke out of the chimney now and then. */
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(PART) != Part.BOILER || random.nextInt(3) != 0) return;
        double[] mouth = rotate(CHIMNEY_TOP[0], CHIMNEY_TOP[2], state.getValue(FACING));
        double x = pos.getX() + (mouth[0] + random.nextDouble() * 2 - 1) / 16.0;
        double z = pos.getZ() + (mouth[1] + random.nextDouble() * 2 - 1) / 16.0;
        level.addParticle(ParticleTypes.SMOKE, x, pos.getY() + CHIMNEY_TOP[1] / 16.0, z, 0.0, 0.04, 0.0);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPES[state.getValue(PART).ordinal()][state.getValue(FACING).get2DDataValue()];
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    public boolean isPathfindable(BlockState state, PathComputationType type) {
        return false;
    }

    private static VoxelShape shape(double[][] boxes, Direction facing) {
        VoxelShape shape = Shapes.empty();
        for (double[] box : boxes) {
            double[] a = rotate(box[0], box[2], facing);
            double[] b = rotate(box[3], box[5], facing);
            shape = Shapes.or(shape, Block.box(Math.min(a[0], b[0]), box[1], Math.min(a[1], b[1]),
                    Math.max(a[0], b[0]), box[4], Math.max(a[1], b[1])));
        }
        return shape.optimize();
    }

    /** A point in a block's pixels, facing north, turned to {@code facing} the way the blockstate turns the model. */
    private static double[] rotate(double x, double z, Direction facing) {
        switch (facing) {
            case EAST: return new double[] { 16 - z, x };
            case SOUTH: return new double[] { 16 - x, 16 - z };
            case WEST: return new double[] { z, 16 - x };
            default: return new double[] { x, z };
        }
    }

    /** The two halves. */
    public enum Part implements StringRepresentable {
        BOILER("boiler"),
        TUB("tub");

        private final String name;

        Part(String name) {
            this.name = name;
        }

        @Override
        public String getSerializedName() {
            return name;
        }
    }
}
