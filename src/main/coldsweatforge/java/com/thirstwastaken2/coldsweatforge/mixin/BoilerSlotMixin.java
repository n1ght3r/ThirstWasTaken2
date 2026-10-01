package com.thirstwastaken2.coldsweatforge.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.thirstwastaken2.coldsweatforge.BoiledWater;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The Boiler menu's water slots, an anonymous class, take graded water as the block does through a
 * hopper; see {@link BoilerBlockEntityMixin}. Named by its synthetic name, so the mixin plugin checks it
 * is still there and still has {@code mayPlace}, which is Minecraft's and remapped.
 */
@Mixin(targets = "com.momosoftworks.coldsweat.common.container.BoilerContainer$2")
abstract class BoilerSlotMixin {
    @WrapOperation(method = "mayPlace", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/tags/TagKey;)Z"))
    private boolean thirst$acceptWater(ItemStack stack, TagKey<Item> tag, Operation<Boolean> original) {
        return original.call(stack, tag) || BoiledWater.accepts(stack);
    }
}
