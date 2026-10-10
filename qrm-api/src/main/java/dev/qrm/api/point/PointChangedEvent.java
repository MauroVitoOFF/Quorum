package dev.qrm.api.point;

import dev.qrm.api.event.Event;

/** Modifica, anche di posizione. */
public record PointChangedEvent(InteractionPoint before, InteractionPoint after) implements Event {}
