package dev.qrm.wardrobe.client;

import dev.qrm.wardrobe.logic.WardrobeWire;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Ciò che il client sa: il catalogo ricevuto dal server e chi indossa che cosa. */
public final class ClientOutfits {
    private static final Map<String, WardrobeWire.CatalogEntry> CATALOG = new LinkedHashMap<>();
    private static volatile Map<UUID, String> worn = Map.of();
    private static volatile int generation;

    private ClientOutfits() {}

    /** Cambia a ogni modifica del catalogo o di chi indossa che cosa: serve a invalidare le texture. */
    public static int generation() {
        return generation;
    }

    public static synchronized void onCatalog(int total, int index, WardrobeWire.CatalogEntry entry) {
        if (total == 0 || index == 0) CATALOG.clear();
        if (total > 0 && entry != null) CATALOG.put(entry.id(), entry);
        generation++;
    }

    public static synchronized Optional<WardrobeWire.CatalogEntry> entry(String id) {
        return Optional.ofNullable(CATALOG.get(id));
    }

    public static synchronized List<WardrobeWire.CatalogEntry> all() {
        return new ArrayList<>(CATALOG.values());
    }

    public static void setWorn(Map<UUID, String> map) {
        worn = Map.copyOf(map);
        generation++;
    }

    public static Optional<String> wornBy(UUID player) {
        return Optional.ofNullable(worn.get(player));
    }
}
