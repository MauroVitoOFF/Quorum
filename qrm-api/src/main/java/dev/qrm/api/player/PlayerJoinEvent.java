package dev.qrm.api.player;

import dev.qrm.api.event.Event;
import dev.qrm.api.id.PlayerId;

public record PlayerJoinEvent(PlayerId playerId) implements Event {}
