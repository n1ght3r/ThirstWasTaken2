package com.thirstwastaken2.client.platform;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Builds the screen for a menu the server opened, as {@code ClientLoader.registerScreen} hands it over.
 * Vanilla's own interface for this is private, opened differently by each loader, so the mod's screens
 * are written against this one.
 */
@FunctionalInterface
public interface ScreenFactory<M extends AbstractContainerMenu, S extends Screen & MenuAccess<M>> {
    S create(M menu, Inventory inventory, Component title);
}
