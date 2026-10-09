package dev.qrm.example;

import dev.qrm.client.api.hud.HudLine;
import dev.qrm.client.api.hud.HudRows;
import dev.qrm.client.api.theme.ThemeFragments;
import java.util.Optional;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

/**
 * Parte client del mod di esempio: mostra come usare l'API client di QRM (frammento di tema, riga dell'HUD, schermata).
 * Caricata solo sul client: il server dedicato non la tocca mai.
 */
@Mod(value = "qrm_example", dist = Dist.CLIENT)
public final class ExampleClient {
    private static final String MOD_ID = "qrm_example";
    private static final KeyMapping.Category CATEGORY =
            new KeyMapping.Category(Identifier.fromNamespaceAndPath(MOD_ID, "main"));
    private static final KeyMapping DEMO = new KeyMapping("key.qrm_example.demo", GLFW.GLFW_KEY_U, CATEGORY);

    public ExampleClient(IEventBus modBus) {
        // Chiavi di tema del modulo: devono iniziare per "qrm_example."; un resource pack può sostituirne i valori.
        ThemeFragments.register(MOD_ID, Identifier.fromNamespaceAndPath(MOD_ID, "theme/qrm.json"));
        // Riga dell'HUD: compare solo se il server la elenca in gui.hud.elements = [..., "qrm_example:demo"].
        HudRows.register(MOD_ID + ":demo", () -> Optional.of(
                new HudLine(Component.translatable("qrm_example.hud.demo").getString(), "qrm_example.accent", "statusOn")));
        modBus.addListener(ExampleClient::onRegisterKeys);
        NeoForge.EVENT_BUS.addListener(ExampleClient::onClientTick);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(CATEGORY);
        event.register(DEMO);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        while (DEMO.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null && mc.gui.screen() == null) mc.setScreenAndShow(new ExampleScreen());
        }
    }
}
