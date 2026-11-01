package dev.qrm.police.logic;

/**
 * Una voce dello storico: multa, arresto, rapporto, nota o mandato. {@code time} è in millisecondi epoch. Nei dati
 * vecchi il tipo manca: con mesi di carcere è un arresto, altrimenti una multa.
 *
 * <p>Una voce archiviata non si cancella: resta consultabile ma non conta più nella fedina del cittadino.
 */
public record CriminalRecord(String id, long time, String officer, String offense, long fine, int jailMonths,
        String notes, RecordKind kind, boolean archived, String archivedBy) {

    public static RecordKind legacyKind(int jailMonths) {
        return jailMonths > 0 ? RecordKind.ARREST : RecordKind.FINE;
    }

    public CriminalRecord withArchived(boolean archived, String by) {
        return new CriminalRecord(id, time, officer, offense, fine, jailMonths, notes, kind, archived,
                archived ? by : "");
    }
}
