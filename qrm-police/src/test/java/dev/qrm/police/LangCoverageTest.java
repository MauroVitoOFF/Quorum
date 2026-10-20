package dev.qrm.police;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qrm.police.weapon.logic.AmmoBand;
import dev.qrm.police.weapon.logic.Hint;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

/** Le due lingue hanno le stesse chiavi e i testi della pistola (i test girano dalla cartella qrm-police/). */
class LangCoverageTest {
    private static final String[] FILES = {"en_us.json", "it_it.json"};

    /** Chiavi fisse della pistola: oggetti, tasti, controllo munizioni, armeria, messaggi di morte. */
    private static final List<String> PISTOL_KEYS = List.of(
            "item.qrm_police.police_pistol",
            "item.qrm_police.police_magazine",
            "qrm_police.magazine.unchecked",
            "qrm_police.ammo_check",
            "qrm_police.ammo_check.no_magazine",
            "qrm_police.yes",
            "qrm_police.no",
            "key.category.qrm_police.main",
            "key.qrm_police.reload",
            "key.qrm_police.interact",
            "qrm_police.point.armory",
            "qrm_police.armory.restocked",
            "qrm_police.armory.no_character",
            "death.attack.qrm_police.pistol",
            "death.attack.qrm_police.pistol.item",
            "death.attack.qrm_police.pistol.player");

    private static JsonObject lang(String file) throws IOException {
        return JsonParser.parseString(
                Files.readString(Path.of("src/main/resources/assets/qrm_police/lang/" + file))).getAsJsonObject();
    }

    @Test
    void bothLanguagesDefineTheSameKeys() throws IOException {
        assertEquals(lang("en_us.json").keySet(), lang("it_it.json").keySet());
    }

    @Test
    void pistolKeysExistInBothLanguages() throws IOException {
        for (String file : FILES) {
            JsonObject lang = lang(file);
            for (String key : PISTOL_KEYS) {
                assertTrue(lang.has(key), file + " manca " + key);
            }
        }
    }

    @Test
    void everyAmmoBandHasANameInBothLanguages() throws IOException {
        for (String file : FILES) {
            JsonObject lang = lang(file);
            for (AmmoBand b : AmmoBand.values()) {
                String key = "qrm_police.band." + b.name().toLowerCase(Locale.ROOT);
                assertTrue(lang.has(key), file + " manca " + key);
            }
        }
    }

    @Test
    void everyHintHasATextInBothLanguages() throws IOException {
        for (String file : FILES) {
            JsonObject lang = lang(file);
            for (Hint h : Hint.values()) {
                if (h == Hint.NONE) continue;
                String key = "qrm_police.hint." + h.name().toLowerCase(Locale.ROOT);
                assertTrue(lang.has(key), file + " manca " + key);
            }
        }
    }
}
