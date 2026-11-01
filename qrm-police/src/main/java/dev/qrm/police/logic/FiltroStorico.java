package dev.qrm.police.logic;

/**
 * Filtri dello storico interventi. {@code priority} 0 = tutte, {@code category} vuota = tutte, {@code state} null =
 * concluse e annullate. {@code all} false limita ai soli interventi in cui è coinvolto l'agente indicato.
 */
public record FiltroStorico(String query, String category, int priority, StatoChiamata state, long from, long to,
        boolean all, String involvedId, String involvedNo) {}
