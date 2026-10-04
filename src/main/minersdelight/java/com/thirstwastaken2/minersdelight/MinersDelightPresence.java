package com.thirstwastaken2.minersdelight;

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
 * Whether Miner's Delight still has the class and method a mixin targets.
 *
 * <p>Names no class of the mod, so the mixin plugin can ask before anything a mixin targets is loaded:
 * every answer is a resource lookup or a read of a class file, neither of which loads a class. A method
 * renamed upstream skips its mixin, logged once, rather than failing the game at load.
 */
public final class MinersDelightPresence {
    private static final Logger LOGGER = LoggerFactory.getLogger("thirstwastaken2");

    private static final Map<String, Boolean> METHODS = new ConcurrentHashMap<>();

    private MinersDelightPresence() { }

    /**
     * Whether {@code targetClassName}, a binary name as Mixin passes it, is on the classpath and declares a
     * method called any of {@code names}: a Minecraft override is listed under its Mojang name, for
     * NeoForge and development, and its SRG name, for a Forge player's game.
     */
    public static boolean hasMethod(String targetClassName, String... names) {
        return METHODS.computeIfAbsent(targetClassName + '#' + String.join("|", names),
                key -> readMethod(targetClassName, names));
    }

    private static boolean readMethod(String targetClassName, String[] names) {
        String resource = targetClassName.replace('.', '/') + ".class";
        try (InputStream in = MinersDelightPresence.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) return false;
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
                LOGGER.warn("{} has no {}, so it is not a version ThirstWasTaken2 supports; "
                        + "its copper cup will scoop water without a grade", targetClassName, names[0]);
            }
            return found[0];
        } catch (IOException e) {
            LOGGER.warn("Could not read {} to look for {}; leaving that part of the integration off", targetClassName, names[0], e);
            return false;
        }
    }
}
