package dev.qrm.api.event;

/** Evento annullabile. Il primo handler che annulla determina motivo e autore. */
public abstract class CancellableEvent implements Event {
    private boolean cancelled;
    private String cancelledBy;
    private String cancelReason;

    public boolean isCancelled() {
        return cancelled;
    }

    public String cancelledBy() {
        return cancelledBy;
    }

    public String cancelReason() {
        return cancelReason;
    }

    public void cancel(String by, String reason) {
        if (cancelled) return;
        cancelled = true;
        cancelledBy = by;
        cancelReason = reason;
    }
}
