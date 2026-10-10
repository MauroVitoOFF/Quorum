package dev.qrm.locker.client;

import dev.qrm.client.api.theme.ThemeFragments;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.ContainerScreen;
import net.minecraft.resources.Identifier;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

/** Parte client dell'armadietto. Caricata solo sul client. */
@Mod(value = "qrm_locker", dist = Dist.CLIENT)
public final class LockerClient {
    public LockerClient(IEventBus modBus) {
        ThemeFragments.register("qrm_locker", Identifier.fromNamespaceAndPath("qrm_locker", "theme/qrm.json"));
        NeoForge.EVENT_BUS.addListener(LockerClient::onOpening);
    }

    private static void onOpening(ScreenEvent.Opening event) {
        if (!(event.getNewScreen() instanceof ContainerScreen screen) || screen instanceof LockerScreen) return;
        if (screen.getMenu().getRowCount() != 3 || !LockerClientBridge.consume()) return;
        event.setNewScreen(new LockerScreen(screen.getMenu(), Minecraft.getInstance().player.getInventory(),
                screen.getTitle()));
    }
}
