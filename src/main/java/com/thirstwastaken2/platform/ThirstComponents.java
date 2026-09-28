package com.thirstwastaken2.platform;

//? if >=1.20.5 {
import com.mojang.serialization.Codec;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.item.WaterskinItem;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.codec.ByteBufCodecs;
//?}

/**
 * The data component types a water container's servings, grade and salt are stored in. Only
 * {@link ItemWaterData} and the fluid code of each loader name them. Minecraft 1.20.1 has no data
 * components, so there this class is empty and {@link ItemWaterData} keeps the same three values in the
 * stack's tag instead.
 */
public final class ThirstComponents {
    //? if >=1.20.5 {
    // Servings in a waterskin, canteen or flask. Widening the range keeps every saved stack valid.
    public static final DataComponentType<Integer> WATER_SERVINGS = DataComponentType.<Integer>builder()
            .persistent(Codec.intRange(0, WaterskinItem.MAX_CAPACITY))
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build();

    // The grade of the fresh water inside. Salt water carries WATER_SALTY instead.
    public static final DataComponentType<Integer> WATER_PURITY = DataComponentType.<Integer>builder()
            .persistent(Codec.intRange(WaterPurity.MIN, WaterPurity.MAX))
            .networkSynchronized(ByteBufCodecs.VAR_INT)
            .build();

    // Sea water. It is not a grade: a salty container carries no WATER_PURITY at all, so nothing can
    // read a grade off water that has none, and no purification recipe can match it.
    public static final DataComponentType<Boolean> WATER_SALTY = DataComponentType.<Boolean>builder()
            .persistent(Codec.BOOL)
            .networkSynchronized(ByteBufCodecs.BOOL)
            .build();
    //?}

    private ThirstComponents() { }

    /**
     * Registers the component types. They are built with the class, but registered only here, so touching
     * a field early never writes into a registry a loader may still have frozen.
     */
    public static void register() {
        //? if >=1.20.5 {
        register("water_servings", WATER_SERVINGS);
        register("water_purity", WATER_PURITY);
        register("water_salty", WATER_SALTY);
        //?}
    }

    //? if >=1.20.5 {
    private static void register(String name, DataComponentType<?> type) {
        Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, ThirstWasTaken2.id(name), type);
    }
    //?}
}
