package com.thirstwastaken2.hearthandharvest;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.hearthandharvest.mixins.json} only where Hearth and Harvest is
 * installed, and each mixin only while its target still declares every method it injects into.
 */
public final class HearthAndHarvestMixinPlugin implements IMixinConfigPlugin {
    private static final String PACKAGE = "alabaster.hearthandharvest.common.";

    /** The methods each target declares and a mixin injects into, all of which must still be there. */
    private static final Map<String, List<String>> NEEDS_ALL_OF = Map.of(
            PACKAGE + "fluid.HHFluidHandling", List.of("testEmptying", "testFilling"),
            PACKAGE + "block.BasinBlock", List.of("useItemOn", "tick"),
            PACKAGE + "block.SprinklerBlock", List.of("useItemOn"),
            PACKAGE + "item.JugBlockItem", List.of("use"),
            PACKAGE + "block.entity.TroughBlockEntity", List.of("serverTick"),
            PACKAGE + "block.entity.CaskBlockEntity", List.of("getMatchingRecipe"),
            PACKAGE + "crafting.StompingBasinRecipe", List.of("getResultFluid"));

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        List<String> methods = NEEDS_ALL_OF.get(targetClassName);
        if (methods == null) return false;
        for (String method : methods) {
            if (!HearthAndHarvestPresence.hasMethod(targetClassName, method)) return false;
        }
        return true;
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
