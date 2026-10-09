package dev.qrm.api.job;

import dev.qrm.api.event.Event;
import dev.qrm.api.id.CharacterId;

public record DutyChangedEvent(CharacterId character, boolean onDuty, DutyReason reason) implements Event {}
