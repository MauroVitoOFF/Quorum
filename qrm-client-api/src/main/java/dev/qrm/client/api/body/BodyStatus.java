package dev.qrm.client.api.body;

/** Quanto è ferita ogni zona: da 0 (illesa) a 1 (fuori uso). Chi disegna la sagoma non sa da dove arriva il dato. */
@FunctionalInterface
public interface BodyStatus {
    BodyStatus HEALTHY = part -> 0f;

    float severity(BodyPart part);

    /** Vero se almeno una zona è ferita. */
    default boolean anyInjured() {
        for (BodyPart part : BodyPart.values()) {
            if (severity(part) > 0f) return true;
        }
        return false;
    }
}
