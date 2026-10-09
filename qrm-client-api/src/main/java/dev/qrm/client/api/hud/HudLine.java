package dev.qrm.client.api.hud;

import java.util.Objects;

/**
 * Una riga dell'HUD fornita da un modulo: il testo, la chiave di tema del colore del testo (assente o sconosciuta:
 * {@code text}) e la chiave facoltativa del colore di un quadratino di stato davanti al testo.
 */
public record HudLine(String text, String colorKey, String dotColorKey) {
    public HudLine {
        Objects.requireNonNull(text, "text");
        if (colorKey == null) colorKey = "text";
    }

    public static HudLine of(String text) {
        return new HudLine(text, "text", null);
    }
}
