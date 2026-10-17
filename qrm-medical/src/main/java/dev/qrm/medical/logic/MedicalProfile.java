package dev.qrm.medical.logic;

import java.util.ArrayList;
import java.util.List;

/** Stato medico di una Character. Modificabile; lo gestisce {@link MedicalRules} sul thread del server. */
public final class MedicalProfile {
    public static final int MAX_INJURIES = 8;

    private MedicalState state = MedicalState.HEALTHY;
    private int bleedOutTicks;
    private int downedTicks;
    private final List<InjuryRecord> injuries = new ArrayList<>();

    public MedicalState state() {
        return state;
    }

    public void setState(MedicalState state) {
        this.state = state;
    }

    public int bleedOutTicks() {
        return bleedOutTicks;
    }

    public void setBleedOutTicks(int ticks) {
        this.bleedOutTicks = Math.max(0, ticks);
    }

    /** Tick trascorsi a terra, senza contare le pause per la RCP. */
    public int downedTicks() {
        return downedTicks;
    }

    public void setDownedTicks(int ticks) {
        this.downedTicks = Math.max(0, ticks);
    }

    /** Dalla più vecchia alla più recente. */
    public List<InjuryRecord> injuries() {
        return List.copyOf(injuries);
    }

    public void addInjury(InjuryRecord r) {
        injuries.add(r);
        while (injuries.size() > MAX_INJURIES) injuries.remove(0);
    }

    public void clearInjuries() {
        injuries.clear();
    }
}
