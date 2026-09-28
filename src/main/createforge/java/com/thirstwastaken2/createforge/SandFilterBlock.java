package com.thirstwastaken2.createforge;

import com.simibubi.create.AllShapes;
import com.simibubi.create.content.equipment.wrench.IWrenchable;
import com.simibubi.create.foundation.advancement.AdvancementBehaviour;
import com.simibubi.create.foundation.block.IBE;
import com.simibubi.create.foundation.blockEntity.ComparatorUtil;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Water goes in at the top and comes out of the bottom one grade cleaner, as in the original mod. The
 * NeoForge block on 1.20.1's signatures: pipes find the two tanks through the block entity's fluid
 * capability; the sides have none, so a pipe beside the filter does not connect to it.
 */
public final class SandFilterBlock extends Block implements IWrenchable, IBE<SandFilterBlockEntity> {

    public SandFilterBlock(Properties properties) {
        super(properties);
    }

    @Override
    @SuppressWarnings("deprecation")
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        // The same outline as the model, which is the Spout's silhouette.
        return AllShapes.SPOUT;
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        AdvancementBehaviour.setPlacedBy(level, pos, placer);
    }

    @Override
    @SuppressWarnings("deprecation")
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        // Lets the block entity's behaviours tear down before it goes, as every Create block does.
        IBE.onRemove(state, level, pos, newState);
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    @SuppressWarnings("deprecation")
    public int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos) {
        // How much filtered water is waiting. Create's own helper only reads a single-tank block.
        SandFilterBlockEntity filter = getBlockEntity(level, pos);
        if (filter == null) return 0;
        SmartFluidTank output = filter.output().getPrimaryHandler();
        return ComparatorUtil.fractionToRedstoneLevel((double) output.getFluidAmount() / output.getCapacity());
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type) {
        return false;
    }

    @Override
    public Class<SandFilterBlockEntity> getBlockEntityClass() {
        return SandFilterBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends SandFilterBlockEntity> getBlockEntityType() {
        return SandFilter.blockEntity();
    }
}
