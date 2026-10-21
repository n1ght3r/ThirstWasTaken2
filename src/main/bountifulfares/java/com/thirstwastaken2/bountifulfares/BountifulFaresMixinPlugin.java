package com.thirstwastaken2.bountifulfares;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.bountifulfares.mixins.json} only where Bountiful Fares is installed, and
 * each mixin on one of its classes only where that class, and the method of its own the mixin needs, is
 * still there.
 *
 * <p>Names no class of the mod's, of Minecraft's or of a loader: every answer is a resource lookup or a
 * read of a class file, neither of which loads a class, and both loaders compile this directory. Only the
 * mod's own methods are probed. A Minecraft override such as {@code use} is named differently in a Fabric
 * player's game, and a vanilla override does not move within the one Minecraft version this is built for.
 */
public final class BountifulFaresMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("thirstwastaken2");

    /** A class every build of the mod has, in its common module. */
    private static final String COMMON = "net/hecco/bountifulfares/BountifulFares.class";

    /** The mod's own methods a mixin injects into or calls, by mixin. */
    private static final Map<String, List<String>> NEEDS_METHODS = Map.of(
            "CupItemMixin", List.of("turnCupIntoItem"));

    private boolean installed;

    @Override
    public void onLoad(String mixinPackage) {
        installed = BountifulFaresMixinPlugin.class.getClassLoader().getResource(COMMON) != null;
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        if (!installed) return false;
        if (!targetClassName.startsWith("net.hecco.bountifulfares.")) return true;
        String resource = targetClassName.replace('.', '/') + ".class";
        List<String> methods = NEEDS_METHODS.getOrDefault(mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1), List.of());
        try (InputStream in = BountifulFaresMixinPlugin.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                LOGGER.warn("Bountiful Fares has no {}, so it is not a version ThirstWasTaken2 supports; "
                        + "that part of the integration is off", targetClassName);
                return false;
            }
            if (methods.isEmpty()) return true;
            Set<String> declared = new HashSet<>();
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    declared.add(name);
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            for (String method : methods) {
                if (!declared.contains(method)) {
                    LOGGER.warn("Bountiful Fares has no {}.{}, so it is not a version ThirstWasTaken2 supports; "
                            + "its cup will scoop water without a grade", targetClassName, method);
                    return false;
                }
            }
            return true;
        } catch (IOException e) {
            LOGGER.warn("Could not read {}; leaving that part of the integration off", targetClassName, e);
            return false;
        }
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
