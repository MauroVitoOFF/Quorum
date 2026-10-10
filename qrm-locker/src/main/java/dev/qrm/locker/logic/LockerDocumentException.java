package dev.qrm.locker.logic;

/** Il documento di un armadietto non si legge: JSON corrotto, versione sconosciuta o slot non validi. */
public final class LockerDocumentException extends Exception {
    public LockerDocumentException(String message) {
        super(message);
    }

    public LockerDocumentException(String message, Throwable cause) {
        super(message, cause);
    }
}
