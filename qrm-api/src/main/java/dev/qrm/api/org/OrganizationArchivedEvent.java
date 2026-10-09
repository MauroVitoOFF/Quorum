package dev.qrm.api.org;

import dev.qrm.api.event.Event;

public record OrganizationArchivedEvent(Organization org) implements Event {}
