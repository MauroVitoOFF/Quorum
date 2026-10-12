package dev.qrm.boss.logic;

import static org.junit.jupiter.api.Assertions.*;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;

/** Le texture del menu esistono, hanno le dimensioni dichiarate e le chiavi usate dal codice (da qrm-boss/). */
class BossAssetsTest {
    private static final Path ASSETS = Path.of("src/main/resources/assets/qrm_boss");
    private static final List<String> USED_KEYS = List.of("frame", "title_plate", "inset", "balance_plate", "row",
            "row_hover", "row_selected", "button_normal", "button_hover", "button_pressed", "button_disabled",
            "button_confirm_normal", "button_confirm_hover", "button_confirm_pressed", "button_danger_normal",
            "button_danger_hover", "button_danger_pressed", "tab", "tab_hover", "tab_selected", "icon_overview",
            "icon_members", "icon_account", "icon_add", "icon_promote", "icon_fire", "icon_deposit", "icon_withdraw",
            "icon_desk", "boss_mascot");

    private static JsonObject textures() throws IOException {
        return JsonParser.parseString(Files.readString(ASSETS.resolve("theme/qrm.json")))
                .getAsJsonObject().getAsJsonObject("textures");
    }

    @Test
    void everyKeyTheCodeUsesIsDeclared() throws IOException {
        JsonObject textures = textures();
        for (String key : USED_KEYS) assertTrue(textures.has("qrm_boss." + key), "manca qrm_boss." + key);
        assertEquals(USED_KEYS.size(), textures.size(), "texture dichiarate ma non usate o viceversa");
    }

    @Test
    void texturesExistWithTheDeclaredSize() throws IOException {
        JsonObject textures = textures();
        for (String key : textures.keySet()) {
            JsonObject t = textures.getAsJsonObject(key);
            String id = t.get("id").getAsString();
            assertTrue(id.startsWith("qrm_boss:"), key);
            Path png = ASSETS.resolve(id.substring("qrm_boss:".length()));
            assertTrue(Files.exists(png), "manca " + png);
            BufferedImage img = ImageIO.read(png.toFile());
            assertNotNull(img, "non e' un PNG: " + png);
            assertEquals(t.getAsJsonArray("size").get(0).getAsInt(), img.getWidth(), png.toString());
            assertEquals(t.getAsJsonArray("size").get(1).getAsInt(), img.getHeight(), png.toString());
        }
    }

    @Test
    void everyPngIsDeclared() throws IOException {
        JsonObject textures = textures();
        try (var files = Files.list(ASSETS.resolve("textures/gui"))) {
            for (Path p : files.toList()) {
                String name = p.getFileName().toString().replace(".png", "");
                assertTrue(textures.has("qrm_boss." + name), "texture non dichiarata: " + name);
            }
        }
    }
}
