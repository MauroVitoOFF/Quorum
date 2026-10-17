package dev.qrm.police.logic;

/** Un precedente o una multa. {@code time} è in millisecondi epoch. */
public record CriminalRecord(String id, long time, String officer, String offense, long fine, int jailMonths,
        String notes) {}
