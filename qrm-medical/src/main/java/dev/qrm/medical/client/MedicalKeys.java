package dev.qrm.medical.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

/** I tasti di chi è ferito. Funzionano solo quando si è a terra o svenuti. */
final class MedicalKeys {
    static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath("qrm_medical", "main"));
    static final KeyMapping SURRENDER = new KeyMapping("key.qrm_medical.surrender", GLFW.GLFW_KEY_G, CATEGORY);
    static final KeyMapping ALERT = new KeyMapping("key.qrm_medical.alert", GLFW.GLFW_KEY_C, CATEGORY);

    private MedicalKeys() {}
}
