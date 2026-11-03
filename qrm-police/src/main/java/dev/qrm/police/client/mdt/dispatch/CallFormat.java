package dev.qrm.police.client.mdt.dispatch;

import static dev.qrm.police.client.mdt.dispatch.CallFormat.*;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.BLUE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.DIM;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.GREEN;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.ORANGE;
import static dev.qrm.police.client.mdt.ui.MdtWidgets.RED;

import com.google.gson.JsonObject;
import dev.qrm.police.logic.StatoChiamata;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;

/** Come si leggono e si mostrano le chiamate del Dispatch: stato, priorità e distanza. */
public final class CallFormat {
    private CallFormat() {}

    public static final int[] STATE_COLORS = {ORANGE, BLUE, BLUE, GREEN, GREEN, RED, DIM, DIM};

    public static int stateColor(StatoChiamata s) {
        return STATE_COLORS[s.ordinal()];
    }

    public static StatoChiamata stateOf(JsonObject call) {
        return StatoChiamata.parse(str(call, "state"), StatoChiamata.PENDING);
    }

    public static int prioOf(JsonObject call) {
        return Math.max(1, Math.min(4, call.get("prio").getAsInt()));
    }

    /** Distanza in linea retta; "?" se la chiamata è in un altro mondo (non si dice quale). */
    public static String distance(JsonObject call) {
        var p = Minecraft.getInstance().player;
        var level = Minecraft.getInstance().level;
        if (p == null || level == null) return "?";
        if (!level.dimension().identifier().toString().equals(str(call, "dim"))) return "?";
        double dx = call.get("x").getAsInt() + 0.5 - p.getX();
        double dz = call.get("z").getAsInt() + 0.5 - p.getZ();
        return (int) Math.sqrt(dx * dx + dz * dz) + " m (in linea retta)";
    }

    /** Metri in linea retta dal giocatore; {@link Integer#MAX_VALUE} se non calcolabile (altro mondo). */
    public static int meters(JsonObject call) {
        var p = Minecraft.getInstance().player;
        var level = Minecraft.getInstance().level;
        if (p == null || level == null) return Integer.MAX_VALUE;
        if (!level.dimension().identifier().toString().equals(str(call, "dim"))) return Integer.MAX_VALUE;
        double dx = call.get("x").getAsInt() + 0.5 - p.getX();
        double dz = call.get("z").getAsInt() + 0.5 - p.getZ();
        return (int) Math.sqrt(dx * dx + dz * dz);
    }

    /** Distanza breve per le righe: "120 m", oppure "?" se non calcolabile. */
    public static String distanceShort(JsonObject call) {
        String d = distance(call);
        int i = d.indexOf(" (");
        return i < 0 ? d : d.substring(0, i);
    }

    public static final int CALL_ROW_H = 32;

    /** Le chiamate aperte: prima le più gravi, a parità quella in attesa da più tempo. */
    public static List<JsonObject> sorted(List<JsonObject> calls) {
        List<JsonObject> out = new ArrayList<>(calls);
        out.sort((a, b) -> {
            int byPriority = Integer.compare(prioOf(a), prioOf(b));
            return byPriority != 0 ? byPriority : Long.compare(a.get("time").getAsLong(), b.get("time").getAsLong());
        });
        return out;
    }

    private static String str(JsonObject o, String key) {
        return o.get(key).getAsString();
    }

}
