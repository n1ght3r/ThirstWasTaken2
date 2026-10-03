package com.thirstwastaken2.hearthandharvest;

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
 * Whether Hearth and Harvest still has each class and method a mixin targets.
 *
 * <p>Names no class of the mod, so the mixin plugin can ask before anything a mixin targets is loaded:
 * every answer is a resource lookup or a read of a class file, neither of which loads a class. A method
 * renamed upstream skips its mixin, logged once, rather than failing the game at load.
 */
public final class HearthAndHarvestPresence {
    private static final Logger LOGGER = LoggerFactory.getLogger("thirstwastaken2");

    private static final Map<String, Boolean> TARGETS = new ConcurrentHashMap<>();
    private static final Map<String, Boolean> METHODS = new ConcurrentHashMap<>();

    private HearthAndHarvestPresence() { }

    /** Whether {@code targetClassName}, a binary name as Mixin passes it, is on the classpath. */
    public static boolean hasTarget(String targetClassName) {
        return TARGETS.computeIfAbsent(targetClassName, name ->
                HearthAndHarvestPresence.class.getClassLoader().getResource(name.replace('.', '/') + ".class") != null);
    }

    /**
     * Whether {@code targetClassName} declares a method called {@code method}: read off the class file,
     * which never loads the class. Logged when the class is there but the method is not, which is an
     * upstream change the integration has to follow.
     */
    public static boolean hasMethod(String targetClassName, String method) {
        if (!hasTarget(targetClassName)) return false;
        return METHODS.computeIfAbsent(targetClassName + '#' + method, key -> readMethod(targetClassName, method));
    }

    private static boolean readMethod(String targetClassName, String method) {
        String resource = targetClassName.replace('.', '/') + ".class";
        try (InputStream in = HearthAndHarvestPresence.class.getClassLoader().getResourceAsStream(resource)) {
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
                        + "its water may lose or gain a grade there", targetClassName, method);
            }
            return found[0];
        } catch (IOException e) {
            LOGGER.warn("Could not read {} to look for {}; leaving that part of the integration off", targetClassName, method, e);
            return false;
        }
    }
}
