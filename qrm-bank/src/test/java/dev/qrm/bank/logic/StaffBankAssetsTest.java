package dev.qrm.bank.logic;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qrm.bank.net.StaffBankResult;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Testi e texture della schermata staff della banca (i test girano dalla cartella qrm-bank/). */
class StaffBankAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/qrm_bank");

    private static JsonObject json(Path file) throws IOException {
        return JsonParser.parseString(Files.readString(file)).getAsJsonObject();
    }

    @Test
    void everyStaffResultHasATextInBothLanguages() throws IOException {
        for (String file : new String[] {"en_us.json", "it_it.json"}) {
            JsonObject lang = json(ASSETS.resolve("lang/" + file));
            for (StaffBankResult r : StaffBankResult.values()) {
                String key = "qrm_bank.staff.result." + r.name().toLowerCase(Locale.ROOT);
                assertTrue(lang.has(key), file + " manca " + key);
            }
        }
    }

    @Test
    void themeTexturesExistWithTheDeclaredSize() throws IOException {
        JsonObject textures = json(ASSETS.resolve("theme/qrm.json")).getAsJsonObject("textures");
        assertTrue(textures.has("qrm_bank.staff.tile"));
        assertTrue(textures.has("qrm_bank.staff.icon"));
        for (String key : textures.keySet()) {
            JsonObject t = textures.getAsJsonObject(key);
            String id = t.get("id").getAsString();
            assertTrue(id.startsWith("qrm_bank:"), key);
            Path png = ASSETS.resolve(id.substring("qrm_bank:".length()));
            assertTrue(Files.exists(png), "manca " + png);
            BufferedImage img = ImageIO.read(png.toFile());
            assertNotNull(img, "non e' un PNG: " + png);
            assertEquals(t.getAsJsonArray("size").get(0).getAsInt(), img.getWidth(), png.toString());
            assertEquals(t.getAsJsonArray("size").get(1).getAsInt(), img.getHeight(), png.toString());
        }
    }
}
