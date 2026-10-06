package com.thirstwastaken2.mixin;

import com.thirstwastaken2.data.HealthRegen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Natural healing from food, through {@code HealthRegen}: dehydration and Upset Stomach stop it, the food
 * cost vanilla would have charged for a skipped heal is refunded so hunger is not silently drained, and
 * a heal that goes through gets the water reserve's bonus. As the original MixinFoodData, without its
 * slow heal for a nearly hydrated player.
 */
@Mixin(FoodData.class)
abstract class FoodDataMixin {
    // FoodData#tick took any Player until 1.21.2 narrowed it to ServerPlayer, and heals through it.
    //? if >=1.21.2 {
    private static final String HEAL = "Lnet/minecraft/server/level/ServerPlayer;heal(F)V";
    //?} else
    //private static final String HEAL = "Lnet/minecraft/world/entity/player/Player;heal(F)V";

    @Shadow public abstract void addExhaustion(float amount);

    @Shadow public abstract float getSaturationLevel();

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 0, target = HEAL))
    //? if >=1.21.2 {
    private void thirst$healWithSaturation(ServerPlayer player, float amount) {
    //?} else
    //private void thirst$healWithSaturation(Player player, float amount) {
        if (HealthRegen.blocksFoodHeal(player)) addExhaustion(-Math.min(getSaturationLevel(), HealthRegen.MAX_REFUND));
        else player.heal(amount);
    }

    @Redirect(method = "tick", at = @At(value = "INVOKE", ordinal = 1, target = HEAL))
    //? if >=1.21.2 {
    private void thirst$healWithHunger(ServerPlayer player, float amount) {
    //?} else
    //private void thirst$healWithHunger(Player player, float amount) {
        if (HealthRegen.blocksFoodHeal(player)) addExhaustion(-HealthRegen.MAX_REFUND);
        else player.heal(amount);
    }
}
