package com.thirstwastaken2.platform;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.S2CPlayChannelEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

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
 * {@link Loader}'s player data and clientbound payloads on Minecraft 1.20.1, where Fabric API has no
 * payload types and its attachments save themselves but do not sync. Every later version has its own
 * copy of this class in {@code src/main/fabric-payload}; the build compiles one of the two.
 *
 * <p>A payload is a plain channel named by its id. A player's value is sent to that player whenever it
 * is set, and again whenever their client makes a new player entity: on joining, respawning and changing
 * dimension. Before 1.20.2 a client says which channels it takes only once play has begun, after the
 * server has already sent what it sends on joining, so a value for a client that has not said so yet
 * waits here, the newest per channel, until it does. A client without the mod never says so, and what
 * waited for it is dropped when it leaves.
 */
public final class FabricNetworking {
    private static final List<Receiver<?>> RECEIVERS = new ArrayList<>();
    /** Sends each player's data again, for a client that made a new player entity. */
    private static final List<Consumer<ServerPlayer>> RESYNC = new ArrayList<>();
    /** What waits for each player's client to take a channel. Only touched on the server thread. */
    private static final Map<ServerPlayer, Map<Identifier, Consumer<ServerPlayer>>> PENDING = new HashMap<>();
    private static boolean listening;

    /** The client half of one channel, held until the client entrypoint registers it. */
    public record Receiver<T>(Identifier id, Function<FriendlyByteBuf, T> read, BiConsumer<Player, T> handler) { }

    private FabricNetworking() { }

    /** See {@link Loader#playerData}. */
    public static <T> PlayerData<T> playerData(Identifier id, Supplier<T> initial, Codec<T> codec,
                                               BiConsumer<T, FriendlyByteBuf> write,
                                               Function<FriendlyByteBuf, T> read) {
        AttachmentType<T> type = AttachmentRegistry.<T>builder()
                .initializer(initial)
                .persistent(codec)
                .buildAndRegister(id);
        Clientbound<T> sync = channel(id, write, read, (player, value) -> player.setAttached(type, value));
        RESYNC.add(player -> sync.send(player, player.getAttachedOrCreate(type)));
        return new PlayerData<>() {
            @Override
            public T get(Player player) {
                return player.getAttachedOrCreate(type);
            }

            @Override
            public void set(Player player, T value) {
                player.setAttached(type, value);
                if (player instanceof ServerPlayer serverPlayer) sync.send(serverPlayer, value);
            }
        };
    }

    /** See {@link Loader#clientboundPayload}. */
    public static <T> Clientbound<T> clientboundPayload(Identifier id, BiConsumer<T, FriendlyByteBuf> write,
                                                        Function<FriendlyByteBuf, T> read, Consumer<T> handler) {
        return channel(id, write, read, (player, value) -> handler.accept(value));
    }

    /** Every channel declared so far. The main entrypoint always runs before the client one. */
    public static List<Receiver<?>> receivers() {
        return List.copyOf(RECEIVERS);
    }

    private static <T> Clientbound<T> channel(Identifier id, BiConsumer<T, FriendlyByteBuf> write,
                                              Function<FriendlyByteBuf, T> read, BiConsumer<Player, T> handler) {
        listen();
        RECEIVERS.add(new Receiver<>(id, read, handler));
        return (player, value) -> {
            // A player still being loaded has no connection yet; joining sends their data anyway.
            if (player.connection == null) return;
            Consumer<ServerPlayer> send = target -> {
                FriendlyByteBuf buffer = PacketByteBufs.create();
                write.accept(value, buffer);
                ServerPlayNetworking.send(target, id, buffer);
            };
            if (ServerPlayNetworking.canSend(player, id)) {
                send.accept(player);
            } else {
                PENDING.computeIfAbsent(player, ignored -> new LinkedHashMap<>()).put(id, send);
            }
        };
    }

    private static void listen() {
        if (listening) return;
        listening = true;
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> resync(handler.player));
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
            PENDING.remove(oldPlayer);
            resync(newPlayer);
        });
        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register((player, origin, destination) -> resync(player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> server.execute(() -> PENDING.remove(handler.player)));
        // Channel registrations arrive on the network thread.
        S2CPlayChannelEvents.REGISTER.register((handler, sender, server, channels) -> server.execute(() -> {
            Map<Identifier, Consumer<ServerPlayer>> waiting = PENDING.get(handler.player);
            if (waiting == null) return;
            for (Identifier channel : channels) {
                Consumer<ServerPlayer> send = waiting.remove(channel);
                if (send != null) send.accept(handler.player);
            }
            if (waiting.isEmpty()) PENDING.remove(handler.player);
        }));
    }

    private static void resync(ServerPlayer player) {
        RESYNC.forEach(sync -> sync.accept(player));
    }
}
