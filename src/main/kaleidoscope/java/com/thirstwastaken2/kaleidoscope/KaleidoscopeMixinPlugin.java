package com.thirstwastaken2.kaleidoscope;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.kaleidoscope.mixins.json} only where Kaleidoscope Cookery is a build
 * the integration supports, and each mixin only where its own target is still there, so a renamed teapot
 * does not take the stockpot down with it.
 */
public final class KaleidoscopeMixinPlugin implements IMixinConfigPlugin {
    /**
     * Mixins on a method only some builds have, by simple name, with that method: applied only where the
     * target declares it. Dripstone fills a teapot only on the 1.21.1 builds; only Refabricated hands a
     * player a bucket through {@code giveItemToPlayer}.
     */
    private static final Map<String, String> NEEDS_METHOD = Map.of(
            "TeapotDripstoneMixin", "receiveDripstoneFluid",
            "ItemUtilsPlayerMixin", "giveItemToPlayer");

    @Override
    public void onLoad(String mixinPackage) {
        // Asked here as well as per mixin, so the warning about an unsupported build is logged at startup
        // whatever the config lists.
        KaleidoscopePresence.isSupported();
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    /**
     * The mixin on Kaleidoscope Tavern's tap, which is not this mod's class: applied only where Kaleidoscope
     * Chinese Food has merged its stockpot fill into it.
     */
    private static final String CHINESE_FOOD_TAP_MIXIN = "TapCookeryMixin";

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        String simpleName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        if (simpleName.equals(CHINESE_FOOD_TAP_MIXIN)) return KaleidoscopePresence.hasChineseFoodTap();
        String method = NEEDS_METHOD.get(simpleName);
        return method == null
                ? KaleidoscopePresence.hasTarget(targetClassName)
                : KaleidoscopePresence.hasMethod(targetClassName, method);
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
