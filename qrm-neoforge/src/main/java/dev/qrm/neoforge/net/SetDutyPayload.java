package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** C→S: il giocatore chiede di entrare ({@code true}) o uscire ({@code false}) di servizio. */
public record SetDutyPayload(boolean onDuty) implements CustomPacketPayload {
    public static final Type<SetDutyPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("qrm", "set_duty"));
    public static final StreamCodec<ByteBuf, SetDutyPayload> CODEC =
            ByteBufCodecs.BOOL.map(SetDutyPayload::new, SetDutyPayload::onDuty);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
