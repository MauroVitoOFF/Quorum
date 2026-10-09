package dev.qrm.neoforge.client;

/** Quando mostrare il promemoria "nessun personaggio attivo". Pura: nessuna dipendenza da Minecraft. */
public final class ReminderPolicy {
    private ReminderPolicy() {}

    /** Motivo per cui il promemoria è sospeso, oppure {@code null} se va contato il tempo. */
    public static String blockedBy(boolean inGame, boolean noScreenOpen, boolean serverHasQrm, boolean rosterKnown,
            boolean hasActiveCharacter, int cards, int slots) {
        if (!inGame) return "no player";
        if (!noScreenOpen) return "a screen is open";
        if (!serverHasQrm) return "server has no QRM channel";
        if (!rosterKnown) return "no roster received";
        if (hasActiveCharacter) return "an active character is set";
        if (cards <= 0 && slots <= 0) return "no characters and no slots";
        return null;
    }
}
