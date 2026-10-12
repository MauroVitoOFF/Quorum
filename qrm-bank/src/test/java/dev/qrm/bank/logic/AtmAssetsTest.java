package dev.qrm.bank.logic;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Texture e colori dell'interfaccia dell'ATM (i test girano dalla cartella qrm-bank/). */
class AtmAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/qrm_bank");

    private static JsonObject theme() throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve("theme/qrm.json"))).getAsJsonObject();
    }

    @Test
    void everyTextureTheScreenAsksForIsDeclared() throws IOException {
        JsonObject textures = theme().getAsJsonObject("textures");
        String[] plain = {"frame", "title_plate", "inset", "balance_plate", "lcd_plate", "field", "field_focus", "button_disabled",
                "tab", "tab_hover", "tab_selected", "icon_account", "icon_cash", "icon_transfer", "icon_withdraw",
                "icon_deposit", "icon_send", "icon_coin", "icon_person", "icon_note", "icon_ok", "icon_error", "icon_in",
                "icon_out", "icon_admin", "icon_cycle", "icon_power", "mascot"};
        for (String name : plain) assertTrue(textures.has("qrm_bank.ui." + name), "manca qrm_bank.ui." + name);
        for (String style : new String[] {"button", "button_confirm", "button_danger"}) {
            for (String state : new String[] {"normal", "hover", "pressed"}) {
                String key = "qrm_bank.ui." + style + "_" + state;
                assertTrue(textures.has(key), "manca " + key);
            }
        }
        // tasti con la scritta disegnata a pixel: un set per ogni stato
        for (String graphic : new String[] {"preset_25", "preset_50", "preset_75", "preset_max", "send"}) {
            for (String state : new String[] {"normal", "hover", "pressed", "disabled"}) {
                String key = "qrm_bank.ui." + graphic + "_" + state;
                assertTrue(textures.has(key), "manca " + key);
            }
        }
    }

    @Test
    void screenColorsAreDeclared() throws IOException {
        JsonObject colors = theme().getAsJsonObject("colors");
        for (String name : new String[] {"positive", "negative", "muted", "cream", "disabled", "gold", "ink", "ink_soft",
                "plate_ink", "ledger_pos", "ledger_neg", "lcd"}) {
            assertTrue(colors.has("qrm_bank." + name), "manca il colore qrm_bank." + name);
        }
    }

    @Test
    void mascotIsAVerticalSheetOfWholeFrames() throws IOException {
        BufferedImage sheet = ImageIO.read(ASSETS.resolve("textures/gui/atm/mascot.png").toFile());
        assertEquals(32, sheet.getWidth());
        assertEquals(40 * 8, sheet.getHeight());
        // ogni frame deve avere dei pixel visibili (non vuoto) e quello del suo stesso disegno
        for (int f = 0; f < 8; f++) {
            boolean visible = false;
            for (int y = 0; y < 40 && !visible; y++) {
                for (int x = 0; x < 32; x++) {
                    if ((sheet.getRGB(x, f * 40 + y) >>> 24) != 0) {
                        visible = true;
                        break;
                    }
                }
            }
            assertTrue(visible, "frame " + f + " vuoto");
        }
    }

    @Test
    void slicesFitInsideTheirTexture() throws IOException {
        JsonObject textures = theme().getAsJsonObject("textures");
        for (String key : textures.keySet()) {
            JsonObject t = textures.getAsJsonObject(key);
            if (!t.has("slice")) continue;
            int w = t.getAsJsonArray("size").get(0).getAsInt();
            int h = t.getAsJsonArray("size").get(1).getAsInt();
            var s = t.getAsJsonArray("slice");
            assertTrue(s.get(0).getAsInt() + s.get(2).getAsInt() < w, key);
            assertTrue(s.get(1).getAsInt() + s.get(3).getAsInt() < h, key);
        }
    }
}
