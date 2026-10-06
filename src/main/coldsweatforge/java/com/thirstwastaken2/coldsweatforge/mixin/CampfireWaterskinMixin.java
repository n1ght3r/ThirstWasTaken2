package com.thirstwastaken2.coldsweatforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.thirstwastaken2.coldsweatforge.BoiledWater;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Cold Sweat's waterskin boiled on a campfire, as on NeoForge. Cold Sweat's own recipe takes any filled
 * skin and hands back a new one, which would drop the grade. Rather than a recipe of this mod's competing
 * with it for the same input, the skin that comes off keeps the water it went on with, boiled Clean
 * as anything heat touches, Clean and Pure as they were, salt water still salt. Cold Sweat's own campfire
 * mixin sets the temperature on the same call, through a {@code @ModifyArg} that leaves the stack's tag
 * otherwise alone.
 */
@Mixin(CampfireBlockEntity.class)
abstract class CampfireWaterskinMixin {
    @WrapOperation(method = "cookTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/Containers;dropItemStack(Lnet/minecraft/world/level/Level;DDDLnet/minecraft/world/item/ItemStack;)V"))
    private static void thirst$boilWaterskin(Level level, double x, double y, double z, ItemStack result,
                                             Operation<Void> original, @Local(ordinal = 0) ItemStack cooked) {
        if (!WaterPurity.isStamped(result) && WaterPurity.isStamped(cooked) && result.getItem() == cooked.getItem()
                && WaterPurity.isWaterContainer(result)) {
            WaterPurity.setQuality(result, BoiledWater.campfire(WaterPurity.quality(cooked)));
        }
        original.call(level, x, y, z, result);
    }
}
