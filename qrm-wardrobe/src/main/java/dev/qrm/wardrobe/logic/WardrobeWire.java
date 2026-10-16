package dev.qrm.wardrobe.logic;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Formato dei pacchetti, in puro Java. Ogni decodifica rifiuta dati troncati o oltre i limiti con IllegalArgumentException. */
public final class WardrobeWire {
    public static final int MAX_ID = 64;
    public static final int MAX_WORN = 1024;

    private WardrobeWire() {}

    /** Una divisa del catalogo, come la vede il client. {@code minGrade} vuoto = nessun grado minimo. */
    public record CatalogEntry(String id, String label, String job, String minGrade, boolean slim, byte[] png) {}

    /** L'elenco delle divise scelte per un punto e quella indossata ora (vuoto = abito civile). */
    public record Open(List<String> outfitIds, String current) {}

    // ---- voce del catalogo -------------------------------------------------------------------------------

    public static byte[] encode(CatalogEntry e) {
        if (e.png().length > OutfitCatalog.MAX_PNG_BYTES) throw new IllegalArgumentException("PNG troppo grande");
        return write(out -> {
            string(out, e.id(), MAX_ID);
            string(out, e.label(), OutfitCatalog.MAX_LABEL * 4);
            string(out, e.job(), MAX_ID);
            string(out, e.minGrade(), MAX_ID);
            out.writeBoolean(e.slim());
            out.writeInt(e.png().length);
            out.write(e.png());
        });
    }

    public static CatalogEntry decodeEntry(byte[] data) {
        return read(data, in -> {
            String id = string(in, MAX_ID);
            String label = string(in, OutfitCatalog.MAX_LABEL * 4);
            String job = string(in, MAX_ID);
            String minGrade = string(in, MAX_ID);
            boolean slim = in.readBoolean();
            int len = in.readInt();
            if (len < 0 || len > OutfitCatalog.MAX_PNG_BYTES) throw new IllegalArgumentException("PNG fuori misura");
            byte[] png = new byte[len];
            in.readFully(png);
            return new CatalogEntry(id, label, job, minGrade, slim, png);
        });
    }

    // ---- chi indossa che cosa ----------------------------------------------------------------------------

    public static byte[] encodeWorn(Map<UUID, String> worn) {
        if (worn.size() > MAX_WORN) throw new IllegalArgumentException("troppi giocatori");
        return write(out -> {
            out.writeInt(worn.size());
            for (Map.Entry<UUID, String> e : worn.entrySet()) {
                out.writeLong(e.getKey().getMostSignificantBits());
                out.writeLong(e.getKey().getLeastSignificantBits());
                string(out, e.getValue(), MAX_ID);
            }
        });
    }

    public static Map<UUID, String> decodeWorn(byte[] data) {
        return read(data, in -> {
            int n = in.readInt();
            if (n < 0 || n > MAX_WORN) throw new IllegalArgumentException("troppi giocatori");
            Map<UUID, String> out = new LinkedHashMap<>();
            for (int i = 0; i < n; i++) out.put(new UUID(in.readLong(), in.readLong()), string(in, MAX_ID));
            return out;
        });
    }

    // ---- apertura dell'armadietto ------------------------------------------------------------------------

    public static byte[] encode(Open open) {
        if (open.outfitIds().size() > OutfitCatalog.MAX_OUTFITS) throw new IllegalArgumentException("troppe divise");
        return write(out -> {
            out.writeInt(open.outfitIds().size());
            for (String id : open.outfitIds()) string(out, id, MAX_ID);
            string(out, open.current(), MAX_ID);
        });
    }

    public static Open decodeOpen(byte[] data) {
        return read(data, in -> {
            int n = in.readInt();
            if (n < 0 || n > OutfitCatalog.MAX_OUTFITS) throw new IllegalArgumentException("troppe divise");
            List<String> ids = new ArrayList<>();
            for (int i = 0; i < n; i++) ids.add(string(in, MAX_ID));
            return new Open(List.copyOf(ids), string(in, MAX_ID));
        });
    }

    // ---- scelta del giocatore ----------------------------------------------------------------------------

    public static byte[] encodeChoose(String outfitId) {
        return write(out -> string(out, outfitId, MAX_ID));
    }

    public static String decodeChoose(byte[] data) {
        return read(data, in -> string(in, MAX_ID));
    }

    // ---- utilità -----------------------------------------------------------------------------------------

    private interface Writer {
        void write(DataOutputStream out) throws IOException;
    }

    private interface Reader<T> {
        T read(DataInputStream in) throws IOException;
    }

    private static byte[] write(Writer w) {
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream(); DataOutputStream out = new DataOutputStream(bytes)) {
            w.write(out);
            out.flush();
            return bytes.toByteArray();
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage());
        }
    }

    private static <T> T read(byte[] data, Reader<T> r) {
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(data))) {
            T value = r.read(in);
            if (in.available() > 0) throw new IllegalArgumentException("dati in eccesso");
            return value;
        } catch (IOException e) {
            throw new IllegalArgumentException("dati troncati");
        }
    }

    private static void string(DataOutputStream out, String s, int max) throws IOException {
        if (s == null) s = "";
        if (s.length() > max) throw new IllegalArgumentException("testo troppo lungo");
        out.writeUTF(s);
    }

    private static String string(DataInputStream in, int max) throws IOException {
        String s = in.readUTF();
        if (s.length() > max) throw new IllegalArgumentException("testo troppo lungo");
        return s;
    }
}
