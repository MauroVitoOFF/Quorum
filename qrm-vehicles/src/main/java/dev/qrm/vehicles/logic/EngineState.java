package dev.qrm.vehicles.logic;

/** In che condizione si trova il motore di un veicolo. */
public enum EngineState {
    /** Spento: nessuna spinta, nessun consumo. */
    OFF,
    /** Messa in moto: il motorino gira, ancora nessuna spinta. */
    STARTING,
    /** Acceso al minimo, fermo e senza gas. */
    IDLE,
    /** Acceso e in lavoro: gas premuto o veicolo in movimento. */
    RUNNING,
    /** Si è spento da solo (serbatoio vuoto o acqua): per ripartire serve di nuovo l'accensione. */
    STALLED;

    /** Il motore produce spinta e consuma carburante. */
    public boolean isRunning() {
        return this == IDLE || this == RUNNING;
    }

    /** Valore per la sincronizzazione sulla rete. */
    public int id() {
        return ordinal();
    }

    /** Lo stato con questo id; un valore sconosciuto vale spento. */
    public static EngineState byId(int id) {
        EngineState[] all = values();
        return id >= 0 && id < all.length ? all[id] : OFF;
    }
}
