package com.thirstwastaken2.farmandcharm;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.farmandcharm.mixins.json} only where Farm & Charm is installed, and each
 * mixin only on the targets that are there: Candlelight's Large Cooking Pot is absent without Candlelight,
 * and the other targets stay.
 */
public final class FarmAndCharmMixinPlugin implements IMixinConfigPlugin {
    /**
     * The mods' own methods a mixin injects into, by target class. A target is skipped unless it declares
     * at least one, so a method renamed upstream skips that target rather than failing it.
     */
    private static final Map<String, List<String>> NEEDS_ONE_OF = Map.of(
            "net.satisfy.farm_and_charm.core.block.entity.CookingPotBlockEntity", List.of("matchesInventory"),
            "net.satisfy.farm_and_charm.core.block.entity.StoveBlockEntity", List.of("findBestMatchingSlot", "countMatchingSlots"),
            "net.satisfy.farm_and_charm.core.block.entity.CraftingBowlBlockEntity", List.of("matchExact"),
            "net.satisfy.candlelight.core.block.entity.LargeCookingPotBlockEntity", List.of("matchesInventory"));

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        List<String> methods = NEEDS_ONE_OF.get(targetClassName);
        if (methods == null) return FarmAndCharmPresence.hasTarget(targetClassName);
        for (String method : methods) {
            if (FarmAndCharmPresence.hasMethod(targetClassName, method)) return true;
        }
        return false;
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
