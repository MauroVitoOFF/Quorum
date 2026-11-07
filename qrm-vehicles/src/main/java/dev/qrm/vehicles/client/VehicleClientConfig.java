package dev.qrm.vehicles.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Impostazioni del client sulla visuale a bordo, modificabili in gioco (tasto "Impostazioni visuale" o Mods →
 * QRM Vehicles → Config). Gli scostamenti sono in unità del modello (16 = 1 blocco) rispetto al sedile e valgono
 * solo per il giocatore locale in prima persona.
 */
public final class VehicleClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue VIEW_RIGHT;
    public static final ModConfigSpec.IntValue VIEW_UP;
    public static final ModConfigSpec.IntValue VIEW_FORWARD;
    public static final ModConfigSpec.BooleanValue SHOW_GLASS_INSIDE;
    public static final ModConfigSpec.BooleanValue PARTICLES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("view");
        VIEW_RIGHT = b.comment("Sposta la visuale a destra (+) o a sinistra (-), in sedicesimi di blocco.")
                .translation("qrm_vehicles.configuration.viewRight").defineInRange("right", 0, -12, 12);
        VIEW_UP = b.comment("Alza (+) o abbassa (-) la visuale, in sedicesimi di blocco.")
                .translation("qrm_vehicles.configuration.viewUp").defineInRange("up", 0, -12, 12);
        VIEW_FORWARD = b.comment("Porta la visuale avanti (+) o indietro (-), in sedicesimi di blocco.")
                .translation("qrm_vehicles.configuration.viewForward").defineInRange("forward", 0, -16, 16);
        SHOW_GLASS_INSIDE = b.comment("Disegna i vetri anche guardando dall'interno del veicolo.")
                .translation("qrm_vehicles.configuration.showGlassInside").define("showGlassInside", false);
        b.pop();
        b.push("effects");
        PARTICLES = b.comment("Polvere dalle ruote, fumo delle gomme e sbuffo allo scarico.")
                .translation("qrm_vehicles.configuration.particles").define("particles", true);
        b.pop();
        SPEC = b.build();
    }

    private VehicleClientConfig() {}
}
