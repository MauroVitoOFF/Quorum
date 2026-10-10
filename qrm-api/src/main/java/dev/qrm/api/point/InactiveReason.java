package dev.qrm.api.point;

/**
 * Perché un punto salvato non è utilizzabile. Un punto inattivo non viene mai cancellato in automatico.
 * Il Core valuta le prime tre cause; le ultime due richiedono il mondo e le valuta l'adattatore di piattaforma.
 */
public enum InactiveReason { UNKNOWN_TYPE, UNKNOWN_OWNER, UNKNOWN_GRADE, UNKNOWN_DIMENSION, MISSING_BLOCK }
