package dev.qrm.neoforge.client;

import dev.qrm.api.staff.StaffNodes;
import java.util.List;

/** Permessi staff comunicati dal server: servono solo a filtrare la UI (il server ricontrolla tutto). */
public final class ClientStaffAccess {
    private record State(boolean all, List<String> patterns) {}

    private static volatile State state = new State(false, List.of());

    private ClientStaffAccess() {}

    public static void set(boolean all, List<String> patterns) {
        state = new State(all, List.copyOf(patterns));
    }

    public static boolean allows(String node) {
        State s = state;
        return s.all() || StaffNodes.matchesAny(s.patterns(), node);
    }

    public static void reset() {
        state = new State(false, List.of());
    }
}
