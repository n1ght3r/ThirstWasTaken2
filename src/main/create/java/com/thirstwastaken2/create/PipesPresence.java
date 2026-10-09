package com.thirstwastaken2.create;

import net.neoforged.fml.loading.LoadingModList;
import net.neoforged.fml.loading.moddiscovery.ModFileInfo;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

/**
 * Whether Create Pipes n Physics is installed, and still has the two engine methods
 * {@code BoundaryColumnMixin} needs, exactly as 3.2.1 declares them.
 *
 * <p>The addon says its engine classes move between releases, and a {@code @Shadow} that finds no
 * method fails the whole class, so the gate reads the class file out of the addon's jar rather than
 * trusting the mod id. Names no class of the addon's and loads none, like {@link CreatePresence}.
 */
public final class PipesPresence {
    public static final String MOD_ID = "pipesnphysics";
    private static final String BOUNDARY_COLUMN = "de/devin/pipesnphysics/engine/boundary/BoundaryColumn.class";
    private static final String PROBE = "drinkableSource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ZZ)"
            + "Lnet/neoforged/neoforge/fluids/FluidStack;";
    private static final String WORLD_POS = "worldOutputPos(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;)"
            + "Lnet/minecraft/core/BlockPos;";
    private static final String INTAKE = "intakeFluid(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;ZZ)"
            + "Lnet/neoforged/neoforge/fluids/FluidStack;";

    private static final Logger LOGGER = LoggerFactory.getLogger("thirstwastaken2");

    private static volatile Boolean present;

    private PipesPresence() { }

    public static boolean isPresent() {
        Boolean known = present;
        if (known == null) {
            ModFileInfo pipes = LoadingModList.get().getModFileById(MOD_ID);
            known = pipes != null && hasEngine(pipes.getFile().findResource(BOUNDARY_COLUMN));
            if (pipes != null && !known) {
                LOGGER.warn("Create Pipes n Physics is installed but its engine is not the one of 3.2.1; "
                        + "pumps drawing water through an open pipe end may move nothing");
            }
            present = known;
        }
        return known;
    }

    private static boolean hasEngine(Path classFile) {
        if (!Files.exists(classFile)) return false;
        Set<String> methods = new HashSet<>();
        try (InputStream in = Files.newInputStream(classFile)) {
            new ClassReader(in).accept(new ClassVisitor(Opcodes.ASM9) {
                @Override
                public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
                    methods.add(name + descriptor);
                    return null;
                }
            }, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
        } catch (IOException e) {
            LOGGER.warn("Could not read Create Pipes n Physics' BoundaryColumn; leaving its intake fix off", e);
            return false;
        }
        return methods.contains(PROBE) && methods.contains(WORLD_POS) && methods.contains(INTAKE);
    }
}
