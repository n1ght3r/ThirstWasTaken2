package com.thirstwastaken2.client.platform;

import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * A screen that scrolls with the mouse wheel. 1.20.2 gave the wheel a horizontal amount as well, which
 * changed the method a screen overrides, so the override lives here once and a screen implements
 * {@link #scrolled} instead, with the same arguments on every version.
 */
public abstract class ScrollingScreen extends Screen {
    protected ScrollingScreen(Component title) {
        super(title);
    }

    /**
     * The wheel turned over the screen. Returns whether the screen used it; when it did not, the widget
     * under the pointer gets it as usual.
     */
    protected abstract boolean scrolled(double mouseX, double mouseY, double scrollX, double scrollY);

    //? if >=1.20.5 {
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        return scrolled(mouseX, mouseY, scrollX, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    //?} else {
    /*@Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollY) {
        return scrolled(mouseX, mouseY, 0.0, scrollY) || super.mouseScrolled(mouseX, mouseY, scrollY);
    }
    *///?}
}
