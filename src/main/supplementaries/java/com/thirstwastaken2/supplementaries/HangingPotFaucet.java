package com.thirstwastaken2.supplementaries;

import com.thirstwastaken2.block.HangingPotBlock;
import com.thirstwastaken2.purity.WaterQuality;
import net.mehvahdjukaar.moonlight.api.fluids.FluidOffer;
import net.mehvahdjukaar.supplementaries.common.block.faucet.FaucetSource;
import net.mehvahdjukaar.supplementaries.common.block.faucet.FaucetTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The copper and iron hanging pots as something a faucet can draw from and pour into, registered with
 * Supplementaries' faucet from {@code FaucetBehaviorsManagerMixin}.
 *
 * <p>No built-in behaviour claims these blocks, so it does not matter that this is registered after
 * them: the faucet takes the first behaviour that answers, and for a pot only this one does.
 *
 * <p>A serving of the pot is a bottle of soft fluid, which is what makes the two counts the same
 * number. Everything about what the pot then holds is the pot's own: a pour keeps the worse of the two
 * grades and what has boiled so far, through {@link HangingPotBlock#pour}, exactly as pouring a
 * container in by hand does.
 */
public final class HangingPotFaucet implements FaucetSource.BlState, FaucetTarget.BlState {
    @Override
    public FluidOffer getProvidedFluid(Level level, BlockPos pos, Direction dir, BlockState source) {
        if (!(source.getBlock() instanceof HangingPotBlock)) return null;
        WaterQuality quality = HangingPotBlock.quality(source);
        if (quality == null) return null;
        return FluidOffer.of(SoftFluidQuality.water(level, quality, HangingPotBlock.servings(level, pos)), 1);
    }

    @Override
    public void drain(Level level, BlockPos pos, Direction dir, BlockState source, int amount) {
        HangingPotBlock.draw(level, pos, amount);
    }

    @Override
    public Integer fill(Level level, BlockPos pos, BlockState target, FluidOffer offer) {
        if (!(target.getBlock() instanceof HangingPotBlock)) return null;
        // Zero rather than null once the block is a pot: nothing else may claim it, so a pot that is
        // full, or anything but water above it, is a refusal and not a pass.
        if (!SoftFluidQuality.isWater(offer.fluid())) return 0;
        int space = HangingPotBlock.room(level, pos);
        if (space <= 0) return 0;

        int servings = Math.min(space, offer.fluid().getCount());
        HangingPotBlock.pour(level, pos, servings, SoftFluidQuality.quality(offer.fluid()));
        return Math.max(offer.minAmount(), servings);
    }
}
