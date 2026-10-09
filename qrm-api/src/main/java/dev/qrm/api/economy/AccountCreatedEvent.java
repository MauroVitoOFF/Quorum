package dev.qrm.api.economy;

import dev.qrm.api.event.Event;

public record AccountCreatedEvent(Account account) implements Event {}
