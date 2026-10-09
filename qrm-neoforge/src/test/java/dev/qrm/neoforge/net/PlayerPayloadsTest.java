package dev.qrm.neoforge.net;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

class PlayerPayloadsTest {
    @Test
    void actionRoundTrips() {
        PlayerActionPayload p = new PlayerActionPayload(PlayerAction.GIVE_ITEM, UUID.randomUUID(), "diamond 16");
        ByteBuf buf = Unpooled.buffer();
        PlayerActionPayload.CODEC.encode(buf, p);
        assertEquals(p, PlayerActionPayload.CODEC.decode(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void oversizedArgumentIsRejectedOnDecode() {
        ByteBuf evil = Unpooled.buffer();
        FriendlyByteBuf f = new FriendlyByteBuf(evil);
        f.writeVarInt(PlayerAction.KICK.ordinal());
        f.writeUUID(UUID.randomUUID());
        f.writeUtf("x".repeat(PlayerActionPayload.MAX_ARG + 1), 1000);
        assertThrows(RuntimeException.class, () -> PlayerActionPayload.CODEC.decode(evil));
    }

    @Test
    void resultWithLinesRoundTrips() {
        PlayerActionResultPayload p = new PlayerActionResultPayload(PlayerAction.VIEW_PERMISSIONS, PlayerResult.OK,
                UUID.randomUUID(), List.of(new PlayerInfoLine("qrm.staff.info.permission", "police.arrest"),
                        new PlayerInfoLine("qrm.staff.info.override_deny", "police.fine")));
        ByteBuf buf = Unpooled.buffer();
        PlayerActionResultPayload.CODEC.encode(buf, p);
        assertEquals(p, PlayerActionResultPayload.CODEC.decode(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void unknownOrdinalsFallBackSafely() {
        assertEquals(PlayerResult.ERROR, PlayerResult.fromOrdinal(999));
        assertEquals(PlayerAction.VIEW_CHARACTER, PlayerAction.fromOrdinal(-1));
    }

    @Test
    void everyActionHasAStaffPlayerNodeExceptHistory() {
        for (PlayerAction a : PlayerAction.values()) {
            if (a == PlayerAction.HISTORY) continue;
            assertEquals("staff.player." + a.name().toLowerCase(java.util.Locale.ROOT), a.node());
        }
        assertEquals("staff.player.teleport_to", PlayerAction.TELEPORT_TO.node());
        assertEquals("staff.logs", PlayerAction.HISTORY.node());
    }

    @Test
    void readOnlyActionsAreTheOnesThatDoNotChangeAnything() {
        assertTrue(PlayerAction.VIEW_CHARACTER.readOnly());
        assertTrue(PlayerAction.VIEW_PERMISSIONS.readOnly());
        assertTrue(PlayerAction.HISTORY.readOnly());
        assertFalse(PlayerAction.BAN.readOnly());
        assertFalse(PlayerAction.HEAL.readOnly());
    }

    @Test
    void pageRoundTrips() {
        PagePayload p = new PagePayload("qrm.staff.log.title", List.of(new PlayerInfoLine("=09/10 14:32 Marco", "panel.ban → Luca"),
                new PlayerInfoLine("qrm.staff.log.empty", "")));
        ByteBuf buf = Unpooled.buffer();
        PagePayload.CODEC.encode(buf, p);
        assertEquals(p, PagePayload.CODEC.decode(buf));
        assertEquals(0, buf.readableBytes());
        assertTrue(p.lines().get(0).literal());
        assertFalse(p.lines().get(1).literal());
    }

    @Test
    void pageRequestRoundTripsForEveryKind() {
        for (PageKind kind : PageKind.values()) {
            ByteBuf buf = Unpooled.buffer();
            PageRequestPayload.CODEC.encode(buf, new PageRequestPayload(kind));
            assertEquals(new PageRequestPayload(kind), PageRequestPayload.CODEC.decode(buf));
        }
    }

    @Test
    void pageKindsKnowTheirNodeAndTitle() {
        assertEquals("staff.logs", PageKind.LOG.node());
        assertEquals("staff.moderation", PageKind.MUTES.node());
        assertEquals("staff.moderation", PageKind.WARNINGS.node());
        for (PageKind k : PageKind.values()) assertTrue(k.titleKey().startsWith("qrm.staff."));
        assertEquals(PageKind.LOG, PageKind.fromOrdinal(99));
    }

    @Test
    void moderationActionsAreWiredLikeTheOthers() {
        assertEquals("staff.player.mute", PlayerAction.MUTE.node());
        assertEquals("staff.player.unmute", PlayerAction.UNMUTE.node());
        assertEquals("staff.player.warn", PlayerAction.WARN.node());
        assertEquals("staff.player.note", PlayerAction.NOTE.node());
        assertTrue(PlayerAction.WARNINGS.readOnly());
        assertTrue(PlayerAction.NOTES.readOnly());
        assertFalse(PlayerAction.MUTE.readOnly());
        assertFalse(PlayerAction.UNMUTE.readOnly());
        assertFalse(PlayerAction.WARN.readOnly());
        assertFalse(PlayerAction.NOTE.readOnly());
    }
}
