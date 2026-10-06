package com.thirstwastaken2.block;

import com.thirstwastaken2.platform.SavedBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;

/**
 * What a hanging pot holds: how many servings, and how many of its boiling steps are done. The grade
 * stays in the blockstate's {@code purity}, where the model and a cauldron's code read it, and the
 * blockstate's {@code level} only says how full the pot looks, so a pot can hold up to 64 servings
 * without a blockstate for each.
 *
 * <p>Storage only: no ticker. Boiling runs on the block's scheduled ticks while there is something to
 * boil over a lit fire, as it did before the pot had a block entity, so an idle pot costs nothing.
 *
 * <p>A pot saved before it had one comes back with no servings recorded, and reads them from its
 * blockstate the first time it is asked: its old {@code level} counted servings, every pot then holding
 * three, so no water is lost. The client is told the servings, which it needs to decide a click the
 * way the server will, but not each step of the boil.
 */
public final class HangingPotBlockEntity extends SavedBlockEntity {
    /** Servings, or -1 until the first read of a pot saved before this existed. */
    private int servings = -1;
    private int boiled;

    public HangingPotBlockEntity(BlockPos pos, BlockState state) {
        super(ThirstBlockEntities.HANGING_POT, pos, state);
    }

    /** Servings in the pot. */
    public int servings() {
        if (servings < 0) servings = getBlockState().getValue(HangingPotBlock.LEVEL);
        return servings;
    }

    /** Boiling steps done toward the water in the pot. */
    public int boiled() {
        return boiled;
    }

    /** Sets what the pot holds and tells the client. */
    void setWater(int servings, int boiled) {
        this.servings = servings;
        this.boiled = boiled;
        changed();
    }

    /** Sets how far the boil has got, saved but not sent: the client draws nothing from it. */
    void setBoiled(int boiled) {
        this.boiled = boiled;
        setChanged();
    }

    @Override
    protected boolean syncsToClient() {
        return true;
    }

    @Override
    protected void save(Output output) {
        output.putInt("servings", servings());
        output.putInt("boiled", boiled);
    }

    @Override
    protected void load(Input input) {
        servings = input.getInt("servings", -1);
        boiled = input.getInt("boiled", 0);
    }
}
