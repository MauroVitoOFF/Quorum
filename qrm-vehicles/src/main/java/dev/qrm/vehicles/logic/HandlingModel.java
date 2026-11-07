package dev.qrm.vehicles.logic;

import java.util.Locale;

/** Come un veicolo gira e tiene la strada. */
public enum HandlingModel {
    /** Forze sui due assi con gomme che saturano (modello a bicicletta): realistico, più "pesante" da guidare. */
    DYNAMIC,
    /**
     * Arcade: lo sterzo fa ruotare il muso per geometria e la velocità viene riportata lungo il muso, ma solo fino
     * al limite di aderenza delle gomme; oltre quel limite l'auto scivola. Risposta diretta e prevedibile.
     */
    ARCADE,
    /**
     * Classico: sterzo ampio e dolce da girare, muso per geometria con perdita di efficacia ad alta velocità, e gomme
     * che riportano il moto verso il muso di pochi gradi per tick (vedi {@link ClassicHandling}).
     */
    CLASSIC;

    /** Il modello con questo nome (maiuscole indifferenti). */
    public static HandlingModel parse(String name) {
        try {
            return valueOf(name.trim().toUpperCase(Locale.ROOT));
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("handling deve essere \"dynamic\", \"arcade\" o \"classic\"");
        }
    }

    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
