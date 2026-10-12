package dev.qrm.neoforge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qrm.api.Money;
import dev.qrm.api.economy.Currency;
import dev.qrm.api.economy.CurrencyRegistry;
import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.job.JobRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Carica le definizioni dei lavori da {@code <dir>/*.json}. Un file non valido viene scartato con un errore nel log. */
public final class JobLoader {
    private static final Logger LOG = LoggerFactory.getLogger("qrm");

    private JobLoader() {}

    /** Crea la cartella se manca (senza scrivere esempi). @return lavori caricati */
    public static int load(JobRegistry jobs, CurrencyRegistry currencies, Path dir) throws IOException {
        Files.createDirectories(dir);
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        }
        int loaded = 0;
        for (Path file : files) {
            try {
                jobs.register(parse(JsonParser.parseString(Files.readString(file)).getAsJsonObject(), currencies));
                loaded++;
            } catch (IOException | RuntimeException e) {
                LOG.error("Invalid job file {}: {}", file, e.getMessage());
            }
        }
        return loaded;
    }

    static JobDefinition parse(JsonObject o, CurrencyRegistry currencies) {
        List<GradeDefinition> grades = new ArrayList<>();
        for (JsonElement el : req(o, "grades").getAsJsonArray()) {
            JsonObject g = el.getAsJsonObject();
            Set<String> permissions = new LinkedHashSet<>();
            if (g.has("permissions")) {
                JsonArray array = g.getAsJsonArray("permissions");
                array.forEach(p -> permissions.add(p.getAsString()));
            }
            Money salary = null;
            if (g.has("salary")) {
                JsonObject s = g.getAsJsonObject("salary");
                String code = req(s, "currency").getAsString();
                Currency currency = currencies.find(code)
                        .orElseThrow(() -> new IllegalArgumentException("unknown currency in salary: " + code));
                salary = Money.of(code, currency.parseAmount(req(s, "amount").getAsString()));
            }
            grades.add(new GradeDefinition(req(g, "id").getAsString(), req(g, "label").getAsString(),
                    req(g, "level").getAsInt(), permissions, salary));
        }
        String id = req(o, "id").getAsString();
        if (o.has("org")) LOG.warn("Job {}: the \"org\" key is ignored, every job has an organization with its own id", id);
        return new JobDefinition(id, req(o, "label").getAsString(), grades, id);
    }

    private static JsonElement req(JsonObject o, String key) {
        JsonElement e = o.get(key);
        if (e == null || e.isJsonNull()) throw new IllegalArgumentException("missing field: " + key);
        return e;
    }
}
