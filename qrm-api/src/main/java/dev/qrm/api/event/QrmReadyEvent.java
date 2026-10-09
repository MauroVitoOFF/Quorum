package dev.qrm.api.event;

/** Pubblicato quando tutti i servizi QRM sono registrati e il database è pronto. */
public record QrmReadyEvent() implements Event {}
