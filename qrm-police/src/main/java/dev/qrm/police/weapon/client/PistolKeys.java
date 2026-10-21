package dev.qrm.police.weapon.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/**
 * I tasti della pistola. Mira e sparo usano i tasti vanilla "usa" e "attacca"; qui ci sono solo ricarica e
 * interazione (pressione breve: carrello o controllo; tenuto: ispezione).
 */
public final class PistolKeys {
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath("qrm_police", "main"));
    public static final KeyMapping RELOAD = new KeyMapping("key.qrm_police.reload", GLFW.GLFW_KEY_R, CATEGORY);
    public static final KeyMapping INTERACT = new KeyMapping("key.qrm_police.interact", GLFW.GLFW_KEY_V, CATEGORY);

    private PistolKeys() {}

    /** Da registrare sul bus della mod, lato client. */
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(RELOAD);
        event.register(INTERACT);
    }
}
