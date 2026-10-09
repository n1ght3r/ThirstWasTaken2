package com.thirstwastaken2.kaleidoscope.mixin;

import com.thirstwastaken2.kaleidoscope.BrewedWaterQuality;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Water a Kaleidoscope Tavern tap pours into the stockpot or the teapot below it. Neither mod does this:
 * Kaleidoscope Chinese Food merges {@code kcf$fillCookery(Level, BlockPos tapPos, BlockState source,
 * BlockState destination)} into Tavern's tap, which sets the block's fluid through accessors of its own,
 * past the two calls {@link StockpotBlockEntityMixin} and {@link TeapotBlockEntityMixin} read a grade on.
 * Without this the water came back out unstamped, read as Clean, even from a Dirty cauldron or from the
 * sea, and the teapot brewed tea from sea water.
 *
 * <p>At its head, so the block holds the grade before the addon saves and syncs it. On a method another
 * mod's mixin merges in, named by a string that can change in any release: the plugin applies this only
 * where that mixin still declares it, a priority above the addon's default makes it apply after the
 * addon's has merged the method, and {@code require = 0} lets a missing method skip it rather than fail
 * the game. Tavern's tap is named by string too, since this directory is not compiled against Tavern.
 */
@Mixin(targets = "com.github.ysbbbbbb.kaleidoscopetavern.block.brew.TapBlock", remap = false, priority = 1100)
abstract class TapCookeryMixin {
    @Inject(method = "kcf$fillCookery", at = @At("HEAD"), require = 0)
    private static void thirst$gradeTapped(Level level, BlockPos tapPos, BlockState source, BlockState destination,
                                           CallbackInfo ci) {
        BrewedWaterQuality.tapped(level, tapPos, source);
    }
}
