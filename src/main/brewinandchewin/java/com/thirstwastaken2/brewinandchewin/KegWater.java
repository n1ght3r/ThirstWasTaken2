package com.thirstwastaken2.brewinandchewin;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.platform.ThirstComponents;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluids;
import umpaz.brewinandchewin.common.utility.AbstractedFluidStack;

import java.util.Optional;

/**
 * Water quality on the keg's fluid, the only place that reads or writes one on Brewin' and Chewin's
 * {@link AbstractedFluidStack}.
 *
 * <p>The grade lives on the fluid itself, as one component, {@code water_purity} for a grade or
 * {@code water_salty} for sea water: the same component NeoForge's {@code FluidStack} and Fabric's
 * {@code FluidVariant} carry for this mod, which is what both loaders' keg tanks turn the stack into.
 * So the tank saves it, sends it to the client, keeps it in a picked-up keg and refuses to mix two
 * grades, all with no help from here. What this adds is the way in and out through the keg's pouring
 * recipes, which build their water from the recipe rather than from the bucket or bottle.
 *
 * <p>Both loaders keep a stack's components only when they are a {@link PatchedDataComponentMap}, which
 * is what a stamped stack is built with.
 */
public final class KegWater {
    private KegWater() { }

    public static boolean isWater(AbstractedFluidStack stack) {
        return !stack.isEmpty() && stack.fluid().isSame(Fluids.WATER);
    }

    /** Unstamped water, poured in before this integration or from a plain bucket, counts as {@code defaultPurity}. */
    public static WaterQuality quality(AbstractedFluidStack stack) {
        DataComponentMap components = stack.components();
        if (Boolean.TRUE.equals(components.get(ThirstComponents.WATER_SALTY))) return WaterQuality.SALT;
        Integer purity = components.get(ThirstComponents.WATER_PURITY);
        return WaterQuality.fresh(purity != null ? purity : ThirstConfig.get().defaultPurity);
    }

    /**
     * What a pouring recipe's {@code fluid} is once {@code container} pours it: the recipe's water with
     * the container's grade. Only a stamped container stamps the water, so a plain bucket still pours
     * plain water and tops up a keg filled before this integration existed.
     */
    public static AbstractedFluidStack pouredFrom(ItemStack container, AbstractedFluidStack fluid) {
        if (!isWater(fluid) || !WaterPurity.isWaterContainer(container) || !WaterPurity.isStamped(container)) {
            return fluid;
        }
        return stamped(fluid, WaterPurity.quality(container));
    }

    /**
     * The container a pouring recipe hands back from the keg, stamped with the grade of the water it
     * came from. Stamped even from plain water, through {@code WaterPurity.setQuality}, so a drawn bottle
     * gets {@code water_salty: false} and stays cookable.
     */
    public static ItemStack drawn(ItemStack result, AbstractedFluidStack tank) {
        if (isWater(tank) && WaterPurity.isWaterContainer(result)) WaterPurity.setQuality(result, quality(tank));
        return result;
    }

    /**
     * Whether the keg holds sea water. Asked on every fermenting tick, so it reads the patch the stack
     * already has rather than building a component map.
     */
    public static boolean isSalt(AbstractedFluidStack stack) {
        if (!isWater(stack)) return false;
        Optional<? extends Boolean> salty = stack.componentPatch().get(ThirstComponents.WATER_SALTY);
        return salty != null && salty.isPresent() && salty.get();
    }

    /**
     * Whether {@code held} is a stamped water container and {@code expected}, a recipe's stack, is not:
     * the one case where the two may still be the same water container with a grade on top.
     */
    public static boolean isGradedFormOf(ItemStack expected, ItemStack held) {
        return WaterPurity.isStamped(held) && WaterPurity.isWaterContainer(held) && !WaterPurity.isStamped(expected);
    }

    /** {@code held} without its grade, to compare with a recipe's plain stack. */
    public static ItemStack plain(ItemStack held) {
        return WaterPurity.unstamped(held);
    }

    /** A copy of {@code stack} holding {@code quality} as its one quality component. */
    private static AbstractedFluidStack stamped(AbstractedFluidStack stack, WaterQuality quality) {
        PatchedDataComponentMap components = PatchedDataComponentMap.fromPatch(DataComponentMap.EMPTY, stack.componentPatch());
        switch (quality) {
            case WaterQuality.Salt ignored -> {
                components.remove(ThirstComponents.WATER_PURITY);
                components.set(ThirstComponents.WATER_SALTY, true);
            }
            case WaterQuality.Fresh fresh -> {
                components.remove(ThirstComponents.WATER_SALTY);
                components.set(ThirstComponents.WATER_PURITY, fresh.purity());
            }
        }
        // No loader stack: both tanks would take it over the components.
        return new AbstractedFluidStack(stack.fluid(), stack.amount(), components, stack.unit(), null);
    }
}
