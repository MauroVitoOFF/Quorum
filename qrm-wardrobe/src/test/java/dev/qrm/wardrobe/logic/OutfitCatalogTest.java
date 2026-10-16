package dev.qrm.wardrobe.logic;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OutfitCatalogTest {
    private static final JobLookup JOBS = new JobLookup() {
        @Override
        public boolean hasJob(String job) {
            return job.equals("police");
        }

        @Override
        public OptionalInt gradeLevel(String job, String grade) {
            if (!job.equals("police")) return OptionalInt.empty();
            return switch (grade) {
                case "officer" -> OptionalInt.of(1);
                case "captain" -> OptionalInt.of(2);
                default -> OptionalInt.empty();
            };
        }
    };

    @TempDir
    Path dir;

    private final List<String> problems = new ArrayList<>();

    private static byte[] png(int w, int h) throws IOException {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) img.setRGB(x, y, 0xFF000000 | (x * 4) << 8 | y);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private void write(String id, String json, byte[] png) throws IOException {
        if (json != null) Files.writeString(dir.resolve(id + ".json"), json);
        if (png != null) Files.write(dir.resolve(id + ".png"), png);
    }

    private static String json(String label, String job, String minGrade, String model) {
        StringBuilder sb = new StringBuilder("{\"label\":\"" + label + "\",\"job\":\"" + job + "\"");
        if (minGrade != null) sb.append(",\"minGrade\":\"").append(minGrade).append('"');
        if (model != null) sb.append(",\"model\":\"").append(model).append('"');
        return sb.append('}').toString();
    }

    private OutfitCatalog load() {
        return OutfitCatalog.load(dir, JOBS, problems::add);
    }

    @Test
    void loadsAValidOutfit() throws IOException {
        write("agente", json("Agente", "police", "officer", null), png(64, 64));
        OutfitCatalog c = load();
        OutfitDefinition o = c.find("agente").orElseThrow();
        assertEquals("Agente", o.label());
        assertEquals("police", o.job());
        assertEquals("officer", o.minGrade());
        assertEquals(OutfitModel.CLASSIC, o.model());
        assertTrue(problems.isEmpty());
        assertEquals(List.of(o), c.forJob("police"));
        assertTrue(c.forJob("taxi").isEmpty());
    }

    @Test
    void minGradeIsOptionalAndSlimIsRead() throws IOException {
        write("gala", json("Gala", "police", null, "slim"), png(64, 64));
        OutfitDefinition o = load().find("gala").orElseThrow();
        assertNull(o.minGrade());
        assertEquals(OutfitModel.SLIM, o.model());
    }

    @Test
    void skipsMissingPngButKeepsTheOthers() throws IOException {
        write("senza", json("Senza", "police", null, null), null);
        write("ok", json("Ok", "police", null, null), png(64, 64));
        OutfitCatalog c = load();
        assertTrue(c.find("senza").isEmpty());
        assertTrue(c.find("ok").isPresent());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("senza"));
    }

    @Test
    void skipsMissingJson() throws IOException {
        write("solo", null, png(64, 64));
        assertTrue(load().all().isEmpty());
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("solo"));
    }

    @Test
    void skipsWrongSizeOversizedAndNotAPng() throws IOException {
        write("vecchia", json("Vecchia", "police", null, null), png(64, 32));
        write("grossa", json("Grossa", "police", null, null), new byte[OutfitCatalog.MAX_PNG_BYTES + 1]);
        write("finta", json("Finta", "police", null, null), new byte[] {1, 2, 3, 4});
        assertTrue(load().all().isEmpty());
        assertEquals(3, problems.size());
    }

    @Test
    void skipsUnknownJobOrGradeBadLabelBadModelAndBadJson() throws IOException {
        write("a", json("A", "taxi", null, null), png(64, 64));
        write("b", json("B", "police", "chief", null), png(64, 64));
        write("c", json("", "police", null, null), png(64, 64));
        write("d", json("x".repeat(41), "police", null, null), png(64, 64));
        write("e", json("E", "police", null, "wide"), png(64, 64));
        write("f", "{not json", png(64, 64));
        assertTrue(load().all().isEmpty());
        assertEquals(6, problems.size());
    }

    @Test
    void skipsInvalidId() throws IOException {
        write("Bad Name", json("Bad", "police", null, null), png(64, 64));
        assertTrue(load().all().isEmpty());
        assertEquals(1, problems.size());
    }

    @Test
    void capsAt128Outfits() throws IOException {
        byte[] p = png(64, 64);
        for (int i = 0; i < 130; i++) write(String.format("d%03d", i), json("D", "police", null, null), p);
        assertEquals(OutfitCatalog.MAX_OUTFITS, load().all().size());
        assertFalse(problems.isEmpty());
    }

    @Test
    void emptyOrMissingDirectoryIsAnEmptyCatalog() {
        assertTrue(load().all().isEmpty());
        assertTrue(OutfitCatalog.load(dir.resolve("nope"), JOBS, problems::add).all().isEmpty());
        assertTrue(problems.isEmpty());
    }
}
