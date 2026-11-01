package dev.qrm.police.logic;

/** Chi compie un'azione sul dispatch: nome del personaggio, matricola (numero ID) e id del giocatore. */
public record Agente(String name, String no, String id) {
    /** Per gli eventi che nessun agente ha causato (ricezione, disconnessione). */
    public static final Agente SYSTEM = new Agente("Centrale", "", "");
}
