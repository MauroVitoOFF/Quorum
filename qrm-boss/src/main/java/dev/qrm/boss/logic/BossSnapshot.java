package dev.qrm.boss.logic;

import java.util.List;
import java.util.UUID;

/**
 * Stato completo del menu per un Character e un'azienda, calcolato dal server. Il client non lo modifica mai: mostra
 * l'ultimo ricevuto.
 *
 * @param level livello del rank esplicito di chi guarda, -1 se arriva al menu solo tramite job in servizio
 * @param rankLabel nome del rank di chi guarda (vuoto se {@code level} è -1)
 * @param canManage può cambiare rank, aggiungere e licenziare
 * @param online quanti membri hanno il proprio Character attivo in gioco
 * @param onDuty quanti di loro sono in servizio, -1 se l'azienda non ha un lavoro (niente servizio)
 * @param notice avviso fisso dell'azienda (vuoto se non c'è), con {@code noticeAuthor} e {@code noticeAt} (ms epoch)
 * @param canNotice può scrivere l'avviso
 * @param currency codice della valuta usata per i bonus
 */
public record BossSnapshot(String orgId, String orgLabel, UUID character, String rankLabel, int level,
        boolean showMembers, boolean canManage, int memberCount, int online, int onDuty, String notice, String noticeAuthor, long noticeAt,
        boolean canNotice, String currency, List<Member> members, List<RankOption> ranks, List<Candidate> candidates) {
    public static final int MAX_MEMBERS = 100;
    public static final int MAX_RANKS = 32;
    public static final int MAX_CANDIDATES = 50;
    public static final int MAX_ORGS = 64;
    public static final int MAX_LOG = 3;

    /**
     * @param editable vero se chi guarda può cambiare il rank o licenziare questo membro
     * @param title titolo interno assegnato dal boss (vuoto se non ne ha)
     * @param canTitle vero se chi guarda può cambiargli il titolo
     * @param canBonus vero se chi guarda può dargli un bonus
     * @param player l'account del giocatore, per mostrarne la testa
     * @param presence 0 offline, 1 online ma fuori servizio, 2 online (azienda senza servizio), 3 in servizio
     * @param log le ultime annotazioni su di lui (bonus e titoli), dalla più recente
     */
    public record Member(UUID character, String name, String rankId, String rankLabel, int level, boolean editable,
            String title, boolean canTitle, boolean canBonus, UUID player, int presence, List<LogEntry> log) {
        /** Vero se la persona è presente e al lavoro: la testa si mostra a colori. */
        public boolean active() {
            return presence >= 2;
        }
    }

    /** Un'annotazione nel fascicolo di un dipendente. */
    public record LogEntry(int kind, long at, String text) {
        public static final int BONUS = 0;
        public static final int TITLE = 1;
    }

    /** Rank che chi guarda può assegnare (level &lt;= al proprio), per livello crescente. */
    public record RankOption(String id, String label, int level) {}

    public record Candidate(UUID character, String name) {}

    /** Un'azienda per il selettore. */
    public record OrgChoice(String id, String label) {}
}
