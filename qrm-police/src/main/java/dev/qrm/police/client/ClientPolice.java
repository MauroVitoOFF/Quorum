package dev.qrm.police.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import net.neoforged.fml.loading.FMLPaths;

/** Lo stato locale dell'operatore: la chiamata che sta raggiungendo e le note rapide. */
final class ClientPolice {
    /** Una chiamata presa in carico: dove andare e cosa mostrare. */
    record Waypoint(String callId, String label, int x, int y, int z) {}

    static final int NOTE_LINES = 4;
    private static final int NOTE_MAX = 120;

    private static Waypoint waypoint;
    private static String[] notes;

    private ClientPolice() {}

    static Waypoint waypoint() {
        return waypoint;
    }

    static void setWaypoint(Waypoint w) {
        waypoint = w;
    }

    static void clearWaypoint() {
        waypoint = null;
    }

    private static Path notesFile() {
        return FMLPaths.GAMEDIR.get().resolve("qrm_police_notes.txt");
    }

    /** Le note, lette una volta dal file: restano anche dopo aver chiuso il gioco. */
    static String[] notes() {
        if (notes == null) {
            notes = new String[NOTE_LINES];
            java.util.Arrays.fill(notes, "");
            try {
                Path f = notesFile();
                if (Files.exists(f)) {
                    String[] lines = Files.readString(f, StandardCharsets.UTF_8).split("\n", -1);
                    for (int i = 0; i < NOTE_LINES && i < lines.length; i++) {
                        notes[i] = lines[i].length() > NOTE_MAX ? lines[i].substring(0, NOTE_MAX) : lines[i];
                    }
                }
            } catch (IOException | RuntimeException ignored) {
                // Nessuna nota salvata, o file illeggibile: si riparte vuoti.
            }
        }
        return notes;
    }

    static void saveNote(int line, String text) {
        notes()[line] = text.replace("\n", " ");
        try {
            Files.writeString(notesFile(), String.join("\n", notes), StandardCharsets.UTF_8);
        } catch (IOException ignored) {
            // Le note restano in memoria per questa sessione.
        }
    }
}
