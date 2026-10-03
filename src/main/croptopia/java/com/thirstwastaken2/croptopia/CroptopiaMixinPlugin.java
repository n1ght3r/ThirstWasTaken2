package com.thirstwastaken2.croptopia;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.croptopia.mixins.json} only where Croptopia is installed. The one mixin
 * targets Minecraft's crafting recipes, which are always there, so the plugin looks up Croptopia's
 * common class as a resource instead, which loads no class. Names no class of the mod, no Minecraft
 * class and no loader.
 */
public final class CroptopiaMixinPlugin implements IMixinConfigPlugin {
    /** Croptopia's common class from 4.1 on: the 1.21.1, 26.1 and 26.2 builds. */
    private static final String COMMON = "com/epherical/croptopia/CroptopiaCommon.class";
    /** The same class in the 1.20.1 builds, on Fabric and on Forge. */
    private static final String COMMON_1_20 = "com/epherical/croptopia/CroptopiaMod.class";

    private boolean installed;

    @Override
    public void onLoad(String mixinPackage) {
        ClassLoader loader = CroptopiaMixinPlugin.class.getClassLoader();
        installed = loader.getResource(COMMON) != null || loader.getResource(COMMON_1_20) != null;
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
