package dev.qrm.neoforge.net;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.network.codec.StreamCodec;
import org.junit.jupiter.api.Test;

class PlayerStatusTest {
    private static <T> T roundTrip(StreamCodec<ByteBuf, T> codec, T value) {
        ByteBuf buf = Unpooled.buffer();
        codec.encode(buf, value);
        T out = codec.decode(buf);
        assertEquals(0, buf.readableBytes(), "bytes left over");
        return out;
    }

    private static PlayerStatus full() {
        return new PlayerStatus("Anna Rossi", "USD",
                List.of(new PlayerStatus.CurrencyInfo("USD", "$", 2), new PlayerStatus.CurrencyInfo("EUR", "€", 2)),
                List.of(new PlayerStatus.BalanceEntry("USD", 1250), new PlayerStatus.BalanceEntry("EUR", 500)),
                Optional.of(new PlayerStatus.JobInfo("Polizia", "Recluta", true, 1_780_000_000L, true)),
                List.of(new PlayerStatus.OrgEntry("Polizia Distrettuale", "Agente", false),
                        new PlayerStatus.OrgEntry("Gilda", "", true)), UiConfig.DEFAULT);
    }

    @Test
    void payloadRoundTrips() {
        PlayerStatusPayload p = new PlayerStatusPayload(full());
        assertEquals(p, roundTrip(PlayerStatusPayload.CODEC, p));
        PlayerStatusPayload empty = new PlayerStatusPayload(PlayerStatus.EMPTY);
        assertEquals(empty, roundTrip(PlayerStatusPayload.CODEC, empty));
    }

    @Test
    void emptyStatusHasNoCharacter() {
        assertFalse(PlayerStatus.EMPTY.hasCharacter());
        assertTrue(full().hasCharacter());
    }

    @Test
    void formatsKnownAndUnknownCurrencies() {
        PlayerStatus s = full();
        assertEquals("12.50 $", s.format(new PlayerStatus.BalanceEntry("USD", 1250)));
        assertEquals("77 GBP", s.format(new PlayerStatus.BalanceEntry("GBP", 77)));
    }

    @Test
    void invalidCurrencyInfoFallsBackToTheRawAmount() {
        PlayerStatus s = new PlayerStatus("A B", "XX", List.of(new PlayerStatus.CurrencyInfo("XX", "x", 99)),
                List.of(), Optional.empty(), List.of(), UiConfig.DEFAULT);
        assertEquals("5 XX", s.format(new PlayerStatus.BalanceEntry("XX", 5)));
    }

    @Test
    void primaryBalanceIsTheOneInThePrimaryCurrency() {
        assertEquals(Optional.of(new PlayerStatus.BalanceEntry("USD", 1250)), full().primaryBalance());
        assertEquals(Optional.empty(), PlayerStatus.EMPTY.primaryBalance());
    }

    @Test
    void tooManyBalancesAreRejectedByTheCodec() {
        List<PlayerStatus.BalanceEntry> many = new ArrayList<>();
        for (int i = 0; i <= PlayerStatus.MAX_BALANCES; i++) many.add(new PlayerStatus.BalanceEntry("C" + i + "X", i));
        PlayerStatus s = new PlayerStatus("A B", "", List.of(), many, Optional.empty(), List.of(), UiConfig.DEFAULT);
        assertThrows(RuntimeException.class, () -> roundTrip(PlayerStatus.CODEC, s));
    }

    @Test
    void tooLongTextIsRejectedByTheCodec() {
        PlayerStatus s = new PlayerStatus("A".repeat(PlayerStatus.MAX_FULL_NAME + 1), "", List.of(), List.of(),
                Optional.empty(), List.of(), UiConfig.DEFAULT);
        assertThrows(RuntimeException.class, () -> roundTrip(PlayerStatus.CODEC, s));
    }

    @Test
    void tooManyCurrencyInfosAreRejectedByTheCodec() {
        List<PlayerStatus.CurrencyInfo> many = new ArrayList<>();
        for (int i = 0; i <= PlayerStatus.MAX_CURRENCIES; i++) many.add(new PlayerStatus.CurrencyInfo("C" + i + "X", "c", 2));
        PlayerStatus s = new PlayerStatus("A B", "", many, List.of(), Optional.empty(), List.of(), UiConfig.DEFAULT);
        assertThrows(RuntimeException.class, () -> roundTrip(PlayerStatus.CODEC, s));
    }

    @Test
    void tooManyOrgsAreRejectedByTheCodec() {
        List<PlayerStatus.OrgEntry> many = new ArrayList<>();
        for (int i = 0; i <= PlayerStatus.MAX_ORGS; i++) many.add(new PlayerStatus.OrgEntry("Org" + i, "", false));
        PlayerStatus s = new PlayerStatus("A B", "", List.of(), List.of(), Optional.empty(), many, UiConfig.DEFAULT);
        assertThrows(RuntimeException.class, () -> roundTrip(PlayerStatus.CODEC, s));
    }

    @Test
    void tooLongCurrencySymbolIsRejectedByTheCodec() {
        PlayerStatus s = new PlayerStatus("A B", "", List.of(
                new PlayerStatus.CurrencyInfo("USD", "S".repeat(PlayerStatus.MAX_SYMBOL + 1), 2)), List.of(),
                Optional.empty(), List.of(), UiConfig.DEFAULT);
        assertThrows(RuntimeException.class, () -> roundTrip(PlayerStatus.CODEC, s));
    }

    @Test
    void tooLongJobLabelIsRejectedByTheCodec() {
        PlayerStatus s = new PlayerStatus("A B", "", List.of(), List.of(),
                Optional.of(new PlayerStatus.JobInfo("J".repeat(PlayerStatus.MAX_TEXT + 1), "g", false, 0L, true)),
                List.of(), UiConfig.DEFAULT);
        assertThrows(RuntimeException.class, () -> roundTrip(PlayerStatus.CODEC, s));
    }

    @Test
    void uiConfigTravelsWithTheStatus() {
        PlayerStatus base = full();
        PlayerStatus s = new PlayerStatus(base.characterName(), base.primaryCurrency(), base.currencies(),
                base.balances(), base.job(), base.orgs(), UiConfig.of(List.of("job"), List.of("name", "job"), false));
        PlayerStatus back = roundTrip(PlayerStatus.CODEC, s);
        assertEquals(s, back);
        assertEquals(List.of("job"), back.ui().hubTabs());
        assertFalse(back.ui().selfDuty());
    }
}
