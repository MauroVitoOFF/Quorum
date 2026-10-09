package dev.qrm.neoforge.net;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PanelPayloadsTest {
    @Test
    void snapshotRoundTrips() {
        PanelSnapshot s = new PanelSnapshot(List.of(
                new PanelSnapshot.PlayerEntry(UUID.randomUUID(), "Anna", Optional.of("Anna Rossi"),
                        Optional.of("Polizia"), Optional.of("Club, Altro")),
                new PanelSnapshot.PlayerEntry(UUID.randomUUID(), "Bob", Optional.empty(), Optional.empty(),
                        Optional.empty())));
        ByteBuf buf = Unpooled.buffer();
        PanelSnapshotPayload.CODEC.encode(buf, new PanelSnapshotPayload(s));
        assertEquals(s, PanelSnapshotPayload.CODEC.decode(buf).snapshot());
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void emptySnapshotRoundTrips() {
        ByteBuf buf = Unpooled.buffer();
        PanelSnapshotPayload.CODEC.encode(buf, new PanelSnapshotPayload(PanelSnapshot.EMPTY));
        assertTrue(PanelSnapshotPayload.CODEC.decode(buf).snapshot().players().isEmpty());
    }

    @Test
    void requestRoundTrips() {
        ByteBuf buf = Unpooled.buffer();
        PanelRequestPayload.CODEC.encode(buf, new PanelRequestPayload());
        assertEquals(new PanelRequestPayload(), PanelRequestPayload.CODEC.decode(buf));
    }

    @Test
    void standingRoundTrips() {
        PanelSnapshot s = new PanelSnapshot(List.of(
                new PanelSnapshot.PlayerEntry(UUID.randomUUID(), "Anna", Optional.empty(), Optional.empty(), Optional.empty(),
                        new PanelSnapshot.Standing(-1, 3, 7)),
                new PanelSnapshot.PlayerEntry(UUID.randomUUID(), "Bob", Optional.empty(), Optional.empty(), Optional.empty(),
                        new PanelSnapshot.Standing(1_800_000_000_000L, 0, 0))));
        ByteBuf buf = Unpooled.buffer();
        PanelSnapshotPayload.CODEC.encode(buf, new PanelSnapshotPayload(s));
        assertEquals(s, PanelSnapshotPayload.CODEC.decode(buf).snapshot());
        assertEquals(0, buf.readableBytes());
    }
}
