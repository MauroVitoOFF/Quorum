package dev.qrm.bank.api;

import dev.qrm.api.Money;
import dev.qrm.api.economy.Transaction;
import dev.qrm.api.event.Event;
import dev.qrm.api.id.CharacterId;

/**
 * Pubblicato dopo un bonifico riuscito dall'ATM. Il destinatario è un personaggio ({@code toCharacter}) oppure
 * un'organizzazione ({@code toOrganization}, id): l'altro campo è {@code null}. {@code note} è la causale (può essere
 * vuota). Non annullabile: per intervenire prima usare {@code TransactionPreEvent} con motivo {@code bank.transfer}.
 */
public record BankTransferEvent(CharacterId from, CharacterId toCharacter, String toOrganization, Money amount,
        String note, Transaction transaction) implements Event {}
