package com.thirstwastaken2.client.config;

import com.thirstwastaken2.client.platform.ClientVanilla;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * The colours and small drawing helpers every part of the config screen shares. They are vanilla's:
 * greys for text, rows bare on one translucent dark panel as an options list's are, a selection
 * darkened and outlined in grey as a list entry is, and white only for the outline of what the mouse
 * is on. The blurred world or title panorama vanilla draws behind every screen still shows through.
 * Amber, for a changed setting, is the only colour of the screen's own; the icons carry the rest.
 */
final class ConfigTheme {
    /** White, as vanilla outlines a hovered or focused button. */
    static final int FOCUS = 0xFFFFFFFF;
    /** Amber, marking a setting that differs from its default. */
    static final int CHANGED = 0xFFF2B84B;
    static final int TEXT = 0xFFFFFFFF;
    /** Vanilla's light grey, for text over the bright blurred background of the header: "Settings". */
    static final int SOFT = 0xFFE0E0E0;
    static final int MUTED = 0xFFA0A0A0;
    static final int FAINT = 0xFF707070;

    /** The list area between header and footer, sidebar included, darkened as vanilla's option lists are. */
    static final int PANEL = 0x80000000;
    /** The two lines of vanilla's header and footer separators: a light one beside a dark one. */
    static final int SEPARATOR_LIGHT = 0x40FFFFFF;
    static final int SEPARATOR_DARK = 0xC0000000;
    /** A row's own background: none, as an options list's rows have none. */
    static final int ROW = 0x00000000;
    static final int ROW_HOVER = 0x14FFFFFF;
    /** The selected page and tab, darkened as vanilla darkens a selected list entry. */
    static final int SELECTED = 0x60000000;
    /** The grey outline vanilla draws round a selected list entry. */
    static final int SELECTED_EDGE = 0xFF808080;
    /** The black outline of a vanilla button. */
    static final int EDGE = 0xFF000000;
    /** A heading inside a page's list, such as a mod's group of items: a darker band, not a lighter one. */
    static final int BAND = 0x40000000;
    static final int LINE = 0x20FFFFFF;
    static final int NOTE = 0x40F2B84B;

    private static final String ELLIPSIS = "...";
    private static final int WHITE = 0xFFFFFFFF;

    private ConfigTheme() { }

    static void border(GuiGraphicsExtractor graphics, int x, int y, int width, int height, int argb) {
        graphics.fill(x, y, x + width, y + 1, argb);
        graphics.fill(x, y + height - 1, x + width, y + height, argb);
        graphics.fill(x, y + 1, x + 1, y + height - 1, argb);
        graphics.fill(x + width - 1, y + 1, x + width, y + height - 1, argb);
    }

    /** {@code text} in one line of at most {@code width} pixels, cut with an ellipsis when it is longer. */
    static void clippedText(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int width, int argb) {
        if (font.width(text) <= width) {
            ClientVanilla.text(graphics, font, text, x, y, argb);
            return;
        }
        String cut = font.plainSubstrByWidth(text.getString(), Math.max(0, width - font.width(ELLIPSIS)));
        ClientVanilla.text(graphics, font, Component.literal(cut + ELLIPSIS).withStyle(text.getStyle()), x, y, argb);
    }

    /** A whole 16x16 texture, such as an item's, drawn at its own size. */
    static void icon(GuiGraphicsExtractor graphics, Identifier texture, int x, int y) {
        ClientVanilla.blit(graphics, texture, x, y, 0, 0, 16, 16, 16, 16, WHITE);
    }

    /**
     * A small picture given as rows of {@code #}, one GUI pixel each, so it stays crisp at any scale.
     */
    static void glyph(GuiGraphicsExtractor graphics, String[] rows, int x, int y, int argb) {
        for (int row = 0; row < rows.length; row++) {
            for (int column = 0; column < rows[row].length(); column++) {
                if (rows[row].charAt(column) != '#') continue;
                graphics.fill(x + column, y + row, x + column + 1, y + row + 1, argb);
            }
        }
    }

    /**
     * An item's icon at 16x16, as a slot draws it but without a count. Only call this in a world: 26.1
     * and later bind item components when one loads, and a stack built before that crashes the game.
     */
    static void item(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        graphics.fakeItem(stack, x, y);
    }
}
