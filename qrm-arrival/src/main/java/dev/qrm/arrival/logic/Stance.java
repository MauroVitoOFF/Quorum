package dev.qrm.arrival.logic;

import dev.qrm.arrival.logic.ArrivalSession.Phase;

/** La posizione del corpo durante il risveglio: a terra, accovacciato, in piedi. Lo strato Minecraft la traduce in una posa. */
public enum Stance {
    LYING, CROUCHING, STANDING;

    /** A terra finché dura LYING; il rialzo passa dall'accovacciato (prima metà) e poi in piedi. */
    public static Stance of(Phase phase, int elapsed, int lyingTicks, int risingTicks) {
        return switch (phase) {
            case LYING -> LYING;
            case RISING -> elapsed < lyingTicks + risingTicks / 2 ? CROUCHING : STANDING;
            case DONE, ABORTED -> STANDING;
        };
    }
}
