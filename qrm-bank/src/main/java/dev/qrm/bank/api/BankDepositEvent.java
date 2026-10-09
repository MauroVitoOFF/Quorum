package dev.qrm.bank.api;

import dev.qrm.api.Money;
import dev.qrm.api.economy.Transaction;
import dev.qrm.api.event.Event;
import dev.qrm.api.id.CharacterId;

/** Pubblicato dopo un deposito riuscito (contante già tolto). Non annullabile: usare {@code TransactionPreEvent}. */
public record BankDepositEvent(CharacterId character, Money amount, Transaction transaction) implements Event {}
