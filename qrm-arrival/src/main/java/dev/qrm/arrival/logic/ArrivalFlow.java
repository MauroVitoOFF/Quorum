package dev.qrm.arrival.logic;

import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.id.CharacterId;
import dev.qrm.arrival.logic.ArrivalSession.Phase;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Il risveglio dei giocatori, senza Minecraft: chi parte, chi finisce, chi viene interrotto. Lo strato Minecraft gli
 * presta le azioni vere ({@link Hooks}) e lo chiama dal server, sempre dal thread principale.
 */
public final class ArrivalFlow {
    /** Le azioni sul gioco. */
    public interface Hooks {
        /** @return false se il punto non è raggiungibile (dimensione inesistente, giocatore non online): niente arrivo */
        boolean teleport(UUID player, ArrivalPoint point);

        void giveKit(UUID player);

        /** Avvisa i client della fase corrente. */
        void phase(UUID player, Phase phase, int elapsed);
    }

    private record Active(CharacterId character, ArrivalSession session) {}

    private final ArrivalPolicy policy;
    private final ArrivalStorage storage;
    private final ScenarioPicker picker;
    private final ArrivalFiles.Timing timing;
    private final Hooks hooks;
    private final Map<UUID, Active> sessions = new HashMap<>();

    public ArrivalFlow(ArrivalPolicy policy, ArrivalStorage storage, ScenarioPicker picker,
            ArrivalFiles.Timing timing, Hooks hooks) {
        this.policy = policy;
        this.storage = storage;
        this.picker = picker;
        this.timing = timing;
        this.hooks = hooks;
    }

    /**
     * Un Character è stato selezionato.
     *
     * @return true se è partito un arrivo
     */
    public boolean onSelected(UUID player, PlayerCharacter character) {
        Active current = sessions.get(player);
        if (current != null) {
            if (current.character().equals(character.id())) return false;
            forget(player);
        }
        if (!policy.shouldArrive(character)) return false;
        Optional<ArrivalPoint> point = picker.pick().flatMap(Scenario::point);
        if (point.isEmpty()) return false;
        if (!hooks.teleport(player, point.get())) return false;
        sessions.put(player, new Active(character.id(),
                new ArrivalSession(timing.lyingTicks(), timing.risingTicks(), timing.maxLockTicks())));
        hooks.phase(player, Phase.LYING, 0);
        return true;
    }

    /**
     * Un Character è stato creato. Il primo Character di un giocatore diventa attivo senza che il Core annunci una
     * selezione: se è anche quello attivo, l'arrivo parte qui.
     */
    public boolean onCreated(UUID player, PlayerCharacter created, Optional<CharacterId> active) {
        if (active.isEmpty() || !active.get().equals(created.id())) return false;
        return onSelected(player, created);
    }

    /** Un tick di gioco del giocatore: fa avanzare l'animazione e, a risveglio finito, dà il kit e scrive il contrassegno. */
    public void tick(UUID player) {
        Active a = sessions.get(player);
        if (a == null) return;
        Phase phase = a.session().tick();
        hooks.phase(player, phase, a.session().elapsed());
        if (phase == Phase.DONE) {
            sessions.remove(player);
            if (a.session().completed()) {
                hooks.giveKit(player);
                storage.markArrived(a.character());
            }
        }
    }

    /** Il giocatore esce, muore o cambia Character: l'arrivo si interrompe, senza kit né contrassegno. */
    public void forget(UUID player) {
        Active a = sessions.remove(player);
        if (a == null) return;
        a.session().abort();
        hooks.phase(player, Phase.ABORTED, a.session().elapsed());
    }

    /** Vero finché il giocatore non può muoversi né agire. */
    public boolean locked(UUID player) {
        Active a = sessions.get(player);
        return a != null && a.session().locksPlayer();
    }

    /** Interrompe tutti gli arrivi in corso, avvisando i client, così nessuno resta al buio o disteso. */
    public void reset() {
        for (UUID player : List.copyOf(sessions.keySet())) forget(player);
    }
}
