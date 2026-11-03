package dev.qrm.immersion.client;

import dev.qrm.immersion.logic.BodyPose;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Cio' che il client sa: la posa delle entita' viste e chi il giocatore conosce. */
final class ClientImmersion {
    private static final Map<Integer, BodyPose> POSES = new HashMap<>();
    private static final Map<UUID, Label> KNOWN = new HashMap<>();

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

    /** Nome (nullo se sconosciuto) e codice da mostrare, come li ha decisi il server. */
    record Label(String name, String code) {}

    /** L'etichetta decisa dal server per un'entita', o nulla se non ne ha ancora mandata una. */
    static Label labelOf(UUID id) {
        return KNOWN.get(id);
    }

    static void setKnown(Map<UUID, Label> all) {
        KNOWN.clear();
        KNOWN.putAll(all);
    }

    static void reset() {
        POSES.clear();
        KNOWN.clear();
    }
}
