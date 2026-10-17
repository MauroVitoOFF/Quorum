package dev.qrm.police.logic;

/** Avviso di ricerca rapida. {@code priority}: 1 bassa, 2 media, 3 alta. */
public record BoloEntry(String id, String title, String description, int priority, String author, long time) {}
