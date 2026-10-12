package dev.qrm.boss.net;

import dev.qrm.boss.logic.BossReply;
import dev.qrm.boss.logic.BossWire;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S&rarr;C: le aziende fra cui scegliere, quando il Character ha {@code boss.menu} in più di una. */
public record BossOrgListPayload(BossReply.OrgList list) implements CustomPacketPayload {
    public static final Type<BossOrgListPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm_boss", "org_list"));
    public static final StreamCodec<ByteBuf, BossOrgListPayload> CODEC = ByteBufCodecs.byteArray(1 << 14)
            .map(bytes -> new BossOrgListPayload(BossWire.decodeOrgList(bytes)), p -> BossWire.encode(p.list()));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
