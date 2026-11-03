package dev.qrm.arrival.logic;

import java.util.Optional;

/** Un modo di arrivare: dove si risveglia il personaggio. Vuoto se lo scenario non è utilizzabile (punto non impostato). */
public interface Scenario {
    String id();

    Optional<ArrivalPoint> point();
}
