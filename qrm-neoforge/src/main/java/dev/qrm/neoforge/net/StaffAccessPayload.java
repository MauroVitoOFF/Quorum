package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * S→C: cosa può vedere il giocatore nel pannello staff. {@code all} = operatore (ogni nodo); altrimenti i pattern
 * concessi. Serve solo a filtrare la UI: il server ricontrolla il nodo a ogni azione.
 */
public record StaffAccessPayload(boolean all, List<String> patterns) implements CustomPacketPayload {
    public static final int MAX_PATTERNS = 64;
    public static final Type<StaffAccessPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm", "staff_access"));
    public static final StreamCodec<ByteBuf, StaffAccessPayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, StaffAccessPayload::all,
            ByteBufCodecs.stringUtf8(128).apply(ByteBufCodecs.list(MAX_PATTERNS)), StaffAccessPayload::patterns,
            StaffAccessPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
