package dev.qrm.police.weapon.logic;

import dev.qrm.police.weapon.logic.SessionOutcome.Status;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Sessione di un'azione temporizzata della pistola. Lo stato cambia solo agli istanti di commit dell'azione;
 * un annullamento lascia tutto com'era all'ultimo commit applicato. Ogni caricatore tolto finisce
 * nell'inventario o a terra: nessuna sequenza crea o distrugge colpi.
 */
public final class ActionSession {
    private final PistolAction action;
    private final MagazineInventory inventory;
    private final String characterId;
    /** Stato dell'arma; {@code null} per le sessioni di ispezione di un caricatore in mano. */
    private WeaponState state;
    /** Caricatore in mano (solo sessioni {@link #inspectMagazine}). */
    private final MagazineState inHand;
    private MagazineState inspected;
    private UUID removedId;
    private AmmoBand revealedBand;
    private int tick;
    private Status status = Status.RUNNING;

    private ActionSession(PistolAction action, WeaponState state, MagazineInventory inventory, String characterId,
            MagazineState inHand) {
        this.action = action;
        this.state = state;
        this.inventory = inventory;
        this.characterId = characterId;
        this.inHand = inHand;
    }

    /**
     * Avvia un'azione sull'arma; vuoto se ora non è possibile: ricarica senza un caricatore di scorta con colpi
     * (diverso da quello inserito), carrello senza l'azione corrispondente, {@code INSPECT_MAG} (si usa
     * {@link #inspectMagazine}). La scelta tra ricarica tattica e a vuoto spetta al chiamante.
     */
    public static Optional<ActionSession> start(PistolAction action, WeaponState state, MagazineInventory inventory,
            String characterId) {
        boolean possible = switch (action) {
            case RELOAD_TACTICAL, RELOAD_EMPTY ->
                    inventory.hasLoaded(state.magazine().map(MagazineState::id).orElse(null));
            case SLIDE_RELEASE -> PistolRules.slideActionFor(state) == SlideAction.RELEASE;
            case SLIDE_RACK -> PistolRules.slideActionFor(state) == SlideAction.RACK;
            case INSPECT_MAG -> false;
            case DRAW, INSPECT, AMMO_CHECK -> true;
        };
        return possible ? Optional.of(new ActionSession(action, state, inventory, characterId, null)) : Optional.empty();
    }

    /**
     * Ispezione di un caricatore tenuto in mano ({@code INSPECT_MAG}). La sessione non riguarda un'arma:
     * {@link #state()} lancia {@link IllegalStateException} e gli esiti hanno {@code state == null};
     * in {@link #tick} {@code sameWeapon} significa "lo stesso caricatore è ancora in mano".
     */
    public static ActionSession inspectMagazine(MagazineState inHand, String characterId) {
        return new ActionSession(PistolAction.INSPECT_MAG, null, null, characterId, inHand);
    }

    /**
     * Avanza di un tick. Se l'arma non è più la stessa o il personaggio è incapacitato la sessione è annullata
     * senza applicare i commit di questo tick; altrimenti applica i commit del tick corrente. Se al commit
     * {@code MAG_IN} non c'è nessun caricatore carico da prendere la sessione finisce annullata: la pistola resta senza
     * caricatore e nessun colpo si sposta.
     *
     * @throws IllegalStateException se la sessione è già conclusa o annullata
     */
    public SessionOutcome tick(boolean sameWeapon, boolean incapacitated) {
        if (status != Status.RUNNING) {
            throw new IllegalStateException("session already " + status);
        }
        tick++;
        if (!sameWeapon || incapacitated) {
            status = Status.CANCELLED;
            return outcome();
        }
        for (Map.Entry<Integer, CommitPoint> c : action.commits()) {
            if (c.getKey() == tick) {
                apply(c.getValue());
                // Un commit fallito (MAG_IN senza caricatori carichi) annulla l'azione: i commit seguenti non valgono.
                if (status != Status.RUNNING) {
                    return outcome();
                }
            }
        }
        if (tick >= action.durationTicks()) {
            status = Status.DONE;
        }
        return outcome();
    }

    private void apply(CommitPoint point) {
        switch (point) {
            case MAG_OUT -> {
                Optional<MagazineState> out = state.magazine();
                if (out.isPresent()) {
                    state = PistolRules.magOut(state);
                    removedId = out.get().id();
                    if (!inventory.store(out.get())) {
                        inventory.drop(out.get());
                    }
                }
            }
            case MAG_IN -> {
                if (state.magazine().isEmpty()) {
                    Optional<MagazineState> next = inventory.takeFullest(removedId);
                    if (next.isPresent()) {
                        state = PistolRules.magIn(state, next.get());
                    } else {
                        // Specifica §7: nessun caricatore carico da inserire, l'azione è annullata e la pistola
                        // resta senza caricatore (quello tolto è già nell'inventario o a terra).
                        status = Status.CANCELLED;
                    }
                }
            }
            case SLIDE -> state = PistolRules.applySlide(state);
            case CHECK -> state.magazine().ifPresent(m -> {
                MagazineState known = m.knownTo(characterId);
                state = new WeaponState(state.id(), Optional.of(known), state.chambered(), state.slideLocked());
                revealedBand = AmmoBand.of(known.rounds(), known.capacity());
            });
            case INSPECT_MAG -> {
                if (inHand != null) {
                    inspected = inHand.knownTo(characterId);
                }
            }
        }
    }

    private SessionOutcome outcome() {
        return new SessionOutcome(status, state, Optional.ofNullable(revealedBand));
    }

    /** L'azione di questa sessione (per esempio per sapere se blocca lo sparo). */
    public PistolAction action() {
        return action;
    }

    /**
     * Stato dell'arma dopo l'ultimo commit applicato.
     *
     * @throws IllegalStateException per le sessioni create da {@link #inspectMagazine}
     */
    public WeaponState state() {
        if (state == null) {
            throw new IllegalStateException("inspect_mag session has no weapon");
        }
        return state;
    }

    /** Caricatore ispezionato, marcato come noto al personaggio, dopo il commit {@code INSPECT_MAG}. */
    public Optional<MagazineState> inspectedMagazine() {
        return Optional.ofNullable(inspected);
    }
}
