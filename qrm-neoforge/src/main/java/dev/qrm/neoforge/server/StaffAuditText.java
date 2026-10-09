package dev.qrm.neoforge.server;

/** Testo del dettaglio delle voci del registro staff. Pura. */
public final class StaffAuditText {
    public static final int MAX_DETAIL = 256;

    private StaffAuditText() {}

    /** Il testo di un comando senza la {@code /} iniziale, troncato a {@value #MAX_DETAIL} caratteri. */
    public static String detailOf(String commandInput) {
        String s = commandInput == null ? "" : commandInput.strip();
        if (s.startsWith("/")) s = s.substring(1);
        return s.length() <= MAX_DETAIL ? s : s.substring(0, MAX_DETAIL);
    }

    /** L'argomento di un'azione del pannello: i valori dei campi (uno per riga) uniti da {@code " | "}. */
    public static String panelDetail(String arg) {
        if (arg == null || arg.isBlank()) return "";
        String s = String.join(" | ", arg.split("\n", -1)).strip();
        return s.length() <= MAX_DETAIL ? s : s.substring(0, MAX_DETAIL);
    }
}
