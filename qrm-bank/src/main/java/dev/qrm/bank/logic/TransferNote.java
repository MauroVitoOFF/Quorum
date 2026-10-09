package dev.qrm.bank.logic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

/** Causale di un bonifico: testo libero, breve, senza caratteri di controllo; vive nei metadati della transazione. */
public final class TransferNote {
    public static final int MAX_LENGTH = 64;
    private static final String KEY = "note";

    private TransferNote() {}

    /** Toglie i caratteri di controllo e gli spazi ai bordi e tronca a {@value #MAX_LENGTH} caratteri. */
    public static String clean(String raw) {
        if (raw == null) return "";
        StringBuilder out = new StringBuilder(raw.length());
        raw.codePoints().filter(cp -> !Character.isISOControl(cp)).forEach(out::appendCodePoint);
        String text = out.toString().strip();
        if (text.codePointCount(0, text.length()) > MAX_LENGTH) {
            text = text.substring(0, text.offsetByCodePoints(0, MAX_LENGTH)).strip();
        }
        return text;
    }

    /** Metadati della transazione per una causale già pulita ({@code "{}"} se vuota). */
    public static String toJson(String note) {
        if (note == null || note.isEmpty()) return "{}";
        JsonObject o = new JsonObject();
        o.addProperty(KEY, note);
        return o.toString();
    }

    /** Causale letta dai metadati; vuota se assente o se il JSON non è quello atteso. */
    public static String fromJson(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) return "";
        try {
            JsonObject o = JsonParser.parseString(metadataJson).getAsJsonObject();
            return o.has(KEY) && o.get(KEY).isJsonPrimitive() ? clean(o.get(KEY).getAsString()) : "";
        } catch (JsonParseException | IllegalStateException e) {
            return "";
        }
    }
}
