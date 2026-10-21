package dev.qrm.police.weapon.logic;

/**
 * Decisioni pure del server sulle intenzioni del giocatore: quale azione avviare e come trattare una sessione già in
 * corso. Nessuno stato.
 */
public final class PistolIntents {
    private PistolIntents() {}

    /** Esito di una nuova richiesta rispetto alla sessione in corso. */
    public enum Admission {
        /** Nessuna sessione in corso: si procede. */
        START,
        /** C'è una sessione che non blocca lo sparo: va annullata, poi si procede. */
        PREEMPT_AND_START,
        /** C'è una sessione che blocca lo sparo (estrazione, ricarica, carrello): la richiesta è rifiutata. */
        REFUSE
    }

    /** Ricarica tattica se c'è il colpo in canna, altrimenti ricarica a vuoto. */
    public static PistolAction reloadFor(WeaponState s) {
        return s.chambered() ? PistolAction.RELOAD_TACTICAL : PistolAction.RELOAD_EMPTY;
    }

    /** Pressione breve del tasto di interazione con la pistola in mano: il carrello ha la precedenza sul controllo. */
    public static PistolAction tapFor(WeaponState s) {
        return switch (PistolRules.slideActionFor(s)) {
            case RELEASE -> PistolAction.SLIDE_RELEASE;
            case RACK -> PistolAction.SLIDE_RACK;
            case NONE -> PistolAction.AMMO_CHECK;
        };
    }

    /**
     * Come trattare una nuova richiesta (azione o sparo) quando è in corso {@code running} ({@code null} = nessuna
     * sessione).
     */
    public static Admission admit(PistolAction running) {
        if (running == null) return Admission.START;
        return running.blocksFire() ? Admission.REFUSE : Admission.PREEMPT_AND_START;
    }

    /** La mira si accende solo con la pistola in mano e il giocatore non a terra. */
    public static boolean allowsAim(boolean holdingPistol, boolean incapacitated) {
        return holdingPistol && !incapacitated;
    }
}
