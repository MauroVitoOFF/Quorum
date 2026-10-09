package dev.qrm.client.api.hud;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Registro delle righe dell'HUD dei moduli. Un id ha la forma {@code spazio:nome}; una riga compare solo se il server
 * la elenca in {@code gui.hud.elements}.
 */
public final class HudRows {
    public static final int MAX_ID_LENGTH = 48;
    private static final Pattern ID = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");
    private static final Map<String, HudRow> ROWS = new ConcurrentHashMap<>();

    private HudRows() {}

    /**
     * @throws IllegalArgumentException se l'id è malformato o più lungo di {@link #MAX_ID_LENGTH}
     * @throws IllegalStateException se l'id è già registrato
     */
    public static void register(String id, HudRow row) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(row, "row");
        if (id.length() > MAX_ID_LENGTH || !ID.matcher(id).matches()) {
            throw new IllegalArgumentException("invalid HUD row id: " + id);
        }
        if (ROWS.putIfAbsent(id, row) != null) throw new IllegalStateException("HUD row already registered: " + id);
    }

    public static Optional<HudRow> find(String id) {
        return Optional.ofNullable(ROWS.get(id));
    }
}
