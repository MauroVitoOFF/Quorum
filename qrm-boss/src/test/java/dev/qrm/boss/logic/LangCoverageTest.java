package dev.qrm.boss.logic;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Ogni esito ha un testo in entrambe le lingue (i test girano dalla cartella qrm-boss/). */
class LangCoverageTest {
    private static JsonObject lang(String file) throws IOException {
        return JsonParser.parseString(
                Files.readString(Path.of("src/main/resources/assets/qrm_boss/lang/" + file))).getAsJsonObject();
    }

    @Test
    void everyResultHasATextInBothLanguages() throws IOException {
        for (String file : new String[] {"en_us.json", "it_it.json"}) {
            JsonObject lang = lang(file);
            for (BossResult r : BossResult.values()) {
                assertTrue(lang.has(r.translationKey()), file + " manca " + r.translationKey());
            }
            for (String status : new String[] {"active", "archived"}) {
                assertTrue(lang.has("qrm_boss.status." + status), file + " manca lo stato " + status);
            }
        }
    }

    @Test
    void bothLanguagesDefineTheSameKeys() throws IOException {
        assertEquals(lang("en_us.json").keySet(), lang("it_it.json").keySet());
    }

    @Test
    void translationKeysAreLowerCase() {
        for (BossResult r : BossResult.values()) {
            assertEquals(r.translationKey(), r.translationKey().toLowerCase(Locale.ROOT));
        }
    }
}
