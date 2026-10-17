package dev.qrm.medical.logic;

import dev.qrm.api.id.CharacterId;
import java.util.Optional;

/** Dove si ricorda lo stato medico di una Character. */
public interface MedicalStorage {
    Optional<MedicalProfile> read(CharacterId character);

    /** Un profilo sano e senza ferite cancella la riga. */
    void write(CharacterId character, MedicalProfile profile);

    Optional<Hospital> hospital();

    void setHospital(Hospital h);

    /** Il punto di ricovero: dimensione (es. {@code minecraft:overworld}), coordinate e rotazione. */
    record Hospital(String dimension, double x, double y, double z, float yaw) {}
}
