package dev.qrm.neoforge.client;

import dev.qrm.client.api.theme.QrmTheme;
import dev.qrm.neoforge.QrmMod;
import dev.qrm.neoforge.client.screen.HubScreen;
import dev.qrm.neoforge.client.theme.BuiltInTheme;
import dev.qrm.neoforge.client.theme.ThemeLoader;
import dev.qrm.neoforge.net.RosterRequestPayload;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Punto d'ingresso solo client: tasto, tema, promemoria. Il server dedicato non carica mai questa classe. */
@Mod(value = QrmMod.MOD_ID, dist = Dist.CLIENT)
public final class QrmClient {
    private static final int REMINDER_TICKS = 30 * 20;
    private static final Logger LOG = LoggerFactory.getLogger("qrm");
    private static int reminderTicks;
    /** Ultimo motivo di sospensione registrato nel log, per scrivere una riga solo quando cambia. */
    private static String lastBlockedReason = "";

    public QrmClient(IEventBus modBus, ModContainer container) {
        QrmTheme.set(BuiltInTheme.manifest());
        CoreStaffEntries.register();
        modBus.addListener(QrmClient::onRegisterKeys);
        modBus.addListener(QrmClient::onAddReloadListeners);
        modBus.addListener(QrmClient::onRegisterGuiLayers);
        container.registerConfig(ModConfig.Type.CLIENT, ClientConfig.SPEC);
        NeoForge.EVENT_BUS.addListener(QrmClient::onClientTick);
        NeoForge.EVENT_BUS.addListener(QrmClient::onLoggingOut);
        NeoForge.EVENT_BUS.addListener(QrmClient::onKey);
        NeoForge.EVENT_BUS.addListener(QrmClient::onMouse);
        NeoForge.EVENT_BUS.addListener(QrmClient::onScroll);
    }

    private static void onRegisterKeys(RegisterKeyMappingsEvent event) {
        event.registerCategory(KeyBindings.CATEGORY);
        event.register(KeyBindings.CHARACTERS);
        event.register(KeyBindings.HUB);
        event.register(KeyBindings.HUD_TOGGLE);
        event.register(KeyBindings.STAFF);
        event.register(KeyBindings.STAFF_CURSOR);
    }

    private static void onAddReloadListeners(AddClientReloadListenersEvent event) {
        event.addListener(ThemeLoader.ID, new ThemeLoader());
    }

    private static void onRegisterGuiLayers(RegisterGuiLayersEvent event) {
        HudOverlay.register(event);
        StaffPanel.register(event);
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        Minecraft mc = Minecraft.getInstance();
        while (KeyBindings.CHARACTERS.consumeClick()) {
            if (mc.player != null && ClientState.serverHasQrm()) {
                ClientPacketDistributor.sendToServer(new RosterRequestPayload());
            }
        }
        while (KeyBindings.HUD_TOGGLE.consumeClick()) {
            ClientConfig.toggleHud();
        }
        while (KeyBindings.HUB.consumeClick()) {
            if (mc.player != null && ClientState.serverHasQrm()) HubScreen.open();
        }
        while (KeyBindings.STAFF.consumeClick()) {
            if (mc.player != null && mc.gui.screen() == null) StaffPanel.toggle(mc);
        }
        StaffPanel.tick(mc);
        var roster = ClientState.roster();
        String blocked = ReminderPolicy.blockedBy(mc.player != null, mc.gui.screen() == null, ClientState.serverHasQrm(),
                roster != null, ClientState.hasActiveCharacter(), roster == null ? 0 : roster.cards().size(),
                roster == null ? 0 : roster.slots());
        if (!Objects.equals(blocked, lastBlockedReason)) {
            lastBlockedReason = blocked;
            LOG.debug("QRM reminder: {}", blocked == null ? "counting" : "suspended (" + blocked + ")");
        }
        if (blocked == null) {
            if (++reminderTicks >= REMINDER_TICKS) {
                reminderTicks = 0;
                LOG.debug("QRM reminder: shown");
                mc.player.sendOverlayMessage(Component.translatable("qrm.character.reminder",
                        KeyBindings.CHARACTERS.getTranslatedKeyMessage()));
            }
        } else {
            reminderTicks = 0;
        }
    }

    private static void onKey(InputEvent.Key event) {
        StaffPanel.onKey(event);
    }

    private static void onMouse(InputEvent.MouseButton.Pre event) {
        StaffPanel.onMouse(event);
    }

    private static void onScroll(InputEvent.MouseScrollingEvent event) {
        StaffPanel.onScroll(event);
    }

    private static void onLoggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        ClientState.reset();
        StaffPanel.reset();
        lastBlockedReason = "";
    }
}
