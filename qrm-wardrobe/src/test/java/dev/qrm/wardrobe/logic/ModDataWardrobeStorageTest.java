package dev.qrm.wardrobe.logic;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.data.ModDataService;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.id.CharacterId;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ModDataWardrobeStorageTest {
    static final class MemoryData implements ModDataService {
        final Map<String, String> map = new HashMap<>();

        private static String k(String ns, OwnerRef o, String key) {
            return ns + "/" + o.id() + "/" + key;
        }

        @Override
        public void put(String ns, OwnerRef o, String key, String json) {
            map.put(k(ns, o, key), json);
        }

        @Override
        public Optional<String> get(String ns, OwnerRef o, String key) {
            return Optional.ofNullable(map.get(k(ns, o, key)));
        }

        @Override
        public void delete(String ns, OwnerRef o, String key) {
            map.remove(k(ns, o, key));
        }

        @Override
        public int purgeNamespace(String ns) {
            return 0;
        }
    }

    final CharacterId anna = new CharacterId(UUID.randomUUID());
    final MemoryData data = new MemoryData();
    final ModDataWardrobeStorage storage = new ModDataWardrobeStorage(data);

    @Test
    void writesAndReadsBack() {
        storage.write(anna, "patrol");
        assertEquals(Optional.of("patrol"), storage.read(anna));
        storage.write(anna, null);
        assertTrue(storage.read(anna).isEmpty());
        assertTrue(data.map.isEmpty());
    }

    @Test
    void corruptOrEmptyDocumentsReadAsNothing() {
        OwnerRef owner = OwnerRef.character(anna);
        data.put(ModDataWardrobeStorage.NAMESPACE, owner, ModDataWardrobeStorage.KEY, "{not json");
        assertTrue(storage.read(anna).isEmpty());
        data.put(ModDataWardrobeStorage.NAMESPACE, owner, ModDataWardrobeStorage.KEY, "{\"outfit\":\"\"}");
        assertTrue(storage.read(anna).isEmpty());
        data.put(ModDataWardrobeStorage.NAMESPACE, owner, ModDataWardrobeStorage.KEY, "[]");
        assertTrue(storage.read(anna).isEmpty());
    }
}
