package com.thirstwastaken2.kaleidoscopetavern;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.kaleidoscopetavern.mixins.json} only where Kaleidoscope Tavern is
 * installed, and each mixin only where its own target is still there, so a renamed tap behaviour does
 * not take the barrel down with it. {@code BottleDropMixin} targets a Minecraft class, which is always
 * there, so it is applied whenever the mod is.
 */
public final class KaleidoscopeTavernMixinPlugin implements IMixinConfigPlugin {
    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return KaleidoscopeTavernPresence.hasTarget(targetClassName);
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) { }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) { }
}
