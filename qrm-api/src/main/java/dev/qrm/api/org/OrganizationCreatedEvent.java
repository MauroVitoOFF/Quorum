package dev.qrm.api.org;

import dev.qrm.api.event.Event;

public record OrganizationCreatedEvent(Organization org) implements Event {}
