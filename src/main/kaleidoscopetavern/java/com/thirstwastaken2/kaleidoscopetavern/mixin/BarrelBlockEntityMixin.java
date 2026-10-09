package com.thirstwastaken2.kaleidoscopetavern.mixin;

import com.github.ysbbbbbb.kaleidoscopetavern.blockentity.brew.BarrelBlockEntity;
import com.github.ysbbbbbb.kaleidoscopetavern.crafting.container.BarrelRecipeContainer;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thirstwastaken2.kaleidoscopetavern.BarrelWater;
import com.thirstwastaken2.kaleidoscopetavern.ReturnedWater;
import com.thirstwastaken2.kaleidoscopetavern.TavernWater;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The barrel keeps the grade of the water poured into it and hands it back on the container drawn out,
 * refuses sea water, and on Refabricated refuses this mod's own containers; see {@link TavernWater}. The
 * barrel is eight blocks with one block entity, the one every click reaches through
 * {@code BarrelBlock.getBarrelEntity}, so this is the only place a grade is kept.
 *
 * <p>The fermented drink is its own item and carries no grade: alcohol and weeks in a barrel stand in for
 * boiling, as they do for Brewin' and Chewin's keg.
 */
@Mixin(value = BarrelBlockEntity.class, remap = false)
abstract class BarrelBlockEntityMixin implements BarrelWater {
    /** What was poured in. Only meaningful while {@link #thirst$heldWater} says there is water. */
    @Unique private WaterQuality thirst$quality;

    /**
     * Whether the tank held water when last read, and the game tick it was read on. Jade asks every
     * frame while a player looks at the barrel, so the tank is read at most once a tick. Plain fields of
     * this block entity, gone with it. Forgotten whenever this mixin sees the tank change: a fill, a
     * draw and a load, which is also how the client hears of a change.
     */
    @Unique private boolean thirst$tankWater;
    @Unique private long thirst$waterReadAt = Long.MIN_VALUE;

    @Override
    public WaterQuality thirst$heldWater() {
        if (thirst$quality == null) return null;
        BarrelBlockEntity barrel = (BarrelBlockEntity) (Object) this;
        // Once a recipe starts, the tank is emptied into the brew. Read live, so the cached tank below is
        // never asked while the barrel brews.
        if (barrel.isBrewing()) return null;
        return thirst$holdsWater(barrel) ? thirst$quality : null;
    }

    /**
     * The tank is NeoForge's or Forge's FluidTank on the official mod and a Fabric Transfer API storage on
     * Refabricated, so its fluid is read the way the mod's own recipe check reads it, through the
     * container both builds make the same way and which hands back a plain Fluid. Without a level there
     * is no tick to key on, and it is read every time.
     */
    @Unique
    private boolean thirst$holdsWater(BarrelBlockEntity barrel) {
        long now = barrel.getLevel() == null ? Long.MIN_VALUE : barrel.getLevel().getGameTime();
        if (now == Long.MIN_VALUE || now != thirst$waterReadAt) {
            thirst$tankWater = TavernWater.isWater(new BarrelRecipeContainer(barrel.getIngredient(), barrel.getFluid()).getFluid());
            thirst$waterReadAt = now;
        }
        return thirst$tankWater;
    }

    @Unique
    private void thirst$forgetTank() {
        thirst$waterReadAt = Long.MIN_VALUE;
    }

    /**
     * A refused container uses the click up rather than failing it: the barrel would otherwise try to fill
     * it, and then pass the click on, and a bucket of sea water would be emptied over the barrel's lid.
     */
    @WrapMethod(method = "addFluid")
    private boolean thirst$keepPouredGrade(LivingEntity user, ItemStack stack, Operation<Boolean> original) {
        if (TavernWater.refuses(user, stack)) return true;
        // Read before the call: it empties the container.
        WaterQuality held = thirst$heldWater();
        WaterQuality poured = TavernWater.of(stack);
        boolean added = original.call(user, stack);
        thirst$forgetTank();
        if (added) thirst$quality = TavernWater.pouredOnto(held, poured);
        return added;
    }

    @WrapMethod(method = "removeFluid")
    private boolean thirst$returnGrade(LivingEntity user, ItemStack stack, Operation<Boolean> original) {
        if (TavernWater.refusesContainer(stack)) return false;
        boolean removed = ReturnedWater.during(thirst$heldWater(), () -> original.call(user, stack));
        thirst$forgetTank();
        return removed;
    }

    // Minecraft's own methods, so remapped: the Fabric 1.21.x jars name them in intermediary. From
    // 1.21.6 they take a ValueOutput / ValueInput rather than a tag; before 1.20.5 they take no
    // registries, and loading is `load`.
    //? if >=1.21.6 {
    @Inject(method = "saveAdditional", at = @At("TAIL"), remap = true)
    private void thirst$saveGrade(net.minecraft.world.level.storage.ValueOutput output, CallbackInfo ci) {
        TavernWater.save(output::putInt, thirst$heldWater());
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"), remap = true)
    private void thirst$loadGrade(net.minecraft.world.level.storage.ValueInput input, CallbackInfo ci) {
        thirst$quality = TavernWater.load(input::getIntOr);
        thirst$forgetTank();
    }
    //?} elif >=1.20.5 {
    /*@Inject(method = "saveAdditional", at = @At("TAIL"), remap = true)
    private void thirst$saveGrade(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, CallbackInfo ci) {
        TavernWater.save(tag::putInt, thirst$heldWater());
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"), remap = true)
    private void thirst$loadGrade(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries, CallbackInfo ci) {
        thirst$quality = TavernWater.load((key, fallback) -> com.thirstwastaken2.platform.Vanilla.getInt(tag, key, fallback));
        thirst$forgetTank();
    }
    *///?} else {
    /*@Inject(method = "saveAdditional", at = @At("TAIL"), remap = true)
    private void thirst$saveGrade(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        TavernWater.save(tag::putInt, thirst$heldWater());
    }

    @Inject(method = "load", at = @At("TAIL"), remap = true)
    private void thirst$loadGrade(net.minecraft.nbt.CompoundTag tag, CallbackInfo ci) {
        thirst$quality = TavernWater.load((key, fallback) -> com.thirstwastaken2.platform.Vanilla.getInt(tag, key, fallback));
        thirst$forgetTank();
    }
    *///?}
}
