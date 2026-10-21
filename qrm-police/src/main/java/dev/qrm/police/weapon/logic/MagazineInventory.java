package dev.qrm.police.weapon.logic;

import java.util.Optional;
import java.util.UUID;

/** Vista dei caricatori che il personaggio porta con sé, usata dalle sessioni d'azione. */
public interface MagazineInventory {
    /**
     * Vero se c'è almeno un caricatore con colpi il cui id è diverso da {@code excluding}
     * ({@code null} = nessuna esclusione). Non modifica l'inventario.
     */
    boolean hasLoaded(UUID excluding);

    /**
     * Toglie dall'inventario e restituisce il caricatore con più colpi tra quelli con almeno un colpo e id diverso
     * da {@code excluding} ({@code null} = nessuna esclusione); a parità vince il primo nell'ordine dell'inventario.
     * Vuoto se non ce n'è nessuno.
     */
    Optional<MagazineState> takeFullest(UUID excluding);

    /** Ripone il caricatore nell'inventario; {@code false} se l'inventario è pieno (il caricatore non è stato preso). */
    boolean store(MagazineState m);

    /** Lascia il caricatore a terra vicino al personaggio. */
    void drop(MagazineState m);
}
