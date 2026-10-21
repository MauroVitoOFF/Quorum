package dev.qrm.police.weapon.client;

import dev.qrm.police.weapon.mc.WeaponClientHooks;
import java.util.Optional;

/**
 * Id del Character del giocatore locale, come lo comunica il server con {@code Identity}. Serve al tooltip dei
 * caricatori (che ricordano solo ciò che ha controllato il proprio Character). Si azzera all'uscita dal mondo.
 */
public final class ClientIdentity {
    private static volatile String characterId;

    private ClientIdentity() {}

    /** Collega il tooltip dei caricatori a questa identità: da chiamare una volta all'avvio del client. */
    public static void init() {
        WeaponClientHooks.characterId = ClientIdentity::characterId;
    }

    public static Optional<String> characterId() {
        return Optional.ofNullable(characterId);
    }

    /** Un id vuoto vale come nessun Character. */
    static void set(String id) {
        characterId = id == null || id.isEmpty() ? null : id;
    }

    public static void clear() {
        characterId = null;
    }
}
