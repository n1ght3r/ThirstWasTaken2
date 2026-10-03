package com.thirstwastaken2.spelunkery;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.spelunkery.mixins.json} only where Spelunkery is installed. The one mixin
 * targets Minecraft's cooking recipe, which is always there, so the plugin looks up Spelunkery's main
 * class as a resource instead, which loads no class. Names no class of the mod, no Minecraft class and no
 * loader.
 */
public final class SpelunkeryMixinPlugin implements IMixinConfigPlugin {
    private static final String SPELUNKERY = "com/ordana/spelunkery/Spelunkery.class";

    private boolean installed;

    @Override
    public void onLoad(String mixinPackage) {
        installed = SpelunkeryMixinPlugin.class.getClassLoader().getResource(SPELUNKERY) != null;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return installed;
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
