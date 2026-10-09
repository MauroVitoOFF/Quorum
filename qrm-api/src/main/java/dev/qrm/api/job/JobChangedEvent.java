package dev.qrm.api.job;

import dev.qrm.api.event.Event;
import dev.qrm.api.id.CharacterId;

/** @param before null se era disoccupato; @param after null se licenziato */
public record JobChangedEvent(CharacterId character, Assignment before, Assignment after) implements Event {}
