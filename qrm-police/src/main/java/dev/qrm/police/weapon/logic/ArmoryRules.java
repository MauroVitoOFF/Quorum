package dev.qrm.police.weapon.logic;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

/** Regole di rifornimento dell'armeria (logica pura). */
public final class ArmoryRules {
    private ArmoryRules() {}

    /**
     * Rifornisce i caricatori: ognuno di quelli portati viene riempito alla propria capienza e
     * marcato come noto a {@code characterId}; poi si aggiungono caricatori nuovi pieni (di
     * capienza {@code capacity}) finché non si raggiunge {@code target}. Non toglie mai caricatori.
     */
    public static List<MagazineState> restock(
            List<MagazineState> carried, int target, int capacity, String characterId, Supplier<UUID> ids) {
        List<MagazineState> out = new ArrayList<>();
        for (MagazineState m : carried) {
            out.add(m.withRounds(m.capacity()).knownTo(characterId));
        }
        while (out.size() < target) {
            out.add(MagazineState.full(ids.get(), capacity).knownTo(characterId));
        }
        return out;
    }
}
