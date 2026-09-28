package com.thirstwastaken2.client.config;

import com.thirstwastaken2.client.platform.ClientVanilla;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * One row of the settings list: a page heading, a setting, a note, the preview or a button. A row owns
 * its widgets, and the screen places it and shows or hides it as the list scrolls. Scrolling moves a
 * whole row at a time, so a row is either fully in view or hidden, and nothing needs clipping.
 */
abstract class ConfigRow {
    static final int OPTION_HEIGHT = 22;
    static final int CONTROL_HEIGHT = 20;
    static final int RESET_WIDTH = 20;
    static final int GAP = 4;
    static final int PADDING = 6;
    static final int TAB_HEIGHT = 18;

    private final List<AbstractWidget> widgets;
    /** The heading this row is listed under, kept at the top while the row scrolls past it; null for none. */
    private ConfigRow heading;

    ConfigRow(List<AbstractWidget> widgets) {
        this.widgets = widgets;
    }

    /** Lists this row under {@code heading}, which stays in view above it once scrolled past. */
    final ConfigRow under(ConfigRow heading) {
        this.heading = heading;
        return this;
    }

    final ConfigRow heading() {
        return heading;
    }

    /** The height of this row at {@code width}; only a note wraps, so only a note depends on it. */
    abstract int height(int width);

    /** Moves the row's widgets to {@code x, y} and sizes them to {@code width}. */
    abstract void place(int x, int y, int width);

    /** Widgets in drawing order, the row's own background first. */
    final List<AbstractWidget> widgets() {
        return widgets;
    }

    final void setVisible(boolean visible) {
        for (AbstractWidget widget : widgets) widget.visible = visible;
    }

    /** Called every tick while the row is on screen. */
    void tick() { }

    static Font font() {
        return Minecraft.getInstance().font;
    }

    /** The page's name and what it holds, above its rows. */
    static ConfigRow heading(Component title, Component description, Identifier icon) {
        AbstractWidget canvas = ClientVanilla.canvas(0, 30, title, (graphics, widget, mouseX, mouseY) -> {
            Font font = font();
            int x = widget.getX();
            int y = widget.getY();
            int textX = x;
            if (icon != null) {
                ConfigTheme.icon(graphics, icon, x, y + 2);
                textX += 20;
            }
            ClientVanilla.text(graphics, font, title, textX, y + 2, ConfigTheme.TEXT);
            ConfigTheme.clippedText(graphics, font, description, textX, y + 13, x + widget.getWidth() - textX, ConfigTheme.MUTED);
            graphics.fill(x, y + widget.getHeight() - 3, x + widget.getWidth(), y + widget.getHeight() - 2, ConfigTheme.LINE);
        });
        canvas.setTooltip(Tooltip.create(description));
        return new ConfigRow(List.of(canvas)) {
            @Override
            int height(int width) {
                return 30;
            }

            @Override
            void place(int x, int y, int width) {
                canvas.setPosition(x, y);
                canvas.setWidth(width);
            }
        };
    }

    /**
     * The tabs of a page split into sections, below its heading, the shown one lit. Each tab is as wide
     * as its name and the spare width is shared out; when the names do not fit, every tab shrinks in
     * proportion and a cut name shows in full on hover.
     */
    static ConfigRow tabs(List<Component> titles, int selected, IntConsumer onSelect) {
        List<AbstractWidget> tabs = new ArrayList<>();
        for (int i = 0; i < titles.size(); i++) {
            int index = i;
            Component title = titles.get(i);
            tabs.add(ClientVanilla.button(0, TAB_HEIGHT, title, () -> onSelect.accept(index), (graphics, widget, mouseX, mouseY) -> {
                int x = widget.getX();
                int y = widget.getY();
                int right = x + widget.getWidth();
                int bottom = y + widget.getHeight();
                boolean current = index == selected;
                boolean lit = current || widget.isHoveredOrFocused();
                graphics.fill(x, y, right, bottom, current ? ConfigTheme.SELECTED : lit ? ConfigTheme.ROW_HOVER : ConfigTheme.ROW);
                if (current) graphics.fill(x, bottom - 2, right, bottom, ConfigTheme.ACCENT);
                int textWidth = Math.min(font().width(title), widget.getWidth() - PADDING);
                ConfigTheme.clippedText(graphics, font(), title, x + (widget.getWidth() - textWidth) / 2,
                        y + (widget.getHeight() - 8) / 2, textWidth, lit ? ConfigTheme.TEXT : ConfigTheme.MUTED);
            }));
        }
        return new ConfigRow(tabs) {
            @Override
            int height(int width) {
                return TAB_HEIGHT;
            }

            @Override
            void place(int x, int y, int width) {
                int gaps = GAP * (tabs.size() - 1);
                int natural = 0;
                for (Component title : titles) natural += font().width(title) + PADDING * 2;
                int spare = width - gaps - natural;
                int tabX = x;
                for (int i = 0; i < tabs.size(); i++) {
                    AbstractWidget tab = tabs.get(i);
                    int own = font().width(titles.get(i)) + PADDING * 2;
                    int tabWidth = spare >= 0 ? own + spare / tabs.size() : (width - gaps) * own / natural;
                    // The last tab takes what rounding left over, so the strip ends flush with the rows.
                    if (i == tabs.size() - 1) tabWidth = x + width - tabX;
                    tab.setPosition(tabX, y);
                    tab.setWidth(tabWidth);
                    tab.setTooltip(font().width(titles.get(i)) > tabWidth - PADDING ? Tooltip.create(titles.get(i)) : null);
                    tabX += tabWidth + GAP;
                }
            }
        };
    }

    /** A smaller heading, naming the page the search results below it come from. */
    static ConfigRow subheading(Component title, Identifier icon) {
        AbstractWidget canvas = ClientVanilla.canvas(0, 20, title, (graphics, widget, mouseX, mouseY) -> {
            int x = widget.getX();
            int y = widget.getY();
            ConfigTheme.icon(graphics, icon, x, y + 1);
            ClientVanilla.text(graphics, font(), title, x + 20, y + 5, ConfigTheme.ACCENT);
        });
        return new ConfigRow(List.of(canvas)) {
            @Override
            int height(int width) {
                return 20;
            }

            @Override
            void place(int x, int y, int width) {
                canvas.setPosition(x, y);
                canvas.setWidth(width);
            }
        };
    }

    /**
     * A setting: its name on the left, its control on the right, and a button that puts back its
     * default beside it. The background and the name carry the description as a tooltip, and an amber
     * bar marks a setting that differs from its default.
     */
    static ConfigRow option(ConfigEntry<?> entry, int controlWidth, Runnable onReset) {
        AbstractWidget background = ClientVanilla.canvas(0, OPTION_HEIGHT, entry.label(), (graphics, widget, mouseX, mouseY) -> {
            int x = widget.getX();
            int y = widget.getY();
            int right = x + widget.getWidth();
            graphics.fill(x, y, right, y + widget.getHeight(), widget.isHovered() ? ConfigTheme.ROW_HOVER : ConfigTheme.ROW);
            boolean changed = !entry.isDefault();
            if (changed) graphics.fill(x, y, x + 2, y + widget.getHeight(), ConfigTheme.CHANGED);
            int labelWidth = widget.getWidth() - controlWidth - PADDING * 2 - GAP;
            ConfigTheme.clippedText(graphics, font(), entry.label(), x + PADDING, y + (widget.getHeight() - 8) / 2,
                    labelWidth, changed ? ConfigTheme.CHANGED : ConfigTheme.TEXT);
        });
        background.setTooltip(Tooltip.create(entry.description()));
        AbstractWidget control = entry.control(controlWidth);
        AbstractWidget reset = ClientVanilla.button(RESET_WIDTH, CONTROL_HEIGHT,
                Component.translatable("thirstwastaken2.config.reset_option"), () -> {
                    entry.reset();
                    onReset.run();
                }, ConfigRow::paintReset);
        reset.setTooltip(Tooltip.create(Component.translatable("thirstwastaken2.config.reset_option")));
        reset.active = !entry.isDefault();
        return new ConfigRow(List.of(background, control, reset)) {
            @Override
            int height(int width) {
                return OPTION_HEIGHT;
            }

            @Override
            void place(int x, int y, int width) {
                int resetX = x + width - RESET_WIDTH;
                background.setPosition(x, y);
                background.setWidth(resetX - GAP - x);
                control.setPosition(resetX - GAP - 1 - controlWidth, y + 1);
                reset.setPosition(resetX, y + 1);
            }

            @Override
            void tick() {
                reset.active = !entry.isDefault();
            }
        };
    }

    /** A circular arrow, one row per string, drawn a GUI pixel at a time so it stays crisp at any scale. */
    static final String[] RESET_ICON = {
            "..####.#",
            ".#....##",
            "#....###",
            "#.......",
            "#.......",
            "#......#",
            ".#....#.",
            "..####..",
    };

    private static void paintReset(GuiGraphicsExtractor graphics, AbstractWidget widget,
                                   int mouseX, int mouseY) {
        paintIconButton(graphics, widget, RESET_ICON, ConfigTheme.CHANGED);
    }

    /**
     * A square button showing {@code icon}, 8x8 as strings of {@code #}, in {@code color}: faint while
     * inactive and white while hovered.
     */
    static void paintIconButton(GuiGraphicsExtractor graphics, AbstractWidget widget, String[] icon, int color) {
        int x = widget.getX();
        int y = widget.getY();
        boolean lit = widget.active && widget.isHoveredOrFocused();
        graphics.fill(x, y, x + widget.getWidth(), y + widget.getHeight(), lit ? ConfigTheme.ROW_HOVER : ConfigTheme.ROW);
        ConfigTheme.border(graphics, x, y, widget.getWidth(), widget.getHeight(), lit ? ConfigTheme.ACCENT : ConfigTheme.LINE);
        int shade = !widget.active ? ConfigTheme.FAINT : lit ? ConfigTheme.TEXT : color;
        ConfigTheme.glyph(graphics, icon, x + (widget.getWidth() - 8) / 2, y + (widget.getHeight() - 8) / 2, shade);
    }

    /** A labelled button that is not a setting, such as opening the config file. */
    static ConfigRow action(Component label, Component description, Component buttonText, Runnable onPress) {
        AbstractWidget background = ClientVanilla.canvas(0, OPTION_HEIGHT, label, (graphics, widget, mouseX, mouseY) -> {
            int x = widget.getX();
            int y = widget.getY();
            graphics.fill(x, y, x + widget.getWidth(), y + widget.getHeight(),
                    widget.isHovered() ? ConfigTheme.ROW_HOVER : ConfigTheme.ROW);
            ClientVanilla.text(graphics, font(), label, x + PADDING, y + (widget.getHeight() - 8) / 2, ConfigTheme.TEXT);
        });
        background.setTooltip(Tooltip.create(description));
        Button button = Button.builder(buttonText, pressed -> onPress.run())
                .bounds(0, 0, 80, CONTROL_HEIGHT).build();
        return new ConfigRow(List.of(background, button)) {
            @Override
            int height(int width) {
                return OPTION_HEIGHT;
            }

            @Override
            void place(int x, int y, int width) {
                background.setPosition(x, y);
                background.setWidth(width);
                button.setWidth(Math.min(120, Math.max(80, font().width(buttonText) + 16)));
                button.setPosition(x + width - button.getWidth() - 1, y + 1);
            }
        };
    }

    /** A wrapped line of text in an amber box, for something the player should know before editing. */
    static ConfigRow note(Component text) {
        AbstractWidget canvas = ClientVanilla.canvas(0, 20, text, (graphics, widget, mouseX, mouseY) -> {
            int x = widget.getX();
            int y = widget.getY();
            graphics.fill(x, y, x + widget.getWidth(), y + widget.getHeight() - 4, ConfigTheme.NOTE);
            graphics.fill(x, y, x + 2, y + widget.getHeight() - 4, ConfigTheme.CHANGED);
            List<FormattedCharSequence> lines = font().split(text, widget.getWidth() - PADDING * 2);
            for (int i = 0; i < lines.size(); i++) {
                ClientVanilla.text(graphics, font(), lines.get(i), x + PADDING, y + 5 + i * 10, ConfigTheme.TEXT);
            }
        });
        return new ConfigRow(List.of(canvas)) {
            @Override
            int height(int width) {
                return font().split(text, width - PADDING * 2).size() * 10 + 10 + 4;
            }

            @Override
            void place(int x, int y, int width) {
                canvas.setPosition(x, y);
                canvas.setWidth(width);
                ClientVanilla.setHeight(canvas, height(width));
            }
        };
    }

    /** The live thirst bar, food bar and tooltip, as wide as the list. */
    static ConfigRow preview() {
        AbstractWidget canvas = ConfigPreview.widget();
        return new ConfigRow(List.of(canvas)) {
            @Override
            int height(int width) {
                return ConfigPreview.HEIGHT + 6;
            }

            @Override
            void place(int x, int y, int width) {
                canvas.setPosition(x, y);
                canvas.setWidth(width);
            }
        };
    }
}
