package com.thirstwastaken2.herbalbrews;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.herbalbrews.mixins.json} only where HerbalBrews is installed, and each
 * mixin only when every method of the mod's it injects into or calls is still there, so a method
 * renamed upstream turns that mixin off rather than failing the game.
 */
public final class HerbalBrewsMixinPlugin implements IMixinConfigPlugin {
    private static final String KETTLE = "net.satisfy.herbalbrews.core.blocks.entity.TeaKettleBlockEntity";
    private static final String JUG = "net.satisfy.herbalbrews.core.blocks.JugBlock";
    private static final String JUG_ENTITY = "net.satisfy.herbalbrews.core.blocks.entity.JugBlockEntity";

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (targetClassName.equals(KETTLE)) return HerbalBrewsPresence.hasMethod(KETTLE, "tick");
        if (targetClassName.equals(JUG)) {
            return HerbalBrewsPresence.hasClass(JUG)
                    && HerbalBrewsPresence.hasMethod(JUG_ENTITY, "clearDrinks")
                    && HerbalBrewsPresence.hasMethod(JUG_ENTITY, "getDrinks");
        }
        return HerbalBrewsPresence.hasClass(targetClassName);
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
