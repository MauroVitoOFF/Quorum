package dev.qrm.medical.client;

import dev.qrm.client.api.body.BodyPart;
import dev.qrm.client.api.body.BodyStatus;

/**
 * La gravità di ogni zona del giocatore locale. Per ora la scrive solo il comando di prova {@code /qrmbody}: quando
 * esisterà la logica delle ferite per zona, arriverà dal server e passerà da qui.
 */
final class ClientBody {
    private static final float[] SEVERITY = new float[BodyPart.values().length];
    private static final BodyStatus STATUS = part -> SEVERITY[part.ordinal()];

    private ClientBody() {}

    static BodyStatus status() {
        return STATUS;
    }

    static void set(BodyPart part, float severity) {
        SEVERITY[part.ordinal()] = Math.max(0f, Math.min(1f, severity));
    }

    static void clear() {
        java.util.Arrays.fill(SEVERITY, 0f);
    }
}
