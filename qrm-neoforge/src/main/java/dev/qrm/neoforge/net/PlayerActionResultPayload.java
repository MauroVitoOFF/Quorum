package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S→C: esito di un'azione sul giocatore, con (per le azioni informative) le righe da mostrare. */
public record PlayerActionResultPayload(PlayerAction action, PlayerResult result, UUID target, List<PlayerInfoLine> lines)
        implements CustomPacketPayload {
    public static final int MAX_LINES = 80;
    public static final Type<PlayerActionResultPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm", "player_action_result"));
    public static final StreamCodec<ByteBuf, PlayerActionResultPayload> CODEC = StreamCodec.composite(
            PayloadCodecs.PLAYER_ACTION, PlayerActionResultPayload::action,
            PayloadCodecs.PLAYER_RESULT, PlayerActionResultPayload::result,
            UUIDUtil.STREAM_CODEC, PlayerActionResultPayload::target,
            PayloadCodecs.INFO_LINES, PlayerActionResultPayload::lines,
            PlayerActionResultPayload::new);

    public PlayerActionResultPayload {
        lines = List.copyOf(lines);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
