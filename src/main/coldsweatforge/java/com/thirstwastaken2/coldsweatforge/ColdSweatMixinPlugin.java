package com.thirstwastaken2.coldsweatforge;

import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.coldsweatforge.mixins.json} only when Cold Sweat is installed, and each
 * mixin only where its target still declares the methods it injects into, so a method renamed upstream
 * skips that mixin rather than failing it.
 */
public final class ColdSweatMixinPlugin implements IMixinConfigPlugin {
    /**
     * The methods each mixin on a Cold Sweat class injects into, by simple name; each entry lists the
     * names any one of which will do. One that overrides Minecraft's is listed under its Mojang name, for
     * development, and its SRG name, for a player's game. {@code WaterskinItemMixin} names a lambda and
     * {@code BoilerSlotMixin} an anonymous class by their synthetic names, the likeliest of all to move.
     * {@code CampfireWaterskinMixin} targets vanilla, so it is not listed.
     */
    private static final Map<String, List<String[]>> NEEDS_METHODS = Map.of(
            "WaterskinItemMixin", List.of(new String[] {"useOn", "m_6225_"}, new String[] {"getFilledItem"},
                    new String[] {"lambda$useOn$0"}),
            "FilledWaterskinItemMixin", List.<String[]>of(new String[] {"getCraftingRemainingItem"}),
            "BoilerBlockEntityMixin", List.of(new String[] {"canPlaceItemThroughFace", "m_7155_"},
                    new String[] {"tick"}, new String[] {"checkForItems"}),
            "BoilerSlotMixin", List.<String[]>of(new String[] {"mayPlace", "m_5857_"}));

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!ColdSweatPresence.isPresent()) return false;
        List<String[]> methods = NEEDS_METHODS.get(mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1));
        if (methods == null) return true;
        for (String[] names : methods) {
            if (!ColdSweatPresence.hasMethod(targetClassName, names)) return false;
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
