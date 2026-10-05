package com.thirstwastaken2.client.screen;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.block.DistillerMenu;
import com.thirstwastaken2.client.platform.ClientVanilla;
import com.thirstwastaken2.client.platform.MachineScreen;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.purity.WaterQuality;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

import java.util.ArrayList;
import java.util.List;

/**
 * The copper distiller's GUI, drawn from {@code textures/gui/container/copper_distiller.png}: a bar of
 * fuel between the water and the fuel slot emptying as the fire burns, the arrow filling as a serving
 * distils (red while the tub is dry, and saying why), the boiler's gauge in the colour of what it holds
 * and the basin's in Pure water's, each naming its servings when hovered. The salt slot's frame shows
 * only while there is salt to make. Every position is the texture's; see
 * {@code tools/distiller/generate_distiller_gui.py}.
 */
public final class DistillerScreen extends MachineScreen<DistillerMenu> {
    private static final Identifier TEXTURE = ThirstWasTaken2.id("textures/gui/container/copper_distiller.png");
    private static final int SHEET = 256;
    private static final int WHITE = 0xFFFFFFFF;

    private static final int FUEL_X = 20;
    private static final int FUEL_Y = 42;
    private static final int FUEL_W = 16;
    private static final int FUEL_H = 4;
    private static final int ARROW_X = 70;
    private static final int ARROW_Y = 35;
    private static final int ARROW_W = 24;
    private static final int ARROW_H = 17;
    private static final int BOILER_X = 46;
    private static final int BASIN_X = 100;
    private static final int GAUGE_Y = 16;
    private static final int GAUGE_W = 16;
    private static final int GAUGE_H = 54;
    private static final int SALT_X = 149;
    private static final int SALT_Y = 52;
    /** Where the water columns start on the sheet, one per grade, then salt. */
    private static final int WATER_U = 176;
    private static final int WATER_V = 70;
    private static final int SALT_COLUMN = 4;

    public DistillerScreen(DistillerMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, DistillerMenu.WIDTH, DistillerMenu.HEIGHT);
    }

    @Override
    protected void drawBackground(GuiGraphicsExtractor graphics, int left, int top, int mouseX, int mouseY) {
        blit(graphics, left, top, 0, 0, DistillerMenu.WIDTH, DistillerMenu.HEIGHT);
        if (menu.burnLeft() > 0) {
            int fuel = (int) Math.ceil(menu.burnLeft() * FUEL_W / (double) Math.max(1, menu.burnTotal()));
            blit(graphics, left + FUEL_X, top + FUEL_Y, 176, 0, Math.min(fuel, FUEL_W), FUEL_H);
        }
        if (!menu.cooled()) {
            blit(graphics, left + ARROW_X, top + ARROW_Y, 176, 31, ARROW_W, ARROW_H);
        } else if (menu.progress() > 0) {
            blit(graphics, left + ARROW_X, top + ARROW_Y, 176, 14, menu.progress() * ARROW_W / menu.servingTicks(), ARROW_H);
        }
        WaterQuality boiler = menu.boilerQuality();
        if (boiler != null) gauge(graphics, left + BOILER_X, top, menu.boilerServings(), column(boiler));
        gauge(graphics, left + BASIN_X, top, menu.basinServings(), WaterPurity.MAX);
        if (menu.makesSalt()) blit(graphics, left + SALT_X, top + SALT_Y, 176, 48, 18, 18);
    }

    /** {@code servings} of water in the gauge at {@code x}, rising from its bottom, from the sheet's column {@code column}. */
    private void gauge(GuiGraphicsExtractor graphics, int x, int top, int servings, int column) {
        if (servings <= 0) return;
        int height = Math.min(GAUGE_H, servings * GAUGE_H / menu.tank());
        blit(graphics, x, top + GAUGE_Y + GAUGE_H - height, WATER_U + column * GAUGE_W, WATER_V + GAUGE_H - height,
                GAUGE_W, height);
    }

    private static void blit(GuiGraphicsExtractor graphics, int x, int y, int u, int v, int width, int height) {
        ClientVanilla.blit(graphics, TEXTURE, x, y, u, v, width, height, SHEET, SHEET, WHITE);
    }

    private static int column(WaterQuality quality) {
        return quality instanceof WaterQuality.Fresh fresh ? fresh.purity() : SALT_COLUMN;
    }

    @Override
    protected void drawTooltips(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {
        int x = mouseX - leftPos;
        int y = mouseY - topPos;
        List<Component> lines = new ArrayList<>();
        if (over(x, y, BOILER_X, GAUGE_Y, GAUGE_W, GAUGE_H)) {
            lines.add(Component.translatable("container.thirstwastaken2.copper_distiller.boiler",
                    menu.boilerServings(), menu.tank()));
            WaterQuality quality = menu.boilerQuality();
            if (quality instanceof WaterQuality.Fresh fresh) lines.add(WaterPurity.purityName(fresh.purity()));
            else if (quality != null) lines.add(WaterPurity.saltTooltip());
        } else if (over(x, y, BASIN_X, GAUGE_Y, GAUGE_W, GAUGE_H)) {
            lines.add(Component.translatable("container.thirstwastaken2.copper_distiller.basin",
                    menu.basinServings(), menu.tank()));
            if (menu.basinServings() > 0) lines.add(WaterPurity.purityName(WaterPurity.MAX));
        } else if (!menu.cooled() && over(x, y, ARROW_X, ARROW_Y, ARROW_W, ARROW_H)) {
            lines.add(Component.translatable("container.thirstwastaken2.copper_distiller.dry"));
            lines.add(Component.translatable("container.thirstwastaken2.copper_distiller.dry.hint"));
        }
        if (!lines.isEmpty()) ClientVanilla.tooltip(graphics, font, lines, mouseX, mouseY);
    }

    private static boolean over(int x, int y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
