package dev.qrm.police.weapon.logic.input;

import dev.qrm.police.weapon.logic.FireResult;
import dev.qrm.police.weapon.logic.PistolAction;
import dev.qrm.police.weapon.logic.PistolRules;
import dev.qrm.police.weapon.logic.PistolTimings;
import dev.qrm.police.weapon.logic.WeaponState;

/**
 * Decisione del client quando il giocatore preme "attacca" con la pistola: se mandare lo sparo e quale clip avviare
 * subito (previsione). Non decide nulla di autoritativo: munizioni, colpi a segno e danno restano al server.
 *
 * <p>Regole, le stesse del server: l'intervallo minimo di {@link PistolTimings#FIRE_COOLDOWN_TICKS} tick tra due
 * pressioni e nessuno sparo mentre è in corso un'azione che {@linkplain PistolAction#blocksFire() blocca lo sparo}.
 * Un'azione che non lo blocca (controllo, ispezione) viene interrotta, come fa il server.
 *
 * <p>Previsione: si applica {@link PistolRules#fire} allo stato dell'arma che il client vede nello stack. Se lo
 * stack è ancora quello su cui si è basata la previsione precedente (il server non l'ha ancora aggiornato), si parte
 * dallo stato previsto, così due colpi ravvicinati con molta latenza danno comunque {@code fire_last} al momento
 * giusto. La previsione si dimentica dopo un rifiuto del server o dopo {@link #PREDICTION_TTL_TICKS} tick.
 *
 * <p>Arma disabilitata ({@link #setDisabled}, giocatore a terra secondo il server): ogni pressione è
 * {@link Outcome#BLOCKED}, quindi niente clip, suono, lampo, contraccolpo né pacchetto.
 *
 * <p>Estrazione prevista ({@link #predictAction}): appena la pistola compare in mano il client blocca lo sparo per la
 * durata di {@code DRAW} senza aspettare {@code ActionStarted} (un RTT dopo). Un'azione prevista scade alla sua durata,
 * senza margine; quando il server la annuncia vale di nuovo il margine {@link #ACTION_GRACE_TICKS}.
 *
 * <p>Il tempo è un contatore di tick del client qualsiasi, purché crescente.
 */
public final class FireGate {
    /** Margine oltre la durata dell'azione dopo cui la si considera finita anche senza {@code ActionEnded}. */
    public static final int ACTION_GRACE_TICKS = 20;
    /** Dopo quanti tick una previsione non confermata smette di valere. */
    public static final int PREDICTION_TTL_TICKS = 20;

    private static final long NEVER = Long.MIN_VALUE / 2;

    /** Esito di una pressione. */
    public enum Outcome {
        /** Intervallo non trascorso o azione che blocca lo sparo: niente clip, niente pacchetto. */
        BLOCKED(null),
        FIRE("fire"),
        /** Ultimo colpo: il carrello resta indietro. */
        FIRE_LAST("fire_last"),
        /** Camera vuota: clic a vuoto, solo in locale. */
        DRY("dry_fire");

        private final String clip;

        Outcome(String clip) {
            this.clip = clip;
        }

        /** Clip da avviare, {@code null} per {@link #BLOCKED}. */
        public String clip() {
            return clip;
        }

        /** Vero se la clip è una di quelle che la previsione avvia ({@code fire}, {@code fire_last}, {@code dry_fire}). */
        public static boolean isPredictedClip(String clip) {
            return clip != null && (clip.equals(FIRE.clip) || clip.equals(FIRE_LAST.clip) || clip.equals(DRY.clip));
        }
    }

    /** Esito e stato previsto dell'arma dopo la pressione (uguale a quello di partenza se non spara). */
    public record Decision(Outcome outcome, WeaponState predicted) {}

    private long lastPull = NEVER;
    private PistolAction action;
    private long actionStart;
    /** Margine dopo la durata: {@link #ACTION_GRACE_TICKS} per le azioni annunciate, 0 per quelle previste. */
    private int actionGrace;
    private boolean disabled;
    private WeaponState basis;
    private WeaponState predicted;
    private long predictedAt;

    /** Il server ha avviato un'azione temporizzata per il giocatore locale. */
    public void actionStarted(PistolAction a, long now) {
        action = a;
        actionStart = now;
        actionGrace = ACTION_GRACE_TICKS;
    }

    /** Il client prevede un'azione (estrazione) prima che il server la annunci: scade alla sua durata. */
    public void predictAction(PistolAction a, long now) {
        action = a;
        actionStart = now;
        actionGrace = 0;
    }

    /** Arma disabilitata (giocatore a terra): finché vale, nessuna pressione spara né manda nulla. */
    public void setDisabled(boolean value) {
        disabled = value;
    }

    public boolean disabled() {
        return disabled;
    }

    /** L'azione del giocatore locale è finita o annullata. */
    public void actionEnded() {
        action = null;
    }

    /** Azione in corso nota al client, o {@code null}; scade dopo durata + margine (vedi {@link #predictAction}). */
    public PistolAction running(long now) {
        if (action != null && now - actionStart >= action.durationTicks() + actionGrace) action = null;
        return action;
    }

    /** Una pressione di "attacca" con lo stato dell'arma visto nello stack. */
    public Decision pull(WeaponState state, long now) {
        PistolAction run = running(now);
        if (disabled || (run != null && run.blocksFire()) || !PistolTimings.canFire(lastPull, now)) {
            return new Decision(Outcome.BLOCKED, state);
        }
        lastPull = now;
        // Il server interrompe un'azione che non blocca lo sparo: lo si fa anche qui.
        action = null;

        boolean usePrediction = predicted != null && state.equals(basis) && now - predictedAt <= PREDICTION_TTL_TICKS;
        WeaponState from = usePrediction ? predicted : state;
        FireResult result = PistolRules.fire(from);
        if (!result.fired()) return new Decision(Outcome.DRY, from);
        basis = state;
        predicted = result.state();
        predictedAt = now;
        return new Decision(result.state().slideLocked() ? Outcome.FIRE_LAST : Outcome.FIRE, result.state());
    }

    /** Il server ha rifiutato lo sparo: la previsione non vale più. */
    public void rejected() {
        basis = null;
        predicted = null;
    }

    /** Dimentica tutto (cambio d'arma, uscita dal mondo), tranne l'arma disabilitata: quella la decide il server. */
    public void reset() {
        lastPull = NEVER;
        action = null;
        basis = null;
        predicted = null;
    }
}
