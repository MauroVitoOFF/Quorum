package dev.qrm.neoforge.net;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import java.util.List;
import org.junit.jupiter.api.Test;

class StaffAccessPayloadTest {
    @Test
    void roundTrips() {
        StaffAccessPayload p = new StaffAccessPayload(false, List.of("staff.jobs", "staff.module.*"));
        ByteBuf buf = Unpooled.buffer();
        StaffAccessPayload.CODEC.encode(buf, p);
        assertEquals(p, StaffAccessPayload.CODEC.decode(buf));
        assertEquals(0, buf.readableBytes());
    }

    @Test
    void operatorFlagRoundTrips() {
        StaffAccessPayload p = new StaffAccessPayload(true, List.of());
        ByteBuf buf = Unpooled.buffer();
        StaffAccessPayload.CODEC.encode(buf, p);
        assertEquals(p, StaffAccessPayload.CODEC.decode(buf));
    }
}
