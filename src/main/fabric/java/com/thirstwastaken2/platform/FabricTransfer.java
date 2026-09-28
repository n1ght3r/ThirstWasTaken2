package com.thirstwastaken2.platform;

import com.thirstwastaken2.purity.WaterQuality;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.minecraft.world.level.material.Fluids;

/**
 * Fabric Transfer API calls whose names changed between the Fabric API versions the nodes use, and how
 * the grade rides on a fluid variant: one component, {@code water_purity} or {@code water_salty}, like
 * Create's fluid stacks. Only these two are read, and any other data on the water is ignored. On 1.20.1,
 * which has no components, a variant carries NBT instead, and the grade goes in the same
 * {@code thirstwastaken2} compound {@link ItemWaterData} gives an item: {@code purity} or {@code salty}.
 */
public final class FabricTransfer {
    private static final WaterQuality[] QUALITIES = {
            WaterQuality.fresh(0), WaterQuality.fresh(1), WaterQuality.fresh(2), WaterQuality.fresh(3),
            WaterQuality.SALT};
    //? if <1.20.5 {
    /*private static final String TAG = "thirstwastaken2";
    *///?}
    private static FluidVariant[] waters;

    private FabricTransfer() { }

    /** Water of {@code quality} as a variant. Built once per quality, since pipes ask for it every tick. */
    public static FluidVariant water(WaterQuality quality) {
        FluidVariant[] built = waters;
        if (built == null) {
            built = new FluidVariant[QUALITIES.length];
            for (int i = 0; i < QUALITIES.length; i++) built[i] = build(QUALITIES[i]);
            waters = built;
        }
        return built[quality instanceof WaterQuality.Fresh fresh ? fresh.purity() : QUALITIES.length - 1];
    }

    private static FluidVariant build(WaterQuality quality) {
        //? if >=1.20.5 {
        net.minecraft.core.component.DataComponentPatch patch = quality instanceof WaterQuality.Fresh fresh
                ? net.minecraft.core.component.DataComponentPatch.builder()
                        .set(ThirstComponents.WATER_PURITY, fresh.purity()).build()
                : net.minecraft.core.component.DataComponentPatch.builder()
                        .set(ThirstComponents.WATER_SALTY, true).build();
        return FluidVariant.of(Fluids.WATER, patch);
        //?} else {
        /*net.minecraft.nbt.CompoundTag data = new net.minecraft.nbt.CompoundTag();
        if (quality instanceof WaterQuality.Fresh fresh) {
            data.putInt("purity", fresh.purity());
        } else {
            data.putBoolean("salty", true);
        }
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.put(TAG, data);
        return FluidVariant.of(Fluids.WATER, tag);
        *///?}
    }

    /** Whether a variant records sea water. */
    public static boolean salty(FluidVariant variant) {
        //? if >=1.20.5 {
        return Boolean.TRUE.equals(component(variant, ThirstComponents.WATER_SALTY));
        //?} else {
        /*net.minecraft.nbt.CompoundTag data = data(variant);
        return data != null && data.getBoolean("salty");
        *///?}
    }

    /** The grade a variant records, or {@code null} when it records none. */
    public static Integer grade(FluidVariant variant) {
        //? if >=1.20.5 {
        return component(variant, ThirstComponents.WATER_PURITY);
        //?} else {
        /*net.minecraft.nbt.CompoundTag data = data(variant);
        return data != null && data.contains("purity", net.minecraft.nbt.Tag.TAG_ANY_NUMERIC) ? data.getInt("purity") : null;
        *///?}
    }

    /** Whether a variant carries no data of its own at all, this mod's or anyone's. */
    public static boolean isPlain(FluidVariant variant) {
        //? if >=1.20.5 {
        return variant.componentsMatch(net.minecraft.core.component.DataComponentPatch.EMPTY);
        //?} else {
        /*return !variant.hasNbt();
        *///?}
    }

    // One component a fluid variant carries, or null. Fabric API 8 (Minecraft 26.1 and later) renamed
    // the full component map from getComponentMap to getComponents, a name that returned the patch before.
    //? if >=26.1 {
    private static <T> T component(FluidVariant variant, net.minecraft.core.component.DataComponentType<T> type) {
        return variant.getComponents().get(type);
    }
    //?} elif >=1.20.5 {
    /*private static <T> T component(FluidVariant variant, net.minecraft.core.component.DataComponentType<T> type) {
        return variant.getComponentMap().get(type);
    }
    *///?} else {
    /*// The variant's own compound of this mod's, or null.
    private static net.minecraft.nbt.CompoundTag data(FluidVariant variant) {
        net.minecraft.nbt.CompoundTag tag = variant.getNbt();
        return tag != null && tag.contains(TAG, net.minecraft.nbt.Tag.TAG_COMPOUND) ? tag.getCompound(TAG) : null;
    }
    *///?}
}
