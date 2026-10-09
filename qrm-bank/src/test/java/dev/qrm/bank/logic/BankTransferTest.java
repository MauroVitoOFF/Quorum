package dev.qrm.bank.logic;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.Money;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.economy.Transaction;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class BankTransferTest {
    private BankEnv env = new BankEnv();

    @AfterEach
    void tearDown() {
        env.close();
    }

    private BankResult send(PlayerCharacter from, String to, long amount) {
        return env.ops.transfer(from.id(), to, Money.of(BankEnv.USD, amount));
    }

    @Test
    void transfersByFullNameIgnoringCase() {
        PlayerCharacter anna = env.newCharacterNamed("Anna", "Rossi");
        PlayerCharacter bruno = env.newCharacterNamed("Bruno", "Verdi");
        env.fund(anna.id(), 1_000);
        assertEquals(BankResult.OK, send(anna, "  bruno VERDI ", 300));
        assertEquals(700, env.balance(anna.id()));
        assertEquals(300, env.balance(bruno.id()));
        assertTrue(env.audit.verify().isEmpty());
    }

    @Test
    void unknownRecipient() {
        PlayerCharacter anna = env.newCharacterNamed("Anna", "Rossi");
        env.fund(anna.id(), 1_000);
        assertEquals(BankResult.UNKNOWN_RECIPIENT, send(anna, "Nessuno Qui", 100));
        assertEquals(BankResult.UNKNOWN_RECIPIENT, send(anna, "  ", 100));
        assertEquals(1_000, env.balance(anna.id()));
    }

    @Test
    void ambiguousRecipientWhenNamesAreNotUnique() {
        env.close();
        env = new BankEnv(false);
        PlayerCharacter anna = env.newCharacterNamed("Anna", "Rossi");
        env.newCharacterNamed("Mario", "Bianchi");
        env.newCharacterNamed("Mario", "Bianchi");
        env.fund(anna.id(), 1_000);
        assertEquals(BankResult.AMBIGUOUS_RECIPIENT, send(anna, "Mario Bianchi", 100));
        assertEquals(1_000, env.balance(anna.id()));
    }

    @Test
    void selfTransferIsRejected() {
        PlayerCharacter anna = env.newCharacterNamed("Anna", "Rossi");
        env.fund(anna.id(), 1_000);
        assertEquals(BankResult.SELF_TRANSFER, send(anna, "Anna Rossi", 100));
    }

    @Test
    void insufficientFundsAndInvalidAmount() {
        PlayerCharacter anna = env.newCharacterNamed("Anna", "Rossi");
        env.newCharacterNamed("Bruno", "Verdi");
        env.fund(anna.id(), 100);
        assertEquals(BankResult.INSUFFICIENT_FUNDS, send(anna, "Bruno Verdi", 500));
        assertEquals(BankResult.INVALID_AMOUNT, send(anna, "Bruno Verdi", 0));
    }

    @Test
    void transfersToAnOrganizationAccount() {
        PlayerCharacter anna = env.newCharacterNamed("Anna", "Rossi");
        env.orgs.create("pd", "Polizia");
        env.fund(anna.id(), 1_000);
        assertEquals(BankResult.OK, send(anna, "@pd", 250));
        assertEquals(750, env.balance(anna.id()));
        assertEquals(250, env.accounts.balance(env.orgs.find("pd").orElseThrow().account(), BankEnv.USD).amount());
    }

    @Test
    void unknownOrArchivedOrMalformedOrganization() {
        PlayerCharacter anna = env.newCharacterNamed("Anna", "Rossi");
        env.orgs.create("pd", "Polizia");
        env.orgs.archive("pd");
        env.fund(anna.id(), 1_000);
        assertEquals(BankResult.UNKNOWN_RECIPIENT, send(anna, "@nope", 100));
        assertEquals(BankResult.UNKNOWN_RECIPIENT, send(anna, "@pd", 100));
        assertEquals(BankResult.UNKNOWN_RECIPIENT, send(anna, "@Bad Id!", 100));
    }

    @Test
    void historyListsMostRecentFirstAndBalancesAreExposed() {
        PlayerCharacter anna = env.newCharacterNamed("Anna", "Rossi");
        env.newCharacterNamed("Bruno", "Verdi");
        env.fund(anna.id(), 1_000);
        send(anna, "Bruno Verdi", 100);
        env.clock.advance(java.time.Duration.ofMinutes(1));
        send(anna, "Bruno Verdi", 200);
        List<Transaction> history = env.ops.history(anna.id(), 10);
        assertEquals(3, history.size());
        assertEquals(200, history.get(0).amount().amount());
        assertEquals(List.of(Money.of(BankEnv.USD, 700)), env.ops.balances(anna.id()));
    }

    @Test
    void noteIsStoredInMetadataAndCleaned() {
        PlayerCharacter anna = env.newCharacterNamed("Anna", "Rossi");
        env.newCharacterNamed("Bruno", "Verdi");
        env.fund(anna.id(), 1_000);
        env.clock.advance(java.time.Duration.ofMinutes(1));
        assertEquals(BankResult.OK, env.ops.transfer(anna.id(), "Bruno Verdi", Money.of(BankEnv.USD, 100),
                "  Affitto \"ottobre\"\n\t "));
        Transaction tx = env.ops.history(anna.id(), 1).get(0);
        assertEquals("Affitto \"ottobre\"", TransferNote.fromJson(tx.metadataJson()));
        assertEquals(BankOperations.REASON_TRANSFER, tx.reason());
    }

    @Test
    void transferWithoutNoteHasEmptyNote() {
        PlayerCharacter anna = env.newCharacterNamed("Anna", "Rossi");
        env.newCharacterNamed("Bruno", "Verdi");
        env.fund(anna.id(), 1_000);
        env.clock.advance(java.time.Duration.ofMinutes(1));
        send(anna, "Bruno Verdi", 100);
        assertEquals("", TransferNote.fromJson(env.ops.history(anna.id(), 1).get(0).metadataJson()));
    }

    @Test
    void transferEventCarriesRecipientAndNote() {
        PlayerCharacter anna = env.newCharacterNamed("Anna", "Rossi");
        PlayerCharacter bruno = env.newCharacterNamed("Bruno", "Verdi");
        env.orgs.create("pd", "Polizia");
        env.fund(anna.id(), 1_000);
        env.ops.transfer(anna.id(), "Bruno Verdi", Money.of(BankEnv.USD, 100), "cena");
        env.ops.transfer(anna.id(), "@pd", Money.of(BankEnv.USD, 50), "");
        var toPerson = (dev.qrm.bank.api.BankTransferEvent) env.events.get(0);
        assertEquals(anna.id(), toPerson.from());
        assertEquals(bruno.id(), toPerson.toCharacter());
        assertNull(toPerson.toOrganization());
        assertEquals("cena", toPerson.note());
        var toOrg = (dev.qrm.bank.api.BankTransferEvent) env.events.get(1);
        assertNull(toOrg.toCharacter());
        assertEquals("pd", toOrg.toOrganization());
    }

    @Test
    void noEventWhenTheTransferFails() {
        PlayerCharacter anna = env.newCharacterNamed("Anna", "Rossi");
        env.newCharacterNamed("Bruno", "Verdi");
        env.fund(anna.id(), 100);
        assertEquals(BankResult.INSUFFICIENT_FUNDS, send(anna, "Bruno Verdi", 500));
        assertEquals(BankResult.UNKNOWN_RECIPIENT, send(anna, "Nessuno Qui", 50));
        assertTrue(env.events.isEmpty());
    }

    @Test
    void historyLinesShowCounterpartyAndNoteOnBothSides() {
        PlayerCharacter anna = env.newCharacterNamed("Anna", "Rossi");
        PlayerCharacter bruno = env.newCharacterNamed("Bruno", "Verdi");
        env.fund(anna.id(), 1_000);
        env.clock.advance(java.time.Duration.ofMinutes(1));
        env.ops.transfer(anna.id(), "Bruno Verdi", Money.of(BankEnv.USD, 300), "cena");

        HistoryLine sent = env.ops.historyLines(anna.id(), 10).get(0);
        assertEquals(-300, sent.signedAmount());
        assertEquals("Bruno Verdi", sent.counterparty());
        assertEquals("cena", sent.note());
        assertEquals(BankOperations.REASON_TRANSFER, sent.reason());

        HistoryLine received = env.ops.historyLines(bruno.id(), 10).get(0);
        assertEquals(300, received.signedAmount());
        assertEquals("Anna Rossi", received.counterparty());
        assertEquals("cena", received.note());
    }

    @Test
    void historyLinesNameOrganizationsAndHideBankAndSystem() {
        PlayerCharacter anna = env.newCharacterNamed("Anna", "Rossi");
        env.orgs.create("pd", "Polizia");
        env.fund(anna.id(), 2_000);
        env.clock.advance(java.time.Duration.ofMinutes(1));
        env.ops.withdraw(anna.id(), Money.of(BankEnv.USD, 500), env.inventory);
        env.clock.advance(java.time.Duration.ofMinutes(1));
        env.ops.transfer(anna.id(), "@pd", Money.of(BankEnv.USD, 100), "");

        List<HistoryLine> lines = env.ops.historyLines(anna.id(), 10);
        assertEquals("Polizia", lines.get(0).counterparty());
        assertEquals("", lines.get(1).counterparty());
        assertEquals(BankOperations.REASON_WITHDRAW, lines.get(1).reason());
        assertEquals("", lines.get(2).counterparty());
        assertEquals("test.fund", lines.get(2).reason());
        assertEquals("", lines.get(0).note());
    }
}
