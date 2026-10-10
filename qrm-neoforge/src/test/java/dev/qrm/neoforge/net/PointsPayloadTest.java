package dev.qrm.neoforge.net;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class PointsPayloadTest {
    @Test
    void roundTrips() {
        PointsPayload p = new PointsPayload(List.of(
                new PointEntry(1, "duty", "minecraft:overworld", 10, -64, -300, "qrm.point.duty", Optional.of("Centrale"), Optional.of("north")),
                new PointEntry(2, "duty", "minecraft:the_nether", 0, 70, 5, "qrm.point.duty", Optional.empty(), Optional.empty())));
        ByteBuf buf = Unpooled.buffer();
        PointsPayload.CODEC.encode(buf, p);
        assertEquals(p, PointsPayload.CODEC.decode(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void emptyListRoundTrips() {
        ByteBuf buf = Unpooled.buffer();
        PointsPayload.CODEC.encode(buf, new PointsPayload(List.of()));
        assertTrue(PointsPayload.CODEC.decode(buf).points().isEmpty());
    }

    @Test
    void oversizedListIsRejectedByTheCodec() {
        List<PointEntry> many = new ArrayList<>();
        for (int i = 0; i <= PointsPayload.MAX_POINTS; i++) {
            many.add(new PointEntry(i, "duty", "minecraft:overworld", i, 0, 0, "k", Optional.empty(), Optional.empty()));
        }
        assertThrows(RuntimeException.class,
                () -> PointsPayload.CODEC.encode(Unpooled.buffer(), new PointsPayload(many)));
    }

    @Test
    void protocolVersionWasBumped() {
        assertEquals("12", QrmNetwork.PROTOCOL_VERSION);
    }
}
