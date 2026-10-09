package dev.qrm.client.api.hud;

import java.util.Optional;

/** Riga dell'HUD di un modulo. {@link #line()} è chiamata a ogni disegno: deve essere leggera. */
@FunctionalInterface
public interface HudRow {
    /** @return la riga da mostrare, o vuoto se in questo momento non c'è nulla da mostrare */
    Optional<HudLine> line();
}
