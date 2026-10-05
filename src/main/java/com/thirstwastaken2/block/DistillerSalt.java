package com.thirstwastaken2.block;

import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.config.ThirstConfig;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;

/**
 * The salt the copper distiller leaves behind from sea water. The mod adds no salt of its own: it is
 * another mod's, found through {@link #TAG}, whose entries are all optional, so with none of those mods
 * there is none, and the distiller makes no salt and shows no slot for it. {@code distillerSaltItem} in
 * the config pins one item in a modpack where the tag finds several, or one the tag does not know.
 *
 * <p>Resolved once and kept until the tags or the config change; the machine asks every tick.
 */
public final class DistillerSalt {
    /** Every salt item that may come out of the distiller; the first is the one that does. */
    public static final TagKey<Item> TAG = TagKey.create(Registries.ITEM, ThirstWasTaken2.id("distiller_salt"));

    private static volatile Item cached;
    private static volatile int cachedGeneration = -1;

    private DistillerSalt() { }

    /** The salt the distiller makes, or {@code null} when there is none. */
    public static Item item() {
        int generation = ThirstConfig.generation();
        if (cachedGeneration != generation) {
            cached = resolve(ThirstConfig.get().distillerSaltItem, BuiltInRegistries.ITEM.getTagOrEmpty(TAG));
            cachedGeneration = generation;
        }
        return cached;
    }

    /** Forgets the salt, for the tags that name it were loaded again. */
    public static void clearCache() {
        cachedGeneration = -1;
    }

    /**
     * The item {@code pinned} names when it names one, else the first item in {@code tagged}, else
     * {@code null}. A pin that names no item, a typo or a mod that is gone, falls back to the tag.
     */
    public static Item resolve(String pinned, Iterable<Holder<Item>> tagged) {
        if (pinned != null && !pinned.isEmpty()) {
            Identifier id = Identifier.tryParse(pinned);
            Item item = id == null ? Items.AIR : BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
            if (item != Items.AIR) return item;
        }
        for (Holder<Item> holder : tagged) {
            if (holder.value() != Items.AIR) return holder.value();
        }
        return null;
    }
}
