package dev.qrm.neoforge.net;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.jupiter.api.Test;

class SetDutyPayloadTest {
    @Test
    void roundTrips() {
        for (boolean on : new boolean[] {true, false}) {
            ByteBuf buf = Unpooled.buffer();
            SetDutyPayload.CODEC.encode(buf, new SetDutyPayload(on));
            assertEquals(new SetDutyPayload(on), SetDutyPayload.CODEC.decode(buf));
            assertEquals(0, buf.readableBytes());
        }
    }
}
