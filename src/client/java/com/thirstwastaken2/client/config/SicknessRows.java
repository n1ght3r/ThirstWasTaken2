package com.thirstwastaken2.client.config;

import com.thirstwastaken2.client.platform.ClientVanilla;
import com.thirstwastaken2.config.SicknessEffect;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.platform.Vanilla;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.effect.MobEffect;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * One difficulty's tab of the sickness tables, on the Sickness page: under each water grade, one
 * row per effect a drink may give (its chance, seconds and level, and a button that takes it off), then
 * a row that adds one. The grade's heading puts back the mod's lines for that grade.
 *
 * <p>Edits land in the live config like every other control, and in singleplayer the server reads the
 * same instance from its own thread. So every edit builds new lists and maps and swaps
 * {@code sicknessEffects} whole, never changing a list the server may be reading.
 */
final class SicknessRows {
    private static final String PREFIX = "thirstwastaken2.config.sickness.";
    private static final int CHANCE_WIDTH = 30;
    private static final int SECONDS_WIDTH = 30;
    private static final int LEVEL_WIDTH = 24;
    private static final int ADD_WIDTH = 60;
    private static final int EFFECT_ICON = 18;
    private static final int REMOVE = 0xFFFF5555;
    private static final String[] REMOVE_ICON = {
            "#......#",
            ".#....#.",
            "..#..#..",
            "...##...",
            "...##...",
            "..#..#..",
            ".#....#.",
            "#......#",
    };

    private SicknessRows() { }

    /** The trailing rows of the tab for {@code difficulty}, one of {@link SicknessEffect#DIFFICULTIES}. */
    static BiConsumer<List<ConfigRow>, Runnable> page(String difficulty) {
        return (rows, refresh) -> {
            for (int purity = 0; purity < SicknessEffect.GRADES.length; purity++) {
                String grade = SicknessEffect.GRADES[purity];
                ConfigRow heading = gradeRow(difficulty, grade, purity, refresh);
                rows.add(heading);
                List<SicknessEffect> lines = lines(difficulty, grade);
                for (int index = 0; index < lines.size(); index++) {
                    rows.add(effectRow(difficulty, grade, index, lines.get(index), refresh).under(heading));
                }
                rows.add(addRow(difficulty, grade, refresh).under(heading));
            }
        };
    }

    // ---- rows ---------------------------------------------------------------

    /**
     * A grade's heading: its name, the names of the three number columns, the amber bar while its lines
     * differ from the mod's, and reset.
     */
    private static ConfigRow gradeRow(String difficulty, String grade, int purity, Runnable refresh) {
        Component title = ConfigEntry.gradeName(purity);
        Component chance = Component.translatable(PREFIX + "chance");
        Component seconds = Component.translatable(PREFIX + "seconds");
        Component level = Component.translatable(PREFIX + "level");
        AbstractWidget background = ClientVanilla.canvas(0, ConfigRow.OPTION_HEIGHT, title, (graphics, widget, mouseX, mouseY) -> {
            int x = widget.getX();
            int y = widget.getY();
            int height = widget.getHeight();
            graphics.fill(x, y, x + widget.getWidth(), y + height, ConfigTheme.SELECTED);
            if (isChanged(difficulty, grade)) graphics.fill(x, y, x + 2, y + height, ConfigTheme.CHANGED);
            int[] columns = columnsFrom(x + widget.getWidth() + ConfigRow.GAP + ConfigRow.RESET_WIDTH);
            int textY = y + (height - 8) / 2;
            // The grade's name carries its tooltip colour, which wins over the colour passed here.
            ConfigTheme.clippedText(graphics, ConfigRow.font(), title, x + ConfigRow.PADDING, textY,
                    columns[0] - ConfigRow.GAP - x - ConfigRow.PADDING, ConfigTheme.TEXT);
            centred(graphics, chance, columns[0], CHANCE_WIDTH, textY);
            centred(graphics, seconds, columns[1], SECONDS_WIDTH, textY);
            centred(graphics, level, columns[2], LEVEL_WIDTH, textY);
        });
        background.setTooltip(Tooltip.create(Component.empty().append(title)
                .append("\n").append(Component.translatable(PREFIX + "chance.tooltip"))
                .append("\n").append(Component.translatable(PREFIX + "seconds.tooltip"))
                .append("\n").append(Component.translatable(PREFIX + "level.tooltip"))));

        Component resetLabel = Component.translatable(PREFIX + "reset_grade");
        AbstractWidget reset = ClientVanilla.button(ConfigRow.RESET_WIDTH, ConfigRow.CONTROL_HEIGHT, resetLabel, () -> {
            setLines(difficulty, grade, SicknessEffect.defaults(difficulty, grade));
            refresh.run();
        }, (graphics, widget, mouseX, mouseY) -> ConfigRow.paintIconButton(graphics, widget, ConfigRow.RESET_ICON, ConfigTheme.CHANGED));
        reset.setTooltip(Tooltip.create(resetLabel));
        reset.active = isChanged(difficulty, grade);

        return new ConfigRow(List.of(background, reset)) {
            @Override
            int height(int width) {
                return ConfigRow.OPTION_HEIGHT;
            }

            @Override
            void place(int x, int y, int width) {
                int resetX = x + width - ConfigRow.RESET_WIDTH;
                background.setPosition(x, y);
                background.setWidth(resetX - ConfigRow.GAP - x);
                reset.setPosition(resetX, y + 1);
            }

            @Override
            void tick() {
                reset.active = isChanged(difficulty, grade);
            }
        };
    }

    /** One effect: its icon and name, its chance, seconds and level, and the button that takes it off. */
    private static ConfigRow effectRow(String difficulty, String grade, int index, SicknessEffect line, Runnable refresh) {
        Identifier id = Identifier.tryParse(line.effect);
        Holder<MobEffect> effect = id == null ? null : Vanilla.mobEffect(id);
        Component name = effect == null ? Component.literal(line.effect)
                : Component.translatable(effect.value().getDescriptionId());
        Identifier icon = effect == null ? null
                : Identifier.fromNamespaceAndPath(id.getNamespace(), "textures/mob_effect/" + id.getPath() + ".png");
        AbstractWidget background = ClientVanilla.canvas(0, ConfigRow.OPTION_HEIGHT, name, (graphics, widget, mouseX, mouseY) -> {
            int x = widget.getX();
            int y = widget.getY();
            int right = x + widget.getWidth();
            graphics.fill(x, y, right, y + widget.getHeight(), widget.isHovered() ? ConfigTheme.ROW_HOVER : ConfigTheme.ROW);
            int textX = x + ConfigRow.PADDING;
            if (icon != null) {
                ClientVanilla.blit(graphics, icon, textX, y + (widget.getHeight() - EFFECT_ICON) / 2, 0, 0,
                        EFFECT_ICON, EFFECT_ICON, EFFECT_ICON, EFFECT_ICON, 0xFFFFFFFF);
                textX += EFFECT_ICON + 4;
            }
            ConfigTheme.clippedText(graphics, ConfigRow.font(), name, textX, y + (widget.getHeight() - 8) / 2,
                    right - textX - ConfigRow.PADDING, effect == null ? ConfigTheme.FAINT : ConfigTheme.TEXT);
        });
        // A line that shares its roll with others says so, since that is what keeps its chance inside theirs.
        background.setTooltip(Tooltip.create(effect == null
                ? Component.translatable(PREFIX + "unknown", line.effect)
                : line.group == null ? Component.literal(line.effect)
                : Component.translatable(PREFIX + "grouped", line.effect, line.group)));

        EditBox chance = numberBox(CHANCE_WIDTH, line.chance, 0, 100, "chance",
                value -> update(difficulty, grade, index, edited -> edited.chance = value));
        EditBox seconds = numberBox(SECONDS_WIDTH, line.seconds, 1, SicknessEffect.MAX_SECONDS, "seconds",
                value -> update(difficulty, grade, index, edited -> edited.seconds = value));
        EditBox level = numberBox(LEVEL_WIDTH, line.level, 1, SicknessEffect.MAX_LEVEL, "level",
                value -> update(difficulty, grade, index, edited -> edited.level = value));

        Component removeLabel = Component.translatable(PREFIX + "remove");
        AbstractWidget remove = ClientVanilla.button(ConfigRow.RESET_WIDTH, ConfigRow.CONTROL_HEIGHT, removeLabel, () -> {
            List<SicknessEffect> next = SicknessEffect.copyOf(lines(difficulty, grade));
            if (index < next.size()) next.remove(index);
            setLines(difficulty, grade, next);
            refresh.run();
        }, (graphics, widget, mouseX, mouseY) -> ConfigRow.paintIconButton(graphics, widget, REMOVE_ICON, REMOVE));
        remove.setTooltip(Tooltip.create(removeLabel));

        return new ConfigRow(List.of(background, chance, seconds, level, remove)) {
            @Override
            int height(int width) {
                return ConfigRow.OPTION_HEIGHT;
            }

            @Override
            void place(int x, int y, int width) {
                int[] columns = columnsFrom(x + width);
                background.setPosition(x, y);
                background.setWidth(columns[0] - ConfigRow.GAP - x);
                chance.setPosition(columns[0] + 1, y + 2);
                seconds.setPosition(columns[1] + 1, y + 2);
                level.setPosition(columns[2] + 1, y + 2);
                remove.setPosition(columns[3], y + 1);
            }
        };
    }

    /**
     * A box for an effect id and the button that adds it. The box completes the id in grey as it is
     * typed, and Add takes the completion, so {@code poi} is enough for {@code minecraft:poison}. A new
     * line starts at a certain chance, 10 seconds, level I.
     */
    private static ConfigRow addRow(String difficulty, String grade, Runnable refresh) {
        Component hint = Component.translatable(PREFIX + "add.hint");
        EditBox box = new EditBox(ConfigRow.font(), 0, 0, 100, ConfigRow.CONTROL_HEIGHT - 2, hint);
        box.setHint(hint);
        box.setMaxLength(128);
        Button add = Button.builder(Component.translatable(PREFIX + "add"), button -> {
            Identifier id = complete(box.getValue());
            if (id == null) return;
            List<SicknessEffect> next = SicknessEffect.copyOf(lines(difficulty, grade));
            next.add(new SicknessEffect(id.toString(), 100, 10, 1));
            setLines(difficulty, grade, next);
            refresh.run();
        }).bounds(0, 0, ADD_WIDTH, ConfigRow.CONTROL_HEIGHT).build();
        add.setTooltip(Tooltip.create(Component.translatable(PREFIX + "add.tooltip")));
        add.active = false;
        box.setResponder(text -> {
            Identifier id = complete(text);
            add.active = id != null;
            String full = id == null ? "" : id.toString();
            String typed = text.toLowerCase(Locale.ROOT);
            box.setSuggestion(!typed.isEmpty() && full.startsWith(typed) ? full.substring(typed.length()) : null);
        });
        return new ConfigRow(List.of(box, add)) {
            @Override
            int height(int width) {
                return ConfigRow.OPTION_HEIGHT;
            }

            @Override
            void place(int x, int y, int width) {
                add.setPosition(x + width - ADD_WIDTH, y + 1);
                box.setPosition(x + 1, y + 2);
                box.setWidth(width - ADD_WIDTH - ConfigRow.GAP - 2);
            }
        };
    }

    /**
     * A whole number box from {@code min} to {@code max}. A keystroke that makes it more than {@code max}
     * or not a number is undone; a value under {@code min}, such as the 0 on the way to 5, is not written.
     */
    private static EditBox numberBox(int width, int value, int min, int max, String key, IntConsumer setter) {
        Component label = Component.translatable(PREFIX + key);
        EditBox box = new EditBox(ConfigRow.font(), 0, 0, width - 2, ConfigRow.CONTROL_HEIGHT - 2, label);
        box.setMaxLength(Integer.toString(max).length());
        box.setValue(Integer.toString(value));
        // 26.1 took the input filter off EditBox, so a keystroke that makes the text invalid is undone.
        String[] accepted = {box.getValue()};
        box.setResponder(text -> {
            if (!text.isEmpty() && !isNumberUpTo(text, max)) {
                box.setValue(accepted[0]);
                return;
            }
            accepted[0] = text;
            if (!text.isEmpty() && Integer.parseInt(text) >= min) setter.accept(Integer.parseInt(text));
        });
        box.setTooltip(Tooltip.create(Component.translatable(PREFIX + key + ".tooltip")));
        return box;
    }

    private static boolean isNumberUpTo(String text, int max) {
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) return false;
        }
        return Integer.parseInt(text) <= max;
    }

    private static void centred(GuiGraphicsExtractor graphics, Component text, int x, int width, int y) {
        ClientVanilla.text(graphics, ConfigRow.font(), text, x + (width - ConfigRow.font().width(text)) / 2, y, ConfigTheme.MUTED);
    }

    /** The left edge of each column, right to left from {@code right}: chance, seconds, level, remove. */
    private static int[] columnsFrom(int right) {
        int remove = right - ConfigRow.RESET_WIDTH;
        int level = remove - ConfigRow.GAP - LEVEL_WIDTH;
        int seconds = level - ConfigRow.GAP - SECONDS_WIDTH;
        int chance = seconds - ConfigRow.GAP - CHANCE_WIDTH;
        return new int[]{chance, seconds, level, remove};
    }

    /**
     * The registered effect {@code text} names: its exact id, a bare path in the {@code minecraft}
     * namespace, or else the shortest id that starts with it, or whose path does.
     */
    private static Identifier complete(String text) {
        String typed = text.trim().toLowerCase(Locale.ROOT);
        if (typed.isEmpty()) return null;
        Identifier exact = Identifier.tryParse(typed);
        if (exact != null && Vanilla.mobEffect(exact) != null) return exact;
        Identifier best = null;
        for (Identifier id : BuiltInRegistries.MOB_EFFECT.keySet()) {
            if (!id.toString().startsWith(typed) && !id.getPath().startsWith(typed)) continue;
            if (best == null || id.toString().length() < best.toString().length()) best = id;
        }
        return best;
    }

    // ---- reading and writing the config -----------------------------------------

    /** The tables a fresh config holds, built once: the screen asks every tick whether any differs. */
    private static final Map<String, Map<String, List<SicknessEffect>>> DEFAULTS = SicknessEffect.defaults();

    /** Whether any difficulty's table differs from the mod's, for the footer's Reset on this page. */
    static boolean anyChanged() {
        return !ThirstConfig.get().sicknessEffects.equals(DEFAULTS);
    }

    /** Puts every difficulty's table back to the mod's, as a fresh copy the config owns. */
    static void resetAll() {
        ThirstConfig.get().sicknessEffects = SicknessEffect.defaults();
    }

    private static List<SicknessEffect> lines(String difficulty, String grade) {
        Map<String, List<SicknessEffect>> grades = ThirstConfig.get().sicknessEffects.get(difficulty);
        List<SicknessEffect> lines = grades == null ? null : grades.get(grade);
        return lines == null ? List.of() : lines;
    }

    private static boolean isChanged(String difficulty, String grade) {
        return !lines(difficulty, grade).equals(SicknessEffect.defaults(difficulty, grade));
    }

    /**
     * Replaces line {@code index} with a copy {@code edit} has changed one number of. The other numbers
     * come from the config as it is now, not from the line the row was built with, so editing one box
     * never puts back an older value of another.
     */
    private static void update(String difficulty, String grade, int index, Consumer<SicknessEffect> edit) {
        List<SicknessEffect> next = SicknessEffect.copyOf(lines(difficulty, grade));
        if (index >= next.size()) return;
        edit.accept(next.get(index));
        setLines(difficulty, grade, next);
    }

    /** Swaps in new tables with {@code lines} under {@code difficulty} and {@code grade}. */
    private static void setLines(String difficulty, String grade, List<SicknessEffect> lines) {
        ThirstConfig config = ThirstConfig.get();
        Map<String, Map<String, List<SicknessEffect>>> tables = new LinkedHashMap<>(config.sicknessEffects);
        Map<String, List<SicknessEffect>> grades = new LinkedHashMap<>(tables.getOrDefault(difficulty, Map.of()));
        grades.put(grade, new ArrayList<>(lines));
        tables.put(difficulty, grades);
        config.sicknessEffects = tables;
    }
}
