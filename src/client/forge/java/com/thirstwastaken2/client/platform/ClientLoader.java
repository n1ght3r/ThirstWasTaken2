package com.thirstwastaken2.client.platform;

import com.thirstwastaken2.ThirstWasTaken2;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLModContainer;
import squeek.appleskin.ModConfig;

import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Every client call into the mod loader, for Forge 47. The client half of
 * {@link com.thirstwastaken2.platform.Loader}, under the same rules.
 */
public final class ClientLoader {
    private ClientLoader() { }

    /**
     * Adds a row to the right-hand status bar stack, drawn after the food bar. While {@code visible}
     * holds it takes {@code height} pixels of the stack, so vanilla's air bubbles and other mods' rows
     * move up past it.
     *
     * <p>The same arrangement as NeoForge's GUI layers, one generation earlier: each overlay reads
     * {@code ForgeGui.rightHeight}, draws there and advances it, and only survival-like modes draw the
     * food bar's row at all.
     */
    public static void addRightStatusBar(Identifier id, int height, Predicate<Player> visible, StatusBarRenderer renderer) {
        modBus().addListener((RegisterGuiOverlaysEvent event) -> event.registerAbove(VanillaGuiOverlay.FOOD_LEVEL.id(),
                id.getPath(), (gui, graphics, partialTick, width, screenHeight) -> {
                    Minecraft minecraft = Minecraft.getInstance();
                    Player player = minecraft.player;
                    if (player == null || minecraft.options.hideGui || !gui.shouldDrawSurvivalElements()
                            || !visible.test(player)) {
                        return;
                    }
                    renderer.render(graphics, screenHeight - gui.rightHeight);
                    gui.rightHeight += height;
                }));
    }

    /**
     * Draws {@code block} with its transparent pixels cut out, which a model with a chain in it needs.
     * The block is asked for once client setup runs, after every block is registered.
     */
    @SuppressWarnings("deprecation")
    public static void renderCutout(Supplier<Block> block) {
        modBus().addListener((FMLClientSetupEvent event) ->
                event.enqueueWork(() -> ItemBlockRenderTypes.setRenderLayer(block.get(), RenderType.cutout())));
    }

    /**
     * Whether AppleSkin's own exhaustion underlay setting is on. Only call once AppleSkin is known to be
     * loaded: it names AppleSkin's classes.
     */
    public static boolean appleSkinShowsExhaustionUnderlay() {
        return AppleSkinConfig.showsExhaustionUnderlay();
    }

    private static IEventBus modBus() {
        return ((FMLModContainer) ModList.get().getModContainerById(ThirstWasTaken2.MOD_ID).orElseThrow()).getEventBus();
    }

    /** Loaded only when asked, so {@link ClientLoader} itself never names AppleSkin's classes. */
    private static final class AppleSkinConfig {
        private static boolean showsExhaustionUnderlay() {
            // Forge loads AppleSkin's config, not AppleSkin, and reading a value before then throws.
            return ModConfig.SPEC.isLoaded() && ModConfig.SHOW_FOOD_EXHAUSTION_UNDERLAY.get();
        }
    }
}
