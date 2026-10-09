package dev.qrm.neoforge.net;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.network.codec.StreamCodec;
import org.junit.jupiter.api.Test;

class PayloadCodecsTest {
    private static <T> T roundTrip(StreamCodec<ByteBuf, T> codec, T value) {
        ByteBuf buf = Unpooled.buffer();
        codec.encode(buf, value);
        T out = codec.decode(buf);
        assertEquals(0, buf.readableBytes(), "bytes left over");
        return out;
    }

    private static CharacterCard card(String first) {
        UUID id = UUID.randomUUID();
        return new CharacterCard(id, first, "Rossi", LocalDate.of(1990, 3, 12), CharacterCard.documentNumberOf(id),
                Optional.of("Polizia"));
    }

    @Test
    void rosterRoundTrip() {
        CharacterRosterPayload p = new CharacterRosterPayload(List.of(card("Anna"), card("Bruno")), 3,
                Optional.of(UUID.randomUUID()), true);
        assertEquals(p, roundTrip(CharacterRosterPayload.CODEC, p));
        CharacterRosterPayload empty = new CharacterRosterPayload(List.of(), 0, Optional.empty(), false);
        assertEquals(empty, roundTrip(CharacterRosterPayload.CODEC, empty));
    }

    private static CharacterRosterPayload rosterOf(int n) {
        List<CharacterCard> cards = new java.util.ArrayList<>();
        for (int i = 0; i < n; i++) cards.add(card("Anna"));
        return new CharacterRosterPayload(cards, n, Optional.empty(), false);
    }

    @Test
    void rosterWithFiftyAndMaxCardsRoundTrips() {
        CharacterRosterPayload fifty = rosterOf(50);
        assertEquals(fifty, roundTrip(CharacterRosterPayload.CODEC, fifty));
        CharacterRosterPayload max = rosterOf(CharacterCard.MAX_ROSTER_CARDS);
        assertEquals(max, roundTrip(CharacterRosterPayload.CODEC, max));
    }

    @Test
    void rosterAboveMaxFailsToEncode() {
        CharacterRosterPayload tooMany = rosterOf(CharacterCard.MAX_ROSTER_CARDS + 1);
        assertThrows(RuntimeException.class, () -> CharacterRosterPayload.CODEC.encode(Unpooled.buffer(), tooMany));
    }

    @Test
    void resultAndRequestsRoundTrip() {
        CharacterResultPayload r = new CharacterResultPayload(CharacterAction.CREATE, ResultCode.SLOTS_FULL);
        assertEquals(r, roundTrip(CharacterResultPayload.CODEC, r));
        UUID id = UUID.randomUUID();
        assertEquals(new SelectCharacterPayload(id), roundTrip(SelectCharacterPayload.CODEC, new SelectCharacterPayload(id)));
        assertEquals(new ArchiveCharacterPayload(id), roundTrip(ArchiveCharacterPayload.CODEC, new ArchiveCharacterPayload(id)));
        CreateCharacterPayload c = new CreateCharacterPayload("Anna", "Rossi", 1990, 3, 12);
        assertEquals(c, roundTrip(CreateCharacterPayload.CODEC, c));
        assertEquals(new RosterRequestPayload(), roundTrip(RosterRequestPayload.CODEC, new RosterRequestPayload()));
    }

    @Test
    void oversizedNameIsRejected() {
        CreateCharacterPayload tooLong = new CreateCharacterPayload("x".repeat(65), "Rossi", 1990, 3, 12);
        assertThrows(RuntimeException.class, () -> roundTrip(CreateCharacterPayload.CODEC, tooLong));
    }

    @Test
    void unknownResultOrdinalDecodesToError() {
        ByteBuf buf = Unpooled.buffer();
        net.minecraft.network.codec.ByteBufCodecs.VAR_INT.encode(buf, CharacterAction.CREATE.ordinal());
        net.minecraft.network.codec.ByteBufCodecs.VAR_INT.encode(buf, 99);
        assertEquals(ResultCode.ERROR, CharacterResultPayload.CODEC.decode(buf).code());
    }
}
