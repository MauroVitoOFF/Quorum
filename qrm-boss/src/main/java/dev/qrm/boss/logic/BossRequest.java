package dev.qrm.boss.logic;

import java.util.Optional;
import java.util.UUID;

/**
 * C&rarr;S: una richiesta del menu. {@code character} è il Character con cui il client ha aperto il menu: se non è più
 * quello attivo il server rifiuta. {@code requestId} compone la chiave di idempotenza di deposito e prelievo.
 */
public record BossRequest(BossAction action, String orgId, UUID character, Optional<UUID> target, String rankId,
        String amount, UUID requestId) {
    public static final int MAX_ID = 32;
    public static final int MAX_AMOUNT = 24;

    public static BossRequest refresh(String orgId, UUID character) {
        return new BossRequest(BossAction.REFRESH, orgId, character, Optional.empty(), "", "", UUID.randomUUID());
    }
}
