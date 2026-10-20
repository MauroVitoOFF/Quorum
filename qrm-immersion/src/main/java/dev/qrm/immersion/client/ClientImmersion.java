package dev.qrm.immersion.client;

import dev.qrm.immersion.logic.BodyPose;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Cio' che il client sa: la posa delle entita' viste e chi il giocatore conosce. */
final class ClientImmersion {
    private static final Map<Integer, BodyPose> POSES = new HashMap<>();
    private static final Map<UUID, String> KNOWN = new HashMap<>();

    private ClientImmersion() {}

    static BodyPose poseOf(int entityId) {
        return POSES.getOrDefault(entityId, BodyPose.NONE);
    }

    static void setPose(int entityId, BodyPose pose) {
        if (pose == BodyPose.NONE) POSES.remove(entityId);
        else POSES.put(entityId, pose);
    }

    static void forgetEntity(int entityId) {
        POSES.remove(entityId);
    }

    /** Il nome RP di una persona conosciuta, o nulla se e' una sconosciuta. */
    static String knownName(UUID id) {
        return KNOWN.get(id);
    }

    static void setKnown(Map<UUID, String> all) {
        KNOWN.clear();
        KNOWN.putAll(all);
    }

    static void reset() {
        POSES.clear();
        KNOWN.clear();
    }
}
