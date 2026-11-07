package dev.qrm.vehicles.logic;

/**
 * Lo stato del motore e da quanti tick ci si trova. Le transizioni:
 *
 * <pre>
 * OFF ──accensione──▶ STARTING ──{@value #STARTING_TICKS} tick──▶ IDLE ◀──▶ RUNNING
 *  ▲                      │                                        │
 *  └───── accensione ─────┴────────────────────────────────────────┘
 * STALLED ──accensione (con carburante e fuori dall'acqua)──▶ STARTING
 * STARTING / IDLE / RUNNING ──serbatoio vuoto o in acqua──▶ STALLED
 * </pre>
 *
 * Immutabile e senza Minecraft: il veicolo chiama {@link #toggle} quando si preme l'accensione e {@link #tick} a ogni
 * tick del server.
 */
public record EngineStatus(EngineState state, int ticks) {

    /** Durata della messa in moto, in tick (2 secondi, quanto il suono di avviamento). */
    public static final int STARTING_TICKS = 40;
    /** Sotto questa velocità (m/s) e senza gas il motore è al minimo. */
    public static final double IDLE_SPEED = 0.5;
    /** Gas oltre il quale il motore lavora. */
    public static final double IDLE_THROTTLE = 0.01;

    public static final EngineStatus OFF = new EngineStatus(EngineState.OFF, 0);

    /**
     * L'accensione premuta. Da spento o in stallo si parte se c'è carburante e non si è in acqua; da qualsiasi stato
     * acceso (o durante la messa in moto) si spegne.
     */
    public EngineStatus toggle(boolean hasFuel, boolean blocked) {
        return switch (state) {
            case OFF, STALLED -> hasFuel && !blocked ? new EngineStatus(EngineState.STARTING, 0) : this;
            case STARTING, IDLE, RUNNING -> OFF;
        };
    }

    /**
     * Un tick del server.
     *
     * @param fuelEmpty il serbatoio è vuoto
     * @param blocked il veicolo è in acqua o in lava
     * @param throttle gas richiesto dal guidatore, -1..1
     * @param speedMs velocità in avanti in m/s
     */
    public EngineStatus tick(boolean fuelEmpty, boolean blocked, double throttle, double speedMs) {
        switch (state) {
            case OFF, STALLED:
                return this;
            case STARTING:
                if (fuelEmpty || blocked) return new EngineStatus(EngineState.STALLED, 0);
                return ticks + 1 >= STARTING_TICKS ? new EngineStatus(EngineState.IDLE, 0)
                        : new EngineStatus(EngineState.STARTING, ticks + 1);
            default:
                if (fuelEmpty || blocked) return new EngineStatus(EngineState.STALLED, 0);
                boolean busy = Math.abs(throttle) > IDLE_THROTTLE || Math.abs(speedMs) > IDLE_SPEED;
                EngineState next = busy ? EngineState.RUNNING : EngineState.IDLE;
                return new EngineStatus(next, next == state ? ticks + 1 : 0);
        }
    }
}
