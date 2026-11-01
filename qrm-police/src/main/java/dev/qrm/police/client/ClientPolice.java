package dev.qrm.police.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;

/** Lo stato locale dell'operatore: la chiamata che sta raggiungendo e le note rapide. */
public final class ClientPolice {
    /** Una chiamata presa in carico: dove andare e cosa mostrare. */
    public record Waypoint(String callId, String label, int x, int y, int z) {}

    public static final int NOTE_MAX = 600;

    private static Waypoint waypoint;
    private static String notes;

    private ClientPolice() {}

    public static Waypoint waypoint() {
        return waypoint;
    }

    public static void setWaypoint(Waypoint w) {
        waypoint = w;
    }

    public static void clearWaypoint() {
        waypoint = null;
    }

    private static Path notesFile() {
        return FMLPaths.GAMEDIR.get().resolve("qrm_police_notes.txt");
    }

    /** Le note, lette una volta dal file: restano anche dopo aver chiuso il gioco. */
    public static String notes() {
        if (notes == null) {
            notes = "";
            try {
                Path f = notesFile();
                if (Files.exists(f)) {
                    String text = Files.readString(f, StandardCharsets.UTF_8).replace("\r", "").stripTrailing();
                    notes = text.length() > NOTE_MAX ? text.substring(0, NOTE_MAX) : text;
                }
            } catch (IOException | RuntimeException ignored) {
                // Nessuna nota salvata, o file illeggibile: si riparte vuoti.
            }
        }
        return notes;
    }

    public static void saveNotes(String text) {
        notes = text.length() > NOTE_MAX ? text.substring(0, NOTE_MAX) : text;
        try {
            Files.writeString(notesFile(), notes, StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Le note restano in memoria per questa sessione.
        }
    }
}
