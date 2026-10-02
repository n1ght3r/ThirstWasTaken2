package com.thirstwastaken2.herbalbrews.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.satisfy.herbalbrews.core.blocks.entity.TeaKettleBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Sea water is not the water a Tea Kettle asks for, as Farm & Charm's Cooking Pot and Brewin' and
 * Chewin's keg make nothing from it. The kettle's tick empties its water slot into a level that keeps no
 * grade, once the stack is in {@code #herbalbrews:small_water_fill} (every potion) or
 * {@code #herbalbrews:large_water_fill} (the water bucket), matched by item alone.
 *
 * <p>A salty stack is in neither tag here, so it stays in the slot, unused. The heat item check in the
 * same tick goes through the same call; no heat item is ever salty, so it is untouched. Fresh water of
 * any grade still fills the kettle: it boils, and what comes out is its own item.
 */
@Mixin(value = TeaKettleBlockEntity.class, remap = false)
abstract class TeaKettleMixin {
    // Minecraft's own method, so remapped on Fabric, whose HerbalBrews jar is in intermediary.
    @WrapOperation(method = "tick", at = @At(value = "INVOKE", remap = true,
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean thirst$seaWaterFillsNothing(ItemStack stack, TagKey<Item> tag, Operation<Boolean> original) {
        return original.call(stack, tag) && !WaterPurity.isSalty(stack);
    }
}
