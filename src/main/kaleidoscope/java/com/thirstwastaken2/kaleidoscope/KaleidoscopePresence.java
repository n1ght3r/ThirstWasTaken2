package com.thirstwastaken2.kaleidoscope;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whether Kaleidoscope Cookery is installed, is a build the integration was written against, and still
 * has each class a mixin targets.
 *
 * <p>Names no Kaleidoscope Cookery class, no Minecraft class and no loader, so the mixin plugin can ask
 * before anything a mixin targets is loaded: every answer here is a resource lookup, which never loads a
 * class. {@code Class.forName} would load a target before Mixin has transformed it. Both loaders compile
 * this directory, which is why it probes the classpath rather than asking a mod list.
 *
 * <p>The teapot is the version check. The official Fabric build stopped at 1.0.1, under the same mod id
 * and package as Refabricated but without a teapot, so a Kaleidoscope Cookery with no teapot is that
 * build, and the whole integration stays off. Past that, each target is probed on its own, so a class
 * renamed upstream skips the mixins on it and leaves the rest working. A mixin whose method only some
 * builds have asks {@link #hasMethod}, which reads the target's bytes the same way.
 */
public final class KaleidoscopePresence {
    private static final String MOD = "com/github/ysbbbbbb/kaleidoscopecookery/KaleidoscopeCookery.class";
    private static final String TEAPOT =
            "com/github/ysbbbbbb/kaleidoscopecookery/blockentity/kitchen/TeapotBlockEntity.class";

    private static final Logger LOGGER = LoggerFactory.getLogger("thirstwastaken2");

    /**
     * Kaleidoscope Chinese Food's mixin on Kaleidoscope Tavern's tap, which merges {@code kcf$fillCookery}
     * into it: a tap on water fills a stockpot or a teapot below through accessors of its own.
     */
    private static final String CHINESE_FOOD_TAP = "com/bmt/kaleidoscope_chinesefood/mixins/tavern/TapBlockMixin.class";
    private static final String CHINESE_FOOD_FILL = "kcf$fillCookery";

    private static volatile Boolean supported;
    private static volatile Boolean chineseFoodTap;
    private static final Map<String, Boolean> TARGETS = new ConcurrentHashMap<>();

    private KaleidoscopePresence() { }

    /** Whether a Kaleidoscope Cookery the integration supports is installed. Warns once if one is not. */
    public static boolean isSupported() {
        Boolean known = supported;
        if (known == null) {
            known = has(TEAPOT);
            if (!known && has(MOD)) {
                LOGGER.warn("Kaleidoscope Cookery is installed but has no teapot, so it is the official Fabric "
                        + "build, which ThirstWasTaken2 does not support; install Kaleidoscope Cookery "
                        + "Refabricated instead. Its drinks and soups still restore thirst, but water poured "
                        + "into a stockpot comes back out without its grade");
            }
            supported = known;
        }
        return known;
    }

    /**
     * Whether a mixin on {@code targetClassName}, a binary name as Mixin passes it, may be applied: the
     * mod is supported and still has that class. A missing target is logged once and skipped.
     */
    public static boolean hasTarget(String targetClassName) {
        if (!isSupported()) return false;
        return TARGETS.computeIfAbsent(targetClassName, name -> {
            boolean found = has(name.replace('.', '/') + ".class");
            if (!found) {
                LOGGER.warn("Kaleidoscope Cookery has no {}, so it is not a version ThirstWasTaken2 supports; "
                        + "what the integration does there is disabled", name);
            }
            return found;
        });
    }

    /**
     * Whether {@code targetClassName}, a binary name, declares a method called {@code method}: read off
     * the class file, which never loads the class. For a mixin on a method only some builds have, such
     * as the teapot's dripstone hook (the 1.21.1 builds) or {@code giveItemToPlayer} (Refabricated), so
     * the builds without it skip that mixin rather than fail it. Asked once per mixin, at startup.
     */
    public static boolean hasMethod(String targetClassName, String method) {
        return hasTarget(targetClassName) && declares(targetClassName.replace('.', '/') + ".class", method);
    }

    private static boolean declares(String resource, String method) {
        try (InputStream in = KaleidoscopePresence.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) return false;
            boolean[] found = {false};
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if (name.equals(method)) found[0] = true;
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            return found[0];
        } catch (IOException e) {
            LOGGER.warn("Could not read {} to look for {}; leaving that part of the integration off", resource, method, e);
            return false;
        }
    }

    /**
     * Whether Kaleidoscope Tavern's tap fills a stockpot or a teapot here: Kaleidoscope Chinese Food is
     * installed and its mixin still has {@code kcf$fillCookery}, read off its class file like
     * {@link #hasMethod}. A name that can change in any release of the addon, so a missing one turns only
     * this part off, quietly: most players have neither mod.
     */
    public static boolean hasChineseFoodTap() {
        Boolean known = chineseFoodTap;
        if (known == null) {
            known = isSupported() && has(CHINESE_FOOD_TAP) && declares(CHINESE_FOOD_TAP, CHINESE_FOOD_FILL);
            chineseFoodTap = known;
        }
        return known;
    }

    private static boolean has(String resource) {
        return KaleidoscopePresence.class.getClassLoader().getResource(resource) != null;
    }
}
