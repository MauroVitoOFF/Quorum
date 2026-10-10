package dev.qrm.api.point;

import java.util.Objects;

/** Un blocco del mondo: nome della dimensione (es. {@code minecraft:overworld}) e coordinate intere. */
public record PointPosition(String dimension, int x, int y, int z) {
    public PointPosition {
        Objects.requireNonNull(dimension, "dimension");
        if (dimension.isBlank() || dimension.length() > 128) throw new IllegalArgumentException("dimension must be 1..128 chars");
    }
}
