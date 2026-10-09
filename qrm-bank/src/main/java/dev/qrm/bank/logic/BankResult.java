package dev.qrm.bank.logic;

/** Esito di un'operazione bancaria. L'ordine è parte del protocollo di rete (si trasmette l'indice). */
public enum BankResult {
    OK,
    INVALID_AMOUNT,
    INSUFFICIENT_FUNDS,
    INVENTORY_FULL,
    NOT_REPRESENTABLE,
    UNBACKED_CASH,
    LIMIT_EXCEEDED,
    FROZEN,
    UNKNOWN_RECIPIENT,
    AMBIGUOUS_RECIPIENT,
    SELF_TRANSFER,
    UNKNOWN_CURRENCY,
    NO_CASH,
    TOO_FAR,
    RATE_LIMITED,
    NO_PERMISSION,
    CANCELLED,
    INTERNAL_ERROR
}
