package com.thirstwastaken2.client.config;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.client.platform.ClientVanilla;
import com.thirstwastaken2.client.platform.ScrollingScreen;
import com.thirstwastaken2.config.ThirstConfig;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.util.Mth;

/**
 * The editor for {@code config/thirstwastaken2.json}, opened from Mod Menu on Fabric and the mods list
 * on NeoForge. A header with the mod's name and a search box, a sidebar with one tab per
 * {@link ConfigCategory}, the selected page's settings as a scrolling list of rows, and Reset, Cancel
 * and Done below. A page split into sections shows a strip of tabs under its heading, and the list
 * holds only the chosen tab's rows. Typing in the search box lists matching settings from every page
 * instead.
 *
 * <p>Everything is built from vanilla widgets and plain fills, so it looks at home next to the vanilla
 * options screens on every supported version. Controls write straight into the live config, so the
 * HUD, tooltips and the AppleSkin preview follow every change while the screen is open. Done (or
 * Escape) saves; Cancel puts back the copy taken when the screen opened.
 */
public final class ThirstConfigScreen extends ScrollingScreen {
    private static final int HEADER_HEIGHT = 32;
    private static final int FOOTER_HEIGHT = 32;
    private static final int TAB_HEIGHT = 24;
    /** Below this screen width the sidebar shows only the icons, and each tab names its page on hover. */
    private static final int COMPACT_BELOW = 380;
    private static final int COMPACT_SIDEBAR = 28;
    /** Wider lists put a setting's name too far from its control to read across comfortably. */
    private static final int MAX_LIST_WIDTH = 360;
    private static final int LIST_MARGIN = 10;
    private static final int ROW_GAP = 2;
    private static final int SCROLLBAR_WIDTH = 3;
    private static final int FOOTER_BUTTON_WIDTH = 90;
    private static final int RESET_BUTTON_WIDTH = 110;

    private static final Identifier THIRST_ICONS = ThirstWasTaken2.id("textures/gui/thirst_icons.png");

    private final Screen parent;
    private final ThirstConfig snapshot = ThirstConfig.snapshot();
    private final List<ConfigRow> rows = new ArrayList<>();
    private ConfigCategory selected = ConfigCategory.values()[0];
    /** The tab shown on each split page, kept while the screen is open so returning to a page finds it as left. */
    private final Map<ConfigCategory, Integer> sectionShown = new EnumMap<>(ConfigCategory.class);
    /** How many rows at the top, the heading and any tabs, stay in place while the rest scroll. */
    private int pinned;
    private String query = "";
    private int scroll;

    private EditBox search;
    private Button resetPage;
    private boolean compact;
    private int sidebarWidth;
    private int listX;
    private int listWidth;
    private int listTop;
    private int listBottom;

    public ThirstConfigScreen(Screen parent) {
        super(Component.translatable("thirstwastaken2.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        compact = width < COMPACT_BELOW;
        sidebarWidth = compact ? COMPACT_SIDEBAR : sidebarWidth();
        int available = width - sidebarWidth - LIST_MARGIN * 2 - SCROLLBAR_WIDTH - 4;
        listWidth = Math.min(MAX_LIST_WIDTH, available);
        listX = sidebarWidth + LIST_MARGIN + (available - listWidth) / 2;
        listTop = HEADER_HEIGHT + 8;
        listBottom = height - FOOTER_HEIGHT - 6;

        addRenderableOnly(ClientVanilla.canvas(width, height, title, (graphics, widget, mouseX, mouseY) -> paintFrame(graphics)));
        addSearch();
        addTabs();
        addFooter();
        rows.clear();
        refreshRows();
    }

    private int sidebarWidth() {
        int widest = 0;
        for (ConfigCategory category : ConfigCategory.values()) widest = Math.max(widest, font.width(category.title()));
        return Mth.clamp(widest + 40, 96, 140);
    }

    private void addSearch() {
        int searchWidth = Math.min(150, width / 3);
        if (search == null) {
            Component hint = Component.translatable("thirstwastaken2.config.search");
            search = new EditBox(font, 0, 0, searchWidth, 18, hint);
            search.setHint(hint);
            search.setMaxLength(64);
        }
        search.setWidth(searchWidth);
        search.setPosition(width - searchWidth - 8, (HEADER_HEIGHT - 18) / 2);
        search.setResponder(this::onSearch);
        addRenderableWidget(search);
    }

    private void addTabs() {
        ConfigCategory[] categories = ConfigCategory.values();
        for (int i = 0; i < categories.length; i++) {
            ConfigCategory category = categories[i];
            AbstractWidget tab = ClientVanilla.button(sidebarWidth - 8, TAB_HEIGHT - 2, category.title(),
                    () -> select(category), (graphics, widget, mouseX, mouseY) -> {
                        int x = widget.getX();
                        int y = widget.getY();
                        int right = x + widget.getWidth();
                        int bottom = y + widget.getHeight();
                        boolean current = query.isEmpty() && selected == category;
                        if (current) {
                            graphics.fill(x, y, right, bottom, ConfigTheme.SELECTED);
                            graphics.fill(x, y, x + 2, bottom, ConfigTheme.ACCENT);
                        } else if (widget.isHoveredOrFocused()) {
                            graphics.fill(x, y, right, bottom, ConfigTheme.ROW_HOVER);
                        }
                        int iconX = compact ? x + (widget.getWidth() - 16) / 2 : x + 7;
                        ConfigTheme.icon(graphics, category.icon(), iconX, y + (widget.getHeight() - 16) / 2);
                        if (compact) return;
                        ConfigTheme.clippedText(graphics, font, category.title(), x + 28, y + (widget.getHeight() - 8) / 2,
                                widget.getWidth() - 32, current || widget.isHoveredOrFocused() ? ConfigTheme.TEXT : ConfigTheme.MUTED);
                    });
            tab.setPosition(4, HEADER_HEIGHT + 6 + i * TAB_HEIGHT);
            // The tab shows its page's name, so only the icon-only tabs of a narrow screen name it on hover.
            if (compact) tab.setTooltip(Tooltip.create(category.title()));
            addRenderableWidget(tab);
        }
    }

    private void addFooter() {
        int y = height - FOOTER_HEIGHT + (FOOTER_HEIGHT - 20) / 2;
        resetPage = addRenderableWidget(Button.builder(Component.translatable("thirstwastaken2.config.reset"), button -> resetPage())
                .tooltip(Tooltip.create(Component.translatable("thirstwastaken2.config.reset.tooltip")))
                .bounds(8, y, RESET_BUTTON_WIDTH, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL, button -> cancel())
                .bounds(width - 8 - FOOTER_BUTTON_WIDTH * 2 - 4, y, FOOTER_BUTTON_WIDTH, 20).build());
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, button -> onClose())
                .bounds(width - 8 - FOOTER_BUTTON_WIDTH, y, FOOTER_BUTTON_WIDTH, 20).build());
    }

    /** The header, sidebar, list panel and footer behind every widget, and the list's scrollbar. */
    private void paintFrame(GuiGraphicsExtractor graphics) {
        int footerTop = height - FOOTER_HEIGHT;
        graphics.fill(0, 0, width, HEADER_HEIGHT, ConfigTheme.BAR);
        graphics.fill(0, HEADER_HEIGHT - 1, width, HEADER_HEIGHT, ConfigTheme.ACCENT);
        graphics.fill(0, HEADER_HEIGHT, sidebarWidth, footerTop, ConfigTheme.SIDEBAR);
        graphics.fill(sidebarWidth - 1, HEADER_HEIGHT, sidebarWidth, footerTop, ConfigTheme.LINE);
        graphics.fill(sidebarWidth, HEADER_HEIGHT, width, footerTop, ConfigTheme.PANEL);
        graphics.fill(0, footerTop, width, height, ConfigTheme.BAR);
        graphics.fill(0, footerTop, width, footerTop + 1, ConfigTheme.LINE);

        // The full droplet of the thirst bar, at twice its size: the 9px frame at u = 32 of the 41x9 sheet.
        ClientVanilla.blit(graphics, THIRST_ICONS, 9, (HEADER_HEIGHT - 18) / 2, 64, 0, 18, 18, 82, 18, 0xFFFFFFFF);
        ClientVanilla.text(graphics, font, title, 32, (HEADER_HEIGHT - 8) / 2, ConfigTheme.TEXT);
        int subtitleX = 32 + font.width(title) + 6;
        if (subtitleX + 60 < search.getX()) {
            ClientVanilla.text(graphics, font, Component.translatable("thirstwastaken2.config.subtitle"),
                    subtitleX, (HEADER_HEIGHT - 8) / 2, ConfigTheme.FAINT);
        }

        int maxScroll = maxScroll();
        if (maxScroll > 0) {
            int trackX = listX + listWidth + 4;
            int trackTop = scrollTop();
            int trackHeight = listBottom - trackTop;
            int scrolling = rows.size() - pinned;
            int thumbHeight = Math.max(16, trackHeight * (scrolling - maxScroll) / scrolling);
            int thumbY = trackTop + (trackHeight - thumbHeight) * scroll / maxScroll;
            graphics.fill(trackX, trackTop, trackX + SCROLLBAR_WIDTH, listBottom, ConfigTheme.ROW);
            graphics.fill(trackX, thumbY, trackX + SCROLLBAR_WIDTH, thumbY + thumbHeight, ConfigTheme.MUTED);
        }
    }

    /** Builds the rows for the selected page, or for the search, and lays them out from {@link #scroll}. */
    private void refreshRows() {
        List<AbstractWidget> removed = new ArrayList<>();
        for (ConfigRow row : rows) removed.addAll(row.widgets());
        for (AbstractWidget widget : removed) removeWidget(widget);
        GuiEventListener focused = getFocused();
        if (focused != null && removed.contains(focused)) setFocused(null);
        rows.clear();

        int controlWidth = Mth.clamp(listWidth * 2 / 5, 80, 140);
        if (query.isEmpty()) {
            rows.add(ConfigRow.heading(selected.title(), selected.description(), selected.icon()));
            List<ConfigSection> sections = selected.sections();
            ConfigSection section = sections.get(Math.min(sectionShown.getOrDefault(selected, 0), sections.size() - 1));
            if (sections.size() > 1) {
                List<Component> titles = new ArrayList<>();
                for (ConfigSection each : sections) titles.add(each.title());
                rows.add(ConfigRow.tabs(titles, sections.indexOf(section), this::selectSection));
            }
            pinned = rows.size();
            selected.addLeadingRows(rows);
            for (ConfigEntry<?> entry : section.entries()) rows.add(ConfigRow.option(entry, controlWidth, this::refreshRows));
            section.trailingRows().accept(rows, this::refreshRows);
        } else {
            List<ConfigRow> results = new ArrayList<>();
            int count = 0;
            for (ConfigCategory category : ConfigCategory.values()) {
                List<ConfigEntry<?>> matches = matches(category);
                if (matches.isEmpty()) continue;
                results.add(ConfigRow.subheading(category.title(), category.icon()));
                for (ConfigEntry<?> entry : matches) results.add(ConfigRow.option(entry, controlWidth, this::refreshRows));
                count += matches.size();
            }
            // Items the Item Values page lists are found by id, name or mod, under that page.
            List<ConfigRow> items = new ArrayList<>();
            int itemCount = ItemValueRows.addMatching(items, query, this::refreshRows);
            if (itemCount > 0) {
                results.add(ConfigRow.subheading(Component.translatable("thirstwastaken2.config.item_values"),
                        ConfigCategory.ITEMS.icon()));
                results.addAll(items);
                count += itemCount;
            }
            rows.add(ConfigRow.heading(Component.translatable("thirstwastaken2.config.search_results"),
                    Component.translatable("thirstwastaken2.config.search_results.count", count), null));
            pinned = 1;
            if (count == 0) rows.add(ConfigRow.note(Component.translatable("thirstwastaken2.config.no_results", search.getValue())));
            rows.addAll(results);
        }

        for (ConfigRow row : rows) {
            for (AbstractWidget widget : row.widgets()) addRenderableWidget(widget);
        }
        layoutRows();
    }

    private List<ConfigEntry<?>> matches(ConfigCategory category) {
        List<ConfigEntry<?>> matches = new ArrayList<>();
        for (ConfigEntry<?> entry : category.entries()) {
            if (entry.matches(query)) matches.add(entry);
        }
        return matches;
    }

    /** The settings Reset acts on: the selected page's, or those the search found. */
    private List<ConfigEntry<?>> resettable() {
        if (query.isEmpty()) return selected.entries();
        List<ConfigEntry<?>> all = new ArrayList<>();
        for (ConfigCategory category : ConfigCategory.values()) all.addAll(matches(category));
        return all;
    }

    /**
     * Shows the pinned rows, then the rows {@link #scroll} past them that fit between the header and
     * footer, and hides the rest. When the first of those sits under a heading that has scrolled off,
     * such as an item under its mod's name, that heading is shown above it, so it stays at the top
     * until the next heading takes its place.
     */
    private void layoutRows() {
        scroll = Mth.clamp(scroll, 0, maxScroll());
        ConfigRow sticky = stickyAt(pinned + scroll);
        int y = listTop;
        boolean full = false;
        for (int i = 0; i < rows.size(); i++) {
            ConfigRow row = rows.get(i);
            int rowHeight = row.height(listWidth);
            boolean inView = i < pinned || i - pinned >= scroll || row == sticky;
            boolean shown = inView && !full && y + rowHeight <= listBottom;
            if (inView && !shown) full = true;
            if (shown) {
                row.place(listX, y, listWidth);
                y += rowHeight + ROW_GAP;
            }
            row.setVisible(shown);
        }
    }

    /** The heading kept in view when the scrolling rows start at {@code first}, or null. */
    private ConfigRow stickyAt(int first) {
        return first < rows.size() ? rows.get(first).heading() : null;
    }

    /**
     * How many rows past the pinned ones to skip before every remaining row fits below them, with the
     * heading the first of them is kept under.
     */
    private int maxScroll() {
        int space = listBottom - scrollTop() + ROW_GAP;
        int used = 0;
        for (int i = rows.size() - 1; i >= pinned; i--) {
            used += rows.get(i).height(listWidth) + ROW_GAP;
            ConfigRow sticky = stickyAt(i);
            int needed = used + (sticky == null ? 0 : sticky.height(listWidth) + ROW_GAP);
            if (needed > space) return i + 1 - pinned;
        }
        return 0;
    }

    /** Where the scrolling rows start, below the pinned ones. */
    private int scrollTop() {
        int y = listTop;
        for (int i = 0; i < pinned && i < rows.size(); i++) y += rows.get(i).height(listWidth) + ROW_GAP;
        return y;
    }

    @Override
    protected boolean scrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY != 0 && mouseX >= sidebarWidth && mouseY >= HEADER_HEIGHT && mouseY < height - FOOTER_HEIGHT) {
            int before = scroll;
            scroll -= (int) Math.signum(scrollY);
            layoutRows();
            return scroll != before;
        }
        return false;
    }

    @Override
    public void tick() {
        super.tick();
        for (ConfigRow row : rows) row.tick();
        boolean anyChanged = false;
        for (ConfigEntry<?> entry : resettable()) anyChanged |= !entry.isDefault();
        resetPage.active = anyChanged;
    }

    private void select(ConfigCategory category) {
        selected = category;
        scroll = 0;
        // Clearing the search runs onSearch, which rebuilds the rows itself.
        if (!query.isEmpty()) search.setValue("");
        else refreshRows();
    }

    private void selectSection(int index) {
        sectionShown.put(selected, index);
        scroll = 0;
        refreshRows();
    }

    private void onSearch(String value) {
        String next = value.trim().toLowerCase(Locale.ROOT);
        if (next.equals(query)) return;
        query = next;
        scroll = 0;
        refreshRows();
    }

    private void resetPage() {
        for (ConfigEntry<?> entry : resettable()) entry.reset();
        // Controls hold the values they were built with, so rebuild them to show the defaults.
        refreshRows();
    }

    /** Done and Escape both keep the edits. */
    @Override
    public void onClose() {
        ThirstConfig.commit();
        ClientVanilla.setScreen(minecraft, parent);
    }

    private void cancel() {
        ThirstConfig.restore(snapshot);
        ClientVanilla.setScreen(minecraft, parent);
    }
}
