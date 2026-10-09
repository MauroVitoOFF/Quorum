package dev.qrm.neoforge;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qrm.api.economy.Currency;
import dev.qrm.api.economy.CurrencyRegistry;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CurrencyLoader {
    private static final Logger LOG = LoggerFactory.getLogger("qrm");
    private static final String DEFAULT_FILE = "{\"code\":\"QRM\",\"symbol\":\"Q\",\"decimals\":2}\n";

    private CurrencyLoader() {}

    /** Carica i file JSON della cartella; se vuota o assente scrive la valuta di esempio QRM. @return valute caricate */
    public static int load(CurrencyRegistry registry, Path dir) throws IOException {
        Files.createDirectories(dir);
        List<Path> files;
        try (Stream<Path> s = Files.list(dir)) {
            files = s.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().toList();
        }
        if (files.isEmpty()) {
            Path example = dir.resolve("qrm.json");
            Files.writeString(example, DEFAULT_FILE);
            files = List.of(example);
        }
        int loaded = 0;
        for (Path file : files) {
            try {
                JsonObject o = JsonParser.parseString(Files.readString(file)).getAsJsonObject();
                registry.register(new Currency(
                        o.get("code").getAsString(), o.get("symbol").getAsString(), o.get("decimals").getAsInt()));
                loaded++;
            } catch (IOException | RuntimeException e) {
                LOG.error("Invalid currency file {}: {}", file, e.getMessage());
            }
        }
        return loaded;
    }
}
