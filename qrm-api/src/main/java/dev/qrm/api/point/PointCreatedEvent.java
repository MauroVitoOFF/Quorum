package dev.qrm.api.point;

import dev.qrm.api.event.Event;

public record PointCreatedEvent(InteractionPoint point) implements Event {}
