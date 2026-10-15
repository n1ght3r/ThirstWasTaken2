package com.thirstwastaken2.immersiveengineering;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.immersiveengineering.mixins.json} only where Immersive Engineering is installed, and
 * each mixin only while its target still declares every method it injects into, and the classes it calls
 * still declare what it wraps. An entry {@code Owner#method} names a method of another class.
 */
public final class ImmersiveEngineeringMixinPlugin implements IMixinConfigPlugin {
    private static final String IE = "blusunrize.immersiveengineering.common.";

    private static final Map<String, List<String>> NEEDS_ALL_OF = Map.of(
            IE + "blocks.metal.FluidPumpBlockEntity",
            List.of("tickServer", "gatherInfiniteFluidFromWorld", IE + "util.Utils#drainFluidBlock"),
            IE + "blocks.multiblocks.logic.bottling_machine.BottlingProcess",
            List.of("doProcessTick", "getRecipeItemOutputs"),
            IE + "fluids.IEItemFluidHandler", List.of("fill"));

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
            int hash = method.indexOf('#');
            String owner = hash < 0 ? targetClassName : method.substring(0, hash);
            if (!ImmersiveEngineeringPresence.hasMethod(owner, method.substring(hash + 1))) return false;
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
