package dev.qrm.clock.mc;

import java.time.ZoneId;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Configurazione lato server del calendario di gioco. Le modifiche si applicano senza riavvio. */
public final class ClockConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<String> TIMEZONE;
    public static final ModConfigSpec.BooleanValue SOUTHERN_HEMISPHERE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("clock");
        TIMEZONE = b.comment("Fuso orario di ora e data, per esempio Europe/Rome. Vuoto = quello del server")
                .define("timezone", "", o -> o instanceof String s && (s.isEmpty() || ZoneId.getAvailableZoneIds().contains(s)));
        SOUTHERN_HEMISPHERE = b.comment("Se true, le stagioni sono quelle dell'emisfero sud (inverno a giugno)")
                .define("southernHemisphere", false);
        b.pop();
        SPEC = b.build();
    }

    private ClockConfig() {}

    static ZoneId zone() {
        String id = TIMEZONE.get();
        return id.isEmpty() ? ZoneId.systemDefault() : ZoneId.of(id);
    }
}
