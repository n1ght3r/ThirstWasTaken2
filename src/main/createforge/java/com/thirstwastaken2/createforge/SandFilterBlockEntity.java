package com.thirstwastaken2.createforge;

import com.simibubi.create.api.equipment.goggles.IHaveGoggleInformation;
import com.simibubi.create.foundation.blockEntity.SmartBlockEntity;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import com.simibubi.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour;
import com.simibubi.create.foundation.fluid.SmartFluidTank;
import com.simibubi.create.foundation.utility.CreateLang;
import com.thirstwastaken2.forge.WaterFluids;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.createmod.catnip.lang.LangBuilder;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidStack;

import java.util.List;

/**
 * Two one-bucket tanks: pipes fill the input from above and drain the output from below, and every
 * tick up to {@link #FILTERED_PER_TICK} moves from one to the other, a grade cleaner.
 *
 * <p>Pipes are held to that direction by the tanks' own insertion and extraction flags, which apply to
 * {@link SmartFluidTankBehaviour#getCapability()}. The transfer itself goes through the
 * {@link SmartFluidTank}s underneath, which those flags do not apply to. The NeoForge block entity on
 * Forge 47's fluid stacks and capabilities.
 */
public final class SandFilterBlockEntity extends SmartBlockEntity implements IHaveGoggleInformation {
    /** One bucket, in millibuckets. */
    public static final int CAPACITY = 1000;
    /** 10 mB a tick, the original mod's default: a bucket every five seconds. */
    private static final int FILTERED_PER_TICK = 10;
    /** Grades the filter adds per pass, the original mod's default. */
    private static final int FILTRATION = 1;

    // Assigned from addBehaviours, which runs inside the super constructor. An initializer here would
    // run after it and wipe them.
    private SmartFluidTankBehaviour input;
    private SmartFluidTankBehaviour output;
    /** The input stack {@link #filtered} was built from. Neither is saved; both rebuild on the next tick. */
    private FluidStack filteredFrom;
    private FluidStack filtered;
    /** The output stack already known to hold the same water as {@link #filtered}. */
    private FluidStack matchedOutput;

    public SandFilterBlockEntity(BlockPos pos, BlockState state) {
        super(SandFilter.blockEntity(), pos, state);
    }

    @Override
    public void addBehaviours(List<BlockEntityBehaviour> behaviours) {
        input = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.INPUT, this, 1, CAPACITY, false)
                .forbidExtraction();
        // The input only takes water: anything else would sit in it forever, since nothing drains it.
        input.getPrimaryHandler().setValidator(WaterFluids::isWater);
        output = new SmartFluidTankBehaviour(SmartFluidTankBehaviour.OUTPUT, this, 1, CAPACITY, false)
                .forbidInsertion();
        behaviours.add(input);
        behaviours.add(output);
    }

    public SmartFluidTankBehaviour input() {
        return input;
    }

    public SmartFluidTankBehaviour output() {
        return output;
    }

    /**
     * The tank a pipe on {@code side} reaches: the input from above, the output from below. No side is
     * Create acting for a player's hand, which is only ever allowed to take filtered water out. The
     * sides have none, so a pipe there does not connect. Each tank's behaviour invalidates its own
     * capability when the block entity goes.
     */
    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
        if (capability == ForgeCapabilities.FLUID_HANDLER) {
            if (side == Direction.UP) return input.getCapability().cast();
            if (side == Direction.DOWN || side == null) return output.getCapability().cast();
            return LazyOptional.empty();
        }
        return super.getCapability(capability, side);
    }

    /**
     * Moves water across without building a stack per tick. The filtered form of the input is built once
     * per input stack, and the output is compared against it once per stack that lands there: a tank
     * merging more of the same water keeps its stack object and only changes the amount, so neither
     * cached answer can go stale without the identity changing too. Handing a tank back its own stack
     * is what tells Create the amount changed, so it syncs and saves.
     */
    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide()) return;

        SmartFluidTank from = input.getPrimaryHandler();
        FluidStack dirty = from.getFluid();
        if (dirty.isEmpty()) return;

        SmartFluidTank to = output.getPrimaryHandler();
        FluidStack waiting = to.getFluid();
        int space = to.getCapacity() - (waiting.isEmpty() ? 0 : waiting.getAmount());
        if (space <= 0) return;

        FluidStack filtered = filtered(dirty);
        // Water of a different grade than what is already waiting cannot share the output tank, so the
        // filter holds until a pipe drains it, the same as any other full Create tank.
        if (!waiting.isEmpty() && waiting != matchedOutput) {
            if (!waiting.isFluidEqual(filtered)) return;
            matchedOutput = waiting;
        }

        // A partial last batch is filtered too, so a tank never keeps a few millibuckets it cannot pass on.
        int moved = Math.min(Math.min(FILTERED_PER_TICK, dirty.getAmount()), space);
        if (waiting.isEmpty()) {
            matchedOutput = new FluidStack(filtered, moved);
            to.setFluid(matchedOutput);
        } else {
            waiting.grow(moved);
            to.setFluid(waiting);
        }
        if (moved == dirty.getAmount()) {
            from.setFluid(FluidStack.EMPTY);
        } else {
            dirty.shrink(moved);
            from.setFluid(dirty);
        }
    }

    /** The input water as it leaves the filter, rebuilt only when a different stack sits in the input. */
    private FluidStack filtered(FluidStack dirty) {
        if (dirty != filteredFrom) {
            filtered = filter(new FluidStack(dirty, 1));
            filteredFrom = dirty;
            matchedOutput = null;
        }
        return filtered;
    }

    /**
     * One pass through sand. Sea water comes out as it went in: sand does not take the salt out, and
     * cooking cannot either.
     */
    private static FluidStack filter(FluidStack stack) {
        WaterQuality quality = WaterFluids.quality(stack);
        if (quality instanceof WaterQuality.Fresh fresh) quality = WaterQuality.fresh(fresh.purity() + FILTRATION);
        return WaterFluids.stamp(stack, quality);
    }

    /** Each tank with its grade, the way the original mod showed it through Engineer's Goggles. */
    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        LangBuilder mb = CreateLang.translate("generic.unit.millibuckets");
        CreateLang.translate("gui.goggles.fluid_container").forGoggles(tooltip);

        boolean unfiltered = tankTooltip(tooltip, mb, "unfiltered", input.getPrimaryHandler());
        boolean filtered = tankTooltip(tooltip, mb, "filtered", output.getPrimaryHandler());
        if (!unfiltered && !filtered) {
            CreateLang.translate("gui.goggles.fluid_container.capacity")
                    .add(CreateLang.number(CAPACITY).add(mb).style(ChatFormatting.GOLD))
                    .style(ChatFormatting.GRAY)
                    .forGoggles(tooltip, 1);
        }
        return true;
    }

    private static boolean tankTooltip(List<Component> tooltip, LangBuilder mb, String label, SmartFluidTank tank) {
        FluidStack fluid = tank.getFluid();
        if (fluid.isEmpty()) return false;

        CreateLang.builder()
                .add(Component.translatable("gui.thirstwastaken2.sand_filter." + label))
                .text(": ")
                .add(CreateLang.fluidName(fluid))
                .style(ChatFormatting.GRAY)
                .forGoggles(tooltip);
        if (WaterFluids.isWater(fluid)) {
            WaterQuality quality = WaterFluids.quality(fluid);
            Component line = quality instanceof WaterQuality.Fresh fresh
                    ? WaterPurity.tooltip(fresh.purity()) : WaterPurity.saltTooltip();
            CreateLang.builder().add(line).forGoggles(tooltip, 1);
        }
        CreateLang.builder()
                .add(CreateLang.number(fluid.getAmount()).add(mb).style(ChatFormatting.GOLD))
                .text(ChatFormatting.GRAY, " / ")
                .add(CreateLang.number(tank.getCapacity()).add(mb).style(ChatFormatting.DARK_GRAY))
                .forGoggles(tooltip, 1);
        return true;
    }
}
