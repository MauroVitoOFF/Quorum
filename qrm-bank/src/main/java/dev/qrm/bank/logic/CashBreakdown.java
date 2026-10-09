package dev.qrm.bank.logic;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Scomposizione di un importo in tagli. */
public final class CashBreakdown {
    /** Massimo numero di banconote in un singolo ritiro (evita richieste che occuperebbero migliaia di slot). */
    public static final int MAX_NOTES = 4096;

    private CashBreakdown() {}

    /**
     * Scompone {@code amount} nel minimo numero di tagli. Con tagli "canonici" (ognuno multiplo del precedente più
     * piccolo, vedi {@link CashDenominations}) il metodo greedy è ottimo e l'importo è esprimibile se e solo se è
     * multiplo del taglio minimo.
     *
     * @param descending tagli in ordine decrescente
     * @return taglio → quantità (solo quantità &gt; 0, tagli decrescenti); vuoto se non esprimibile o con più di
     *     {@code Integer.MAX_VALUE} banconote di un taglio
     */
    public static Optional<Map<Long, Integer>> of(List<Long> descending, long amount) {
        if (amount <= 0 || descending.isEmpty()) return Optional.empty();
        long rest = amount;
        Map<Long, Integer> out = new LinkedHashMap<>();
        for (long denomination : descending) {
            long n = rest / denomination;
            if (n <= 0) continue;
            if (n > Integer.MAX_VALUE) return Optional.empty();
            out.put(denomination, (int) n);
            rest -= n * denomination;
        }
        return rest == 0 ? Optional.of(out) : Optional.empty();
    }
}
