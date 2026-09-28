package com.thirstwastaken2.client;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.client.compat.AppleSkinIntegration;
import com.thirstwastaken2.client.platform.ClientVanilla;
import com.thirstwastaken2.compat.AppleSkin;
import com.thirstwastaken2.config.QuenchedOverlay;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.data.ThirstManager;
import com.thirstwastaken2.effect.ThirstEffects;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class ThirstHud {
    private static final Identifier ICONS = ThirstWasTaken2.id("textures/gui/thirst_icons.png");
    /**
     * The same sheet in dry sand, drawn while the player is Parched, the way vanilla recolours the food
     * bar for Hunger. {@code tools/generate_parched_icons.py} draws it from {@link #ICONS}.
     */
    private static final Identifier PARCHED_ICONS = ThirstWasTaken2.id("textures/gui/thirst_icons_parched.png");
    /**
     * The same sheet in a venom green, drawn while the player has Upset Stomach, ahead of Parched.
     * {@code tools/generate_upset_stomach_bar.py} draws it from {@link #ICONS}.
     */
    private static final Identifier UPSET_STOMACH_ICONS =
            ThirstWasTaken2.id("textures/gui/thirst_icons_upset_stomach.png");
    private static final Identifier OVERLAY_ICONS = ThirstWasTaken2.id("textures/gui/appleskin_icons.png");
    private static final Identifier QUENCHED_ICONS = ThirstWasTaken2.id("textures/gui/quenched_overlay.png");
    private static final RandomSource RANDOM = RandomSource.create();

    private static final int ICON_SIZE = 9;
    private static final int ICONS_TEXTURE_WIDTH = 41;
    private static final int ICONS_TEXTURE_HEIGHT = 9;
    /**
     * Fill frames on the sheet, driest first. Frames share their transparent edge columns, so the
     * stride is 8 rather than {@link #ICON_SIZE}.
     */
    private static final int U_EMPTY = 0;
    private static final int[] FILL_FRAMES = {8, 16, 24, 32};
    /** Units of thirst each entry of {@link #FILL_FRAMES} needs; one droplet holds two. */
    private static final float[] FILL_THRESHOLDS = {0.5F, 1.0F, 1.5F, 2.0F};
    private static final int OVERLAY_TEXTURE_SIZE = 256;
    /** Four frames across, one row per coloured {@link QuenchedOverlay}. */
    private static final int QUENCHED_TEXTURE_WIDTH = 36;
    private static final int QUENCHED_TEXTURE_HEIGHT = 45;
    private static final int OPAQUE = 0xFFFFFFFF;
    private static final int EXHAUSTION_TINT = 0xBFFFFFFF;
    private static final int BAR_WIDTH = 81;
    private static final float MAX_EXHAUSTION = 4.0F;

    private ThirstHud() { }

    /**
     * Vanilla draws the food bar for a dead player too, so the hunger bar stays on screen behind the
     * death screen; the thirst bar follows it and is deliberately not gated on {@code isAlive}.
     */
    public static boolean shouldRender(Player player) {
        if (player == null || player.getVehicle() instanceof LivingEntity) return false;
        Minecraft minecraft = Minecraft.getInstance();
        return !ClientVanilla.isHudHidden(minecraft) && ThirstManager.get(player).enabled();
    }

    /** {@code stackTop} is the y the loader assigned this row in the right-hand status bar stack. */
    public static void render(GuiGraphicsExtractor graphics, int stackTop) {
        Player player = Minecraft.getInstance().player;
        if (!shouldRender(player)) return;

        ThirstData data = ThirstManager.get(player);
        int thirst = data.thirst();
        int quenched = data.quenched();

        int right = graphics.guiWidth() / 2 + 91;
        int top = stackTop;

        // Vanilla shakes the hunger bar once saturation runs out; the thirst bar mirrors that.
        boolean shake = quenched <= 0 && player.tickCount % (thirst * 3 + 1) == 0;

        drawBar(graphics, right, top, thirst, quenched, data.exhaustion(), AppleSkin.quenchedOverlay(),
                AppleSkinIntegration.shouldShowExhaustion(), shake, Vanilla.hasEffect(player, ThirstEffects.PARCHED),
                Vanilla.hasEffect(player, ThirstEffects.UPSET_STOMACH));
    }

    /**
     * Draws the bar for any state, with its right edge at {@code right}. The HUD passes the player's
     * synced state; the config screen's preview passes a made-up one.
     */
    public static void drawBar(GuiGraphicsExtractor graphics, int right, int top, int thirst, int quenched,
                               float exhaustion, QuenchedOverlay overlay, boolean exhaustionStrip, boolean shake,
                               boolean parched, boolean upsetStomach) {
        if (exhaustionStrip) renderExhaustion(graphics, right, top, exhaustion);
        Identifier icons = upsetStomach ? UPSET_STOMACH_ICONS : parched ? PARCHED_ICONS : ICONS;

        // Whole points only, as vanilla draws food: exhaustion toward the next point is not shown, so a
        // full bar never looks short of full while drinking is still refused at the maximum.
        for (int i = 0; i < 10; i++) {
            int x = right - i * 8 - ICON_SIZE;
            int y = top;
            if (shake) y += RANDOM.nextInt(3) - 1;

            icon(graphics, icons, x, y, U_EMPTY);
            int fill = fillFrame(thirst - i * 2);
            if (fill >= 0) {
                icon(graphics, icons, x, y, fill);
            }

            renderQuenched(graphics, overlay, x, y, quenched / 2.0F - i);
        }
    }

    /** Texture u of the wettest frame this droplet has earned, or -1 when it is dry. */
    private static int fillFrame(float units) {
        for (int i = FILL_FRAMES.length - 1; i >= 0; i--) {
            if (units >= FILL_THRESHOLDS[i]) return FILL_FRAMES[i];
        }
        return -1;
    }

    private static void icon(GuiGraphicsExtractor graphics, Identifier icons, int x, int y, int u) {
        ClientVanilla.blit(graphics, icons, x, y, u, 0, ICON_SIZE, ICON_SIZE,
                ICONS_TEXTURE_WIDTH, ICONS_TEXTURE_HEIGHT, OPAQUE);
    }

    /**
     * Draws AppleSkin's dithered exhaustion underlay beneath the thirst icons. Public for the config
     * preview, which draws it on the clock of its own sweep rather than from a player's exhaustion.
     */
    public static void renderExhaustion(GuiGraphicsExtractor graphics, int right, int top, float exhaustion) {
        float ratio = Math.min(1.0F, Math.max(0.0F, exhaustion / MAX_EXHAUSTION));
        int width = (int) (ratio * BAR_WIDTH);
        if (width <= 0) return;

        ClientVanilla.blit(graphics, OVERLAY_ICONS,
                right - width, top, BAR_WIDTH - width, 18.0F, width, ICON_SIZE,
                OVERLAY_TEXTURE_SIZE, OVERLAY_TEXTURE_SIZE, EXHAUSTION_TINT);
    }

    /** AppleSkin's saturation outline, for quenched; {@link QuenchedOverlay#OFF} without AppleSkin. */
    private static void renderQuenched(GuiGraphicsExtractor graphics, QuenchedOverlay overlay, int x, int y,
                                       float effective) {
        if (overlay == QuenchedOverlay.OFF || effective <= 0.0F) return;
        int u = effective >= 1.0F ? 27 : effective > 0.5F ? 18 : effective > 0.25F ? 9 : 0;
        int v = overlay.ordinal() * ICON_SIZE;
        ClientVanilla.blit(graphics, QUENCHED_ICONS, x, y, u, v, ICON_SIZE, ICON_SIZE,
                QUENCHED_TEXTURE_WIDTH, QUENCHED_TEXTURE_HEIGHT, OPAQUE);
    }
}
