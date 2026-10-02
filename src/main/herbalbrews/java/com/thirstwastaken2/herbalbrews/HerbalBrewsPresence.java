package com.thirstwastaken2.herbalbrews;

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
 * Whether HerbalBrews still has each class and method a mixin targets or calls.
 *
 * <p>Names no class of the mod, no Minecraft class and no loader, so the mixin plugin can ask before
 * anything a mixin targets is loaded: every answer is a resource lookup or a read of a class file,
 * neither of which loads a class. Both loaders compile this directory, which is why it probes the
 * classpath rather than asking a mod list.
 *
 * <p>Only the mod's own method names are checked: {@code useItemOn} is Minecraft's, and is named in
 * intermediary in a Fabric jar.
 */
public final class HerbalBrewsPresence {
    private static final Logger LOGGER = LoggerFactory.getLogger("thirstwastaken2");

    private static final Map<String, Boolean> TARGETS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> METHODS = new ConcurrentHashMap<>();

    private HerbalBrewsPresence() { }

    /** Whether {@code className}, a binary name as Mixin passes it, is on the classpath. */
    public static boolean hasClass(String className) {
        return TARGETS.computeIfAbsent(className, name ->
                HerbalBrewsPresence.class.getClassLoader().getResource(name.replace('.', '/') + ".class") != null);
    }

    /**
     * Whether {@code className} declares a method called {@code method}: read off the class file, which
     * never loads the class. Logged when the class is there but the method is not, which is an upstream
     * change the integration has to follow.
     */
    public static boolean hasMethod(String className, String method) {
        if (!hasClass(className)) return false;
        return METHODS.computeIfAbsent(className + '#' + method, key -> readMethod(className, method));
    }

    private static boolean readMethod(String className, String method) {
        String resource = className.replace('.', '/') + ".class";
        try (InputStream in = HerbalBrewsPresence.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) return false;
            boolean[] found = {false};
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    if (name.equals(method)) found[0] = true;
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
            if (!found[0]) {
                LOGGER.warn("{} has no {}, so it is not a version ThirstWasTaken2 supports; "
                        + "that part of the HerbalBrews integration is off", className, method);
            }
            return found[0];
        } catch (IOException e) {
            LOGGER.warn("Could not read {} to look for {}; leaving that part of the integration off", className, method, e);
            return false;
        }
    }
}
