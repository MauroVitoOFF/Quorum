package dev.qrm.neoforge;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.client.api.theme.ThemeManifest;
import dev.qrm.core.economy.InMemoryCurrencyRegistry;
import dev.qrm.core.job.InMemoryJobRegistry;
import dev.qrm.neoforge.client.theme.BuiltInTheme;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Gli esempi mostrati nella wiki sono i file di {@code docs/samples}: qui si caricano con gli stessi lettori del mod, così
 * un esempio che smette di essere valido fa fallire la build. La wiki controlla a sua volta di mostrare questi file
 * ({@code website/scripts/check-docs.mjs}).
 */
class WikiSamplesTest {
    private static final Path SAMPLES = findSamples();

    /** La cartella di lavoro dei test dipende dal plugin: si risale finché non compare {@code docs/samples}. */
    private static Path findSamples() {
        for (Path dir = Path.of("").toAbsolutePath(); dir != null; dir = dir.getParent()) {
            Path samples = dir.resolve("docs").resolve("samples");
            if (Files.isDirectory(samples)) return samples;
        }
        throw new IllegalStateException("docs/samples not found above " + Path.of("").toAbsolutePath());
    }

    private static long jsonFiles(Path dir) throws IOException {
        try (Stream<Path> s = Files.list(dir)) {
            return s.filter(p -> p.getFileName().toString().endsWith(".json")).count();
        }
    }

    @Test
    void currenciesAndJobsLoadWithoutSkippingAnyFile() throws IOException {
        // I lettori scartano un file non valido con un errore nel log: il conteggio lo rende un fallimento.
        InMemoryCurrencyRegistry currencies = new InMemoryCurrencyRegistry();
        Path currencyDir = SAMPLES.resolve("currencies");
        assertEquals(jsonFiles(currencyDir), CurrencyLoader.load(currencies, currencyDir));

        InMemoryJobRegistry jobs = new InMemoryJobRegistry();
        Path jobDir = SAMPLES.resolve("jobs");
        assertEquals(jsonFiles(jobDir), JobLoader.load(jobs, currencies, jobDir));

        assertTrue(jobs.find("police").orElseThrow().grade("chief").orElseThrow().salary() != null);
    }

    @Test
    void themeManifestsParseWithoutWarnings() throws IOException {
        for (Path file : List.of(SAMPLES.resolve("theme").resolve("manifest.json"),
                SAMPLES.resolve("qrm-sample-pack").resolve("assets").resolve("qrm").resolve("theme")
                        .resolve("default.json"))) {
            ThemeManifest.Result r = ThemeManifest.parse(Files.readString(file), BuiltInTheme.manifest());
            assertFalse(r.usedFallback(), file.toString());
            assertEquals(List.of(), r.warnings(), file.toString());
        }
    }
}
