package com.thirstwastaken2.gametest.forge;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a gametest method on the Forge node, in place of Fabric API's annotation of the same name.
 * {@code stonecutter.gradle.kts} swaps the import on this node only, so a test file never names this
 * class itself.
 *
 * <p>It takes no arguments on purpose. Every test runs with Fabric API's defaults, which
 * {@link ThirstWasTaken2GameTests} copies on every Minecraft version, and the Fabric 1.21.1
 * replacement relies on {@code @GameTest} being written bare too.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface GameTest {
}
