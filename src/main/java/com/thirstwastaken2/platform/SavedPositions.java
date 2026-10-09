package com.thirstwastaken2.platform;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * One small int per block position, saved with a level: for a value that belongs to a block with no
 * block entity to keep it on. Each name is one file in the dimension's {@code data} folder.
 *
 * <p>A class rather than a method because saving is an override, whose shape changed twice: before
 * 1.20.5 {@code save(CompoundTag)} and a loader function handed to {@code computeIfAbsent}, from 1.20.5 a
 * {@code SavedData.Factory} and {@code save(CompoundTag, HolderLookup.Provider)}, and from 1.21.5 a
 * {@code SavedDataType} with a codec and no save method at all; its id is a string until 26.1 and an
 * {@code Identifier} from then on. Every version stores the same thing, one long array of position and
 * value pairs under {@code entries}.
 *
 * <p>Server thread only, as the level is. Values are positive; nothing stores 0, which is what
 * {@link #remove} hands back for a position with no value.
 */
public final class SavedPositions extends SavedData {
    private static final String ENTRIES = "entries";

    /**
     * The type of each name, made once: the storage caches what it loaded by type, and from 1.21.5 a type
     * is a record whose constructor is compared too, so a second, equal-looking type would load the file
     * again over unsaved changes.
     */
    private static final Map<String, Object> TYPES = new ConcurrentHashMap<>();

    private final Long2IntOpenHashMap values = new Long2IntOpenHashMap();

    private SavedPositions() { }

    /** The values {@code name} keeps in {@code level}, loaded or made the first time they are asked for. */
    public static SavedPositions of(ServerLevel level, String name) {
        //? if >=1.21.5 {
        @SuppressWarnings("unchecked")
        net.minecraft.world.level.saveddata.SavedDataType<SavedPositions> type =
                (net.minecraft.world.level.saveddata.SavedDataType<SavedPositions>) TYPES.computeIfAbsent(name, SavedPositions::type);
        return level.getDataStorage().computeIfAbsent(type);
        //?} elif >=1.20.5 {
        /*@SuppressWarnings("unchecked")
        SavedData.Factory<SavedPositions> factory =
                (SavedData.Factory<SavedPositions>) TYPES.computeIfAbsent(name, SavedPositions::type);
        return level.getDataStorage().computeIfAbsent(factory, fileName(name));
        *///?} else {
        /*return level.getDataStorage().computeIfAbsent(SavedPositions::load, SavedPositions::new, fileName(name));
        *///?}
    }

    /** The value at {@code pos}, or 0. */
    public int get(BlockPos pos) {
        return values.get(pos.asLong());
    }

    /** Sets the value at {@code pos}; 0 or less removes it. */
    public void put(BlockPos pos, int value) {
        if (value <= 0) {
            remove(pos);
            return;
        }
        if (values.put(pos.asLong(), value) != value) setDirty();
    }

    /** Removes the value at {@code pos} and hands it back, or 0 when there was none. */
    public int remove(BlockPos pos) {
        int removed = values.remove(pos.asLong());
        if (removed != 0) setDirty();
        return removed;
    }

    /** The file name of {@code name} before 26.1, where an id is a plain string: the mod id and the name. */
    private static String fileName(String name) {
        return com.thirstwastaken2.ThirstWasTaken2.MOD_ID + "_" + name;
    }

    // A type's id is an Identifier from 26.1, saved under the namespace's own folder.
    //? if >=26.1 {
    private static net.minecraft.resources.Identifier typeId(String name) {
        return com.thirstwastaken2.ThirstWasTaken2.id(name);
    }
    //?} else {
    /*private static String typeId(String name) {
        return fileName(name);
    }
    *///?}

    private long[] pairs() {
        long[] pairs = new long[values.size() * 2];
        int i = 0;
        for (Long2IntMap.Entry entry : values.long2IntEntrySet()) {
            pairs[i++] = entry.getLongKey();
            pairs[i++] = entry.getIntValue();
        }
        return pairs;
    }

    private static SavedPositions fromPairs(long[] pairs) {
        SavedPositions saved = new SavedPositions();
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            if (pairs[i + 1] > 0) saved.values.put(pairs[i], (int) pairs[i + 1]);
        }
        return saved;
    }

    // The datafix type cannot be null: the storage hands it to the fixer unchecked before 26.1 on
    // Fabric. Command storage's schema is a bare compound, which no fixer rewrites.
    //? if >=1.21.5 {
    private static Object type(String name) {
        com.mojang.serialization.Codec<SavedPositions> codec = com.mojang.serialization.Codec.LONG_STREAM
                .xmap(stream -> fromPairs(stream.toArray()), saved -> java.util.Arrays.stream(saved.pairs()))
                .fieldOf(ENTRIES).codec();
        return new net.minecraft.world.level.saveddata.SavedDataType<>(typeId(name), SavedPositions::new, codec,
                net.minecraft.util.datafix.DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    }
    //?} elif >=1.20.5 {
    /*private static Object type(String name) {
        return new SavedData.Factory<>(SavedPositions::new, (tag, registries) -> load(tag),
                net.minecraft.util.datafix.DataFixTypes.SAVED_DATA_COMMAND_STORAGE);
    }

    private static SavedPositions load(net.minecraft.nbt.CompoundTag tag) {
        return fromPairs(tag.getLongArray(ENTRIES));
    }

    @Override
    public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        tag.putLongArray(ENTRIES, pairs());
        return tag;
    }
    *///?} else {
    /*private static SavedPositions load(net.minecraft.nbt.CompoundTag tag) {
        return fromPairs(tag.getLongArray(ENTRIES));
    }

    @Override
    public net.minecraft.nbt.CompoundTag save(net.minecraft.nbt.CompoundTag tag) {
        tag.putLongArray(ENTRIES, pairs());
        return tag;
    }
    *///?}
}
