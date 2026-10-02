package com.thirstwastaken2.beachparty;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies {@code thirstwastaken2.beachparty.mixins.json} only where Beachparty is installed: each target
 * is looked up as a resource, which loads no class. Names no class of the mod, no Minecraft class and no
 * loader. The one mixin injects into {@code useWithoutItem}, Minecraft's method, so there is no method of
 * the mod's to check for.
 */
public final class BeachpartyMixinPlugin implements IMixinConfigPlugin {
    private static final Map<String, Boolean> TARGETS = new ConcurrentHashMap<>();

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        return TARGETS.computeIfAbsent(targetClassName, name ->
                BeachpartyMixinPlugin.class.getClassLoader().getResource(name.replace('.', '/') + ".class") != null);
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
