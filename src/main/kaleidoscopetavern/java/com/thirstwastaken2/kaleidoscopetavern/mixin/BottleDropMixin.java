package com.thirstwastaken2.kaleidoscopetavern.mixin;

import com.thirstwastaken2.kaleidoscopetavern.TavernWater;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/**
 * A placed water bottle drops the grade it was put down with. The mod's bottle block does not override
 * {@code getDrops}, and every way a block drops goes through this one: broken, blown up, pushed by a
 * piston, and picked up by hand, which the bottle builds from {@code Block.getDrops}. Minecraft's class, so
 * remapped; the stacks are stamped in place, which leaves whatever list the loot table built as it is.
 */
@Mixin(BlockBehaviour.class)
abstract class BottleDropMixin {
    @Inject(method = "getDrops", at = @At("RETURN"))
    private void thirst$stampPlacedBottle(BlockState state, LootParams.Builder params,
                                          CallbackInfoReturnable<List<ItemStack>> cir) {
        TavernWater.dropped(cir.getReturnValue(), state, params);
    }
}
