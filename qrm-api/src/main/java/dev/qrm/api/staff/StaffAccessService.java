package dev.qrm.api.staff;

import java.util.Set;
import java.util.UUID;

/**
 * Permessi del pannello staff, legati all'<b>account</b> (UUID del giocatore) e non al personaggio. Conosce solo i grant
 * espliciti: l'operatore vanilla ha ogni nodo, ma questo lo decide l'adattatore Minecraft (il Core non lo conosce).
 */
public interface StaffAccessService {
    /**
     * @param node nodo esatto (mai un pattern)
     * @throws IllegalArgumentException se {@code node} non è un nodo esatto valido
     */
    boolean has(UUID account, String node);

    /** Pattern concessi all'account, in ordine alfabetico. */
    Set<String> grantsOf(UUID account);

    /**
     * @return true se il grant è nuovo
     * @throws IllegalArgumentException se {@code pattern} non è concedibile ({@link StaffNodes#isGrantable})
     */
    boolean grant(UUID account, String pattern);

    /** @return true se il grant esisteva ed è stato tolto */
    boolean revoke(UUID account, String pattern);
}
