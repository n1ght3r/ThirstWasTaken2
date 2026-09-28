package com.thirstwastaken2.platform;

import com.mojang.serialization.Codec;
import com.thirstwastaken2.ThirstWasTaken2;
import com.thirstwastaken2.client.forge.ClientPlayer;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link Loader}'s player data and clientbound payloads on Forge 47, which has neither attachments nor
 * payload types: a capability on every player, and a {@link SimpleChannel} per payload.
 *
 * <p>Every value the mod keeps on a player lives in one capability, {@code thirstwastaken2:player}, saved
 * under Forge's {@code ForgeCaps} with each value under its own id, through its own codec. A value is
 * copied to the new player entity when the old one leaves the End, and starts afresh after a death, the
 * same as a NeoForge attachment without {@code copyOnDeath} and a Fabric one without
 * {@code copyOnDeath}. It is sent to its player whenever it is set, and again whenever their client
 * makes a new player entity: on joining, respawning and changing dimension.
 *
 * <p>Each channel accepts a missing mod on the other side, so a vanilla client may join, and a send
 * first asks whether the client has the channel. Forge negotiates channels during login, so unlike
 * Fabric on 1.20.1 there is nothing to hold back until a client says so.
 */
public final class ForgeNetworking {
    private static final String PROTOCOL = "1";
    private static final Capability<PlayerValues> PLAYER_VALUES = CapabilityManager.get(new CapabilityToken<>() { });
    private static final Identifier PLAYER_VALUES_ID = ThirstWasTaken2.id("player");
    /** Every value declared, by id, in declaration order. */
    private static final Map<Identifier, Entry<?>> ENTRIES = new LinkedHashMap<>();
    /** Sends each player's data again, for a client that made a new player entity. */
    private static final List<Consumer<ServerPlayer>> RESYNC = new ArrayList<>();
    private static boolean listening;

    private ForgeNetworking() { }

    /** See {@link Loader#playerData}. */
    static <T> PlayerData<T> playerData(Identifier id, Supplier<T> initial, Codec<T> codec,
                                        BiConsumer<T, FriendlyByteBuf> write, Function<FriendlyByteBuf, T> read) {
        listen();
        Entry<T> entry = new Entry<>(id, initial, codec);
        ENTRIES.put(id, entry);
        // The handler only ever runs on a client, so ClientPlayer is never loaded on a dedicated server.
        Clientbound<T> sync = channel(id, write, read, value -> {
            Player player = ClientPlayer.get();
            if (player != null) entry.put(player, value);
        });
        RESYNC.add(player -> sync.send(player, entry.get(player)));
        return new PlayerData<>() {
            @Override
            public T get(Player player) {
                return entry.get(player);
            }

            @Override
            public void set(Player player, T value) {
                entry.put(player, value);
                if (player instanceof ServerPlayer serverPlayer) sync.send(serverPlayer, value);
            }
        };
    }

    /** See {@link Loader#clientboundPayload}. */
    static <T> Clientbound<T> clientboundPayload(Identifier id, BiConsumer<T, FriendlyByteBuf> write,
                                                 Function<FriendlyByteBuf, T> read, Consumer<T> handler) {
        return channel(id, write, read, handler);
    }

    private static <T> Clientbound<T> channel(Identifier id, BiConsumer<T, FriendlyByteBuf> write,
                                              Function<FriendlyByteBuf, T> read, Consumer<T> handler) {
        SimpleChannel channel = NetworkRegistry.newSimpleChannel(id, () -> PROTOCOL,
                NetworkRegistry.acceptMissingOr(PROTOCOL), NetworkRegistry.acceptMissingOr(PROTOCOL));
        channel.messageBuilder(Message.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder((message, buffer) -> message.write().accept(buffer))
                .decoder(buffer -> {
                    T value = read.apply(buffer);
                    return new Message(ignored -> { }, () -> handler.accept(value));
                })
                .consumerMainThread((message, context) -> message.handle().run())
                .add();
        return (player, value) -> {
            // A fake player's connection has no network channel to ask, and nothing to send to either.
            // A player still being loaded has no connection yet; joining sends their data anyway.
            if (player instanceof FakePlayer || player.connection == null) return;
            if (!channel.isRemotePresent(player.connection.connection)) return;
            channel.send(PacketDistributor.PLAYER.with(() -> player),
                    new Message(buffer -> write.accept(value, buffer), () -> { }));
        };
    }

    /**
     * One payload, either side of the wire: what the server writes, or what the client does with what it
     * read. A channel keys its messages by class, and each payload has a channel of its own, so one class
     * serves every payload.
     */
    private record Message(Consumer<FriendlyByteBuf> write, Runnable handle) { }

    private static void listen() {
        if (listening) return;
        listening = true;
        Loader.modBus().addListener((RegisterCapabilitiesEvent event) -> event.register(PlayerValues.class));
        MinecraftForge.EVENT_BUS.addGenericListener(Entity.class, (AttachCapabilitiesEvent<Entity> event) -> {
            if (event.getObject() instanceof Player) event.addCapability(PLAYER_VALUES_ID, new PlayerValues());
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.Clone event) -> {
            if (event.isWasDeath()) return;
            // Forge invalidates the old entity's capabilities before this event.
            event.getOriginal().reviveCaps();
            values(event.getOriginal()).ifPresent(original ->
                    values(event.getEntity()).ifPresent(copy -> copy.values.putAll(original.values)));
            event.getOriginal().invalidateCaps();
        });
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerLoggedInEvent event) -> resync(event.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerRespawnEvent event) -> resync(event.getEntity()));
        MinecraftForge.EVENT_BUS.addListener((PlayerEvent.PlayerChangedDimensionEvent event) -> resync(event.getEntity()));
    }

    private static void resync(Player player) {
        if (player instanceof ServerPlayer serverPlayer) RESYNC.forEach(sync -> sync.accept(serverPlayer));
    }

    private static LazyOptional<PlayerValues> values(Player player) {
        return player.getCapability(PLAYER_VALUES);
    }

    /** One declared value: how to make, save and load it. */
    private record Entry<T>(Identifier id, Supplier<T> initial, Codec<T> codec) {
        @SuppressWarnings("unchecked")
        T get(Player player) {
            PlayerValues values = values(player).orElse(null);
            if (values == null) return initial.get();
            return (T) values.values.computeIfAbsent(id, ignored -> initial.get());
        }

        void put(Player player, T value) {
            values(player).ifPresent(values -> values.values.put(id, value));
        }

        Tag save(Object value) {
            @SuppressWarnings("unchecked")
            T typed = (T) value;
            return codec.encodeStart(NbtOps.INSTANCE, typed).result().orElse(null);
        }

        T load(Tag tag) {
            return codec.parse(NbtOps.INSTANCE, tag).resultOrPartial(error ->
                    ThirstWasTaken2.LOGGER.warn("Could not read {} from a saved player: {}", id, error)).orElse(null);
        }
    }

    /** Every value on one player, the capability itself. */
    private static final class PlayerValues implements ICapabilitySerializable<CompoundTag> {
        private final Map<Identifier, Object> values = new HashMap<>();
        private final LazyOptional<PlayerValues> self = LazyOptional.of(() -> this);

        @Override
        public <C> LazyOptional<C> getCapability(Capability<C> capability, Direction side) {
            return PLAYER_VALUES.orEmpty(capability, self);
        }

        @Override
        public CompoundTag serializeNBT() {
            CompoundTag tag = new CompoundTag();
            values.forEach((id, value) -> {
                Tag saved = ENTRIES.get(id).save(value);
                if (saved != null) tag.put(id.toString(), saved);
            });
            return tag;
        }

        @Override
        public void deserializeNBT(CompoundTag tag) {
            values.clear();
            ENTRIES.forEach((id, entry) -> {
                Tag saved = tag.get(id.toString());
                Object value = saved == null ? null : entry.load(saved);
                if (value != null) values.put(id, value);
            });
        }
    }
}
