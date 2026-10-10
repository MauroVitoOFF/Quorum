package dev.qrm.neoforge.net;

import java.util.Optional;

/**
 * Un punto di interazione come lo vede il client: solo ciò che serve a disegnare etichetta e suggerimento.
 *
 * @param typeId id del tipo, per scegliere l'icona
 * @param labelKey chiave di traduzione del nome del tipo
 * @param name nome scelto dallo staff, se presente
 * @param face faccia su cui mostrare l'icona ("north", "south", "east", "west"); vuoto = tutte e quattro
 */
public record PointEntry(int id, String typeId, String dimension, int x, int y, int z, String labelKey, Optional<String> name,
        Optional<String> face) {}
