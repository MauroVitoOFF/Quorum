package dev.qrm.bank.logic;

/**
 * Un movimento del conto come lo vede il titolare. {@code signedAmount} &gt; 0 = accredito, &lt; 0 = addebito.
 * {@code counterparty} è chi manda o riceve (nome del personaggio o etichetta dell'organizzazione), vuoto per i
 * movimenti con la banca stessa (prelievi e depositi) o con il sistema. {@code note} è la causale del bonifico.
 */
public record HistoryLine(long epochSecond, String reason, String currency, long signedAmount,
        String counterparty, String note) {}
