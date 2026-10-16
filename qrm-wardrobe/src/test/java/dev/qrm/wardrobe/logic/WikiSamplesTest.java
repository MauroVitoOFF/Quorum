package dev.qrm.wardrobe.logic;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/** L'esempio mostrato nella wiki ({@code docs/samples/wardrobe}) deve restare una divisa valida per il lavoro di esempio. */
class WikiSamplesTest {
    private static final Path SAMPLES = Path.of("..", "docs", "samples");

    @Test
    void policeSampleOutfitIsValid() throws IOException {
        JsonObject police = JsonParser.parseString(Files.readString(SAMPLES.resolve("jobs").resolve("police.json")))
                .getAsJsonObject();
        JobLookup jobs = new JobLookup() {
            @Override
            public boolean hasJob(String job) {
                return job.equals(police.get("id").getAsString());
            }

            @Override
            public OptionalInt gradeLevel(String job, String grade) {
                if (!hasJob(job)) return OptionalInt.empty();
                for (JsonElement g : police.getAsJsonArray("grades")) {
                    if (g.getAsJsonObject().get("id").getAsString().equals(grade)) {
                        return OptionalInt.of(g.getAsJsonObject().get("level").getAsInt());
                    }
                }
                return OptionalInt.empty();
            }
        };
        List<String> problems = new ArrayList<>();
        OutfitCatalog catalog = OutfitCatalog.load(SAMPLES.resolve("wardrobe"), jobs, problems::add);
        assertEquals(List.of(), problems);
        OutfitDefinition o = catalog.find("agente_pattuglia").orElseThrow();
        assertEquals("police", o.job());
        assertEquals("officer", o.minGrade());
    }
}
