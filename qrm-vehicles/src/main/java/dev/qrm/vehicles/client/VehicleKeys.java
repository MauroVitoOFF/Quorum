package dev.qrm.vehicles.client;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import org.lwjgl.glfw.GLFW;

/** I tasti del guidatore. Gas, retromarcia e sterzo usano i tasti di movimento, il freno la barra spaziatrice. */
public final class VehicleKeys {
    public static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath("qrm_vehicles", "main"));
    public static final KeyMapping IGNITION = new KeyMapping("key.qrm_vehicles.ignition", GLFW.GLFW_KEY_Y, CATEGORY);
    public static final KeyMapping LIGHTS = new KeyMapping("key.qrm_vehicles.lights", GLFW.GLFW_KEY_L, CATEGORY);
    public static final KeyMapping HORN = new KeyMapping("key.qrm_vehicles.horn", GLFW.GLFW_KEY_N, CATEGORY);

    private VehicleKeys() {}

    /** Da registrare sul bus della mod, lato client. */
    public static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(IGNITION);
        event.register(LIGHTS);
        event.register(HORN);
    }
}
