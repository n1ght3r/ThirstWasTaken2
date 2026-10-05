package com.thirstwastaken2.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

/**
 * A block entity that saves a few named values and one list of items, the same way on every version.
 * How a block entity is saved changed twice: a {@code CompoundTag} on 1.20.1, the tag and the registries
 * from 1.20.5, and {@code ValueOutput} and {@code ValueInput} from 1.21.5. The overrides live here once
 * and hand the mod's block entities one {@link #save} and {@link #load} over a small {@link Output} and
 * {@link Input}.
 *
 * <p>The items are saved under vanilla's own key, through {@code ContainerHelper}, as a chest's are, so
 * there is one list per block entity.
 *
 * <p>One that {@link #syncsToClient} also sends what it saves to the client, when the chunk loads and
 * each time {@link #changed} is called; the client reads it through the same {@link #load}.
 *
 * <p>A class rather than a method for the same reason as {@link SupportedBlock}: what differs is an override.
 */
public abstract class SavedBlockEntity extends BlockEntity {
    protected SavedBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    /** Writes this block entity's state. */
    protected abstract void save(Output output);

    /** Reads back what {@link #save} wrote, each value falling back when it is missing. */
    protected abstract void load(Input input);

    /** Whether the client is told what this block entity saves. */
    protected boolean syncsToClient() {
        return false;
    }

    /** Marks the state changed, so the chunk is saved, and when it syncs sends it to the client. */
    protected void changed() {
        setChanged();
        if (syncsToClient() && level != null && !level.isClientSide()) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Where {@link #save} writes. */
    public interface Output {
        void putInt(String key, int value);

        void putBoolean(String key, boolean value);

        void putString(String key, String value);

        /** The block entity's one list of items, empty slots included. */
        void putItems(NonNullList<ItemStack> items);
    }

    /** What {@link #load} reads. */
    public interface Input {
        int getInt(String key, int fallback);

        boolean getBoolean(String key, boolean fallback);

        String getString(String key, String fallback);

        /** Fills {@code items} from the saved list; a slot the save does not name is left as it is. */
        void getItems(NonNullList<ItemStack> items);
    }

    //? if >=1.21.5 {
    @Override
    protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput output) {
        super.saveAdditional(output);
        save(new Output() {
            @Override
            public void putInt(String key, int value) {
                output.putInt(key, value);
            }

            @Override
            public void putBoolean(String key, boolean value) {
                output.putBoolean(key, value);
            }

            @Override
            public void putString(String key, String value) {
                output.putString(key, value);
            }

            @Override
            public void putItems(NonNullList<ItemStack> items) {
                ContainerHelper.saveAllItems(output, items);
            }
        });
    }

    @Override
    protected void loadAdditional(net.minecraft.world.level.storage.ValueInput input) {
        super.loadAdditional(input);
        load(new Input() {
            @Override
            public int getInt(String key, int fallback) {
                return input.getIntOr(key, fallback);
            }

            @Override
            public boolean getBoolean(String key, boolean fallback) {
                return input.getBooleanOr(key, fallback);
            }

            @Override
            public String getString(String key, String fallback) {
                return input.getStringOr(key, fallback);
            }

            @Override
            public void getItems(NonNullList<ItemStack> items) {
                ContainerHelper.loadAllItems(input, items);
            }
        });
    }
    //?} elif >=1.20.5 {
    /*@Override
    protected void saveAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        save(new TagOutput(tag) {
            @Override
            public void putItems(NonNullList<ItemStack> items) {
                ContainerHelper.saveAllItems(tag, items, registries);
            }
        });
    }

    @Override
    protected void loadAdditional(CompoundTag tag, net.minecraft.core.HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        load(new TagInput(tag) {
            @Override
            public void getItems(NonNullList<ItemStack> items) {
                ContainerHelper.loadAllItems(tag, items, registries);
            }
        });
    }
    *///?} else {
    /*@Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        save(new TagOutput(tag) {
            @Override
            public void putItems(NonNullList<ItemStack> items) {
                ContainerHelper.saveAllItems(tag, items);
            }
        });
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        load(new TagInput(tag) {
            @Override
            public void getItems(NonNullList<ItemStack> items) {
                ContainerHelper.loadAllItems(tag, items);
            }
        });
    }
    *///?}

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return syncsToClient() ? ClientboundBlockEntityDataPacket.create(this) : null;
    }

    //? if >=1.20.5 {
    @Override
    public CompoundTag getUpdateTag(net.minecraft.core.HolderLookup.Provider registries) {
        return syncsToClient() ? saveWithoutMetadata(registries) : super.getUpdateTag(registries);
    }
    //?} else {
    /*@Override
    public CompoundTag getUpdateTag() {
        return syncsToClient() ? saveWithoutMetadata() : super.getUpdateTag();
    }
    *///?}

    //? if <1.21.5 {
    /*// The plain values of an Output on a tag, before 1.21.5; the items are the caller's.
    private abstract static class TagOutput implements Output {
        private final CompoundTag tag;

        TagOutput(CompoundTag tag) {
            this.tag = tag;
        }

        @Override
        public void putInt(String key, int value) {
            tag.putInt(key, value);
        }

        @Override
        public void putBoolean(String key, boolean value) {
            tag.putBoolean(key, value);
        }

        @Override
        public void putString(String key, String value) {
            tag.putString(key, value);
        }
    }

    // The plain values of an Input from a tag, before 1.21.5; the items are the caller's.
    private abstract static class TagInput implements Input {
        private final CompoundTag tag;

        TagInput(CompoundTag tag) {
            this.tag = tag;
        }

        @Override
        public int getInt(String key, int fallback) {
            return tag.contains(key) ? tag.getInt(key) : fallback;
        }

        @Override
        public boolean getBoolean(String key, boolean fallback) {
            return tag.contains(key) ? tag.getBoolean(key) : fallback;
        }

        @Override
        public String getString(String key, String fallback) {
            return tag.contains(key) ? tag.getString(key) : fallback;
        }
    }
    *///?}
}
