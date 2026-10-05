package com.thirstwastaken2.block;

import com.thirstwastaken2.platform.SupportedBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.function.IntConsumer;

/**
 * One of the copper distiller's parts placed on its own, drawn as that piece of the machine and facing
 * the player who placed it, as the distiller does. Alone a part is decoration; set together they become
 * the machine (see {@link DistillerBlock}).
 */
public class DistillerPartBlock extends SupportedBlock {
    public static final EnumProperty<Direction> FACING = DistillerBlock.FACING;

    /** The brick firebox: the firebox, its ledge and chimney. */
    static final double[][] FIREBOX_BOXES = { DistillerBlock.FIREBOX_BOX, DistillerBlock.CHIMNEY_BOX };
    /** The boiler standing on the ground: the boiler half's boiler, sunk by its firebox's height. */
    static final double[][] BOILER_BOXES = { sunk(DistillerBlock.BOILER_BOX), sunk(DistillerBlock.HELMET_BOX) };

    /** Outlines by {@link Direction#get2DDataValue()}. */
    private final VoxelShape[] shapes;

    public DistillerPartBlock(Properties properties, double[][] boxes) {
        this(properties, boxes, copied -> new DistillerPartBlock(copied, boxes));
    }

    protected DistillerPartBlock(Properties properties, double[][] boxes,
                                 java.util.function.Function<Properties, ? extends DistillerPartBlock> copy) {
        super(properties, copy);
        this.shapes = DistillerBlock.shapes(boxes);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected BlockState supportChanged(BlockState state, LevelReader level, BlockPos pos, BlockState below,
                                        IntConsumer scheduleTick) {
        return state;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return shapes[state.getValue(FACING).get2DDataValue()];
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    /** A box of the boiler half moved down onto the ground, from on top of the firebox. */
    private static double[] sunk(double[] box) {
        double drop = DistillerBlock.FIREBOX_BOX[4];
        return new double[] { box[0], box[1] - drop, box[2], box[3], box[4] - drop, box[5] };
    }
}
