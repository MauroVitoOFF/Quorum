package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S→C: l'elenco dei giocatori online per il pannello staff. */
public record PanelSnapshotPayload(PanelSnapshot snapshot) implements CustomPacketPayload {
    public static final Type<PanelSnapshotPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm", "panel_snapshot"));
    public static final StreamCodec<ByteBuf, PanelSnapshotPayload> CODEC =
            PayloadCodecs.PANEL_SNAPSHOT.map(PanelSnapshotPayload::new, PanelSnapshotPayload::snapshot);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
