package dev.qrm.immersion.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** I tasti del linguaggio del corpo. */
final class ImmersionKeys {
    static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath("qrm_immersion", "main"));
    static final KeyMapping HANDS_UP = new KeyMapping("key.qrm_immersion.hands_up", GLFW.GLFW_KEY_X, CATEGORY);
    static final KeyMapping POINT = new KeyMapping("key.qrm_immersion.point", GLFW.GLFW_KEY_B, CATEGORY);

    private ImmersionKeys() {}
}
