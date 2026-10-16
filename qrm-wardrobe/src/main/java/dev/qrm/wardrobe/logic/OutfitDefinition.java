package dev.qrm.wardrobe.logic;

import java.util.Objects;

/** Una divisa: skin PNG 64x64 legata a un lavoro e, se indicato, a un grado minimo ({@code minGrade} può essere null). */
public record OutfitDefinition(String id, String label, String job, String minGrade, OutfitModel model, byte[] png) {
    public OutfitDefinition {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(label, "label");
        Objects.requireNonNull(job, "job");
        Objects.requireNonNull(model, "model");
        Objects.requireNonNull(png, "png");
    }
}
