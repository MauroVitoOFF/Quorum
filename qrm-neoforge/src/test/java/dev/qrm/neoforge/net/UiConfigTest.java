package dev.qrm.neoforge.net;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.client.api.hud.HudRow;
import dev.qrm.client.api.hud.HudRows;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class UiConfigTest {
    @Test
    void defaultsShowEverythingAndAllowSelfDuty() {
        assertEquals(List.of("accounts", "job", "orgs"), UiConfig.DEFAULT.hubTabs());
        assertEquals(List.of("name", "balance", "job"), UiConfig.DEFAULT.hudElements());
        assertTrue(UiConfig.DEFAULT.selfDuty());
    }

    @Test
    void ofKeepsOrderAndDropsDuplicatesUnknownAndNonStrings() {
        UiConfig c = UiConfig.of(List.of("job", "accounts", "JOB", "bogus", 5), List.of("Balance", "name", "name", "x"), false);
        assertEquals(List.of("job", "accounts"), c.hubTabs());
        assertEquals(List.of("balance", "name"), c.hudElements());
        assertFalse(c.selfDuty());
    }

    @Test
    void emptyListsStayEmpty() {
        UiConfig c = UiConfig.of(List.of(), List.of("nothing"), true);
        assertEquals(List.of(), c.hubTabs());
        assertEquals(List.of(), c.hudElements());
    }

    @Test
    void hasTabAndHasHudReflectTheLists() {
        UiConfig c = UiConfig.of(List.of("job"), List.of("name"), true);
        assertTrue(c.hasTab("job"));
        assertFalse(c.hasTab("orgs"));
        assertTrue(c.hasHud("name"));
        assertFalse(c.hasHud("balance"));
    }

    @Test
    void codecRoundTrips() {
        UiConfig c = UiConfig.of(List.of("orgs", "job"), List.of("job"), false);
        ByteBuf buf = Unpooled.buffer();
        UiConfig.CODEC.encode(buf, c);
        assertEquals(c, UiConfig.CODEC.decode(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void codecRejectsTooManyNamesAndTooLongNames() {
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 9; i++) many.add("t" + i);
        UiConfig tooMany = new UiConfig(many, List.of(), true);
        assertThrows(RuntimeException.class, () -> UiConfig.CODEC.encode(Unpooled.buffer(), tooMany));
        UiConfig tooLong = new UiConfig(List.of("x".repeat(17)), List.of(), true);
        assertThrows(RuntimeException.class, () -> UiConfig.CODEC.encode(Unpooled.buffer(), tooLong));
    }

    @Test
    void moduleRowIdsAreKeptInConfiguredOrder() {
        UiConfig c = UiConfig.of(List.of(), List.of("Bank:Debt", "name", "bank:debt", "mod:rep", "balance"), true);
        assertEquals(List.of("bank:debt", "name", "mod:rep", "balance"), c.hudElements());
    }

    @Test
    void malformedModuleRowIdsAreDropped() {
        UiConfig c = UiConfig.of(List.of(), List.of("a:", ":b", "a b:c", "x:" + "y".repeat(48), "ok:fine"), true);
        assertEquals(List.of("ok:fine"), c.hudElements());
    }

    @Test
    void codecCarriesSixteenLongHudIds() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 16; i++) ids.add(("m" + i + ":" + "r".repeat(46)).substring(0, 48));
        UiConfig c = UiConfig.of(List.of(), ids, true);
        assertEquals(16, c.hudElements().size());
        ByteBuf buf = Unpooled.buffer();
        UiConfig.CODEC.encode(buf, c);
        assertEquals(c, UiConfig.CODEC.decode(buf));
    }

    @Test
    void codecRejectsMoreThanSixteenHudElements() {
        List<String> ids = new ArrayList<>();
        for (int i = 0; i < 17; i++) ids.add("m" + i + ":r");
        UiConfig c = new UiConfig(List.of(), ids, true);
        assertThrows(RuntimeException.class, () -> UiConfig.CODEC.encode(Unpooled.buffer(), c));
    }

    @Test
    void configAndRegistryAgreeOnWhatAModuleRowIdIs() {
        HudRow row = Optional::empty;
        for (String id : new String[] {"agree:debt", "agree.b:c_d/e-f", "m:" + "x".repeat(46)}) {
            assertEquals(List.of(id), UiConfig.of(List.of(), List.of(id), true).hudElements(), id);
            HudRows.register(id, row); // il registro accetta la stessa forma
        }
        for (String id : new String[] {"a:", "x:y z", "x:" + "y".repeat(47)}) {
            assertEquals(List.of(), UiConfig.of(List.of(), List.of(id), true).hudElements(), id);
            assertThrows(IllegalArgumentException.class, () -> HudRows.register(id, row), id);
        }
    }
}
