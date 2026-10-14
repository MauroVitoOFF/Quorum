package dev.qrm.bank.logic;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Ogni esito ha un testo in entrambe le lingue (i test girano dalla cartella qrm-bank/). */
class LangCoverageTest {
    private static JsonObject lang(String file) throws IOException {
        return JsonParser.parseString(
                Files.readString(Path.of("src/main/resources/assets/qrm_bank/lang/" + file))).getAsJsonObject();
    }

    @Test
    void everyResultHasATextInBothLanguages() throws IOException {
        for (String file : new String[] {"en_us.json", "it_it.json"}) {
            JsonObject lang = lang(file);
            for (BankResult r : BankResult.values()) {
                String key = "qrm_bank.result." + r.name().toLowerCase(Locale.ROOT);
                assertTrue(lang.has(key), file + " manca " + key);
            }
        }
    }

    @Test
    void everyHistoryCategoryHasANameInBothLanguages() throws IOException {
        for (String file : new String[] {"en_us.json", "it_it.json"}) {
            JsonObject lang = lang(file);
            for (HistoryCategory c : HistoryCategory.values()) {
                assertTrue(lang.has("qrm_bank.category." + c.key()), file + " manca la categoria " + c.key());
            }
        }
    }

    @Test
    void bothLanguagesDefineTheSameKeys() throws IOException {
        assertEquals(lang("en_us.json").keySet(), lang("it_it.json").keySet());
    }
}
