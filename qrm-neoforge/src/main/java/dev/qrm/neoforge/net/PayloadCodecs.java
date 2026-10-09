package dev.qrm.neoforge.net;

import io.netty.buffer.ByteBuf;
import java.time.LocalDate;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Codec condivisi e limiti di protocollo. */
final class PayloadCodecs {
    static final int MAX_NAME = 64;
    static final int MAX_DOCUMENT = 16;
    /** Voci massime per elenco dello snapshot del pannello staff: protegge il client da pacchetti enormi. */
    static final int MAX_LIST = 512;

    static final StreamCodec<ByteBuf, LocalDate> DATE = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, LocalDate::getYear,
            ByteBufCodecs.VAR_INT, LocalDate::getMonthValue,
            ByteBufCodecs.VAR_INT, LocalDate::getDayOfMonth,
            (year, month, day) -> LocalDate.of(year, month, day));

    static final StreamCodec<ByteBuf, CharacterCard> CARD = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, CharacterCard::id,
            ByteBufCodecs.stringUtf8(MAX_NAME), CharacterCard::firstName,
            ByteBufCodecs.stringUtf8(MAX_NAME), CharacterCard::lastName,
            DATE, CharacterCard::birthDate,
            ByteBufCodecs.stringUtf8(MAX_DOCUMENT), CharacterCard::documentNumber,
            ByteBufCodecs.optional(ByteBufCodecs.stringUtf8(MAX_NAME)), CharacterCard::jobLabel,
            CharacterCard::new);

    static final StreamCodec<ByteBuf, ResultCode> RESULT =
            ByteBufCodecs.VAR_INT.map(ResultCode::fromOrdinal, ResultCode::ordinal);
    static final StreamCodec<ByteBuf, CharacterAction> ACTION =
            ByteBufCodecs.VAR_INT.map(CharacterAction::fromOrdinal, CharacterAction::ordinal);

    private static final StreamCodec<ByteBuf, PanelSnapshot.Standing> PANEL_STANDING = StreamCodec.composite(
            ByteBufCodecs.VAR_LONG, PanelSnapshot.Standing::muteUntil,
            ByteBufCodecs.VAR_INT, PanelSnapshot.Standing::warnings,
            ByteBufCodecs.VAR_INT, PanelSnapshot.Standing::notes,
            PanelSnapshot.Standing::new);
    private static final StreamCodec<ByteBuf, PanelSnapshot.PlayerEntry> PANEL_PLAYER = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PanelSnapshot.PlayerEntry::id,
            ByteBufCodecs.stringUtf8(MAX_NAME), PanelSnapshot.PlayerEntry::name,
            ByteBufCodecs.optional(ByteBufCodecs.stringUtf8(2 * MAX_NAME)), PanelSnapshot.PlayerEntry::character,
            ByteBufCodecs.optional(ByteBufCodecs.stringUtf8(2 * MAX_NAME)), PanelSnapshot.PlayerEntry::job,
            ByteBufCodecs.optional(ByteBufCodecs.stringUtf8(4 * MAX_NAME)), PanelSnapshot.PlayerEntry::organization,
            PANEL_STANDING, PanelSnapshot.PlayerEntry::standing,
            PanelSnapshot.PlayerEntry::new);
    static final StreamCodec<ByteBuf, PanelSnapshot> PANEL_SNAPSHOT = StreamCodec.composite(
            PANEL_PLAYER.apply(ByteBufCodecs.list(MAX_LIST)), PanelSnapshot::players,
            PanelSnapshot::new);

    static final StreamCodec<ByteBuf, PageKind> PAGE_KIND =
            ByteBufCodecs.VAR_INT.map(PageKind::fromOrdinal, PageKind::ordinal);
    static final StreamCodec<ByteBuf, PlayerAction> PLAYER_ACTION =
            ByteBufCodecs.VAR_INT.map(PlayerAction::fromOrdinal, PlayerAction::ordinal);
    static final StreamCodec<ByteBuf, PlayerResult> PLAYER_RESULT =
            ByteBufCodecs.VAR_INT.map(PlayerResult::fromOrdinal, PlayerResult::ordinal);
    private static final StreamCodec<ByteBuf, PlayerInfoLine> INFO_LINE = StreamCodec.composite(
            ByteBufCodecs.stringUtf8(MAX_NAME), PlayerInfoLine::labelKey,
            ByteBufCodecs.stringUtf8(4 * MAX_NAME), PlayerInfoLine::value,
            PlayerInfoLine::new);
    static final StreamCodec<ByteBuf, java.util.List<PlayerInfoLine>> INFO_LINES =
            INFO_LINE.apply(ByteBufCodecs.list(PlayerActionResultPayload.MAX_LINES));

    private PayloadCodecs() {}
}
