package dev.qrm.locker.logic;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.point.InteractionPoint;
import dev.qrm.api.point.PointOwner;
import dev.qrm.api.point.PointPosition;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LockerFlowTest {
    private static final class FakeStorage implements LockerStorage {
        final Map<String, String> docs = new HashMap<>();
        int writes;

        @Override
        public Optional<String> read(CharacterId c, String key) {
            return Optional.ofNullable(docs.get(c.value() + "/" + key));
        }

        @Override
        public void write(CharacterId c, String key, String json) {
            writes++;
            docs.put(c.value() + "/" + key, json);
        }
    }

    private static final class FakeWorld implements LockerWorld {
        final Map<Integer, InteractionPoint> points = new HashMap<>();
        boolean allowed = true;
        boolean active = true;

        @Override
        public Optional<InteractionPoint> point(int id) {
            return Optional.ofNullable(points.get(id));
        }

        @Override
        public boolean canUse(CharacterId c, InteractionPoint p) {
            return allowed;
        }

        @Override
        public boolean isActive(PlayerId p, CharacterId c) {
            return active;
        }
    }

    private final FakeStorage storage = new FakeStorage();
    private final FakeWorld world = new FakeWorld();
    private final LockerFlow flow = new LockerFlow(storage, world);
    private final PlayerId player = new PlayerId(UUID.randomUUID());
    private final CharacterId character = CharacterId.random();

    private InteractionPoint point(int id, PointOwner owner, int x) {
        InteractionPoint p = new InteractionPoint(id, "locker", owner,
                new PointPosition("minecraft:overworld", x, 64, 0), null, false, null, null, Instant.EPOCH);
        world.points.put(id, p);
        return p;
    }

    private static LockerSlots one(String id) {
        LockerSlots s = LockerSlots.empty();
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        s.put(2, o);
        return s;
    }

    @Test
    void newLockerOpensEmptyAndIsNotWrittenIfUntouched() {
        var r = flow.open(player, character, point(1, PointOwner.job("police"), 0));
        assertTrue(r.ok());
        assertTrue(r.session().loaded().items().isEmpty());
        r.session().bind(LockerSlots::empty);
        flow.close(r.session());
        assertEquals(0, storage.writes);
        assertFalse(flow.isOpen(player));
    }

    @Test
    void existingContentIsLoaded() {
        point(1, PointOwner.job("police"), 0);
        storage.docs.put(character.value() + "/locker.job.police", one("minecraft:stone").toJson());
        var r = flow.open(player, character, world.points.get(1));
        assertEquals(1, r.session().loaded().items().size());
    }

    @Test
    void changesAreSavedOnCloseAndOnFlush() {
        var s = flow.open(player, character, point(1, PointOwner.job("police"), 0)).session();
        s.bind(() -> one("minecraft:apple"));
        s.markDirty();
        flow.flushDirty();
        assertEquals(1, storage.writes);
        flow.flushDirty();
        assertEquals(1, storage.writes, "niente da scrivere se non e' cambiato");
        s.markDirty();
        flow.close(s);
        assertEquals(2, storage.writes);
        assertTrue(storage.docs.get(character.value() + "/locker.job.police").contains("minecraft:apple"));
    }

    @Test
    void closeAllWritesDirtySessions() {
        var s = flow.open(player, character, point(1, PointOwner.job("police"), 0)).session();
        s.bind(() -> one("minecraft:apple"));
        s.markDirty();
        flow.closeAll();
        assertEquals(1, storage.writes);
        assertFalse(flow.isOpen(player));
    }

    @Test
    void secondOpenIsRefusedWhileOneIsOpen() {
        var first = flow.open(player, character, point(1, PointOwner.job("police"), 0));
        var second = flow.open(player, character, point(2, PointOwner.job("police"), 5));
        assertEquals(LockerFlow.Failure.ALREADY_OPEN, second.failure());
        flow.close(first.session());
        assertTrue(flow.open(player, character, world.points.get(2)).ok());
    }

    @Test
    void pointsOfTheSameOwnerShareTheLockerOthersDoNot() {
        var a = flow.open(player, character, point(1, PointOwner.job("police"), 0)).session();
        a.bind(() -> one("minecraft:apple"));
        a.markDirty();
        flow.close(a);
        var same = flow.open(player, character, point(2, PointOwner.job("police"), 9)).session();
        assertEquals(1, same.loaded().items().size());
        flow.close(same);
        var other = flow.open(player, character, point(3, PointOwner.job("medic"), 0)).session();
        assertTrue(other.loaded().items().isEmpty());
    }

    @Test
    void corruptDocumentIsNotOpenedNorOverwritten() {
        point(1, PointOwner.job("police"), 0);
        String key = character.value() + "/locker.job.police";
        storage.docs.put(key, "{broken");
        var r = flow.open(player, character, world.points.get(1));
        assertEquals(LockerFlow.Failure.CORRUPT, r.failure());
        assertFalse(flow.isOpen(player));
        flow.closeAll();
        assertEquals("{broken", storage.docs.get(key));
        assertEquals(0, storage.writes);
    }

    @Test
    void sessionBecomesInvalidWhenAccessIsLost() {
        var s = flow.open(player, character, point(1, PointOwner.job("police"), 0)).session();
        assertTrue(s.stillValid(true));
        assertFalse(s.stillValid(false));
        world.active = false;
        assertFalse(s.stillValid(true));
        world.active = true;
        world.allowed = false;
        assertFalse(s.stillValid(true));
        world.allowed = true;
        world.points.remove(1);
        assertFalse(s.stillValid(true));
    }

    @Test
    void movedPointInvalidatesTheSession() {
        var s = flow.open(player, character, point(1, PointOwner.job("police"), 0)).session();
        point(1, PointOwner.job("police"), 40);
        assertFalse(s.stillValid(true));
    }

    @Test
    void renamingThePointKeepsTheSessionValid() {
        var s = flow.open(player, character, point(1, PointOwner.job("police"), 0)).session();
        InteractionPoint p = world.points.get(1);
        world.points.put(1, new InteractionPoint(1, p.typeId(), p.owner(), p.position(), "grade", true, "Nome", null,
                Instant.EPOCH));
        assertTrue(s.stillValid(true));
    }

    @Test
    void failedWriteKeepsTheSessionDirtyForRetry() {
        var failing = new LockerStorage() {
            boolean fail = true;
            int writes;

            @Override
            public Optional<String> read(CharacterId c, String k) {
                return Optional.empty();
            }

            @Override
            public void write(CharacterId c, String k, String json) {
                if (fail) throw new IllegalStateException("db down");
                writes++;
            }
        };
        var f = new LockerFlow(failing, world);
        var s = f.open(player, character, point(1, PointOwner.job("police"), 0)).session();
        s.bind(() -> one("minecraft:apple"));
        s.markDirty();
        f.flushDirty();
        assertTrue(s.dirty());
        failing.fail = false;
        f.flushDirty();
        assertEquals(1, failing.writes);
    }
}
