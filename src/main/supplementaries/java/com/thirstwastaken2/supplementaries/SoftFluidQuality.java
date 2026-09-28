package com.thirstwastaken2.supplementaries;

import com.mojang.datafixers.util.Pair;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.item.ThirstItems;
import com.thirstwastaken2.platform.ThirstComponents;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import com.thirstwastaken2.supplementaries.mixin.FluidContainerListAccessor;
import net.mehvahdjukaar.moonlight.api.fluids.FluidContainerList;
import net.mehvahdjukaar.moonlight.api.fluids.MLBuiltinSoftFluids;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluid;
import net.mehvahdjukaar.moonlight.api.fluids.SoftFluidStack;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Water quality on Moonlight's soft fluid stacks, the form water takes inside a Supplementaries jar,
 * goblet or faucet.
 *
 * <p>A soft fluid stack carries data components the way an item stack does, so it can hold the grade
 * itself. It keeps exactly one of the two, {@code water_purity} for a grade or {@code water_salty} for
 * sea water, so two stacks of the same water always compare equal and share a tank: Moonlight refuses
 * to mix stacks whose components differ, which is what keeps a jar of Murky water from quietly taking
 * Clean water. A container filled from a tank gets both components back, and its sprite, because
 * {@link WaterPurity#setQuality} stamps it on the way out.
 *
 * <p>This is {@code WaterFluids} for soft fluids, and it keeps the same rules for the same reasons.
 */
public final class SoftFluidQuality {
    /** What a terracotta water bowl holds, in bottles. One serving, as every other part of the mod has it. */
    private static final int BOWL_SERVINGS = 1;

    /**
     * What water of each grade looks like in a jar, a goblet or a faucet, indexed by grade, and the one
     * sea water gets.
     *
     * <p>These are the mod's own water colours, read off the terracotta water bowl's sprites, so that a
     * jar of water looks like a bowl of the same water rather than like a third opinion. They are not
     * the tooltip palette in `WaterPurity`, which is tuned to stay legible on a dark tooltip and puts
     * sea water in the pale cream of dried salt; that is right for a line of text and wrong for a
     * block of water, and `purity/AGENTS.md` already says the sprites differ for exactly that reason.
     *
     * <p>Alpha is part of a soft fluid's tint, so it is written out: a tint with none renders nothing.
     */
    private static final int[] GRADE_TINT = {0xFF53371B, 0xFF344C5B, 0xFF234FCC, 0xFF1977AA};
    private static final int SALT_TINT = 0xFF14707E;

    private SoftFluidQuality() { }

    public static boolean isWater(SoftFluidStack stack) {
        return !stack.isEmpty() && stack.is(MLBuiltinSoftFluids.WATER);
    }

    /** Asked of the registry entry rather than of a stack, by the mark on it; see {@link WaterSoftFluid}. */
    public static boolean isWater(SoftFluid fluid) {
        return fluid instanceof WaterSoftFluid water && water.thirst$isWater();
    }

    /** {@code servings} bottles of water of a known grade, as a faucet or a block hands it on. */
    public static SoftFluidStack water(Level level, WaterQuality quality, int servings) {
        return stamp(SoftFluidStack.of(MLBuiltinSoftFluids.WATER.getHolder(level), servings), quality);
    }

    /** Unstamped water, from a creative tank or a mod that knows nothing of grades, is the config default. */
    public static WaterQuality quality(SoftFluidStack stack) {
        if (Boolean.TRUE.equals(stack.get(ThirstComponents.WATER_SALTY))) return WaterQuality.SALT;
        Integer purity = stack.get(ThirstComponents.WATER_PURITY);
        return WaterQuality.fresh(purity != null ? purity : ThirstConfig.get().defaultPurity);
    }

    /** Writes {@code quality} onto water and returns the same stack. Anything else is left alone. */
    public static SoftFluidStack stamp(SoftFluidStack stack, WaterQuality quality) {
        if (!isWater(stack)) return stack;
        switch (quality) {
            case WaterQuality.Salt ignored -> {
                stack.getComponents().remove(ThirstComponents.WATER_PURITY);
                stack.set(ThirstComponents.WATER_SALTY, true);
            }
            case WaterQuality.Fresh fresh -> {
                stack.getComponents().remove(ThirstComponents.WATER_SALTY);
                stack.set(ThirstComponents.WATER_PURITY, fresh.purity());
            }
        }
        return stack;
    }

    /**
     * Gives water about to enter a tank the grade it already reads as, so that what a tank holds is
     * always stamped. Water that arrives without a grade would otherwise refuse to share a tank with
     * water that has one, although both read as {@code defaultPurity} to everything else.
     *
     * <p>Fresh water poured out of a container arrives carrying both components, since that is what the
     * mod writes on an item; this is also where it comes down to the one the tank compares on.
     */
    public static void normalise(SoftFluidStack stack) {
        if (isWater(stack)) stamp(stack, quality(stack));
    }

    /**
     * Stamps a container a tank has just filled. Moonlight builds it from scratch, so it goes through
     * {@link WaterPurity#setQuality} rather than keeping whatever was copied onto it: that is what
     * writes {@code water_salty: false} on fresh water, which every purification recipe matches on, and
     * what points a salty bottle at the sprite that says so.
     */
    public static Pair<ItemStack, FluidContainerList.Category> filled(
            Pair<ItemStack, FluidContainerList.Category> filled, WaterQuality quality) {
        if (filled != null && quality != null) WaterPurity.setQuality(filled.getFirst(), quality);
        return filled;
    }

    /**
     * The components water carries across every conversion Moonlight makes of its own accord. It copies
     * only what the soft fluid lists as preserved, and {@code moonlight:water} lists another thirst
     * mod's component, not this one's, so without this the grade is dropped the moment water becomes a
     * NeoForge fluid stack: a jar is an {@code IFluidHandler} there, so a pipe or a faucet pointed at a
     * tank moves its water through one.
     */
    public static HolderSet<DataComponentType<?>> preserving(HolderSet<DataComponentType<?>> preserved) {
        List<Holder<DataComponentType<?>>> types = new ArrayList<>(preserved.stream().toList());
        types.add(holder(ThirstComponents.WATER_PURITY));
        types.add(holder(ThirstComponents.WATER_SALTY));
        return HolderSet.direct(types);
    }

    /**
     * The colour a jar or a goblet draws its water in. Water normally takes the colour of the biome it
     * came from, which says nothing a player of this mod wants to know about a jar; the grade does.
     */
    public static int tint(SoftFluidStack stack, int original) {
        if (!isWater(stack)) return original;
        return switch (quality(stack)) {
            case WaterQuality.Salt ignored -> SALT_TINT;
            case WaterQuality.Fresh fresh -> GRADE_TINT[fresh.purity()];
        };
    }

    /** The line the mod adds to the tooltip of a jar that holds water, in the words it uses elsewhere. */
    public static void tooltip(SoftFluidStack stack, Consumer<Component> tooltip) {
        if (!isWater(stack)) return;
        tooltip.accept(switch (quality(stack)) {
            case WaterQuality.Salt ignored -> WaterPurity.saltTooltip();
            case WaterQuality.Fresh fresh -> WaterPurity.tooltip(fresh.purity());
        });
    }

    private static Holder<DataComponentType<?>> holder(DataComponentType<?> type) {
        return BuiltInRegistries.DATA_COMPONENT_TYPE.wrapAsHolder(type);
    }

    /**
     * Marks the water entry of the load in progress, and tells Moonlight that the mod's terracotta bowl
     * is one of its containers, so a jar, a goblet and a faucet fill and empty it the way they do a
     * vanilla bottle. `moonlight:water` lists a handful of other mods' cups by hand and this is the same
     * list; it has to be written before Moonlight builds the map from item to fluid, which is what
     * decides whether an item can be poured out at all.
     *
     * <p>The waterskin is deliberately left out: it holds three servings with a fill level of its own,
     * and a container list maps one empty item to one filled item at a fixed capacity, which cannot say
     * that. On NeoForge a faucet reaches the waterskin through its fluid capability anyway.
     */
    public static void adoptWater(HolderLookup.Provider registries) {
        SoftFluid fluid = MLBuiltinSoftFluids.WATER.get(registries);
        ((WaterSoftFluid) fluid).thirst$markWater();
        FluidContainerListAccessor containers = (FluidContainerListAccessor) (Object) fluid.getContainerList();
        containers.thirst$add(ThirstItems.TERRACOTTA_BOWL, ThirstItems.TERRACOTTA_WATER_BOWL, BOWL_SERVINGS);
    }
}
