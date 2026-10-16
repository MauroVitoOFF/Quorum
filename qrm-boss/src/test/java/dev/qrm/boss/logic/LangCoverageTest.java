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
            for (String key : new String[] {"qrm_boss.mode.rank", "qrm_boss.mode.title", "qrm_boss.mode.bonus",
                    "qrm_boss.notice.title", "qrm_boss.msg.notice", "qrm_boss.msg.bonus", "qrm_boss.msg.bonus_reason"}) {
                assertTrue(lang.has(key), file + " manca " + key);
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
