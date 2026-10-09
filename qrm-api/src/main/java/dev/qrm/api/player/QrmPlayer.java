package dev.qrm.api.player;

import dev.qrm.api.id.PlayerId;
import java.time.Instant;

public record QrmPlayer(PlayerId id, Instant firstSeen, Instant lastSeen, int characterSlots) {}
