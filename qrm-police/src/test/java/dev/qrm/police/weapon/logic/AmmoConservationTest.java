package dev.qrm.police.weapon.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;

import dev.qrm.police.weapon.logic.SessionOutcome.Status;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** Nessuna sequenza di azioni, annullamenti o fallimenti crea o distrugge colpi. */
class AmmoConservationTest {
    private static final int CAPACITY = 17;
    private static final PistolAction[] STARTABLE = {
        PistolAction.DRAW, PistolAction.RELOAD_TACTICAL, PistolAction.RELOAD_EMPTY, PistolAction.SLIDE_RELEASE,
        PistolAction.SLIDE_RACK, PistolAction.AMMO_CHECK, PistolAction.INSPECT
    };

    private static MagazineState randomMag(Random rnd) {
        return new MagazineState(UUID.randomUUID(), rnd.nextInt(CAPACITY + 1), CAPACITY, Set.of());
    }

    private static int weaponRounds(WeaponState s) {
        return (s.chambered() ? 1 : 0) + s.magazine().map(MagazineState::rounds).orElse(0);
    }

    @Test
    void randomSequencesNeverCreateOrDestroyRounds() {
        Random rnd = new Random(20261020L);
        for (int seq = 0; seq < 10_000; seq++) {
            FakeInventory inv = new FakeInventory(rnd.nextInt(5));
            int spares = rnd.nextInt(5);
            for (int i = 0; i < spares; i++) {
                inv.magazines.add(randomMag(rnd));
            }
            boolean chambered = rnd.nextBoolean();
            WeaponState state = new WeaponState(UUID.randomUUID(),
                    rnd.nextBoolean() ? Optional.of(randomMag(rnd)) : Optional.empty(),
                    chambered, !chambered && rnd.nextBoolean());

            int initial = weaponRounds(state) + inv.totalRounds();
            int fired = 0;
            int steps = 1 + rnd.nextInt(30);
            for (int step = 0; step < steps; step++) {
                int choice = rnd.nextInt(10);
                if (choice < 3) {
                    FireResult r = PistolRules.fire(state);
                    if (r.fired()) {
                        fired++;
                    }
                    state = r.state();
                    continue;
                }
                if (choice == 3 && !inv.magazines.isEmpty()) {
                    // il giocatore butta a terra una scorta fuori da ogni sessione
                    inv.throwAway(inv.magazines.get(rnd.nextInt(inv.magazines.size())));
                    continue;
                }
                PistolAction action = STARTABLE[rnd.nextInt(STARTABLE.length)];
                Optional<ActionSession> started = ActionSession.start(action, state, inv, "c" + rnd.nextInt(3));
                if (started.isEmpty()) {
                    continue;
                }
                ActionSession session = started.get();
                int cancelAt = rnd.nextBoolean() ? rnd.nextInt(action.durationTicks() + 1) + 1 : Integer.MAX_VALUE;
                SessionOutcome o;
                int tick = 0;
                do {
                    tick++;
                    if (rnd.nextInt(40) == 0 && !inv.magazines.isEmpty()) {
                        // durante la sessione una scorta viene spostata a terra
                        inv.throwAway(inv.magazines.get(rnd.nextInt(inv.magazines.size())));
                    }
                    boolean cancel = tick == cancelAt;
                    o = session.tick(!cancel || rnd.nextBoolean(), cancel && rnd.nextBoolean());
                    if (cancel && o.status() == Status.RUNNING) {
                        // né cambio arma né incapacitazione estratti: lo forziamo
                        o = session.tick(false, false);
                    }
                } while (o.status() == Status.RUNNING);
                state = o.state();
            }
            int fin = weaponRounds(state) + inv.totalRounds();
            assertEquals(initial, fin + fired, "sequenza " + seq);
        }
    }
}
