package com.thirstwastaken2.mekanism;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.mekanism.mixins.json} only where Mekanism is installed, and each mixin
 * only while its target still declares every method it injects into or calls.
 */
public final class MekanismMixinPlugin implements IMixinConfigPlugin {
    private static final Map<String, List<String>> NEEDS_ALL_OF = Map.of(
            "mekanism.common.tile.machine.TileEntityElectricPump", List.of("suck", "getOutput"),
            "mekanism.api.recipes.basic.BasicRotaryRecipe", List.of("getFluidOutput"));

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
            if (!MekanismPresence.hasMethod(targetClassName, method)) return false;
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
