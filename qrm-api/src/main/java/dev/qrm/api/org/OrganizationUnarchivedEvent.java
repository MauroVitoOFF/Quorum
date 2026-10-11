package dev.qrm.api.org;

import dev.qrm.api.event.Event;

public record OrganizationUnarchivedEvent(Organization org) implements Event {}
