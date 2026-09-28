package com.thirstwastaken2.kaleidoscope.mixin;

import com.github.ysbbbbbb.kaleidoscopecookery.api.blockentity.IStockpot;
import com.github.ysbbbbbb.kaleidoscopecookery.blockentity.kitchen.StockpotBlockEntity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thirstwastaken2.kaleidoscope.BrewedWater;
import com.thirstwastaken2.kaleidoscope.BrewedWaterQuality;
import com.thirstwastaken2.kaleidoscope.ReturnedWater;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The stockpot keeps the grade of the water poured into it and hands it back on the bucket taken out.
 * Sea water goes in like any other: a salted soup is a fair thing to cook, and the bucket comes back out
 * salty. The soup itself is not affected, since food has no grade.
 */
@Mixin(value = StockpotBlockEntity.class, remap = false)
abstract class StockpotBlockEntityMixin implements BrewedWater {
    @Shadow private int status;
    @Shadow private Identifier soupBaseId;

    /** What was poured in. Only meaningful while {@link #thirst$heldWater} says there is water. */
    @Unique private WaterQuality thirst$quality;

    @Override
    public WaterQuality thirst$heldWater() {
        // soupBaseId reads as water on an empty pot too; the status says whether anything is in it.
        return status == IStockpot.PUT_INGREDIENT && BrewedWaterQuality.isWater(soupBaseId) ? thirst$quality : null;
    }

    @Override
    public void thirst$holdWater(WaterQuality quality) {
        thirst$quality = quality;
    }

    @WrapMethod(method = "addSoupBase")
    private boolean thirst$keepPouredGrade(Level level, LivingEntity user, ItemStack bucket, Operation<Boolean> original) {
        // Read before the call: it shrinks the bucket.
        WaterQuality poured = BrewedWaterQuality.of(bucket);
        boolean added = original.call(level, user, bucket);
        if (added) thirst$quality = poured;
        return added;
    }

    @WrapMethod(method = "removeSoupBase")
    private boolean thirst$returnGrade(Level level, LivingEntity user, ItemStack bucket, Operation<Boolean> original) {
        boolean removed = ReturnedWater.during(thirst$heldWater(), () -> original.call(level, user, bucket));
        if (removed) thirst$quality = null;
        return removed;
    }

    // Minecraft's own methods, so remapped: the Fabric 1.21.x jars name them in intermediary. From
    // 1.21.6 they take a ValueOutput / ValueInput rather than a tag; before 1.20.5 they take no
    // registries, and loading is `load`.
    //? if >=1.21.6 {
    @Inject(method = "saveAdditional", at = @At("TAIL"), remap = true)
    private void thirst$saveGrade(net.minecraft.world.level.storage.ValueOutput output, CallbackInfo ci) {
        BrewedWaterQuality.save(output::putInt, thirst$heldWater());
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"), remap = true)
    private void thirst$loadGrade(net.minecraft.world.level.storage.ValueInput input, CallbackInfo ci) {
        thirst$quality = BrewedWaterQuality.load(input::getIntOr);
    }
    //?} elif >=1.20.5 {
    /*@Inject(method = "saveAdditional", at = @At("TAIL"), remap = true)
    private void thirst$saveGrade(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, CallbackInfo ci) {
        BrewedWaterQuality.save(tag::putInt, thirst$heldWater());
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"), remap = true)
    private void thirst$loadGrade(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, CallbackInfo ci) {
        thirst$quality = BrewedWaterQuality.load((key, fallback) -> com.thirstwastaken2.platform.Vanilla.getInt(tag, key, fallback));
    }
    *///?} else {
    /*@Inject(method = "saveAdditional", at = @At("TAIL"), remap = true)
    private void thirst$saveGrade(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        BrewedWaterQuality.save(tag::putInt, thirst$heldWater());
    }

    @Inject(method = "load", at = @At("TAIL"), remap = true)
    private void thirst$loadGrade(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        thirst$quality = BrewedWaterQuality.load((key, fallback) -> com.thirstwastaken2.platform.Vanilla.getInt(tag, key, fallback));
    }
    *///?}
}
