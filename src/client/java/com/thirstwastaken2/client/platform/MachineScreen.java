package com.thirstwastaken2.client.platform;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * A container screen of a fixed size that draws its own background and, over everything, its own
 * tooltips, the same way on every version. 26.1 replaced rendering with extracting: the background is
 * {@code extractBackground} and the size goes to the constructor, where before it was {@code renderBg}
 * and two fields. Before 26.1 the screen draws the hovered slot's tooltip itself, and 1.20.1 its dimmed
 * background as well.
 *
 * <p>A class rather than a method for the same reason as {@code SupportedBlock}: what differs is an override.
 */
public abstract class MachineScreen<M extends AbstractContainerMenu> extends AbstractContainerScreen<M> {
    protected MachineScreen(M menu, Inventory inventory, Component title, int width, int height) {
        //? if >=26.1 {
        super(menu, inventory, title, width, height);
        //?} else {
        /*super(menu, inventory, title);
        this.imageWidth = width;
        this.imageHeight = height;
        *///?}
        this.inventoryLabelY = height - 94;
    }

    /** Draws the background, the panel at {@code left, top}, under the slots. */
    protected abstract void drawBackground(GuiGraphicsExtractor graphics, int left, int top, int mouseX, int mouseY);

    /** Shows the screen's own tooltips, through {@link ClientVanilla#tooltip}, once everything is drawn. */
    protected void drawTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) { }

    //? if >=26.1 {
    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        super.extractBackground(graphics, mouseX, mouseY, partialTick);
        drawBackground(graphics, leftPos, topPos, mouseX, mouseY);
    }

    @Override
    protected void extractTooltip(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        super.extractTooltip(graphics, mouseX, mouseY);
        drawTooltips(graphics, mouseX, mouseY);
    }
    //?} else {
    /*@Override
    protected void renderBg(GuiGraphicsExtractor graphics, float partialTick, int mouseX, int mouseY) {
        drawBackground(graphics, leftPos, topPos, mouseX, mouseY);
    }

    @Override
    public void render(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        dimBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        drawTooltips(graphics, mouseX, mouseY);
    }
    *///?}

    // From 1.20.2 the screen dims the world behind itself inside render; on 1.20.1 it is asked to.
    //? if <1.20.5 {
    /*private void dimBackground(GuiGraphicsExtractor graphics) {
        renderBackground(graphics);
    }
    *///?} elif <26.1 {
    /*private void dimBackground(GuiGraphicsExtractor graphics) { }
    *///?}
}
