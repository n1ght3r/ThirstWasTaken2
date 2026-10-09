package com.thirstwastaken2.create;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.create.mixins.json} only when Create is installed, and its mixin into
 * Create Pipes n Physics only when that addon is installed too. One config rather than one for the addon:
 * the integration table gives a directory one, and the addon only matters where the Create mixins are.
 */
public final class CreateMixinPlugin implements IMixinConfigPlugin {
    private static final String PIPES_MIXIN = "com.thirstwastaken2.create.mixin.BoundaryColumnMixin";

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!CreatePresence.isPresent()) return false;
        return !mixinClassName.equals(PIPES_MIXIN) || PipesPresence.isPresent();
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
