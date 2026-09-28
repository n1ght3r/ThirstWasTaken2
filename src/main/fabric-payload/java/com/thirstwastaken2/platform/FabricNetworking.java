package com.thirstwastaken2.platform;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * {@link Loader}'s player data and clientbound payloads, from Minecraft 1.20.5, where Fabric API has
 * payload types and attachments that sync themselves. 1.20.1 has neither and its own copy of this class
 * in {@code src/main/fabric-legacypayload}; the build compiles one of the two.
 */
public final class FabricNetworking {
    private static final List<Registration<?>> REGISTRATIONS = new ArrayList<>();

    /** The client half of one payload, held until the client entrypoint registers it. */
    public record Registration<T extends CustomPacketPayload>(CustomPacketPayload.Type<T> type, Consumer<T> handler) { }

    private FabricNetworking() { }

    /** See {@link Loader#playerData}. */
    public static <T> PlayerData<T> playerData(Identifier id, Supplier<T> initial, Codec<T> codec,
                                               BiConsumer<T, FriendlyByteBuf> write,
                                               Function<FriendlyByteBuf, T> read) {
        StreamCodec<FriendlyByteBuf, T> streamCodec = StreamCodec.ofMember(write::accept, read::apply);
        AttachmentType<T> type = AttachmentRegistry.create(id, builder -> builder
                .initializer(initial)
                .persistent(codec)
                .syncWith(streamCodec, AttachmentSyncPredicate.targetOnly()));
        return new AttachmentPlayerData<>(type);
    }

    /**
     * See {@link Loader#clientboundPayload}. Fabric API keeps the client receiver in its client module,
     * which common code cannot see, so the handler waits in {@link #registrations} until the client
     * entrypoint registers it.
     */
    public static <T> Clientbound<T> clientboundPayload(Identifier id, BiConsumer<T, FriendlyByteBuf> write,
                                                        Function<FriendlyByteBuf, T> read, Consumer<T> handler) {
        CustomPacketPayload.Type<Wrapped<T>> type = new CustomPacketPayload.Type<>(id);
        StreamCodec<FriendlyByteBuf, Wrapped<T>> codec = StreamCodec.ofMember(
                (payload, buffer) -> write.accept(payload.value(), buffer),
                buffer -> new Wrapped<>(type, read.apply(buffer)));
        //? if >=26.1 {
        PayloadTypeRegistry.clientboundPlay().register(type, codec);
        //?} else {
        /*PayloadTypeRegistry.playS2C().register(type, codec);
        *///?}
        REGISTRATIONS.add(new Registration<>(type, payload -> handler.accept(payload.value())));
        return (player, value) -> {
            if (ServerPlayNetworking.canSend(player, type)) ServerPlayNetworking.send(player, new Wrapped<>(type, value));
        };
    }

    /** Every payload declared so far. The main entrypoint always runs before the client one. */
    public static List<Registration<?>> registrations() {
        return List.copyOf(REGISTRATIONS);
    }

    /** A common-code payload inside the loader's own packet type. */
    private record Wrapped<T>(CustomPacketPayload.Type<Wrapped<T>> type, T value) implements CustomPacketPayload { }

    private record AttachmentPlayerData<T>(AttachmentType<T> type) implements PlayerData<T> {
        @Override
        public T get(Player player) {
            return player.getAttachedOrCreate(type);
        }

        @Override
        public void set(Player player, T value) {
            player.setAttached(type, value);
        }
    }
}
