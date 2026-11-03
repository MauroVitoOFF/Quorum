package dev.qrm.clock.mc;

import java.time.ZoneId;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Configurazione lato server del tempo di gioco. Le modifiche si applicano senza riavvio. */
public final class ClockConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.ConfigValue<Integer> DAY_MINUTES;
    public static final ModConfigSpec.ConfigValue<String> TIMEZONE;
    public static final ModConfigSpec.BooleanValue SOUTHERN_HEMISPHERE;
    public static final ModConfigSpec.BooleanValue CONTROL_WORLD_TIME;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("clock");
        DAY_MINUTES = b.comment("Minuti reali che dura un giorno di gioco. Deve dividere 1440 (10, 12, 15, 20, 24, 30, 36, 40, 45, 48, 60, 72, 90, 120, 180): cosi' a mezzanotte reale comincia un ciclo")
                .define("dayMinutes", 60, o -> o instanceof Integer i && i >= 10 && i <= 180 && 1440 % i == 0);
        TIMEZONE = b.comment("Fuso orario della data di gioco, per esempio Europe/Rome. Vuoto = quello del server")
                .define("timezone", "", o -> o instanceof String s && (s.isEmpty() || ZoneId.getAvailableZoneIds().contains(s)));
        SOUTHERN_HEMISPHERE = b.comment("Se true, le stagioni sono quelle dell'emisfero sud (inverno a giugno)")
                .define("southernHemisphere", false);
        CONTROL_WORLD_TIME = b.comment("Se false, il modulo non tocca il sole e la luna del mondo e fornisce solo data, ora e stagione")
                .define("controlWorldTime", true);
        b.pop();
        SPEC = b.build();
    }

    private ClockConfig() {}

    static ZoneId zone() {
        String id = TIMEZONE.get();
        return id.isEmpty() ? ZoneId.systemDefault() : ZoneId.of(id);
    }
}
