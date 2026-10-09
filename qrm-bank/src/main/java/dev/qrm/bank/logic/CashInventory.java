package dev.qrm.bank.logic;

import java.util.Map;

/** Il contante che un giocatore ha con sé. Implementato dallo strato Minecraft; nei test da un finto. */
public interface CashInventory {
    /** Valore totale (unità minime) del contante della valuta. */
    long count(String currency);

    /** true se tutte le banconote (taglio → quantità) entrano nell'inventario. */
    boolean canFit(String currency, Map<Long, Integer> notes);

    /** Consegna le banconote. Va chiamato solo dopo {@link #canFit}. */
    void give(String currency, Map<Long, Integer> notes);

    /**
     * Toglie TUTTO il contante della valuta, ma solo se il totale è ancora {@code expectedTotal}.
     *
     * @return false (senza modifiche) se il totale è cambiato
     */
    boolean remove(String currency, long expectedTotal);
}
