package com.thirstwastaken2.coldsweatforge;

import net.minecraftforge.fml.loading.LoadingModList;
import net.minecraftforge.fml.loading.moddiscovery.ModFileInfo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Whether the Cold Sweat installed is one the integration was written against, and whether it still
 * has each method a mixin targets, as on NeoForge.
 *
 * <p>Names no Cold Sweat class and no Minecraft class, so the entrypoint and the mixin plugin can ask
 * before anything of Cold Sweat's is loaded. It reads FML's list of discovered mod files, which is
 * complete before any mixin config is read, and reads class files without loading them.
 */
public final class ColdSweatPresence {
    public static final String MOD_ID = "cold_sweat";
    /** The temperature API the climate reads. Looked up in Cold Sweat's own jar, so asking does not load it. */
    private static final String MARKER = "com/momosoftworks/coldsweat/api/util/Temperature.class";

    private static final Logger LOGGER = LoggerFactory.getLogger("thirstwastaken2");

    private static volatile Boolean present;
    private static final Map<String, Boolean> METHODS = new ConcurrentHashMap<>();

    private ColdSweatPresence() { }

    /**
     * Another mod may claim the {@code cold_sweat} id, and a later Cold Sweat may move its API, so the
     * id alone is not enough.
     */
    public static boolean isPresent() {
        Boolean known = present;
        if (known == null) {
            ModFileInfo coldSweat = LoadingModList.get().getModFileById(MOD_ID);
            known = coldSweat != null && Files.exists(coldSweat.getFile().findResource(MARKER));
            if (coldSweat != null && !known) {
                LOGGER.warn("A mod with id 'cold_sweat' is installed but has no temperature API where Cold Sweat 2.4 "
                        + "keeps it; the Cold Sweat integration is disabled");
            }
            present = known;
        }
        return known;
    }

    /**
     * Whether {@code targetClassName}, a binary name, declares a method called any of {@code names}: read
     * off the class file, which never loads the class. A method that overrides Minecraft's is named in
     * Mojang's names in development and in SRG names in a player's game, so such a method is asked for by
     * both. Asked once per mixin, at startup, and logged when the class is there but the method is not,
     * which is an upstream change the integration has to follow.
     */
    public static boolean hasMethod(String targetClassName, String... names) {
        if (!isPresent()) return false;
        return METHODS.computeIfAbsent(targetClassName + '#' + String.join("|", names),
                key -> readMethod(targetClassName, names));
    }

    private static boolean readMethod(String targetClassName, String[] names) {
        String resource = targetClassName.replace('.', '/') + ".class";
        try (InputStream in = ColdSweatPresence.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                LOGGER.warn("Cold Sweat has no {}, so it is not a version ThirstWasTaken2 supports", targetClassName);
                return false;
            }
            boolean[] found = {false};
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    for (String wanted : names) {
                        if (name.equals(wanted)) found[0] = true;
                    }
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            if (!found[0]) {
                LOGGER.warn("Cold Sweat has no {}.{}, so it is not a version ThirstWasTaken2 supports; "
                        + "its waterskin may not keep a grade", targetClassName, names[0]);
            }
            return found[0];
        } catch (IOException e) {
            LOGGER.warn("Could not read {} to look for {}; leaving that part of the integration off", targetClassName, names[0], e);
            return false;
        }
    }
}
