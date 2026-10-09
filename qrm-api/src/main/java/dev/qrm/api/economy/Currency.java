package dev.qrm.api.economy;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.regex.Pattern;

public record Currency(String code, String symbol, int decimals) {
    private static final Pattern CODE = Pattern.compile("[A-Z0-9_]{2,16}");

    public Currency {
        Objects.requireNonNull(code, "code");
        Objects.requireNonNull(symbol, "symbol");
        if (!CODE.matcher(code).matches()) throw new IllegalArgumentException("invalid currency code: " + code);
        if (decimals < 0 || decimals > 8) throw new IllegalArgumentException("decimals must be 0..8");
    }

    /** Converte un testo decimale ("12.50") in unità minime. */
    public long parseAmount(String text) {
        BigDecimal v;
        try {
            v = new BigDecimal(text.strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Not a number: " + text);
        }
        if (v.signum() < 0) throw new IllegalArgumentException("Amount cannot be negative");
        if (v.scale() > decimals) {
            v = v.stripTrailingZeros();
            if (v.scale() > decimals) {
                throw new IllegalArgumentException("Too many decimals (max " + decimals + ")");
            }
        }
        try {
            return v.movePointRight(decimals).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Amount too large");
        }
    }

    public String format(long amount) {
        return BigDecimal.valueOf(amount, decimals).toPlainString() + " " + symbol;
    }
}
