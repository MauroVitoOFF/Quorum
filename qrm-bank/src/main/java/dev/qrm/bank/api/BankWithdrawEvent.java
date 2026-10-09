package dev.qrm.bank.api;

import dev.qrm.api.Money;
import dev.qrm.api.economy.Transaction;
import dev.qrm.api.event.Event;
import dev.qrm.api.id.CharacterId;

/** Pubblicato dopo un ritiro riuscito (contante già consegnato). Non annullabile: usare {@code TransactionPreEvent}. */
public record BankWithdrawEvent(CharacterId character, Money amount, Transaction transaction) implements Event {}
