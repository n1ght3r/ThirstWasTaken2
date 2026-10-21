package com.thirstwastaken2.hearthandharvestforge;

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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Applies {@code thirstwastaken2.hearthandharvestforge.mixins.json} only where Hearth and Harvest is on the
 * classpath, and each mixin only while its target still declares the method it injects into. Each answer
 * is a resource lookup or a read of a class file, neither of which loads a class, so nothing of either mod
 * is named before it is known to be there.
 */
public final class HearthAndHarvestForgeMixinPlugin implements IMixinConfigPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger("thirstwastaken2");
    /** A class every 1.20.1 build of Hearth and Harvest has. */
    private static final String MARKER = "alabaster/hearthandharvest/HearthAndHarvest.class";
    private static final String PACKAGE = "alabaster.hearthandharvest.common.";
    /**
     * The method each target must still declare, by its name in the jar. Each is the mod's own, which keeps
     * its name under SRG, or a lambda's; a Minecraft method a mixin also injects into (the Sink's
     * {@code tick}, the jug item's {@code use}) is an SRG name in the jar and comes with the class. 1.0.12c
     * has only the Jug's lambda; 1.4.1, the 1.21.1 mod ported back, has the rest.
     */
    private static final Map<String, String> NEEDS = Map.ofEntries(
            Map.entry("vectorwing.farmersdelight.common.crafting.CookingPotRecipe", "matches"),
            Map.entry(PACKAGE + "block.JugBlock", "lambda$use$0"),
            Map.entry(PACKAGE + "fluid.HHFluidHandling", "testFilling"),
            Map.entry(PACKAGE + "block.BasinBlock", "hhUseItemOn"),
            Map.entry(PACKAGE + "block.SprinklerBlock", "hhUseItemOn"),
            Map.entry(PACKAGE + "item.JugBlockItem", "pickUpSource"),
            Map.entry(PACKAGE + "block.entity.TroughBlockEntity", "serverTick"),
            Map.entry(PACKAGE + "block.entity.SprinklerBlockEntity", "refill"),
            Map.entry(PACKAGE + "block.entity.KegBlockEntity", "transfer"),
            Map.entry(PACKAGE + "crafting.StompingBasinRecipe", "getResultFluid"));
    /** 1.0.12c's alone: missing on 1.4.1 on, where the placed Jug goes through {@code HHFluidHandling}, so not logged. */
    private static final String OLD_JUG = PACKAGE + "block.JugBlock";

    @Override
    public void onLoad(String mixinPackage) { }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        ClassLoader loader = HearthAndHarvestForgeMixinPlugin.class.getClassLoader();
        if (loader.getResource(MARKER) == null) return false;
        String method = NEEDS.get(targetClassName);
        return method != null && declares(loader, targetClassName, method);
    }

    private static boolean declares(ClassLoader loader, String targetClassName, String method) {
        try (InputStream in = loader.getResourceAsStream(targetClassName.replace('.', '/') + ".class")) {
            if (in == null) return false;
            boolean[] found = {false};
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if (name.equals(method)) found[0] = true;
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            if (!found[0] && !targetClassName.equals(OLD_JUG)) {
                LOGGER.warn("{} has no {}, so it is not a version ThirstWasTaken2 supports; "
                        + "its water may lose its grade there", targetClassName, method);
            }
            return found[0];
        } catch (IOException e) {
            LOGGER.warn("Could not read {} to look for {}; leaving that part of the integration off", targetClassName, method, e);
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
