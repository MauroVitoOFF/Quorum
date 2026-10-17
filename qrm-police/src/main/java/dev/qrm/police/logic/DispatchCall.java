package dev.qrm.police.logic;

import java.util.ArrayList;
import java.util.List;

/** Una chiamata d'emergenza attiva e le unità che l'hanno presa in carico. */
public final class DispatchCall {
    public final String id;
    public final CallType type;
    public final String description;
    public final String dimension;
    public final int x, y, z;
    public final long time;
    public final String caller;
    public final List<String> units = new ArrayList<>();

    public DispatchCall(String id, CallType type, String description, String dimension, int x, int y, int z, long time,
            String caller) {
        this.id = id;
        this.type = type;
        this.description = description;
        this.dimension = dimension;
        this.x = x;
        this.y = y;
        this.z = z;
        this.time = time;
        this.caller = caller;
    }
}
