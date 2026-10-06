package com.thirstwastaken2.extradelight.mixin;

import com.lance5057.extradelight.blocks.jar.JarBlockEntity;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.thirstwastaken2.extradelight.ExtraDelightWater;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;

/**
 * A bucket or other container clicked on a Jar keeps the grade of the water it pours in or draws out;
 * see {@link FluidUtilMixin}. Bottles go through the bottle registry instead, {@link BottleFluidRegistryMixin}.
 */
@Mixin(JarBlockEntity.class)
abstract class JarBlockEntityMixin {
    @WrapMethod(method = "use")
    private boolean thirst$graded(Player player, InteractionHand hand, Operation<Boolean> original) {
        return ExtraDelightWater.graded(() -> original.call(player, hand));
    }
}
