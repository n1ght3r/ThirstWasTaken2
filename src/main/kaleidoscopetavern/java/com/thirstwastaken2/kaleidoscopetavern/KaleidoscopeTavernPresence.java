package com.thirstwastaken2.kaleidoscopetavern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whether Kaleidoscope Tavern is installed, and still has each class a mixin targets.
 *
 * <p>Names no Kaleidoscope Tavern class, no Minecraft class and no loader, so the mixin plugin can ask
 * before anything a mixin targets is loaded: every answer here is a resource lookup, which never loads a
 * class. All three loaders compile this directory, which is why it probes the classpath rather than
 * asking a mod list. A class renamed upstream skips the mixins on it and leaves the rest working.
 */
public final class KaleidoscopeTavernPresence {
    private static final String BARREL =
            "com/github/ysbbbbbb/kaleidoscopetavern/blockentity/brew/BarrelBlockEntity.class";
    /**
     * Refabricated's own fluid helper, over the Fabric Transfer API. The official mod's is
     * {@code util/FluidUtils}, over the loader's fluid capability.
     */
    private static final String REFABRICATED_FLUIDS =
            "com/github/ysbbbbbb/kaleidoscopetavern/util/fluids/FluidUtils.class";

    private static final Logger LOGGER = LoggerFactory.getLogger("thirstwastaken2");

    private static volatile Boolean installed;
    private static volatile Boolean refabricated;
    private static final Map<String, Boolean> TARGETS = new ConcurrentHashMap<>();

    private KaleidoscopeTavernPresence() { }

    /** Whether Kaleidoscope Tavern, either build, is installed: its barrel is on the classpath. */
    public static boolean isInstalled() {
        Boolean known = installed;
        if (known == null) {
            known = has(BARREL);
            installed = known;
        }
        return known;
    }

    /**
     * Whether the installed build is Refabricated, whose barrel hands every container back as a bucket;
     * see {@link TavernWater#refuses}.
     */
    public static boolean isRefabricated() {
        Boolean known = refabricated;
        if (known == null) {
            known = has(REFABRICATED_FLUIDS);
            refabricated = known;
        }
        return known;
    }

    /**
     * Whether a mixin on {@code targetClassName}, a binary name as Mixin passes it, may be applied: the
     * mod is installed and the class is there. A missing target is logged once and skipped.
     */
    public static boolean hasTarget(String targetClassName) {
        if (!isInstalled()) return false;
        return TARGETS.computeIfAbsent(targetClassName, name -> {
            boolean found = has(name.replace('.', '/') + ".class");
            if (!found) {
                LOGGER.warn("Kaleidoscope Tavern has no {}, so it is not a version ThirstWasTaken2 supports; "
                        + "what the integration does there is disabled", name);
            }
            return found;
        });
    }

    private static boolean has(String resource) {
        return KaleidoscopeTavernPresence.class.getClassLoader().getResource(resource) != null;
    }
}
