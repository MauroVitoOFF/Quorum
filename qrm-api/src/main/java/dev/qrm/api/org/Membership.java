package dev.qrm.api.org;

import dev.qrm.api.id.CharacterId;
import java.time.Instant;

/** Appartenenza esplicita di un personaggio a un'organizzazione. */
public record Membership(String orgId, CharacterId character, String rankId, Instant since) {}
