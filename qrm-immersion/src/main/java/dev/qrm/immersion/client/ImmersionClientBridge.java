package dev.qrm.immersion.client;

import dev.qrm.immersion.logic.BodyPose;
import dev.qrm.immersion.net.ImmersionPayloads;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Punto d'ingresso dei pacchetti sul client. Caricata solo quando un pacchetto arriva davvero. */
public final class ImmersionClientBridge {
    private ImmersionClientBridge() {}

    public static void onPose(int entityId, int pose) {
        ClientImmersion.setPose(entityId, BodyPose.byOrdinal(pose));
    }

    public static void onKnown(List<ImmersionPayloads.KnownEntry> entries) {
        Map<UUID, String> all = new HashMap<>();
        for (ImmersionPayloads.KnownEntry e : entries) all.put(e.id(), e.name());
        ClientImmersion.setKnown(all);
    }
}
