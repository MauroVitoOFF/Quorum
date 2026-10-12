package dev.qrm.boss.logic;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** S&rarr;C: l'esito dell'ultima richiesta e, se il menu resta valido, lo snapshot aggiornato. */
public record BossReply(BossResult result, Optional<BossSnapshot> snapshot) {
    /** S&rarr;C: le aziende fra cui scegliere (più di una con {@code boss.menu}). */
    public record OrgList(UUID character, List<BossSnapshot.OrgChoice> orgs) {}
}
