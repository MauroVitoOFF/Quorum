package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S→C: stato dei personaggi del giocatore; {@code openScreen} chiede al client di aprire la schermata. */
public record CharacterRosterPayload(List<CharacterCard> cards, int slots, Optional<UUID> activeId, boolean openScreen)
        implements CustomPacketPayload {
    public static final Type<CharacterRosterPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath("qrm", "character_roster"));
    public static final StreamCodec<ByteBuf, CharacterRosterPayload> CODEC = StreamCodec.composite(
            PayloadCodecs.CARD.apply(ByteBufCodecs.list(CharacterCard.MAX_ROSTER_CARDS)), CharacterRosterPayload::cards,
            ByteBufCodecs.VAR_INT, CharacterRosterPayload::slots,
            ByteBufCodecs.optional(UUIDUtil.STREAM_CODEC), CharacterRosterPayload::activeId,
            ByteBufCodecs.BOOL, CharacterRosterPayload::openScreen,
            CharacterRosterPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
