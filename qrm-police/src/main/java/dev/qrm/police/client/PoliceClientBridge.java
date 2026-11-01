package dev.qrm.police.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.qrm.police.client.mdt.PoliceMdtScreen;
import dev.qrm.police.net.PolicePayloads;
import net.minecraft.client.Minecraft;

/** Ponte dal canale di rete alla UI: caricata solo quando arriva davvero un pacchetto (mai sul server dedicato). */
public final class PoliceClientBridge {
    private PoliceClientBridge() {}

    public static void onSnapshot(PolicePayloads.Snapshot snap) {
        JsonObject data;
        try {
            data = JsonParser.parseString(snap.json()).getAsJsonObject();
        } catch (RuntimeException e) {
            return;
        }
        ClientPolice.Waypoint w = ClientPolice.waypoint();
        if (w != null && !w.callId().startsWith("unit:") && !hasCall(data, w.callId())) ClientPolice.clearWaypoint();

        Minecraft mc = Minecraft.getInstance();
        if (snap.open()) {
            mc.setScreenAndShow(new PoliceMdtScreen(snap.officer(), data));
        } else if (mc.gui.screen() instanceof PoliceMdtScreen screen) {
            screen.update(snap.officer(), data);
        }
    }

    public static void onReply(PolicePayloads.Reply reply) {
        JsonElement parsed;
        try {
            parsed = JsonParser.parseString(reply.json());
        } catch (RuntimeException e) {
            return;
        }
        if (parsed.isJsonArray() && Minecraft.getInstance().gui.screen() instanceof PoliceMdtScreen screen) {
            screen.onReply(reply.kind(), parsed.getAsJsonArray());
        }
    }

    private static boolean hasCall(JsonObject data, String id) {
        for (JsonElement e : data.getAsJsonArray("calls")) {
            if (e.getAsJsonObject().get("id").getAsString().equals(id)) return true;
        }
        return false;
    }
}
