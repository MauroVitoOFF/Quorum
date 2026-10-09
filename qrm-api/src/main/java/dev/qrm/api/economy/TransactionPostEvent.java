package dev.qrm.api.economy;

import dev.qrm.api.event.Event;

public record TransactionPostEvent(Transaction transaction) implements Event {}
