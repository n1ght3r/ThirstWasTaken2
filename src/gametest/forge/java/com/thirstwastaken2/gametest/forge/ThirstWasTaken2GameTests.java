package com.thirstwastaken2.gametest.forge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.GameTestRegistry;
import net.minecraft.gametest.framework.GlobalTestReporter;
import net.minecraft.gametest.framework.JUnitLikeTestReporter;
import net.minecraft.gametest.framework.TestFunction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Rotation;
import net.minecraftforge.event.RegisterGameTestsEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The gametest mod on Forge: finds the same test methods Fabric API finds and registers them the way
 * Fabric API does, so every node runs one list of tests under one set of ids. It is the NeoForge
 * harness's pre-1.21.5 path, which is where Forge 47 stands: a test is a {@code TestFunction} added to
 * vanilla's static registry, since Forge only turns methods carrying vanilla's own annotation into one.
 *
 * <ul>
 *   <li><b>Discovery.</b> The test classes are the {@code fabric-gametest} entrypoints in this mod's
 *       own {@code fabric.mod.json}, read as plain JSON.</li>
 *   <li><b>Ids.</b> {@code thirstwastaken2_gametest:<class>_<method>} in snake case, Fabric API's rule.</li>
 *   <li><b>Defaults.</b> Fabric API's: an empty 8x8x8 structure, which this mod ships, 20 ticks,
 *       required, no rotation, one attempt.</li>
 * </ul>
 *
 * <p>This is test harness, not a loader seam. See {@code src/gametest/java/AGENTS.md}.
 */
@Mod(ThirstWasTaken2GameTests.MOD_ID)
public final class ThirstWasTaken2GameTests {
    static final String MOD_ID = "thirstwastaken2_gametest";
    /** Where to write the JUnit report; Forge 47's server has no {@code --report} option. Set by build.forge.gradle.kts. */
    static final String REPORT_PROPERTY = "thirstwastaken2.gametest.report";

    private static final int MAX_TICKS = 20;

    private final List<TestMethod> tests;

    public ThirstWasTaken2GameTests() {
        tests = findTests(ModLoadingContext.get().getActiveContainer().getModInfo().getOwningFile().getFile()
                .findResource("fabric.mod.json"));
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::registerTests);
    }

    private void registerTests(RegisterGameTestsEvent event) {
        String report = System.getProperty(REPORT_PROPERTY);
        if (report != null) {
            try {
                GlobalTestReporter.replaceWith(new JUnitLikeTestReporter(new File(report)));
            } catch (ParserConfigurationException e) {
                throw new IllegalStateException("Cannot write the gametest report to " + report, e);
            }
        }
        String structure = MOD_ID + ":empty";
        for (TestMethod test : tests) {
            // Vanilla's default batch, and Fabric API's defaults for everything else.
            GameTestRegistry.getAllTestFunctions().add(new TestFunction("defaultBatch", test.id().toString(), structure,
                    Rotation.NONE, MAX_TICKS, 0L, true, 1, 1, test::run));
        }
    }

    private static List<TestMethod> findTests(Path manifestPath) {
        if (!Files.exists(manifestPath)) {
            throw new IllegalStateException(MOD_ID + " has no fabric.mod.json to read its test classes from");
        }
        JsonObject manifest;
        try (InputStream in = Files.newInputStream(manifestPath)) {
            manifest = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        List<TestMethod> tests = new ArrayList<>();
        for (var entry : manifest.getAsJsonObject("entrypoints").getAsJsonArray("fabric-gametest")) {
            TestClass testClass = new TestClass(load(entry.getAsString()));
            List<TestMethod> found = new ArrayList<>();
            for (Class<?> type = testClass.type(); type != null; type = type.getSuperclass()) {
                for (Method method : type.getDeclaredMethods()) {
                    if (method.isAnnotationPresent(GameTest.class)) found.add(new TestMethod(testClass, validate(method)));
                }
            }
            if (found.isEmpty()) throw new IllegalStateException("No @GameTest methods in " + entry.getAsString());
            tests.addAll(found);
        }
        return tests;
    }

    /** Loads a test class without initializing it, for the reason the NeoForge harness gives. */
    private static Class<?> load(String className) {
        try {
            return Class.forName(className, false, ThirstWasTaken2GameTests.class.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Cannot find the test class " + className, e);
        }
    }

    /** Fabric API's own checks, so a test that would not run there does not run here either. */
    private static Method validate(Method method) {
        if (method.getParameterCount() != 1 || method.getParameterTypes()[0] != GameTestHelper.class
                || !Modifier.isPublic(method.getModifiers()) || Modifier.isStatic(method.getModifiers())
                || method.getReturnType() != void.class) {
            throw new IllegalStateException("Test method " + method.getDeclaringClass().getName() + "#" + method.getName()
                    + " must be public, not static, return void and take one GameTestHelper");
        }
        return method;
    }

    /** One instance per test class, created on its first test, the way Fabric API shares an entrypoint. */
    private static final class TestClass {
        private final Class<?> type;
        private Object instance;

        TestClass(Class<?> type) {
            this.type = type;
        }

        Class<?> type() {
            return type;
        }

        synchronized Object instance() {
            if (instance == null) {
                try {
                    instance = type.getDeclaredConstructor().newInstance();
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException("Cannot create the test class " + type.getName(), e);
                }
            }
            return instance;
        }
    }

    private record TestMethod(TestClass testClass, Method method) {
        Identifier id() {
            String name = (testClass.type().getSimpleName() + "_" + method.getName())
                    .replaceAll("([a-z])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
            return Identifier.fromNamespaceAndPath(MOD_ID, name);
        }

        void run(GameTestHelper helper) {
            try {
                method.invoke(testClass.instance(), helper);
            } catch (InvocationTargetException e) {
                // A failed assertion has to reach the runner as itself, or it reports an error instead.
                if (e.getTargetException() instanceof RuntimeException failure) throw failure;
                throw new RuntimeException("Test method " + method.getName() + " threw", e.getTargetException());
            } catch (IllegalAccessException e) {
                throw new IllegalStateException(e);
            }
        }
    }
}
