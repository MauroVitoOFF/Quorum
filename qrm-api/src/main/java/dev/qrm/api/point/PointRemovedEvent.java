package dev.qrm.api.point;

import dev.qrm.api.event.Event;

public record PointRemovedEvent(InteractionPoint point) implements Event {}
