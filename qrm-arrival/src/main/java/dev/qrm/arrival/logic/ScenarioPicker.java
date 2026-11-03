package dev.qrm.arrival.logic;

import java.util.List;
import java.util.Optional;
import java.util.random.RandomGenerator;

/** Sceglie a caso lo scenario d'arrivo tra quelli utilizzabili. */
public final class ScenarioPicker {
    private final List<Scenario> all;
    private final RandomGenerator random;

    public ScenarioPicker(List<Scenario> all, RandomGenerator random) {
        this.all = List.copyOf(all);
        this.random = random;
    }

    public Optional<Scenario> pick() {
        List<Scenario> usable = all.stream().filter(s -> s.point().isPresent()).toList();
        if (usable.isEmpty()) return Optional.empty();
        return Optional.of(usable.get(random.nextInt(usable.size())));
    }
}
