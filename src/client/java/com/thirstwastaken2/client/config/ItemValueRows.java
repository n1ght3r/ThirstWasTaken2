package com.thirstwastaken2.client.config;

import com.thirstwastaken2.client.platform.ClientVanilla;
import com.thirstwastaken2.compat.AppleSkin;
import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.data.ThirstData;
import com.thirstwastaken2.platform.Loader;
import com.thirstwastaken2.tooltip.ThirstTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The per-item thirst values on the Item Values page, so a pack can be tuned without opening the file.
 * One row per item the config names in {@code drinks}, {@code foods} or {@code itemBlacklist}: its
 * thirst and quenched, whether it restores anything at all, and a button that puts back the mod's
 * value or, for an item the mod does not list, takes the line out. A row above them adds an item.
 * The rows are grouped by the mod that adds each item, under a heading that opens and closes the
 * group; vanilla's starts open and the rest closed.
 *
 * <p>Items of a mod that is not installed are left off the page, since nothing can be checked for
 * them, and stay in the file untouched. Which of the two maps holds an item makes no difference to
 * what it restores ({@code ThirstApi} reads both), so a new item goes into {@code drinks}.
 *
 * <p>Edits land in the live config like every other control, and in singleplayer the server reads
 * the same instance from its own thread. So a change that adds or removes a key replaces the map or
 * set with an edited copy rather than restructuring the one the server may be reading, and a value is
 * replaced as a new array under an existing key.
 */
final class ItemValueRows {
    private static final int VALUE_WIDTH = 26;
    private static final int ADD_WIDTH = 60;
    private static final String PREFIX = "thirstwastaken2.config.";
    /** Units that fill one droplet, the size of the droplets over the value columns. */
    private static final int ONE_DROPLET = 2;

    private static final String[] ON_ICON = {
            "........",
            ".......#",
            "......##",
            "#....##.",
            "##..##..",
            ".####...",
            "..##....",
            "........",
    };
    private static final String[] OFF_ICON = {
            "#......#",
            ".#....#.",
            "..#..#..",
            "...##...",
            "...##...",
            "..#..#..",
            ".#....#.",
            "#......#",
    };
    private static final int ON = 0xFF55DD55;
    private static final int OFF = 0xFFFF5555;

    private static final String VANILLA = "minecraft";
    private static final String[] CLOSED_ICON = {
            ".#......",
            ".##.....",
            ".###....",
            ".####...",
            ".###....",
            ".##.....",
            ".#......",
            "........",
    };
    private static final String[] OPEN_ICON = {
            "........",
            "#######.",
            ".#####..",
            "..###...",
            "...#....",
            "........",
            "........",
            "........",
    };

    /**
     * The mods whose items are shown. Only vanilla's start open, since a pack mostly tunes vanilla
     * first and a long modded list would bury it. Kept for the session, so reopening the screen or
     * rebuilding the page after an edit leaves the groups as they were.
     */
    private static final Set<String> EXPANDED = new HashSet<>(Set.of(VANILLA));

    private static ThirstConfig defaults;

    private ItemValueRows() { }

    /**
     * The Item List tab: the row that adds an item, then the installed items grouped by
     * the mod that adds them, each group under a heading that opens and closes it.
     */
    static void addPage(List<ConfigRow> rows, Runnable refresh) {
        rows.add(addRow(refresh));
        Map<String, List<String>> groups = new LinkedHashMap<>();
        int hidden = 0;
        for (String id : listedIds()) {
            if (installed(id) == null) hidden++;
            else groups.computeIfAbsent(namespace(id), key -> new ArrayList<>()).add(id);
        }
        for (String namespace : sortedGroups(groups)) {
            List<String> ids = groups.get(namespace);
            boolean open = EXPANDED.contains(namespace);
            ConfigRow heading = groupRow(namespace, ids, open, () -> {
                if (!EXPANDED.remove(namespace)) EXPANDED.add(namespace);
                refresh.run();
            });
            rows.add(heading);
            if (open) {
                for (String id : ids) rows.add(itemRow(id, installed(id), refresh).under(heading));
            }
        }
        if (hidden > 0) rows.add(ConfigRow.note(Component.translatable(PREFIX + "item_values.hidden", hidden)));
    }

    /**
     * Adds to {@code rows} the installed items whose id or name contains {@code query}, already lower
     * case, under their mod's heading, and every item of a mod whose name or id contains it. Groups
     * are always open here. Returns how many items it added.
     */
    static int addMatching(List<ConfigRow> rows, String query, Runnable refresh) {
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (String id : listedIds()) {
            if (installed(id) != null) groups.computeIfAbsent(namespace(id), key -> new ArrayList<>()).add(id);
        }
        int count = 0;
        for (String namespace : sortedGroups(groups)) {
            boolean wholeMod = namespace.contains(query) || modName(namespace).toLowerCase(Locale.ROOT).contains(query);
            List<String> hits = new ArrayList<>();
            for (String id : groups.get(namespace)) {
                if (wholeMod || id.contains(query) || name(installed(id)).getString().toLowerCase(Locale.ROOT).contains(query)) {
                    hits.add(id);
                }
            }
            if (hits.isEmpty()) continue;
            ConfigRow heading = groupRow(namespace, hits, true, null);
            rows.add(heading);
            for (String id : hits) rows.add(itemRow(id, installed(id), refresh).under(heading));
            count += hits.size();
        }
        return count;
    }

    /** Vanilla first, then the rest by mod name, so a mod is where its name says it is. */
    private static List<String> sortedGroups(Map<String, List<String>> groups) {
        List<String> namespaces = new ArrayList<>(groups.keySet());
        namespaces.sort(Comparator.comparing((String namespace) -> !namespace.equals(VANILLA))
                .thenComparing(namespace -> modName(namespace).toLowerCase(Locale.ROOT)));
        return namespaces;
    }

    private static String namespace(String id) {
        int colon = id.indexOf(':');
        return colon < 0 ? VANILLA : id.substring(0, colon);
    }

    /** The name the mod gives itself; an item's namespace is its mod's id in almost every mod. */
    private static String modName(String namespace) {
        return Loader.modName(namespace);
    }

    // ---- rows ---------------------------------------------------------------

    /**
     * One item: its icon and name, its thirst and quenched, whether it restores thirst, and reset.
     * The name turns amber, with the amber bar, when the line differs from the mod's own.
     */
    private static ConfigRow itemRow(String id, Item item, Runnable refresh) {
        Component name = name(item);
        ItemStack[] icon = new ItemStack[1];
        AbstractWidget background = ClientVanilla.canvas(0, ConfigRow.OPTION_HEIGHT, name, (graphics, widget, mouseX, mouseY) -> {
            int x = widget.getX();
            int y = widget.getY();
            int right = x + widget.getWidth();
            graphics.fill(x, y, right, y + widget.getHeight(), widget.isHovered() ? ConfigTheme.ROW_HOVER : ConfigTheme.ROW);
            boolean changed = isChanged(id);
            if (changed) graphics.fill(x, y, x + 2, y + widget.getHeight(), ConfigTheme.CHANGED);
            int textX = x + ConfigRow.PADDING;
            // Mod Menu can open the screen from the title screen, where a stack cannot be built yet.
            if (Minecraft.getInstance().level != null) {
                if (icon[0] == null) icon[0] = new ItemStack(item);
                ConfigTheme.item(graphics, icon[0], textX, y + (widget.getHeight() - 16) / 2);
                textX += 20;
            }
            int color = isBlacklisted(id) ? ConfigTheme.FAINT : changed ? ConfigTheme.CHANGED : ConfigTheme.TEXT;
            ConfigTheme.clippedText(graphics, ConfigRow.font(), name, textX, y + (widget.getHeight() - 8) / 2,
                    right - textX - ConfigRow.PADDING, color);
        });
        background.setTooltip(Tooltip.create(Component.literal(id)));

        EditBox thirst = valueBox(id, 0, PREFIX + "item_values.thirst");
        EditBox quenched = valueBox(id, 1, PREFIX + "item_values.quenched");

        boolean enabled = !isBlacklisted(id);
        AbstractWidget toggle = ClientVanilla.button(ConfigRow.RESET_WIDTH, ConfigRow.CONTROL_HEIGHT,
                Component.translatable(PREFIX + (enabled ? "item_values.enabled" : "item_values.disabled")), () -> {
                    setBlacklisted(id, enabled);
                    refresh.run();
                }, (graphics, widget, mouseX, mouseY) -> ConfigRow.paintIconButton(graphics, widget,
                        enabled ? ON_ICON : OFF_ICON, enabled ? ON : OFF));
        toggle.setTooltip(Tooltip.create(Component.translatable(PREFIX + (enabled ? "item_values.enabled" : "item_values.disabled"))));

        boolean builtIn = defaultValue(id) != null;
        Component resetLabel = Component.translatable(builtIn ? PREFIX + "reset_option" : PREFIX + "item_values.remove");
        AbstractWidget reset = ClientVanilla.button(ConfigRow.RESET_WIDTH, ConfigRow.CONTROL_HEIGHT, resetLabel, () -> {
            if (builtIn) resetToDefault(id);
            else remove(id);
            refresh.run();
        }, (graphics, widget, mouseX, mouseY) -> ConfigRow.paintIconButton(graphics, widget,
                builtIn ? ConfigRow.RESET_ICON : OFF_ICON, builtIn ? ConfigTheme.CHANGED : OFF));
        reset.setTooltip(Tooltip.create(resetLabel));
        reset.active = !builtIn || isChanged(id);

        return new ConfigRow(List.of(background, thirst, quenched, toggle, reset)) {
            @Override
            int height(int width) {
                return ConfigRow.OPTION_HEIGHT;
            }

            @Override
            void place(int x, int y, int width) {
                int[] columns = columnsFrom(x + width);
                background.setPosition(x, y);
                background.setWidth(columns[0] - ConfigRow.GAP - x);
                thirst.setPosition(columns[0] + 1, y + 2);
                quenched.setPosition(columns[1] + 1, y + 2);
                toggle.setPosition(columns[2], y + 1);
                reset.setPosition(columns[3], y + 1);
            }

            @Override
            void tick() {
                reset.active = !builtIn || isChanged(id);
            }
        };
    }

    /**
     * The heading of one mod's items: an arrow, the mod's name and how many of its items are listed,
     * with the amber bar while any of them differs from the mod's own value, and, while the group is
     * open, the thirst and quenched droplets above those two columns, drawn as the item tooltip draws
     * them. It stays at the top while its items scroll under it.
     * Clicking it opens or closes the group; in search results, where {@code onToggle} is {@code null},
     * it is only a label.
     */
    private static ConfigRow groupRow(String namespace, List<String> ids, boolean open, Runnable onToggle) {
        Component title = Component.literal(modName(namespace));
        Component count = Component.literal(" (" + ids.size() + ")");
        Item first = installed(ids.get(0));
        ItemStack[] icon = new ItemStack[1];
        ClientVanilla.Painter painter = (graphics, widget, mouseX, mouseY) -> {
            int x = widget.getX();
            int y = widget.getY();
            int height = widget.getHeight();
            boolean lit = onToggle != null && widget.isHoveredOrFocused();
            graphics.fill(x, y, x + widget.getWidth(), y + height, lit ? ConfigTheme.SELECTED : ConfigTheme.ROW_HOVER);
            boolean changed = false;
            for (String id : ids) changed |= isChanged(id);
            if (changed) graphics.fill(x, y, x + 2, y + height, ConfigTheme.CHANGED);
            int textX = x + ConfigRow.PADDING;
            if (onToggle != null) {
                ConfigTheme.glyph(graphics, open ? OPEN_ICON : CLOSED_ICON, textX, y + (height - 8) / 2 + 1, ConfigTheme.ACCENT);
                textX += 12;
            }
            if (Minecraft.getInstance().level != null) {
                if (icon[0] == null) icon[0] = new ItemStack(first);
                ConfigTheme.item(graphics, icon[0], textX, y + (height - 16) / 2);
                textX += 20;
            }
            int[] columns = columnsFrom(x + widget.getWidth());
            int countWidth = ConfigRow.font().width(count);
            int titleWidth = Math.min(ConfigRow.font().width(title), columns[0] - ConfigRow.GAP - countWidth - textX);
            ConfigTheme.clippedText(graphics, ConfigRow.font(), title, textX, y + (height - 8) / 2, titleWidth, ConfigTheme.TEXT);
            ClientVanilla.text(graphics, ConfigRow.font(), count, textX + titleWidth, y + (height - 8) / 2, ConfigTheme.MUTED);
            if (open) {
                // Read each frame, so the outline follows the quenched overlay setting as it changes.
                Component thirst = ThirstTooltip.thirst(ONE_DROPLET);
                Component quenched = ThirstTooltip.quenched(ONE_DROPLET, AppleSkin.quenchedOverlay());
                int textY = y + (height - 8) / 2;
                ClientVanilla.text(graphics, ConfigRow.font(), thirst,
                        columns[0] + (VALUE_WIDTH - ConfigRow.font().width(thirst)) / 2, textY, ConfigTheme.TEXT);
                ClientVanilla.text(graphics, ConfigRow.font(), quenched,
                        columns[1] + (VALUE_WIDTH - ConfigRow.font().width(quenched)) / 2, textY, ConfigTheme.TEXT);
            }
        };
        AbstractWidget widget = onToggle != null
                ? ClientVanilla.button(0, ConfigRow.OPTION_HEIGHT, title, onToggle, painter)
                : ClientVanilla.canvas(0, ConfigRow.OPTION_HEIGHT, title, painter);
        // An open group names its two columns by their droplets; a closed one shows no droplets to name.
        widget.setTooltip(Tooltip.create(open ? Component.literal(namespace)
                .append("\n").append(legend(ThirstTooltip.thirst(ONE_DROPLET), "item_values.thirst"))
                .append("\n").append(legend(ThirstTooltip.quenched(ONE_DROPLET, AppleSkin.quenchedOverlay()), "item_values.quenched"))
                : Component.literal(namespace)));
        return new ConfigRow(List.of(widget)) {
            @Override
            int height(int width) {
                return ConfigRow.OPTION_HEIGHT;
            }

            @Override
            void place(int x, int y, int width) {
                widget.setPosition(x, y);
                widget.setWidth(width);
            }
        };
    }

    /**
     * A number box for one of the item's two values, empty while no map holds the item (a line that
     * only switches it off). Anything above the bar, or not a number, is refused as it is typed.
     */
    private static EditBox valueBox(String id, int index, String labelKey) {
        Component label = Component.translatable(labelKey);
        EditBox box = new EditBox(ConfigRow.font(), 0, 0, VALUE_WIDTH - 2, ConfigRow.CONTROL_HEIGHT - 2, label);
        box.setMaxLength(2);
        int[] value = value(id);
        box.setValue(value == null ? "" : Integer.toString(value[index]));
        // 26.1 took the input filter off EditBox, so a keystroke that makes the text invalid is undone
        // by putting back the last text that was valid.
        String[] accepted = {box.getValue()};
        box.setResponder(text -> {
            if (!text.isEmpty() && !isValue(text)) {
                box.setValue(accepted[0]);
                return;
            }
            accepted[0] = text;
            if (!text.isEmpty()) setValue(id, index, Integer.parseInt(text));
        });
        box.setTooltip(Tooltip.create(Component.translatable(labelKey + ".tooltip")));
        return box;
    }

    private static boolean isValue(String text) {
        for (int i = 0; i < text.length(); i++) {
            if (!Character.isDigit(text.charAt(i))) return false;
        }
        return Integer.parseInt(text) <= ThirstData.MAX;
    }

    /** One line of a heading's tooltip: a droplet as its column shows it, then what the column holds. */
    private static Component legend(Component droplet, String key) {
        return Component.empty().append(droplet).append(" ").append(Component.translatable(PREFIX + key));
    }

    /**
     * The left edge of each control column, right to left from {@code right}: thirst, quenched, the
     * switch and reset. Shared by the item rows and the group headings so they line up.
     */
    private static int[] columnsFrom(int right) {
        int reset = right - ConfigRow.RESET_WIDTH;
        int toggle = reset - ConfigRow.GAP - ConfigRow.RESET_WIDTH;
        int quenched = toggle - ConfigRow.GAP - VALUE_WIDTH;
        int thirst = quenched - ConfigRow.GAP - VALUE_WIDTH;
        return new int[]{thirst, quenched, toggle, reset};
    }

    /**
     * A box for an item id and the button that adds it. The box completes the id in grey as it is
     * typed, and Add takes the completion, so {@code melon} is enough for {@code minecraft:melon_slice}.
     */
    private static ConfigRow addRow(Runnable refresh) {
        Component hint = Component.translatable(PREFIX + "item_values.add.hint");
        EditBox box = new EditBox(ConfigRow.font(), 0, 0, 100, ConfigRow.CONTROL_HEIGHT - 2, hint);
        box.setHint(hint);
        box.setMaxLength(128);
        Button add = Button.builder(Component.translatable(PREFIX + "item_values.add"), button -> {
            Identifier id = complete(box.getValue());
            if (id == null || isListed(id.toString())) return;
            add(id.toString());
            // Open the item's group, or the new row would land in a closed one out of sight.
            EXPANDED.add(id.getNamespace());
            refresh.run();
        }).bounds(0, 0, ADD_WIDTH, ConfigRow.CONTROL_HEIGHT).build();
        add.setTooltip(Tooltip.create(Component.translatable(PREFIX + "item_values.add.tooltip")));
        add.active = false;
        box.setResponder(text -> {
            Identifier id = complete(text);
            add.active = id != null && !isListed(id.toString());
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
     * The installed item {@code text} names: its exact id, a bare path in the {@code minecraft}
     * namespace, or else the shortest id that starts with it, or whose path does.
     */
    private static Identifier complete(String text) {
        String typed = text.trim().toLowerCase(Locale.ROOT);
        if (typed.isEmpty()) return null;
        Identifier exact = Identifier.tryParse(typed);
        if (exact != null && installed(exact.toString()) != null) return exact;
        Identifier best = null;
        for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
            if (!id.toString().startsWith(typed) && !id.getPath().startsWith(typed)) continue;
            if (id.getPath().equals("air")) continue;
            if (best == null || id.toString().length() < best.toString().length()) best = id;
        }
        return best;
    }

    // ---- reading and writing the config -----------------------------------------

    /** Every id the config names, sorted, so items of one mod sit together. */
    private static Set<String> listedIds() {
        ThirstConfig config = ThirstConfig.get();
        Set<String> ids = new TreeSet<>(config.drinks.keySet());
        ids.addAll(config.foods.keySet());
        ids.addAll(config.itemBlacklist);
        return ids;
    }

    private static boolean isListed(String id) {
        ThirstConfig config = ThirstConfig.get();
        return config.drinks.containsKey(id) || config.foods.containsKey(id) || config.itemBlacklist.contains(id);
    }

    /** The registered item under {@code id}, or {@code null} when its mod is not installed. */
    private static Item installed(String id) {
        Identifier identifier = Identifier.tryParse(id);
        if (identifier == null) return null;
        Item item = BuiltInRegistries.ITEM.getOptional(identifier).orElse(Items.AIR);
        return item == Items.AIR ? null : item;
    }

    private static Component name(Item item) {
        return Component.translatable(item.getDescriptionId());
    }

    private static int[] value(String id) {
        ThirstConfig config = ThirstConfig.get();
        int[] value = config.drinks.get(id);
        return value != null ? value : config.foods.get(id);
    }

    /** A fresh config, whose maps are the mod's own values. */
    private static ThirstConfig defaults() {
        if (defaults == null) defaults = new ThirstConfig();
        return defaults;
    }

    private static int[] defaultValue(String id) {
        int[] value = defaults().drinks.get(id);
        return value != null ? value : defaults().foods.get(id);
    }

    private static boolean isBlacklisted(String id) {
        return ThirstConfig.get().itemBlacklist.contains(id);
    }

    private static boolean isChanged(String id) {
        return isBlacklisted(id) || !Arrays.equals(value(id), defaultValue(id));
    }

    private static void setValue(String id, int index, int amount) {
        ThirstConfig config = ThirstConfig.get();
        int[] current = value(id);
        int[] next = current == null ? new int[2] : current.clone();
        next[index] = amount;
        if (config.foods.containsKey(id)) config.foods.put(id, next);
        else if (config.drinks.containsKey(id)) config.drinks.put(id, next);
        else config.drinks = with(config.drinks, id, next);
    }

    /** Lists the item with the value the {@code c:drinks} tag gives, the mod's word for "a drink". */
    private static void add(String id) {
        ThirstConfig config = ThirstConfig.get();
        config.drinks = with(config.drinks, id, config.drinkTagValue.clone());
    }

    /** Puts the mod's own value back in the map the mod lists it in, and switches the item on. */
    private static void resetToDefault(String id) {
        ThirstConfig config = ThirstConfig.get();
        ThirstConfig defaults = defaults();
        if (defaults.drinks.containsKey(id)) {
            config.foods = without(config.foods, id);
            config.drinks = with(config.drinks, id, defaults.drinks.get(id).clone());
        } else {
            config.drinks = without(config.drinks, id);
            config.foods = with(config.foods, id, defaults.foods.get(id).clone());
        }
        setBlacklisted(id, false);
    }

    private static void remove(String id) {
        ThirstConfig config = ThirstConfig.get();
        config.drinks = without(config.drinks, id);
        config.foods = without(config.foods, id);
        setBlacklisted(id, false);
    }

    private static void setBlacklisted(String id, boolean blacklisted) {
        ThirstConfig config = ThirstConfig.get();
        if (config.itemBlacklist.contains(id) == blacklisted) return;
        Set<String> next = new LinkedHashSet<>(config.itemBlacklist);
        if (blacklisted) next.add(id);
        else next.remove(id);
        config.itemBlacklist = next;
    }

    private static Map<String, int[]> with(Map<String, int[]> values, String id, int[] value) {
        Map<String, int[]> next = new LinkedHashMap<>(values);
        next.put(id, value);
        return next;
    }

    private static Map<String, int[]> without(Map<String, int[]> values, String id) {
        if (!values.containsKey(id)) return values;
        Map<String, int[]> next = new LinkedHashMap<>(values);
        next.remove(id);
        return next;
    }
}
