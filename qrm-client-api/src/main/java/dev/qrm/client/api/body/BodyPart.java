package dev.qrm.client.api.body;

import java.util.Locale;

/**
 * Le sette zone della sagoma. Destra e sinistra sono quelle del personaggio: guardandolo di fronte, il braccio
 * destro sta a sinistra dello schermo.
 */
public enum BodyPart {
    HEAD, CHEST, ABDOMEN, ARM_RIGHT, ARM_LEFT, LEG_RIGHT, LEG_LEFT;

    /** Nome usato per il file della texture, per esempio {@code arm_right}. */
    public String id() {
        return name().toLowerCase(Locale.ROOT);
    }
}
