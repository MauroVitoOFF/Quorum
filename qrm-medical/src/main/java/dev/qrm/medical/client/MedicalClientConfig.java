package dev.qrm.medical.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Configurazione solo client: la sagoma del corpo sullo schermo. Accesa di default. */
public final class MedicalClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue BODY_ENABLED;
    public static final ModConfigSpec.IntValue BODY_ZOOM;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("body");
        BODY_ENABLED = b.comment("Mostra la sagoma del corpo quando c'e' almeno una ferita")
                .define("enabled", true);
        BODY_ZOOM = b.comment("Ingrandimento della sagoma (1 = alta come la hotbar, 22 pixel GUI)")
                .defineInRange("zoom", 1, 1, 4);
        b.pop();
        SPEC = b.build();
    }

    private MedicalClientConfig() {}
}
