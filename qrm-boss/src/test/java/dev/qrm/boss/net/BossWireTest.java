package dev.qrm.boss.net;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.boss.logic.BossAction;
import dev.qrm.boss.logic.BossReply;
import dev.qrm.boss.logic.BossRequest;
import dev.qrm.boss.logic.BossResult;
import dev.qrm.boss.logic.BossSnapshot;
import dev.qrm.boss.logic.BossWire;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Round-trip di ogni pacchetto: il formato è puro Java, quindi si prova senza Minecraft. */
class BossWireTest {
    private static BossSnapshot snapshot(int members) {
        List<BossSnapshot.Member> list = new ArrayList<>();
        for (int i = 0; i < members; i++) {
            list.add(new BossSnapshot.Member(UUID.randomUUID(), "Nome è Cognome " + i, "worker", "Operaio", 10, i % 2 == 0,
                    i % 3 == 0 ? "Capocantiere" : "", i % 2 == 0, i % 2 == 1,
                    UUID.randomUUID(), i % 4, List.of(new BossSnapshot.LogEntry(0, 1_760_000_000_000L, "250.00 $ · Ottimo turno"),
                            new BossSnapshot.LogEntry(1, 1_759_000_000_000L, "Capocantiere"))));
        }
        return new BossSnapshot("acme", "Acme Inc", UUID.randomUUID(), "Boss", 30, true, true, members, 3, 2,
                "Domani chiusi alle 18", "Anna Rossi", 1_760_000_000_000L, true, "USD", list,
                List.of(new BossSnapshot.RankOption("worker", "Operaio", 10), new BossSnapshot.RankOption("boss", "Boss", 30)),
                List.of(new BossSnapshot.Candidate(UUID.randomUUID(), "Zed Last")));
    }

    @Test
    void requestRoundTrips() {
        for (BossAction action : BossAction.values()) {
            BossRequest r = new BossRequest(action, "acme", UUID.randomUUID(), Optional.of(UUID.randomUUID()), "boss",
                    "25", "Ottimo turno", UUID.randomUUID());
            assertEquals(r, BossWire.decodeRequest(BossWire.encode(r)));
        }
        BossRequest noTarget = BossRequest.refresh("acme", UUID.randomUUID());
        assertEquals(noTarget, BossWire.decodeRequest(BossWire.encode(noTarget)));
    }

    @Test
    void replyRoundTripsWithAndWithoutSnapshot() {
        for (BossResult result : BossResult.values()) {
            BossReply reply = new BossReply(result, Optional.empty());
            assertEquals(reply, BossWire.decodeReply(BossWire.encode(reply)));
        }
        BossReply full = new BossReply(BossResult.OK, Optional.of(snapshot(5)));
        assertEquals(full, BossWire.decodeReply(BossWire.encode(full)));
        BossReply none = new BossReply(BossResult.OK, Optional.of(new BossSnapshot("a", "A", UUID.randomUUID(),
                "", -1, false, false, 0, 0, -1, "", "", 0, false, "", List.of(), List.of(), List.of())));
        assertEquals(none, BossWire.decodeReply(BossWire.encode(none)));
    }

    @Test
    void orgListRoundTrips() {
        BossReply.OrgList list = new BossReply.OrgList(UUID.randomUUID(),
                List.of(new BossSnapshot.OrgChoice("acme", "Acme"), new BossSnapshot.OrgChoice("zeta", "Zeta")));
        assertEquals(list, BossWire.decodeOrgList(BossWire.encode(list)));
    }

    @Test
    void oversizedListsAreCappedOnWrite() {
        BossReply reply = BossWire.decodeReply(BossWire.encode(new BossReply(BossResult.OK, Optional.of(snapshot(300)))));
        assertEquals(BossSnapshot.MAX_MEMBERS, reply.snapshot().orElseThrow().members().size());
    }

    @Test
    void unknownResultOrdinalFallsBackToError() {
        assertEquals(BossResult.ERROR, BossResult.fromOrdinal(999));
        assertEquals(BossAction.REFRESH, BossAction.fromOrdinal(-3));
    }

    @Test
    void malformedPacketsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> BossWire.decodeRequest(new byte[] {1, 2, 3}));
        assertThrows(IllegalArgumentException.class, () -> BossWire.decodeReply(new byte[0]));
        byte[] good = BossWire.encode(BossRequest.refresh("acme", UUID.randomUUID()));
        byte[] truncated = java.util.Arrays.copyOf(good, good.length - 4);
        assertThrows(IllegalArgumentException.class, () -> BossWire.decodeRequest(truncated));
    }

    @Test
    void overlongStringsAreRejectedOnRead() {
        // orgId lungo 40 caratteri (il tetto e' 32): lo scrive a mano un client modificato.
        var bytes = new java.io.ByteArrayOutputStream();
        try (var out = new java.io.DataOutputStream(bytes)) {
            out.writeByte(0);
            out.writeUTF("x".repeat(40));
        } catch (java.io.IOException e) {
            throw new AssertionError(e);
        }
        assertThrows(IllegalArgumentException.class, () -> BossWire.decodeRequest(bytes.toByteArray()));
    }
}
