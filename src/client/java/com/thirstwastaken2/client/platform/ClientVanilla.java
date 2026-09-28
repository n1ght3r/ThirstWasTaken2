package com.thirstwastaken2.client.platform;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

/**
 * The client-side half of {@code com.thirstwastaken2.platform.Vanilla}: every client vanilla call
 * whose shape differs between the supported Minecraft versions. Same rules apply — plumbing only,
 * one signature for every version.
 */
public final class ClientVanilla {
    private ClientVanilla() { }

    /** Whether the player has hidden the HUD (F1). */
    public static boolean isHudHidden(Minecraft minecraft) {
        // 26.2 split the HUD out of Gui into its own object; before that the flag was an option.
        //? if >=26.2 {
        return minecraft.gui.hud.isHidden();
        //?} else {
        /*return minecraft.options.hideGui;
        *///?}
    }

    /** Opens {@code screen}, or closes the current one when it is null. 26.2 moved this onto the GUI. */
    public static void setScreen(Minecraft minecraft, net.minecraft.client.gui.screens.Screen screen) {
        //? if >=26.2 {
        minecraft.gui.setScreen(screen);
        //?} else {
        /*minecraft.setScreen(screen);
        *///?}
    }

    /** What a {@link #canvas} or {@link #button} draws every frame, given the widget for its bounds and state. */
    @FunctionalInterface
    public interface Painter {
        void paint(GuiGraphicsExtractor graphics, AbstractWidget widget, int mouseX, int mouseY);
    }

    /**
     * A widget that only draws, for placing custom drawing in a layout. It takes no input and no
     * focus, and a screen reader reads {@code narration}. It still shows a tooltip on hover. 26.1
     * renamed the method a widget draws in.
     */
    public static AbstractWidget canvas(int width, int height, Component narration, Painter painter) {
        Canvas canvas = new Canvas(width, height, narration, painter);
        canvas.active = false;
        return canvas;
    }

    /** Resizes a widget vertically. Before 1.20.5 only a {@link #canvas} can be, which is all the mod resizes. */
    public static void setHeight(AbstractWidget widget, int height) {
        //? if >=1.20.5 {
        widget.setHeight(height);
        //?} else {
        /*if (widget instanceof Canvas canvas) canvas.resize(height);
        *///?}
    }

    /** See {@link #canvas}. */
    private static final class Canvas extends AbstractWidget {
        private final Painter painter;

        Canvas(int width, int height, Component narration, Painter painter) {
            super(0, 0, width, height, narration);
            this.painter = painter;
        }

        void resize(int height) {
            this.height = height;
        }

        //? if >=26.1 {
        @Override
        protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            painter.paint(graphics, this, mouseX, mouseY);
        }
        //?} else {
        /*@Override
        protected void renderWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
            painter.paint(graphics, this, mouseX, mouseY);
        }
        *///?}

        @Override
        protected void updateWidgetNarration(NarrationElementOutput output) {
            output.add(NarratedElementType.TITLE, getMessage());
        }
    }

    /**
     * Turns the mouse wheel over a screen, {@code scrollY} notches up. See {@link ScrollingScreen}: before
     * 1.20.2 the wheel had no horizontal amount.
     */
    public static boolean mouseScrolled(net.minecraft.client.gui.screens.Screen screen, double mouseX, double mouseY,
                                        double scrollY) {
        //? if >=1.20.5 {
        return screen.mouseScrolled(mouseX, mouseY, 0.0, scrollY);
        //?} else {
        /*return screen.mouseScrolled(mouseX, mouseY, scrollY);
        *///?}
    }

    /**
     * A button drawn entirely by {@code painter}: it clicks, focuses, narrates and plays the click sound
     * like a vanilla button. 1.21.11 made the button draw its contents through a method of its own, and
     * 26.1 renamed it.
     */
    public static AbstractWidget button(int width, int height, Component message, Runnable onPress, Painter painter) {
        // The lambdas are locals, not constructor arguments: Forge 1.20.1's recompiled Button names every
        // constructor parameter the same, so javac rejects a lambda inside this anonymous constructor.
        Button.OnPress press = button -> onPress.run();
        Button.CreateNarration narration = supplier -> supplier.get();
        return new Button(0, 0, width, height, message, press, narration) {
            //? if >=26.1 {
            @Override
            protected void extractContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                painter.paint(graphics, this, mouseX, mouseY);
            }
            //?}
            //? if >1.21.1 <26.1 {
            /*@Override
            protected void renderContents(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                painter.paint(graphics, this, mouseX, mouseY);
            }
            *///?}
            //? if <=1.21.1 {
            /*@Override
            protected void renderWidget(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
                painter.paint(graphics, this, mouseX, mouseY);
            }
            *///?}
        };
    }

    /** Draws a line of text with a shadow. {@code argb} needs its alpha, which later releases honour. */
    public static void text(GuiGraphicsExtractor graphics, Font font, Component text, int x, int y, int argb) {
        //? if >=26.1 {
        graphics.text(font, text, x, y, argb, true);
        //?} else {
        /*graphics.drawString(font, text, x, y, argb, true);
        *///?}
    }

    /** Draws one wrapped line of text, as {@code Font.split} returns them, with a shadow. */
    public static void text(GuiGraphicsExtractor graphics, Font font, FormattedCharSequence text, int x, int y, int argb) {
        //? if >=26.1 {
        graphics.text(font, text, x, y, argb, true);
        //?} else {
        /*graphics.drawString(font, text, x, y, argb, true);
        *///?}
    }

    /**
     * Draws a sprite from the GUI atlas, such as vanilla's {@code hud/food_full}. 1.20.1 has no GUI
     * atlas: vanilla's HUD icons are regions of {@code textures/gui/icons.png}, so there only the sprites
     * the mod draws are known, by where they sit on that sheet, and any other draws nothing.
     */
    public static void blitSprite(GuiGraphicsExtractor graphics, Identifier sprite, int x, int y, int width, int height) {
        //? if >1.21.1 {
        graphics.blitSprite(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, sprite, x, y, width, height);
        //?} elif >=1.20.5 {
        /*com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        graphics.blitSprite(sprite, x, y, width, height);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        *///?} else {
        /*int[] uv = LEGACY_ICONS.get(sprite);
        if (uv == null) return;
        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        graphics.blit(LEGACY_ICON_SHEET, x, y, uv[0], uv[1], width, height);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        *///?}
    }

    //? if <1.20.5 {
    /*private static final Identifier LEGACY_ICON_SHEET = Identifier.withDefaultNamespace("textures/gui/icons.png");
    // Where 1.20.1's Gui draws each sprite from on that sheet.
    private static final java.util.Map<Identifier, int[]> LEGACY_ICONS = java.util.Map.of(
            Identifier.withDefaultNamespace("hud/food_empty"), new int[]{16, 27},
            Identifier.withDefaultNamespace("hud/food_full"), new int[]{52, 27},
            Identifier.withDefaultNamespace("hud/food_half"), new int[]{61, 27});
    *///?}

    /**
     * Draws a {@code width} by {@code height} region of a texture sheet at {@code u, v}, tinted with
     * {@code argb}. Later releases pass the render pipeline and the tint with the draw call; on
     * 1.21.1 both are global render state, set before the draw and reset after it.
     */
    public static void blit(GuiGraphicsExtractor graphics, Identifier texture, int x, int y, float u, float v,
                            int width, int height, int textureWidth, int textureHeight, int argb) {
        //? if >1.21.1 {
        graphics.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED, texture, x, y, u, v,
                width, height, textureWidth, textureHeight, argb);
        //?} else {
        /*com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        graphics.setColor(((argb >> 16) & 0xFF) / 255.0F, ((argb >> 8) & 0xFF) / 255.0F,
                (argb & 0xFF) / 255.0F, ((argb >>> 24) & 0xFF) / 255.0F);
        graphics.blit(texture, x, y, u, v, width, height, textureWidth, textureHeight);
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
        *///?}
    }

    /**
     * Opens a folder or file in the player's file manager. 26.3 moved this off {@code Util.OS}, which
     * took a {@code File} before 1.20.5.
     */
    public static void openPath(java.nio.file.Path path) {
        //? if >=26.3 {
        com.mojang.blaze3d.Blaze3D.openPath(path);
        //?} elif >=1.20.5 {
        /*net.minecraft.util.Util.getPlatform().openPath(path);
        *///?} else {
        /*net.minecraft.util.Util.getPlatform().openFile(path.toFile());
        *///?}
    }
}
