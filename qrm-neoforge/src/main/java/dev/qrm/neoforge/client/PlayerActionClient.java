package dev.qrm.neoforge.client;

import dev.qrm.client.api.staff.StaffPanelModel;
import dev.qrm.neoforge.net.PanelSnapshot;
import dev.qrm.neoforge.net.PlayerAction;
import dev.qrm.neoforge.net.PlayerActionPayload;
import dev.qrm.neoforge.net.PlayerActionResultPayload;
import dev.qrm.neoforge.net.PlayerResult;
import java.util.List;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Lato client delle azioni sul giocatore: invio del pacchetto e presentazione dell'esito. */
public final class PlayerActionClient {
    private PlayerActionClient() {}

    public static void send(PlayerAction action, UUID target, String arg) {
        ClientPacketDistributor.sendToServer(new PlayerActionPayload(action, target, arg));
    }

    /** Esito dal server: le azioni informative aprono una pagina nel pannello, le altre mostrano un messaggio. */
    public static void onResult(PlayerActionResultPayload payload) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (payload.result() == PlayerResult.OK && !payload.lines().isEmpty()) {
            List<StaffPanelModel.InfoLine> lines = payload.lines().stream()
                    .map(l -> new StaffPanelModel.InfoLine(l.labelKey(), l.value())).toList();
            StaffPanel.showInfo("qrm.staff.player.action." + payload.action().id(), lines);
            return;
        }
        String name = PanelData.snapshot().flatMap(s -> s.players().stream()
                        .filter(p -> p.id().equals(payload.target())).map(PanelSnapshot.PlayerEntry::name).findFirst())
                .orElse("?");
        boolean good = switch (payload.result()) {
            case OK, FROZEN, UNFROZEN -> true;
            default -> false;
        };
        String key = payload.result() == PlayerResult.OK ? "qrm.staff.player.done." + payload.action().id()
                : "qrm.staff.player.result." + payload.result().name();
        mc.player.sendOverlayMessage(Component.translatable(key, name).withStyle(good ? ChatFormatting.GREEN : ChatFormatting.RED));
    }
}
