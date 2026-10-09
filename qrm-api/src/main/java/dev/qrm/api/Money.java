package dev.qrm.api;

import java.util.Objects;

/** Importo in unità minime della valuta (es. centesimi). */
public record Money(String currency, long amount) {
    public Money {
        Objects.requireNonNull(currency, "currency");
        if (currency.isBlank()) throw new IllegalArgumentException("currency must not be blank");
        if (amount < 0) throw new IllegalArgumentException("amount must be >= 0");
    }

    public static Money of(String currency, long amount) {
        return new Money(currency, amount);
    }

    public Money plus(Money other) {
        requireSameCurrency(other);
        return new Money(currency, Math.addExact(amount, other.amount));
    }

    public Money minus(Money other) {
        requireSameCurrency(other);
        if (other.amount > amount) throw new IllegalArgumentException("result would be negative");
        return new Money(currency, amount - other.amount);
    }

    public boolean isZero() {
        return amount == 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException("currency mismatch: " + currency + " vs " + other.currency);
        }
    }
}
