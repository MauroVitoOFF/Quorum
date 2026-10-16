package dev.qrm.wardrobe.logic;

import java.util.OptionalInt;

/** Ciò che serve sapere dei lavori, senza dipendere da Minecraft. */
public interface JobLookup {
    boolean hasJob(String job);

    /** Livello del grado, vuoto se il lavoro o il grado non esistono. */
    OptionalInt gradeLevel(String job, String grade);
}
