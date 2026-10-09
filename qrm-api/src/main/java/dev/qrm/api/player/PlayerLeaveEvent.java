package dev.qrm.api.player;

import dev.qrm.api.event.Event;
import dev.qrm.api.id.PlayerId;

public record PlayerLeaveEvent(PlayerId playerId) implements Event {}
