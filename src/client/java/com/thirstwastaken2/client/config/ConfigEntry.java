package com.thirstwastaken2.client.config;

import com.thirstwastaken2.config.ThirstConfig;
import com.thirstwastaken2.platform.Loader;
import com.thirstwastaken2.purity.WaterPurity;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntFunction;

/**
 * One setting of the config screen: its label and description, how it reads and writes the live
 * {@link ThirstConfig}, and the control that edits it. The label is {@code thirstwastaken2.config.<key>}
 * and the description {@code thirstwastaken2.config.<key>.tooltip}, so the key matches the field name
 * in snake_case.
 *
 * <p>An entry reads {@link ThirstConfig#get()} on every call rather than holding an instance, so Cancel
 * swapping the snapshot back in needs nothing from it. Its default comes from a freshly constructed
 * config, which is also what Reset puts back.
 */
abstract class ConfigEntry<T> {
    private static final String PREFIX = "thirstwastaken2.config.";
    /** Doubles are edited as integer percentages, since the slider steps in whole numbers. */
    private static final int PERCENT = 100;
    private static ThirstConfig defaults;

    private final String key;
    private final Function<ThirstConfig, T> getter;
    private final BiConsumer<ThirstConfig, T> setter;
    /** The mod this setting does nothing without, or {@code null} when it always applies. */
    private String requiredMod;
    /** What fills the label's {@code %s}, such as a coloured grade name; none for most settings. */
    private Object[] labelArgs = new Object[0];

    private ConfigEntry(String key, Function<ThirstConfig, T> getter, BiConsumer<ThirstConfig, T> setter) {
        this.key = key;
        this.getter = getter;
        this.setter = setter;
    }

    static ConfigEntry<Boolean> toggle(String key, Function<ThirstConfig, Boolean> getter,
                                       BiConsumer<ThirstConfig, Boolean> setter) {
        return new ConfigEntry<>(key, getter, setter) {
            @Override
            AbstractWidget control(int width) {
                return Button.builder(onOff(value()), button -> {
                    set(!value());
                    button.setMessage(onOff(value()));
                }).bounds(0, 0, width, 20).build();
            }
        };
    }

    /** A button that steps through {@code values}, each labelled by {@code <key>.<value in lower case>}. */
    static <E extends Enum<E>> ConfigEntry<E> choice(String key, E[] values, Function<ThirstConfig, E> getter,
                                                     BiConsumer<ThirstConfig, E> setter) {
        return new ConfigEntry<>(key, getter, setter) {
            @Override
            AbstractWidget control(int width) {
                return Button.builder(valueName(value()), button -> {
                    set(values[(value().ordinal() + 1) % values.length]);
                    button.setMessage(valueName(value()));
                }).bounds(0, 0, width, 20).build();
            }

            private Component valueName(E value) {
                return Component.translatable(PREFIX + key + "." + value.name().toLowerCase(Locale.ROOT));
            }
        };
    }

    /** A water grade from Dirty to Pure, labelled with the grade's name rather than a number. */
    static ConfigEntry<Integer> grade(String key, Function<ThirstConfig, Integer> getter,
                                      BiConsumer<ThirstConfig, Integer> setter) {
        return new ConfigEntry<>(key, getter, setter) {
            @Override
            AbstractWidget control(int width) {
                return new IntSlider(width, 0, 3, value(), this::set, ConfigEntry::gradeName);
            }
        };
    }

    /** A double edited as a whole percentage between {@code min} and {@code max}. */
    static ConfigEntry<Double> percent(String key, int min, int max, Function<ThirstConfig, Double> getter,
                                       BiConsumer<ThirstConfig, Double> setter) {
        return new ConfigEntry<>(key, getter, setter) {
            @Override
            AbstractWidget control(int width) {
                return new IntSlider(width, min, max, (int) Math.round(value() * PERCENT),
                        percent -> set(percent / (double) PERCENT),
                        percent -> Component.literal(percent + "%"));
            }
        };
    }

    /** A whole number between {@code min} and {@code max}, shown through {@code label}. */
    static ConfigEntry<Integer> number(String key, int min, int max, IntFunction<Component> label,
                                       Function<ThirstConfig, Integer> getter, BiConsumer<ThirstConfig, Integer> setter) {
        return new ConfigEntry<>(key, getter, setter) {
            @Override
            AbstractWidget control(int width) {
                return new IntSlider(width, min, max, value(), this::set, label);
            }
        };
    }

    /** A number of seconds, for {@link #number}. */
    static Component seconds(int seconds) {
        return Component.translatable(PREFIX + "unit.seconds", seconds);
    }

    /** A number of game ticks, a twentieth of a second each, for {@link #number}. */
    static Component ticks(int ticks) {
        return Component.translatable(PREFIX + "unit.ticks", ticks);
    }

    /** A plain count of points, for {@link #number}. */
    static Component points(int points) {
        return Component.literal(Integer.toString(points));
    }

    /** A number of drinks a container holds, for {@link #number}. */
    static Component servings(int servings) {
        return Component.translatable(PREFIX + "unit.servings", servings);
    }

    /** A whole percentage, for {@link #number}. */
    static Component wholePercent(int percent) {
        return Component.literal(percent + "%");
    }

    /** A fresh control showing the current value. The screen builds one each time it lays a page out. */
    abstract AbstractWidget control(int width);

    String key() {
        return key;
    }

    /**
     * Shows this setting only while {@code modId} is installed, for a setting that does nothing
     * without that mod. The value stays in the file either way.
     */
    ConfigEntry<T> requires(String modId) {
        requiredMod = modId;
        return this;
    }

    /**
     * Fills the label's {@code %s} with {@code args}, so a part of it, such as a grade's name, keeps a
     * style of its own.
     */
    ConfigEntry<T> labelled(Object... args) {
        labelArgs = args;
        return this;
    }

    /** Whether the screen lists this setting: always, unless the mod it needs is missing. */
    boolean isShown() {
        return requiredMod == null || Loader.isModLoaded(requiredMod);
    }

    Component label() {
        return Component.translatable(PREFIX + key, labelArgs);
    }

    Component description() {
        return Component.translatable(PREFIX + key + ".tooltip");
    }

    T value() {
        return getter.apply(ThirstConfig.get());
    }

    void set(T value) {
        setter.accept(ThirstConfig.get(), value);
    }

    boolean isDefault() {
        return Objects.equals(value(), getter.apply(defaults()));
    }

    void reset() {
        set(getter.apply(defaults()));
    }

    /** Whether {@code query}, already lower case, appears in the label or the description. */
    boolean matches(String query) {
        return label().getString().toLowerCase(Locale.ROOT).contains(query)
                || description().getString().toLowerCase(Locale.ROOT).contains(query);
    }

    private static ThirstConfig defaults() {
        if (defaults == null) defaults = new ThirstConfig();
        return defaults;
    }

    private static Component onOff(boolean on) {
        return on
                ? CommonComponents.OPTION_ON.copy().withStyle(ChatFormatting.GREEN)
                : CommonComponents.OPTION_OFF.copy().withStyle(ChatFormatting.RED);
    }

    /** The name the game gives water of {@code purity}, as its tooltip shows it. */
    static Component gradeName(int purity) {
        return WaterPurity.purityName(purity);
    }

    /** The vanilla slider over whole numbers, labelled with the value alone. */
    private static final class IntSlider extends AbstractSliderButton {
        private final int min;
        private final int max;
        private final IntConsumer setter;
        private final IntFunction<Component> label;
        private int current;

        IntSlider(int width, int min, int max, int initial, IntConsumer setter,
                  IntFunction<Component> label) {
            super(0, 0, width, 20, Component.empty(), (initial - min) / (double) (max - min));
            this.min = min;
            this.max = max;
            this.setter = setter;
            this.label = label;
            this.current = initial;
            updateMessage();
        }

        @Override
        protected void updateMessage() {
            // The constructor of the superclass runs before the label is set.
            if (label != null) setMessage(label.apply(current));
        }

        @Override
        protected void applyValue() {
            int next = min + (int) Math.round(value * (max - min));
            if (next == current) return;
            current = next;
            setter.accept(next);
        }
    }
}
