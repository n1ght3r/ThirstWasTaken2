package com.thirstwastaken2.client;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.block.ThirstBlocks;
import com.thirstwastaken2.block.ThirstMenus;
import com.thirstwastaken2.client.platform.ClientLoader;
import com.thirstwastaken2.client.screen.DistillerScreen;

public final class ThirstWasTaken2Client {
    /** One droplet row, as tall as vanilla's hunger bar. */
    private static final int THIRST_BAR_HEIGHT = 10;

    private ThirstWasTaken2Client() { }

    /** Called once by the loader's client entrypoint. */
    public static void initialize() {
        ClientLoader.addRightStatusBar(ThirstWasTaken2.id("thirst_bar"), THIRST_BAR_HEIGHT,
                ThirstHud::shouldRender, ThirstHud::render);
        ClientLoader.renderCutout(() -> ThirstBlocks.COPPER_HANGING_POT);
        ClientLoader.renderCutout(() -> ThirstBlocks.IRON_HANGING_POT);
        ClientLoader.renderCutout(() -> ThirstBlocks.COPPER_DISTILLER);
        ClientLoader.renderCutout(() -> ThirstBlocks.BRICK_FIREBOX);
        ClientLoader.renderCutout(() -> ThirstBlocks.DISTILLER_BOILER);
        ClientLoader.renderCutout(() -> ThirstBlocks.COOLING_TUB);
        ClientLoader.registerScreen(() -> ThirstMenus.COPPER_DISTILLER, DistillerScreen::new);
    }
}
