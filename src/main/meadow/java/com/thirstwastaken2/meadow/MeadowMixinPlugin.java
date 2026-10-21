package com.thirstwastaken2.meadow;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies {@code thirstwastaken2.meadow.mixins.json} only where Meadow is installed: each class is looked
 * up as a resource, which loads none. Two of the mixins target Minecraft's classes, which are always
 * there, so they ask for Meadow's wooden cauldron interactions instead. Names no class of the mod, no
 * Minecraft class and no loader. Meadow's own methods hooked, {@code interact} and {@code use}, keep
 * their names on both loaders, so there is no method to check for.
 */
public final class MeadowMixinPlugin implements IMixinConfigPlugin {
    /** The class whose presence stands for Meadow's, for the mixins that target Minecraft's classes. */
    private static final String MARKER = "net.satisfy.meadow.core.util.WoodenCauldronBehavior";
    private static final Map<String, Boolean> PRESENT = new ConcurrentHashMap<>();

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String probe = targetClassName.startsWith("net.satisfy.meadow.") ? targetClassName : MARKER;
        return PRESENT.computeIfAbsent(probe, name ->
                MeadowMixinPlugin.class.getClassLoader().getResource(name.replace('.', '/') + ".class") != null);
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
