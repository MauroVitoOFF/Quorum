package dev.qrm.arrival.logic;

import java.util.Optional;
import java.util.function.Supplier;

/** Il risveglio a terra in un vicolo della città. Il punto lo imposta lo staff. */
public final class AlleyScenario implements Scenario {
    public static final String ID = "alley";

    private final Supplier<Optional<ArrivalPoint>> source;

    public AlleyScenario(Supplier<Optional<ArrivalPoint>> source) {
        this.source = source;
    }

    @Override
    public String id() {
        return ID;
    }

    @Override
    public Optional<ArrivalPoint> point() {
        return source.get();
    }
}
