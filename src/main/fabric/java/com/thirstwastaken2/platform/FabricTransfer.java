package com.thirstwastaken2.platform;

import com.thirstwastaken2.purity.ThirstComponents;
import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.world.level.material.Fluids;

/**
 * Fabric Transfer API calls whose names changed between the Fabric API versions the nodes use, and how
 * the grade rides on a fluid variant: one component, {@code water_purity} or {@code water_salty}, like
 * Create's fluid stacks. Only these two are read, and any other data on the water is ignored.
 */
public final class FabricTransfer {
    private static final WaterQuality[] QUALITIES = {
            WaterQuality.fresh(0), WaterQuality.fresh(1), WaterQuality.fresh(2), WaterQuality.fresh(3),
            WaterQuality.SALT};
    private static FluidVariant[] waters;

    private FabricTransfer() { }

    /** Water of {@code quality} as a variant. Built once per quality, since pipes ask for it every tick. */
    public static FluidVariant water(WaterQuality quality) {
        FluidVariant[] built = waters;
        if (built == null) {
            built = new FluidVariant[QUALITIES.length];
            for (int i = 0; i < QUALITIES.length; i++) {
                DataComponentPatch patch = QUALITIES[i] instanceof WaterQuality.Fresh fresh
                        ? DataComponentPatch.builder().set(ThirstComponents.WATER_PURITY, fresh.purity()).build()
                        : DataComponentPatch.builder().set(ThirstComponents.WATER_SALTY, true).build();
                built[i] = FluidVariant.of(Fluids.WATER, patch);
            }
            waters = built;
        }
        return built[quality instanceof WaterQuality.Fresh fresh ? fresh.purity() : QUALITIES.length - 1];
    }

    /** Whether a variant records sea water. */
    public static boolean salty(FluidVariant variant) {
        return Boolean.TRUE.equals(component(variant, ThirstComponents.WATER_SALTY));
    }

    /** The grade a variant records, or {@code null} when it records none. */
    public static Integer grade(FluidVariant variant) {
        return component(variant, ThirstComponents.WATER_PURITY);
    }

    /** Whether a variant carries no data of its own at all, this mod's or anyone's. */
    public static boolean isPlain(FluidVariant variant) {
        return variant.componentsMatch(DataComponentPatch.EMPTY);
    }

    /**
     * One component a fluid variant carries, or {@code null}. Fabric API 8 (Minecraft 26.1 and later)
     * renamed the full component map from {@code getComponentMap} to {@code getComponents}, a name that
     * returned the patch before.
     */
    private static <T> T component(FluidVariant variant, DataComponentType<T> type) {
        //? if >=26.1 {
        return variant.getComponents().get(type);
        //?} else {
        /*return variant.getComponentMap().get(type);
        *///?}
    }
}
