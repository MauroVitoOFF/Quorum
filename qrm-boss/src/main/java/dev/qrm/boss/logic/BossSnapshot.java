package dev.qrm.boss.logic;

import java.util.List;
import java.util.UUID;

/**
 * Stato completo del menu per un Character e un'azienda, calcolato dal server. Il client non lo modifica mai: mostra
 * l'ultimo ricevuto.
 *
 * @param level livello del rank esplicito di chi guarda, -1 se arriva al menu solo tramite job in servizio
 * @param rankLabel nome del rank di chi guarda (vuoto se {@code level} è -1)
 * @param hasBalance falso senza {@code boss.bank.view}: allora {@code balance} è vuoto
 * @param balance saldo già formattato
 * @param currency codice della valuta del menu
 */
public record BossSnapshot(String orgId, String orgLabel, String status, UUID character, String rankLabel, int level,
        boolean showMembers, boolean showAccount, int memberCount, boolean hasBalance, String balance,
        String currency, boolean canDeposit, boolean canWithdraw, List<Member> members, List<RankOption> ranks,
        List<Candidate> candidates) {
    public static final int MAX_MEMBERS = 100;
    public static final int MAX_RANKS = 32;
    public static final int MAX_CANDIDATES = 50;
    public static final int MAX_ORGS = 64;

    /** @param editable vero se chi guarda può cambiare il rank o licenziare questo membro */
    public record Member(UUID character, String name, String rankId, String rankLabel, int level, boolean editable) {}

    /** Rank che chi guarda può assegnare (level &lt;= al proprio), per livello crescente. */
    public record RankOption(String id, String label, int level) {}

    public record Candidate(UUID character, String name) {}

    /** Un'azienda per il selettore. */
    public record OrgChoice(String id, String label) {}
}
