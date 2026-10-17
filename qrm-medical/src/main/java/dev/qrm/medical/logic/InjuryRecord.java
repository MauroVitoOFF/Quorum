package dev.qrm.medical.logic;

/** Una ferita registrata: tipo, quando (millisecondi epoch) e gravità (punti vita persi). */
public record InjuryRecord(InjuryType type, long timestamp, float severity) {}
