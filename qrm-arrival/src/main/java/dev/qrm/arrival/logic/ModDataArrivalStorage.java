package dev.qrm.arrival.logic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qrm.api.data.ModDataService;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.id.CharacterId;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;

/** I contrassegni stanno nei dati dei mod di QRM: namespace {@code qrm_arrival}, chiavi {@code arrived} e {@code installed}. */
public final class ModDataArrivalStorage implements ArrivalStorage {
    public static final String NAMESPACE = "qrm_arrival";
    public static final String ARRIVED = "arrived";
    public static final String INSTALLED = "installed";

    private final ModDataService data;
    private final Clock clock;
    private final Instant installedAt;

    public ModDataArrivalStorage(ModDataService data, Clock clock) {
        this.data = data;
        this.clock = clock;
        this.installedAt = readOrWriteInstalled();
    }

    private Instant readOrWriteInstalled() {
        Optional<Instant> stored = data.get(NAMESPACE, OwnerRef.system(), INSTALLED).flatMap(ModDataArrivalStorage::parseAt);
        if (stored.isPresent()) return stored.get();
        Instant now = clock.instant();
        data.put(NAMESPACE, OwnerRef.system(), INSTALLED, atJson(now));
        return now;
    }

    @Override
    public boolean hasArrived(CharacterId character) {
        return data.get(NAMESPACE, OwnerRef.character(character), ARRIVED).isPresent();
    }

    @Override
    public void markArrived(CharacterId character) {
        data.put(NAMESPACE, OwnerRef.character(character), ARRIVED, atJson(clock.instant()));
    }

    @Override
    public Instant installedAt() {
        return installedAt;
    }

    private static String atJson(Instant at) {
        JsonObject o = new JsonObject();
        o.addProperty("at", at.toString());
        return o.toString();
    }

    private static Optional<Instant> parseAt(String raw) {
        try {
            return Optional.of(Instant.parse(JsonParser.parseString(raw).getAsJsonObject().get("at").getAsString()));
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }
}
