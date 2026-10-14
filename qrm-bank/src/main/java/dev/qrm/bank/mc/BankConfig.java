package dev.qrm.bank.mc;

import dev.qrm.bank.logic.BankLimits;
import dev.qrm.bank.logic.LimitsProvider;
import java.util.List;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Configurazione lato server (letta a ogni uso: le modifiche del file si applicano senza riavvio). */
public final class BankConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue MAX_DISTANCE;
    public static final ModConfigSpec.BooleanValue TRANSFERS_ENABLED;
    public static final ModConfigSpec.IntValue HISTORY_ROWS;
    public static final ModConfigSpec.IntValue MIN_INTERVAL_MS;
    public static final ModConfigSpec.LongValue VIP_BALANCE;
    public static final ModConfigSpec.LongValue MAX_PER_OPERATION;
    public static final ModConfigSpec.LongValue MAX_PER_DAY;
    public static final ModConfigSpec.ConfigValue<List<? extends String>> LIMIT_OVERRIDES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("atm");
        MAX_DISTANCE = b.comment("Distanza massima (in blocchi) dall'ATM aperto entro cui le operazioni sono accettate")
                .defineInRange("maxDistance", 6.0, 2.0, 16.0);
        MIN_INTERVAL_MS = b.comment("Intervallo minimo fra due richieste dello stesso giocatore (millisecondi)")
                .defineInRange("minIntervalMillis", 250, 0, 5000);
        HISTORY_ROWS = b.comment("Movimenti mostrati nello storico")
                .defineInRange("historyRows", 20, 1, 50);
        TRANSFERS_ENABLED = b.comment("Abilita i bonifici dall'ATM").define("transfersEnabled", true);
        VIP_BALANCE = b.comment("Saldo (in unita' intere della valuta, es. 10000 = 10000 euro) da cui il titolare vede il badge VIP; 0 = disattivato")
                .defineInRange("vipBalance", 100000L, 0L, Long.MAX_VALUE);
        b.pop();

        b.push("limits");
        MAX_PER_OPERATION = b.comment("Ritiro massimo per singola operazione, in unita' minime (valore predefinito)")
                .defineInRange("maxPerOperation", BankLimits.DEFAULT.perOperation(), 1L, Long.MAX_VALUE);
        MAX_PER_DAY = b.comment("Ritiro massimo nelle ultime 24 ore per personaggio, in unita' minime (predefinito)")
                .defineInRange("maxPerDay", BankLimits.DEFAULT.perDay(), 1L, Long.MAX_VALUE);
        LIMIT_OVERRIDES = b.comment("Limiti per valuta, voci \"CODICE:perOperazione:alGiorno\" (es. \"EUR:50000:200000\")")
                .defineListAllowEmpty("overrides", List.of(), () -> "EUR:100000:500000",
                        o -> o instanceof String s && LimitsProvider.isValidOverride(s));
        b.pop();
        SPEC = b.build();
    }

    private BankConfig() {}

    /** Limiti correnti: valori predefiniti più le eccezioni per valuta. */
    public static LimitsProvider limits() {
        return currency -> LimitsProvider.of(
                        new BankLimits(MAX_PER_OPERATION.get(), MAX_PER_DAY.get()),
                        LIMIT_OVERRIDES.get().stream().map(String::valueOf).toList())
                .forCurrency(currency);
    }
}
