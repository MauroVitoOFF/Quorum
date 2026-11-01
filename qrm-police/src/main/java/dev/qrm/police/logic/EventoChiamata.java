package dev.qrm.police.logic;

/** Una riga della cronologia di una chiamata. Solo il server ne aggiunge; nessuno le modifica. */
public record EventoChiamata(long time, String type, String agent, String agentNo, String text) {}
