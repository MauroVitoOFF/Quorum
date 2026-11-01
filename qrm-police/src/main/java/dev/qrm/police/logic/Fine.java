package dev.qrm.police.logic;

/**
 * Una multa emessa. Il nome del cittadino è quello al momento dell'emissione. {@code recordId} è la voce
 * corrispondente nello storico del cittadino (vuoto se non c'è più).
 */
public final class Fine {
    public final String id;
    public final String citizenId;
    public final String citizenName;
    public final String code;
    public final String offense;
    public final long amount;
    public final String reason;
    public final String officer;
    public final long time;
    public FineStatus status;
    public String recordId;

    public Fine(String id, String citizenId, String citizenName, String code, String offense, long amount,
            String reason, String officer, long time, FineStatus status, String recordId) {
        this.id = id;
        this.citizenId = citizenId;
        this.citizenName = citizenName;
        this.code = code;
        this.offense = offense;
        this.amount = amount;
        this.reason = reason;
        this.officer = officer;
        this.time = time;
        this.status = status;
        this.recordId = recordId;
    }
}
