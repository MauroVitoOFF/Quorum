package dev.qrm.boss.logic;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonParser;
import dev.qrm.api.data.ModDataService;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.org.Organization;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Cose di roleplay del menu boss che il Core non conosce: la bacheca dell'azienda e il titolo interno dei membri.
 * Stanno nei dati dei mod ({@link ModDataService}, namespace {@code qrm_boss}): nessuna tabella propria.
 */
public final class BossPerks {
    public static final String NAMESPACE = "qrm_boss";
    public static final int MAX_NOTICE = 160;
    public static final int MAX_TITLE = 24;
    public static final int MAX_REASON = 40;

    private static final String NOTICE_KEY = "notice";

    /** L'avviso fisso dell'azienda: testo, chi l'ha scritto e quando (millisecondi epoch). */
    public record Notice(String text, String author, long at) {}

    private final ModDataService data;
    private final Clock clock;

    public BossPerks(ModDataService data, Clock clock) {
        this.data = data;
        this.clock = clock;
    }

    /** Toglie spazi e caratteri di controllo e tronca: il testo arriva da un client e finisce in chat e in un menu. */
    public static String clean(String text, int max) {
        if (text == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : text.toCharArray()) sb.append(Character.isISOControl(c) ? ' ' : c);
        String s = sb.toString().strip().replaceAll(" {2,}", " ");
        return s.length() > max ? s.substring(0, max).strip() : s;
    }

    // ---- bacheca -----------------------------------------------------------------------------------------

    public Optional<Notice> notice(String orgId) {
        return data.get(NAMESPACE, Organization.ownerOf(orgId), NOTICE_KEY).flatMap(json -> {
            try {
                JsonObject o = JsonParser.parseString(json).getAsJsonObject();
                String text = o.get("text").getAsString();
                return text.isBlank() ? Optional.empty() : Optional.of(new Notice(text,
                        o.has("author") ? o.get("author").getAsString() : "", o.has("at") ? o.get("at").getAsLong() : 0));
            } catch (RuntimeException e) {
                return Optional.empty();
            }
        });
    }

    /** Pubblica l'avviso; un testo vuoto lo toglie. @return l'avviso pubblicato, vuoto se è stato tolto */
    public Optional<Notice> setNotice(String orgId, String text, String author) {
        String clean = clean(text, MAX_NOTICE);
        if (clean.isEmpty()) {
            data.delete(NAMESPACE, Organization.ownerOf(orgId), NOTICE_KEY);
            return Optional.empty();
        }
        Notice notice = new Notice(clean, clean(author, 64), clock.millis());
        JsonObject o = new JsonObject();
        o.addProperty("text", notice.text());
        o.addProperty("author", notice.author());
        o.addProperty("at", notice.at());
        data.put(NAMESPACE, Organization.ownerOf(orgId), NOTICE_KEY, o.toString());
        return Optional.of(notice);
    }

    // ---- titolo interno ----------------------------------------------------------------------------------

    private static String titleKey(String orgId) {
        return "title:" + orgId;
    }

    public String title(String orgId, CharacterId character) {
        return data.get(NAMESPACE, OwnerRef.character(character), titleKey(orgId)).map(json -> {
            try {
                return JsonParser.parseString(json).getAsString();
            } catch (RuntimeException e) {
                return "";
            }
        }).orElse("");
    }

    /** Assegna il titolo; un testo vuoto lo toglie. */
    public void setTitle(String orgId, CharacterId character, String text) {
        String clean = clean(text, MAX_TITLE);
        if (clean.isEmpty()) data.delete(NAMESPACE, OwnerRef.character(character), titleKey(orgId));
        else data.put(NAMESPACE, OwnerRef.character(character), titleKey(orgId),
                new JsonPrimitive(clean).toString());
    }

    // ---- fascicolo del dipendente ------------------------------------------------------------------------

    private static final int KEPT_LOG = 20;
    static final int MAX_LOG_TEXT = 80;

    private static String logKey(String orgId) {
        return "log:" + orgId;
    }

    /** Le ultime annotazioni (bonus e titoli) su un membro, dalla più recente, al più {@code max}. */
    public List<BossSnapshot.LogEntry> log(String orgId, CharacterId character, int max) {
        List<BossSnapshot.LogEntry> out = new ArrayList<>();
        Optional<String> json = data.get(NAMESPACE, OwnerRef.character(character), logKey(orgId));
        if (json.isEmpty()) return out;
        try {
            JsonArray all = JsonParser.parseString(json.get()).getAsJsonArray();
            for (int i = all.size() - 1; i >= 0 && out.size() < max; i--) {
                JsonObject o = all.get(i).getAsJsonObject();
                out.add(new BossSnapshot.LogEntry(o.get("kind").getAsInt(), o.get("at").getAsLong(),
                        o.get("text").getAsString()));
            }
        } catch (RuntimeException e) {
            return new ArrayList<>();
        }
        return out;
    }

    /** Aggiunge un'annotazione al fascicolo; ne restano le ultime {@value #KEPT_LOG}. */
    public void addLog(String orgId, CharacterId character, int kind, String text) {
        JsonArray all = new JsonArray();
        data.get(NAMESPACE, OwnerRef.character(character), logKey(orgId)).ifPresent(json -> {
            try {
                for (JsonElement e : JsonParser.parseString(json).getAsJsonArray()) all.add(e);
            } catch (RuntimeException ignored) {
                // un registro illeggibile riparte da zero
            }
        });
        JsonObject o = new JsonObject();
        o.addProperty("kind", kind);
        o.addProperty("at", clock.millis());
        o.addProperty("text", clean(text, MAX_LOG_TEXT));
        all.add(o);
        JsonArray kept = new JsonArray();
        for (int i = Math.max(0, all.size() - KEPT_LOG); i < all.size(); i++) kept.add(all.get(i));
        data.put(NAMESPACE, OwnerRef.character(character), logKey(orgId), kept.toString());
    }

    /** Il motivo di un bonus, già ripulito. */
    static String reason(String text) {
        return clean(text, MAX_REASON);
    }
}
