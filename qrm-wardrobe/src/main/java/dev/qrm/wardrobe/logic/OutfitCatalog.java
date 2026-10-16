package dev.qrm.wardrobe.logic;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qrm.api.job.JobDefinition;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import javax.imageio.ImageIO;

/** Le divise lette da {@code config/qrm_wardrobe/outfits/}: ogni {@code <id>.json} con il suo {@code <id>.png}. */
public final class OutfitCatalog {
    public static final int MAX_PNG_BYTES = 32768;
    public static final int MAX_OUTFITS = 128;
    public static final int MAX_LABEL = 40;
    public static final int SKIN_SIZE = 64;

    private final List<OutfitDefinition> outfits;

    private OutfitCatalog(List<OutfitDefinition> outfits) {
        this.outfits = List.copyOf(outfits);
    }

    public static OutfitCatalog empty() {
        return new OutfitCatalog(List.of());
    }

    /** Legge la cartella. Una divisa non valida viene saltata e segnalata a {@code problems}, senza fermare le altre. */
    public static OutfitCatalog load(Path dir, JobLookup jobs, Consumer<String> problems) {
        if (!Files.isDirectory(dir)) return empty();
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> {
                String n = p.getFileName().toString();
                return n.endsWith(".json") || n.endsWith(".png");
            }).sorted().toList();
        } catch (IOException e) {
            problems.accept(dir + ": " + e.getMessage());
            return empty();
        }
        List<String> ids = new ArrayList<>();
        for (Path p : files) {
            String id = baseName(p.getFileName().toString());
            if (!ids.contains(id)) ids.add(id);
        }
        List<OutfitDefinition> out = new ArrayList<>();
        for (String id : ids) {
            if (out.size() >= MAX_OUTFITS) {
                problems.accept(id + ": oltre il massimo di " + MAX_OUTFITS + " divise, ignorata");
                continue;
            }
            try {
                out.add(read(dir, id, jobs));
            } catch (IllegalArgumentException | IOException e) {
                problems.accept(id + ": " + e.getMessage());
            }
        }
        return new OutfitCatalog(out);
    }

    private static String baseName(String file) {
        return file.substring(0, file.lastIndexOf('.'));
    }

    private static OutfitDefinition read(Path dir, String id, JobLookup jobs) throws IOException {
        if (!JobDefinition.isValidId(id)) throw new IllegalArgumentException("nome del file non valido");
        Path jsonFile = dir.resolve(id + ".json");
        Path pngFile = dir.resolve(id + ".png");
        if (!Files.isRegularFile(jsonFile)) throw new IllegalArgumentException("manca " + id + ".json");
        if (!Files.isRegularFile(pngFile)) throw new IllegalArgumentException("manca " + id + ".png");
        JsonObject o;
        try {
            o = JsonParser.parseString(Files.readString(jsonFile)).getAsJsonObject();
        } catch (RuntimeException e) {
            throw new IllegalArgumentException("JSON non valido");
        }
        String label = string(o, "label");
        if (label == null || label.isBlank() || label.length() > MAX_LABEL) {
            throw new IllegalArgumentException("label deve avere da 1 a " + MAX_LABEL + " caratteri");
        }
        String job = string(o, "job");
        if (job == null || !jobs.hasJob(job)) throw new IllegalArgumentException("lavoro inesistente: " + job);
        String minGrade = string(o, "minGrade");
        if (minGrade != null && jobs.gradeLevel(job, minGrade).isEmpty()) {
            throw new IllegalArgumentException("grado inesistente: " + minGrade);
        }
        String m = string(o, "model");
        OutfitModel model;
        if (m == null || m.equals("classic")) model = OutfitModel.CLASSIC;
        else if (m.equals("slim")) model = OutfitModel.SLIM;
        else throw new IllegalArgumentException("model deve essere classic o slim");
        byte[] png = Files.readAllBytes(pngFile);
        checkPng(png);
        return new OutfitDefinition(id, label, job, minGrade, model, png);
    }

    private static String string(JsonObject o, String key) {
        if (!o.has(key) || !o.get(key).isJsonPrimitive()) return null;
        return o.get(key).getAsString();
    }

    private static void checkPng(byte[] png) throws IOException {
        if (png.length > MAX_PNG_BYTES) throw new IllegalArgumentException("PNG oltre " + MAX_PNG_BYTES + " byte");
        BufferedImage img = ImageIO.read(new ByteArrayInputStream(png));
        if (img == null) throw new IllegalArgumentException("non è un PNG valido");
        if (img.getWidth() != SKIN_SIZE || img.getHeight() != SKIN_SIZE) {
            throw new IllegalArgumentException("la skin deve essere 64x64, non " + img.getWidth() + "x" + img.getHeight());
        }
    }

    public Optional<OutfitDefinition> find(String id) {
        return outfits.stream().filter(o -> o.id().equals(id)).findFirst();
    }

    public List<OutfitDefinition> forJob(String job) {
        return outfits.stream().filter(o -> o.job().equals(job)).toList();
    }

    public List<OutfitDefinition> all() {
        return outfits;
    }
}
