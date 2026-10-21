package dev.qrm.police.weapon.logic.mesh;

import java.util.List;

/**
 * Geometria già pronta di una parte del rig: quad di 4 vertici, calcolati una volta sola alla lettura del JSON.
 * Le posizioni sono in unità del modello (16 = 1 blocco, come i pivot del rig); u/v sono normalizzati 0..1
 * sull'intera texture; la normale è quella della faccia dopo la rotazione dell'elemento.
 */
public record PartMesh(List<Quad> quads) {

    public static final PartMesh EMPTY = new PartMesh(List.of());

    public PartMesh {
        quads = List.copyOf(quads);
    }

    /** Un vertice: posizione in unità del modello e coordinate di texture normalizzate. */
    public record Vertex(float x, float y, float z, float u, float v) {}

    /** Un quad nell'ordine dei vertici di Minecraft per quella faccia, con la sua normale (unitaria). */
    public record Quad(List<Vertex> vertices, float nx, float ny, float nz) {
        public Quad {
            if (vertices.size() != 4) throw new IllegalArgumentException("Un quad ha 4 vertici");
            vertices = List.copyOf(vertices);
        }
    }
}
