package dev.qrm.locker.logic;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

class LockerSlotsTest {
    private static JsonObject item(String id) {
        JsonObject o = new JsonObject();
        o.addProperty("id", id);
        return o;
    }

    @Test
    void roundTripSkipsEmptySlots() throws Exception {
        LockerSlots slots = LockerSlots.empty();
        slots.put(0, item("minecraft:stone"));
        slots.put(5, item("minecraft:apple"));
        String json = slots.toJson();
        assertEquals(2, JsonParser.parseString(json).getAsJsonObject().getAsJsonArray("slots").size());
        LockerSlots back = LockerSlots.fromJson(json);
        assertEquals(slots.items(), back.items());
        assertTrue(back.unreadable().isEmpty());
        assertFalse(JsonParser.parseString(json).getAsJsonObject().has("unreadable"));
    }

    @Test
    void unreadableIsKeptVerbatim() throws Exception {
        LockerSlots slots = LockerSlots.empty();
        slots.put(1, item("minecraft:stone"));
        slots.addUnreadable(3, item("gone_mod:thing"));
        LockerSlots back = LockerSlots.fromJson(slots.toJson());
        assertEquals(1, back.unreadable().size());
        assertEquals(slots.toJson(), back.toJson());
        assertEquals(3, back.unreadable().get(0).get("slot").getAsInt());
    }

    @Test
    void unknownVersionIsRejected() {
        assertThrows(LockerDocumentException.class, () -> LockerSlots.fromJson("{\"version\":2,\"slots\":[]}"));
    }

    @Test
    void corruptDocumentsAreRejected() {
        for (String bad : new String[] {"", "not json", "[]", "{}", "{\"version\":1}",
                "{\"version\":1,\"slots\":[{\"slot\":99,\"item\":{}}]}",
                "{\"version\":1,\"slots\":[{\"slot\":1,\"item\":{}},{\"slot\":1,\"item\":{}}]}",
                "{\"version\":1,\"slots\":[{\"slot\":1}]}"}) {
            assertThrows(LockerDocumentException.class, () -> LockerSlots.fromJson(bad), bad);
        }
    }

    @Test
    void slotOutOfRangeIsRejectedOnPut() {
        assertThrows(IllegalArgumentException.class, () -> LockerSlots.empty().put(27, item("x:y")));
    }
}
