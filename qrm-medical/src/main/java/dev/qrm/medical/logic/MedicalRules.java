package dev.qrm.medical.logic;

/** Le regole di transizione fra gli stati. Pure: nessun Minecraft, tutto testabile. */
public final class MedicalRules {
    public static final int BLEED_OUT_TICKS = 3600;
    /** Sotto questa soglia di tempo rimasto, il ferito perde i sensi. */
    public static final int UNCONSCIOUS_BELOW = 1200;
    /** Tempo minimo a terra prima di poter chiamare i soccorsi con la resa al coma (120 s). */
    public static final int SURRENDER_AFTER_TICKS = 2400;
    /** Quanto va tenuto premuto il tasto di resa (5 s). */
    public static final int SURRENDER_HOLD_TICKS = 100;
    /** Con la RCP il tempo scorre all'80% in meno: avanza un tick su cinque. */
    public static final int RCP_SLOWDOWN = 5;
    public static final int CONVALESCENCE_TICKS = 1200;
    public static final int ALERT_COOLDOWN_TICKS = 600;
    public static final int DEFIBRILLATOR_TICKS = 80;

    private MedicalRules() {}

    /** Il giocatore cade a terra. Riparte il timer; le ferite già registrate restano. */
    public static void down(MedicalProfile p) {
        p.setState(MedicalState.DOWNED);
        p.setBleedOutTicks(BLEED_OUT_TICKS);
        p.setDownedTicks(0);
    }

    /** Il giocatore è di nuovo in piedi: nessun timer, ferite azzerate. */
    public static void revive(MedicalProfile p) {
        p.setState(MedicalState.HEALTHY);
        p.setBleedOutTicks(0);
        p.setDownedTicks(0);
        p.clearInjuries();
    }

    /** Un colpo critico mentre è a terra lo fa svenire. Non fa nulla negli altri stati. */
    public static boolean criticalHit(MedicalProfile p) {
        if (p.state() != MedicalState.DOWNED) return false;
        p.setState(MedicalState.UNCONSCIOUS);
        return true;
    }

    /**
     * Un tick. Con la RCP il dissanguamento rallenta dell'80%.
     *
     * @return il nuovo stato se è cambiato, altrimenti {@code null}
     */
    public static MedicalState tick(MedicalProfile p, boolean rcp, long tickCount) {
        if (!p.state().incapacitated()) return null;
        boolean advance = !rcp || tickCount % RCP_SLOWDOWN == 0;
        if (advance) p.setBleedOutTicks(p.bleedOutTicks() - 1);
        if (!rcp) p.setDownedTicks(p.downedTicks() + 1);
        if (p.bleedOutTicks() <= 0) {
            p.setState(MedicalState.CLINICALLY_DEAD);
            return MedicalState.CLINICALLY_DEAD;
        }
        if (p.state() == MedicalState.DOWNED && p.bleedOutTicks() < UNCONSCIOUS_BELOW) {
            p.setState(MedicalState.UNCONSCIOUS);
            return MedicalState.UNCONSCIOUS;
        }
        return null;
    }

    /** La resa al coma è ammessa solo dopo 120 secondi a terra. */
    public static boolean canSurrender(MedicalProfile p) {
        return p.state().incapacitated() && p.downedTicks() >= SURRENDER_AFTER_TICKS;
    }

    /** Il medico stabilizza il ferito: il timer riparte, ma resta a terra. */
    public static void stabilize(MedicalProfile p) {
        if (!p.state().incapacitated()) return;
        p.setBleedOutTicks(BLEED_OUT_TICKS);
        p.setState(MedicalState.DOWNED);
    }

    /** Il polso: debole nell'ultima parte del tempo e quando è incosciente, a meno che qualcuno faccia la RCP. */
    public static boolean pulseWeak(MedicalProfile p, boolean rcp) {
        if (rcp) return false;
        return p.state() == MedicalState.UNCONSCIOUS || p.bleedOutTicks() < 2 * UNCONSCIOUS_BELOW;
    }
}
