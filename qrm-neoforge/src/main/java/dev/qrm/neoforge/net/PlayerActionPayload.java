package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * C→S: un'azione del pannello staff sul giocatore {@code target}. {@code arg} è la scelta o i valori dei campi (uno per riga);
 * il server ricontrolla il nodo, il bersaglio e valida l'argomento.
 */
public record PlayerActionPayload(PlayerAction action, UUID target, String arg) implements CustomPacketPayload {
    public static final int MAX_ARG = 256;
    public static final Type<PlayerActionPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("qrm", "player_action"));
    public static final StreamCodec<ByteBuf, PlayerActionPayload> CODEC = StreamCodec.composite(
            PayloadCodecs.PLAYER_ACTION, PlayerActionPayload::action,
            UUIDUtil.STREAM_CODEC, PlayerActionPayload::target,
            ByteBufCodecs.stringUtf8(MAX_ARG), PlayerActionPayload::arg,
            PlayerActionPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
