package com.thirstwastaken2.datagen;

import com.thirstwastaken2.damage.ThirstDamageTypes;
import net.fabricmc.fabric.api.datagen.v1.FabricPackOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricTagsProvider;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

import java.util.concurrent.CompletableFuture;

/**
 * Dehydration bypasses armour, like starvation, and hurts the way drowning does: no knockback and no
 * impact, so a thirsty player is not shoved around by their own thirst. Vanilla decides knockback from
 * {@code no_knockback} alone, so a damage type missing from it knocks the player in a random direction.
 *
 * <p>Every entry is optional, because the tags it adds to are vanilla's and the damage type it names
 * is the mod's: a datapack that removes {@code thirstwastaken2:dehydrate} must not break every other
 * entry in the tag with it.
 */
public final class ThirstDamageTypeTagProvider extends FabricTagsProvider<DamageType> {
    public ThirstDamageTypeTagProvider(FabricPackOutput output, CompletableFuture<HolderLookup.Provider> registries) {
        super(output, Registries.DAMAGE_TYPE, registries);
    }

    @Override
    protected void addTags(HolderLookup.Provider registries) {
        addDehydrate(DamageTypeTags.BYPASSES_ARMOR);
        // The no_knockback tag arrived with 1.20.2.
        //? if >=1.20.5
        addDehydrate(DamageTypeTags.NO_KNOCKBACK);
        addDehydrate(DamageTypeTags.NO_IMPACT);
    }

    private void addDehydrate(TagKey<DamageType> tag) {
        // Fabric API's tag builder was called getOrCreateTagBuilder on 1.21.1.
        //? if >1.21.1 {
        builder(tag).addOptional(ThirstDamageTypes.DEHYDRATE);
        //?} else
        //getOrCreateTagBuilder(tag).addOptional(ThirstDamageTypes.DEHYDRATE);
    }

    @Override
    public String getName() {
        return "ThirstWasTaken2 Damage Type Tags";
    }
}
