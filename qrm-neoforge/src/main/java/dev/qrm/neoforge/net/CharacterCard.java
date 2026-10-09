package dev.qrm.neoforge.net;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

/** Una carta documento mostrata nella schermata Personaggi. */
public record CharacterCard(UUID id, String firstName, String lastName, LocalDate birthDate,
        String documentNumber, Optional<String> jobLabel) {

    /**
     * Massimo di carte inviate in un roster (gli slot di config arrivano a 50). Le carte oltre il limite non
     * vengono mostrate: un payload personalizzato non e' saltabile, quindi il limite deve coprire ogni config.
     */
    public static final int MAX_ROSTER_CARDS = 64;

    /** Numero documento di sola visualizzazione: {@code QRM-} + 5 cifre esadecimali dall'hash dell'uuid. */
    public static String documentNumberOf(UUID id) {
        return String.format("QRM-%05X", id.hashCode() & 0xFFFFF);
    }
}
