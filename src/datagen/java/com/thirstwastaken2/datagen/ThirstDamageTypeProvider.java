package com.thirstwastaken2.datagen;

import com.thirstwastaken2.damage.ThirstDamageTypes;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricCodecDataProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.damagesource.DamageScaling;
import net.minecraft.world.damagesource.DamageType;

import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;

/**
 * The {@code thirstwastaken2:dehydrate} damage type, so dying of thirst reads as its own death
 * rather than as starvation. {@code ThirstDamageTypes} falls back to vanilla starvation if a
 * datapack removes this, so it is safe to strip but not safe to rename.
 *
 * <p>Scaling is {@code never} because difficulty already changes how fast thirst drains, and the
 * damage carries no exhaustion of its own: charging a dehydrated player exhaustion for dehydrating
 * would compound.
 */
public final class ThirstDamageTypeProvider extends FabricCodecDataProvider<DamageType> {
    // Before 1.20.5 a codec provider names its directory itself and is handed no registries.
    //? if >=1.20.5 {
    public ThirstDamageTypeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, registries, Registries.DAMAGE_TYPE, DamageType.DIRECT_CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, DamageType> provider, HolderLookup.Provider registries) {
        provider.accept(ThirstDamageTypes.DEHYDRATE.identifier(), dehydrate());
    }
    //?} else {
    /*public ThirstDamageTypeProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, net.minecraft.data.PackOutput.Target.DATA_PACK, "damage_type", DamageType.CODEC);
    }

    @Override
    protected void configure(BiConsumer<Identifier, DamageType> provider) {
        provider.accept(ThirstDamageTypes.DEHYDRATE.identifier(), dehydrate());
    }
    *///?}

    private static DamageType dehydrate() {
        return new DamageType("dehydrate", DamageScaling.NEVER, 0.0F);
    }

    @Override
    public String getName() {
        return "ThirstWasTaken2 Damage Types";
    }
}
