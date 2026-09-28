package com.thirstwastaken2.dev.benchmark;

import com.thirstwastaken2.createfly.SandFilter;
import com.thirstwastaken2.createfly.SandFilterBlockEntity;
import com.thirstwastaken2.createfly.WaterFluids;
import com.thirstwastaken2.platform.ThirstComponents;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import com.zurrtum.create.catnip.math.BlockFace;
import com.zurrtum.create.content.fluids.OpenEndedPipe;
import com.zurrtum.create.content.fluids.transfer.GenericItemEmptying;
import com.zurrtum.create.content.fluids.transfer.GenericItemFilling;
import com.zurrtum.create.foundation.blockEntity.behaviour.fluid.SmartFluidTankBehaviour.TankSegment;
import com.zurrtum.create.infrastructure.fluids.BottleFluidInventory;
import com.zurrtum.create.infrastructure.fluids.FluidStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.material.Fluids;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;

/**
 * The Create Fly work the mod adds: the Sand Filter's tick and the purity hooks on Create's own fluid
 * transfers. Added to {@link InteractionScenario} only when Create Fly is installed, and compiled only by
 * the nodes that build the Sand Filter.
 *
 * <p>The filter is a real block entity attached to the level but not placed in it, so the server does not
 * tick it behind the benchmark's back. Its tank syncs still go through the chunk source, as a placed
 * filter's would.
 */
final class CreateFlyOperations {
    private static final MethodHandle REMOVE_FLUID_FROM_SPACE;

    static {
        try {
            REMOVE_FLUID_FROM_SPACE = MethodHandles.privateLookupIn(OpenEndedPipe.class, MethodHandles.lookup())
                    .findVirtual(OpenEndedPipe.class, "removeFluidFromSpace",
                            MethodType.methodType(FluidStack.class, boolean.class));
        } catch (ReflectiveOperationException error) {
            throw new ExceptionInInitializerError(error);
        }
    }

    /** Keeps results reachable, so the JIT cannot discard work whose result is otherwise unused. */
    private static Object sink;

    private CreateFlyOperations() { }

    /** Called by name from {@link InteractionScenario}. */
    static void add(InteractionScenario scenario, BenchmarkWorld world, BenchmarkPlayer player) {
        ServerLevel level = world.level;
        BlockPos water = world.water();
        // Inside the forced chunks, where nothing else stands, so a tank sync marks a loaded chunk.
        BlockPos filterPos = water.above(3);

        SandFilterBlockEntity filter = new SandFilterBlockEntity(filterPos, SandFilter.block().defaultBlockState());
        filter.setLevel(level);
        TankSegment input = filter.input().getPrimaryHandler();
        TankSegment output = filter.output().getPrimaryHandler();
        FluidStack dirty = water(SandFilterBlockEntity.CAPACITY, WaterQuality.fresh(0));
        FluidStack pure = water(SandFilterBlockEntity.CAPACITY, WaterQuality.fresh(WaterPurity.MAX));

        scenario.batched("createfly_filter_active",
                "Sand Filter tick moving dirty water into an empty output tank, a grade cleaner",
                () -> {
                    input.setFluid(dirty.copy());
                    output.setFluid(FluidStack.EMPTY);
                },
                filter::tick,
                () -> !output.getFluid().isEmpty()
                        && WaterFluids.quality(output.getFluid()).equals(WaterQuality.fresh(1)));
        scenario.batched("createfly_filter_blocked",
                "Sand Filter tick whose output tank is full of water of another grade, so nothing moves",
                () -> {
                    input.setFluid(dirty.copy());
                    output.setFluid(pure.copy());
                },
                filter::tick,
                () -> input.getFluid().getAmount() == SandFilterBlockEntity.CAPACITY);
        scenario.batched("createfly_filter_idle",
                "Sand Filter tick with nothing in its input tank",
                () -> {
                    input.setFluid(FluidStack.EMPTY);
                    output.setFluid(FluidStack.EMPTY);
                },
                filter::tick,
                () -> output.getFluid().isEmpty());

        FluidStack spoutWater = water(SandFilterBlockEntity.CAPACITY, WaterQuality.fresh(2));
        ItemStack[] bottle = new ItemStack[1];
        FluidStack[] available = new FluidStack[1];
        scenario.single("createfly_fill_bottle",
                "GenericItemFilling.fillItem: a Spout filling a glass bottle, and the purity stamp on it",
                () -> {
                    bottle[0] = new ItemStack(Items.GLASS_BOTTLE);
                    available[0] = spoutWater.copy();
                },
                () -> sink = GenericItemFilling.fillItem(level, BottleFluidInventory.CAPACITY, bottle[0], available[0]),
                () -> sink instanceof ItemStack filled && filled.has(ThirstComponents.WATER_PURITY));

        ItemStack gradedBottle = WaterPurity.setQuality(PotionContents.createItemStack(Items.POTION, Potions.WATER),
                WaterQuality.fresh(3));
        scenario.batched("createfly_empty_bottle",
                "GenericItemEmptying.emptyItem, simulated: an Item Drain reading a graded bottle",
                () -> { },
                () -> sink = GenericItemEmptying.emptyItem(level, gradedBottle, true),
                () -> sink instanceof com.zurrtum.create.catnip.data.Pair<?, ?> pair
                        && pair.getFirst() instanceof FluidStack fluid && fluid.has(ThirstComponents.WATER_PURITY));

        OpenEndedPipe pipe = new OpenEndedPipe(new BlockFace(water.above(), Direction.DOWN));
        pipe.manageSource(level, null);
        scenario.batched("createfly_open_pipe_draw",
                "OpenEndedPipe#removeFluidFromSpace, simulated: a pump checking the water source below an open pipe",
                world::refillWater,
                () -> sink = drawSimulated(pipe),
                () -> sink instanceof FluidStack fluid && fluid.isIn(net.minecraft.tags.FluidTags.WATER)
                        && (fluid.has(ThirstComponents.WATER_PURITY) || fluid.has(ThirstComponents.WATER_SALTY)));
    }

    private static FluidStack water(int amount, WaterQuality quality) {
        return WaterFluids.stamp(new FluidStack(Fluids.WATER, amount), quality);
    }

    private static FluidStack drawSimulated(OpenEndedPipe pipe) {
        try {
            return (FluidStack) REMOVE_FLUID_FROM_SPACE.invokeExact(pipe, true);
        } catch (Throwable error) {
            throw new IllegalStateException(error);
        }
    }
}
