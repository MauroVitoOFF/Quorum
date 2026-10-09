package dev.qrm.api.economy;

import dev.qrm.api.Money;
import dev.qrm.api.event.CancellableEvent;

/** Prima della scrittura: gli handler possono annullare o modificare l'importo (stessa valuta). */
public final class TransactionPreEvent extends CancellableEvent {
    private TransferRequest request;

    public TransactionPreEvent(TransferRequest request) {
        this.request = request;
    }

    public TransferRequest request() {
        return request;
    }

    public void setAmount(Money amount) {
        if (!amount.currency().equals(request.amount().currency())) {
            throw new IllegalArgumentException("cannot change currency in a pre-event");
        }
        request = request.withAmount(amount);
    }
}
