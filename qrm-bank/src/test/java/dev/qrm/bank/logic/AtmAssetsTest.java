package dev.qrm.bank.logic;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
        String[] plain = {"frame", "panel", "title_bar", "inset", "lcd_plate", "field", "field_focus", "button_disabled",
                "icon_account", "icon_cash", "icon_transfer", "icon_withdraw",
                "icon_deposit", "icon_send", "icon_coin", "icon_person", "icon_note", "icon_ok", "icon_error", "icon_in",
                "icon_out", "icon_admin", "icon_cycle", "icon_power", "icon_vip", "icon_work", "icon_shop", "icon_fuel"};
        for (String name : plain) assertTrue(textures.has("qrm_bank.ui." + name), "manca qrm_bank.ui." + name);
        for (String style : new String[] {"button", "button_confirm", "button_danger"}) {
            for (String state : new String[] {"normal", "hover", "pressed"}) {
                String key = "qrm_bank.ui." + style + "_" + state;
                assertTrue(textures.has(key), "manca " + key);
            }
        }
    }

    @Test
    void screenColorsAreDeclared() throws IOException {
        JsonObject colors = theme().getAsJsonObject("colors");
        for (String name : new String[] {"positive", "negative", "muted", "cream", "disabled", "gold", "ink", "ink_soft",
                "title", "label", "ledger_pos", "ledger_neg", "lcd"}) {
            assertTrue(colors.has("qrm_bank." + name), "manca il colore qrm_bank." + name);
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
