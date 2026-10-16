package dev.qrm.wardrobe.client;

import dev.qrm.wardrobe.logic.WardrobeWire;

/** Punto d'ingresso dei pacchetti sul client. Caricata solo quando un pacchetto arriva davvero. */
public final class WardrobeClientBridge {
    private WardrobeClientBridge() {}

    public static void onCatalog(int total, int index, byte[] entry) {
        try {
            ClientOutfits.onCatalog(total, index, total == 0 ? null : WardrobeWire.decodeEntry(entry));
        } catch (IllegalArgumentException ignored) {
            // Voce malformata o fuori misura: si scarta, le altre restano valide.
        }
    }

    public static void onWorn(byte[] data) {
        try {
            ClientOutfits.setWorn(WardrobeWire.decodeWorn(data));
        } catch (IllegalArgumentException ignored) {
            // Istantanea malformata: si tiene quella precedente.
        }
    }

    public static void onOpen(byte[] data) {
        // La schermata arriva con il Task 8.
    }
}
