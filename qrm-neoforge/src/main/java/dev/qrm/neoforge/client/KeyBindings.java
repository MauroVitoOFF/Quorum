package dev.qrm.neoforge.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath("qrm", "main"));
    public static final KeyMapping CHARACTERS = new KeyMapping("key.qrm.characters", GLFW.GLFW_KEY_O, CATEGORY);
    public static final KeyMapping HUB = new KeyMapping("key.qrm.hub", GLFW.GLFW_KEY_K, CATEGORY);
    public static final KeyMapping HUD_TOGGLE = new KeyMapping("key.qrm.hud", GLFW.GLFW_KEY_H, CATEGORY);

    public static final KeyMapping STAFF = new KeyMapping("key.qrm.staff", GLFW.GLFW_KEY_F7, CATEGORY);
    /** Da tenere premuto: libera il cursore per usare il mouse sul pannello staff. */
    public static final KeyMapping STAFF_CURSOR =
            new KeyMapping("key.qrm.staff.cursor", GLFW.GLFW_KEY_LEFT_ALT, CATEGORY);

    private KeyBindings() {}
}
