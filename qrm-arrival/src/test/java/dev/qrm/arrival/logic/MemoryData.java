package dev.qrm.arrival.logic;

import dev.qrm.api.data.ModDataService;
import dev.qrm.api.economy.OwnerRef;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/** ModDataService in memoria per i test. */
final class MemoryData implements ModDataService {
    final Map<String, String> map = new HashMap<>();

    private static String k(String ns, OwnerRef o, String key) {
        return ns + "/" + o.type() + "/" + o.id() + "/" + key;
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
