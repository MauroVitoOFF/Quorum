package dev.qrm.bank.logic;

import java.util.Locale;

/**
 * Categoria con cui lo storico dell'ATM sceglie icona e nome di un movimento. Il motivo è una stringa libera scritta
 * da chi muove il denaro: si riconoscono i motivi della banca e, per prefisso, quelli di lavoro, negozio e carburante
 * (per esempio {@code salary}, {@code shop.buy}, {@code fuel.pump}); tutto il resto è «altro».
 */
public enum HistoryCategory {
    WITHDRAW("withdraw"), DEPOSIT("deposit"), REVERT("cycle"), ADMIN("admin"), TRANSFER_IN("in"), TRANSFER_OUT("out"),
    WORK("work"), SHOP("shop"), FUEL("fuel"), OTHER("coin");

    private final String icon;

    HistoryCategory(String icon) {
        this.icon = icon;
    }

    /** Nome dell'icona, da prefissare con {@code qrm_bank.ui.icon_}. */
    public String icon() {
        return icon;
    }

    /** Parte finale della chiave di lingua {@code qrm_bank.category.*}. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static HistoryCategory of(String reason, long signedAmount) {
        String r = reason == null ? "" : reason.toLowerCase(Locale.ROOT);
        return switch (r) {
            case "bank.withdraw" -> WITHDRAW;
            case "bank.deposit" -> DEPOSIT;
            case "bank.deposit_revert", "bank.deposit.revert" -> REVERT;
            case "admin.grant", "admin.take" -> ADMIN;
            case "bank.transfer", "player.pay" -> signedAmount > 0 ? TRANSFER_IN : TRANSFER_OUT;
            default -> byPrefix(r);
        };
    }

    private static HistoryCategory byPrefix(String r) {
        if (startsWithAny(r, "salary", "job", "payroll", "wage", "work")) return WORK;
        if (startsWithAny(r, "shop", "store", "market", "purchase")) return SHOP;
        if (startsWithAny(r, "fuel", "gas", "petrol")) return FUEL;
        return OTHER;
    }

    private static boolean startsWithAny(String reason, String... prefixes) {
        for (String p : prefixes) {
            if (reason.equals(p) || reason.startsWith(p + ".") || reason.startsWith(p + "_")) return true;
        }
        return false;
    }
}
