package dev.qrm.wardrobe.logic;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Le due lingue hanno le stesse chiavi (i test girano dalla cartella qrm-wardrobe/). */
class LangCoverageTest {
    private static JsonObject lang(String file) throws IOException {
        return JsonParser.parseString(Files.readString(
                Path.of("src/main/resources/assets/qrm_wardrobe/lang/" + file))).getAsJsonObject();
    }

    @Test
    void bothLanguagesDefineTheSameKeys() throws IOException {
        assertEquals(lang("en_us.json").keySet(), lang("it_it.json").keySet());
        assertTrue(lang("en_us.json").has("qrm_wardrobe.point.wardrobe"));
    }
}
