package com.thirstwastaken2.block;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The distiller's cooling tub placed on its own. It takes its coolant here as well as in the machine,
 * once, from any water container (see {@link DistillerInteractions}), and keeps it when a copper pipe
 * joins it to a boiler.
 */
public final class CoolingTubBlock extends DistillerPartBlock {
    /** Whether the tub holds its coolant: any water, sea water too, poured in once. The distiller's own. */
    public static final BooleanProperty COOLED = DistillerBlock.COOLED;

    public CoolingTubBlock(Properties properties) {
        super(properties, DistillerBlock.TUB_BOXES, CoolingTubBlock::new);
        registerDefaultState(defaultBlockState().setValue(COOLED, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(COOLED);
    }
}
