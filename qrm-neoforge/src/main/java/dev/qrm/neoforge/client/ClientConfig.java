package dev.qrm.neoforge.client;

import net.neoforged.neoforge.common.ModConfigSpec;

/** Configurazione solo client (salvata nella cartella config del client). */
public final class ClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue HUD_VISIBLE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("hud");
        HUD_VISIBLE = b.comment("Mostra l'HUD del personaggio (si cambia anche con il tasto H)").define("visible", true);
        b.pop();
        SPEC = b.build();
    }

    private ClientConfig() {}

    /** Inverte la visibilità dell'HUD e la salva. */
    public static boolean toggleHud() {
        boolean next = !HUD_VISIBLE.get();
        HUD_VISIBLE.set(next);
        HUD_VISIBLE.save();
        return next;
    }
}
