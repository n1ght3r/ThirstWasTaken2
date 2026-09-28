package com.thirstwastaken2.platform;

import net.minecraft.server.level.ServerPlayer;

/**
 * A payload the server sends to clients, declared by {@code Loader.clientboundPayload}. Common code
 * describes its payloads as plain records with {@code write} and {@code read}; each loader wraps them in
 * its own packet type.
 */
@FunctionalInterface
public interface Clientbound<T> {
    /** Sends {@code value} to {@code player}, or nothing when their client cannot receive it. */
    void send(ServerPlayer player, T value);
}
