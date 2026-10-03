package com.thirstwastaken2.hearthandharvestforge.mixin;

import com.thirstwastaken2.hearthandharvestforge.GradedItemFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandlerItem;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * A click on a placed Jug pours the held container into it, or fills the container from it, through the
 * container's fluid capability, inside a lambda of {@code use}. With Forge's bucket wrapper the grade was
 * lost both ways, so a Dirty bucket came back out Clean. The lambda is handed the container's handler
 * wrapped in {@link GradedItemFluidHandler}, so the jug keeps the grade and refuses a second one.
 *
 * <p>A lambda's name is fixed when the mod is compiled and never remapped, so {@code lambda$use$0} is the
 * same in a production jar; the plugin checks it is still there. Named by string, since nothing
 * compiles against Hearth and Harvest.
 */
@Mixin(targets = "alabaster.hearthandharvest.common.block.JugBlock")
abstract class JugBlockMixin {
    @ModifyVariable(method = "lambda$use$0", at = @At("HEAD"), argsOnly = true)
    private static IFluidHandlerItem thirst$keepGrade(IFluidHandlerItem handler) {
        return GradedItemFluidHandler.of(handler);
    }
}
