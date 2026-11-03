package dev.qrm.arrival.logic;

import dev.qrm.api.id.CharacterId;
import java.time.Instant;

/** Dove si ricorda quali Character hanno già vissuto l'arrivo, e da quando il Module è installato. */
public interface ArrivalStorage {
    boolean hasArrived(CharacterId character);

    void markArrived(CharacterId character);

    /** Il momento della prima installazione del Module: i Character creati prima non vivono l'arrivo. */
    Instant installedAt();
}
