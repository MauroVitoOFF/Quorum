package dev.qrm.neoforge.net;

/** Una riga di una pagina informativa (es. "View permissions"): chiave di traduzione dell'etichetta e valore. */
public record PlayerInfoLine(String labelKey, String value) {
    /** Una chiave di etichetta che inizia con questo carattere è testo letterale, non una chiave di traduzione. */
    public static final char LITERAL = '=';

    public boolean literal() {
        return !labelKey.isEmpty() && labelKey.charAt(0) == LITERAL;
    }
}
