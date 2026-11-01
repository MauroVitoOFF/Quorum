package dev.qrm.police.logic;

/**
 * Un agente collegato: nome, stato operativo, id del giocatore, matricola (numero ID del personaggio), grado e posizione.
 */
public record Unita(String name, StatoAgente status, String id, String no, String rank, int x, int y, int z, String dim) {}
