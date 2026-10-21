package com.thirstwastaken2.config;

import com.thirstwastaken2.ThirstWasTaken2;

import java.util.regex.Pattern;

/**
 * The config's four keyword lists, compiled once each time the config is sanitised, never on a call
 * path. A list that is blank, or does not compile, is {@code null}: it matches nothing.
 */
final class KeywordPatterns {
    final Pattern blacklist;
    final Pattern drink;
    final Pattern soup;
    final Pattern fruit;

    KeywordPatterns(ThirstConfig config) {
        blacklist = compile(config.keywordBlacklist);
        drink = compile(config.drinkKeywords);
        soup = compile(config.soupKeywords);
        fruit = compile(config.fruitKeywords);
    }

    private static Pattern compile(String pattern) {
        if (pattern == null || pattern.isBlank()) return null;
        try {
            // Whole words of the id only, with an optional plural: a substring match made steak_sandwich
            // a drink for the "tea" inside it. Anything but a letter or digit separates words.
            return Pattern.compile("(?<![a-z0-9])(?:" + pattern + ")(?:e?s)?(?![a-z0-9])",
                    Pattern.CASE_INSENSITIVE);
        } catch (Exception exception) {
            ThirstWasTaken2.LOGGER.error("Invalid keyword pattern '{}', ignoring it", pattern, exception);
            return null;
        }
    }
}
