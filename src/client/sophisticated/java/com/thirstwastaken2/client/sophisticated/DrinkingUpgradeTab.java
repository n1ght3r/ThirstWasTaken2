package com.thirstwastaken2.client.sophisticated;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.purity.WaterPurity;
import com.thirstwastaken2.sophisticated.drinking.DrinkAt;
import com.thirstwastaken2.sophisticated.drinking.DrinkingUpgradeContainer;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.p3pp3rf1y.sophisticatedcore.client.gui.StorageScreenBase;
import net.p3pp3rf1y.sophisticatedcore.client.gui.UpgradeSettingsTab;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinition;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ButtonDefinitions;
import net.p3pp3rf1y.sophisticatedcore.client.gui.controls.ToggleButton;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Dimension;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.Position;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.TextureBlitData;
import net.p3pp3rf1y.sophisticatedcore.client.gui.utils.UV;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogic;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicContainer;
import net.p3pp3rf1y.sophisticatedcore.upgrades.FilterLogicControl;

import java.util.HashMap;
import java.util.Map;

/**
 * The Drinking upgrade's settings tab, laid out like the Feeding upgrade's: the basic tab is only a
 * filter, the advanced one adds when to drink and the lowest grade of water to drink.
 */
public abstract class DrinkingUpgradeTab extends UpgradeSettingsTab<DrinkingUpgradeContainer> {
    /**
     * One 16x16 icon per state: the first row is how empty the droplet has to be, the second the grade,
     * drawn as the water bowl of that grade.
     */
    private static final Identifier BUTTONS = ThirstWasTaken2.id("textures/gui/drinking_upgrade_buttons.png");
    private static final Dimension BUTTONS_SIZE = new Dimension(64, 32);

    public static final ButtonDefinition.Toggle<DrinkAt> DRINK_AT = ButtonDefinitions.createToggleButtonDefinition(Map.of(
            DrinkAt.FULL, state(0, 0, "drink_at_full"),
            DrinkAt.HALF, state(16, 0, "drink_at_half"),
            DrinkAt.ANY, state(32, 0, "drink_at_any")));

    public static final ButtonDefinition.Toggle<Integer> MIN_PURITY = minPurityButton();

    protected FilterLogicControl<FilterLogic, FilterLogicContainer<FilterLogic>> filterLogicControl;

    protected DrinkingUpgradeTab(DrinkingUpgradeContainer container, Position position, StorageScreenBase<?> screen,
                                 String name) {
        super(container, position, screen, Component.translatable("gui.thirstwastaken2.upgrades." + name),
                Component.translatable("gui.thirstwastaken2.upgrades." + name + ".tooltip"));
    }

    @Override
    protected void moveSlotsToTab() {
        filterLogicControl.moveSlotsToView();
    }

    private static ButtonDefinition.Toggle<Integer> minPurityButton() {
        Map<Integer, ToggleButton.StateData> states = new HashMap<>();
        for (int purity = WaterPurity.MIN; purity <= WaterPurity.MAX; purity++) {
            states.put(purity, new ToggleButton.StateData(icon(purity * 16, 16),
                    Component.translatable("gui.thirstwastaken2.upgrades.buttons.min_quality", WaterPurity.tooltip(purity)),
                    Component.translatable("gui.thirstwastaken2.upgrades.buttons.min_quality.detail").withStyle(ChatFormatting.GRAY)));
        }
        return ButtonDefinitions.createToggleButtonDefinition(states);
    }

    private static ToggleButton.StateData state(int u, int v, String key) {
        String prefix = "gui.thirstwastaken2.upgrades.buttons." + key;
        return new ToggleButton.StateData(icon(u, v), Component.translatable(prefix),
                Component.translatable(prefix + ".detail").withStyle(ChatFormatting.GRAY));
    }

    private static TextureBlitData icon(int u, int v) {
        return new TextureBlitData(BUTTONS, new Position(1, 1), BUTTONS_SIZE, new UV(u, v), Dimension.SQUARE_16);
    }

    public static final class Basic extends DrinkingUpgradeTab {
        public Basic(DrinkingUpgradeContainer container, Position position, StorageScreenBase<?> screen, int slotsPerRow) {
            super(container, position, screen, "drinking");
            filterLogicControl = addHideableChild(new FilterLogicControl.Basic(screen, new Position(x + 3, y + 24),
                    getContainer().getFilterLogicContainer(), slotsPerRow));
        }
    }

    public static final class Advanced extends DrinkingUpgradeTab {
        public Advanced(DrinkingUpgradeContainer container, Position position, StorageScreenBase<?> screen, int slotsPerRow) {
            super(container, position, screen, "advanced_drinking");
            addHideableChild(new ToggleButton<>(new Position(x + 3, y + 24), DRINK_AT,
                    button -> getContainer().setDrinkAt(getContainer().getDrinkAt().next()),
                    () -> getContainer().getDrinkAt()));
            // Left click raises the grade, right click lowers it, both wrapping round.
            addHideableChild(new ToggleButton<>(new Position(x + 21, y + 24), MIN_PURITY,
                    button -> getContainer().setMinPurity(nextPurity(getContainer().getMinPurity(), button == 1 ? -1 : 1)),
                    () -> getContainer().getMinPurity()));
            filterLogicControl = addHideableChild(new FilterLogicControl.Advanced(screen, new Position(x + 3, y + 44),
                    getContainer().getFilterLogicContainer(), slotsPerRow));
        }

        private static int nextPurity(int purity, int step) {
            int grades = WaterPurity.MAX - WaterPurity.MIN + 1;
            return WaterPurity.MIN + Math.floorMod(purity - WaterPurity.MIN + step, grades);
        }
    }
}
