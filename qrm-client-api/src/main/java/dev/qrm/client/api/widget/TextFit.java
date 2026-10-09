package dev.qrm.client.api.widget;

import net.minecraft.client.gui.Font;

/** Troncamento di testo alla larghezza in pixel, con puntini di sospensione. */
public final class TextFit {
    private static final String ELLIPSIS = "…";

    private TextFit() {}

    public static String fit(Font font, String s, int maxW) {
        if (s == null) return "";
        if (font.width(s) <= maxW) return s;
        int room = maxW - font.width(ELLIPSIS);
        if (room <= 0) return "";
        return font.plainSubstrByWidth(s, room) + ELLIPSIS;
    }
}
