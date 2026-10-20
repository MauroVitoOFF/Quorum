package dev.qrm.police.weapon.mc;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Agganci impostati dal lato client e letti da codice comune (per esempio il tooltip del caricatore). Non contiene
 * classi solo client: sul server dedicato restano i valori predefiniti.
 */
public final class WeaponClientHooks {
    private WeaponClientHooks() {}

    /** Id del personaggio attivo sul client; vuoto finché il client non lo imposta. */
    public static volatile Supplier<Optional<String>> characterId = Optional::empty;
}
