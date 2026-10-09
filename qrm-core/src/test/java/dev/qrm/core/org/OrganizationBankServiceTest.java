package dev.qrm.core.org;

import static dev.qrm.core.org.OrgsEnv.ok;
import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.Money;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.economy.Account;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.economy.TransferRequest;
import dev.qrm.api.economy.TransferResult;
import dev.qrm.api.org.*;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OrganizationBankServiceTest {
    private OrgsEnv env;
    private PlayerCharacter boss;
    private PlayerCharacter clerk;
    private Account orgAccount;
    private Account bossAccount;

    @BeforeEach
    void setUp() {
        env = new OrgsEnv();
        boss = env.newCharacter();
        clerk = env.newCharacter();
        Organization org = ok(env.orgs.create("acme", "Acme"));
        orgAccount = env.accounts.find(org.account()).orElseThrow();
        bossAccount = env.accounts.create(OwnerRef.character(boss.id()), false);
        ok(env.orgs.defineRank("acme", new Rank("clerk", "Impiegato", 1, Set.of("shop.sell"))));
        ok(env.orgs.defineRank("acme", new Rank("boss", "Capo", 5, Set.of("org.account.withdraw"))));
        ok(env.orgs.addMember("acme", boss.id(), "boss"));
        ok(env.orgs.addMember("acme", clerk.id(), "clerk"));
        env.fund(orgAccount, "USD", 10_000);
    }

    @AfterEach
    void tearDown() {
        env.close();
    }

    @Test
    void memberWithTheNodeWithdraws() {
        BankResult r = env.bank.transfer(boss.id(), "acme", bossAccount.id(), Money.of("USD", 2_500), "w-1");
        assertInstanceOf(BankResult.Transferred.class, r);
        assertInstanceOf(TransferResult.Success.class, ((BankResult.Transferred) r).result());
        assertEquals(Money.of("USD", 7_500), env.accounts.balance(orgAccount.id(), "USD"));
        assertEquals(Money.of("USD", 2_500), env.accounts.balance(bossAccount.id(), "USD"));
    }

    @Test
    void memberWithoutTheNodeIsDeniedAndNothingMoves() {
        BankResult r = env.bank.transfer(clerk.id(), "acme", bossAccount.id(), Money.of("USD", 100), "w-2");
        assertEquals(new BankResult.Denied("org.account.withdraw"), r);
        assertEquals(Money.of("USD", 10_000), env.accounts.balance(orgAccount.id(), "USD"));
    }

    @Test
    void nonMemberIsDenied() {
        PlayerCharacter stranger = env.newCharacter();
        assertEquals(new BankResult.Denied("org.account.withdraw"),
                env.bank.transfer(stranger.id(), "acme", bossAccount.id(), Money.of("USD", 100), "w-3"));
        assertEquals(Money.of("USD", 10_000), env.accounts.balance(orgAccount.id(), "USD"));
        assertEquals(Money.of("USD", 0), env.accounts.balance(bossAccount.id(), "USD"));
    }

    @Test
    void sameKeyTwiceIsADuplicateNotADoubleSpend() {
        env.bank.transfer(boss.id(), "acme", bossAccount.id(), Money.of("USD", 1_000), "w-4");
        BankResult second = env.bank.transfer(boss.id(), "acme", bossAccount.id(), Money.of("USD", 1_000), "w-4");
        assertInstanceOf(TransferResult.Duplicate.class, ((BankResult.Transferred) second).result());
        assertEquals(Money.of("USD", 9_000), env.accounts.balance(orgAccount.id(), "USD"));
    }

    @Test
    void insufficientFundsIsReportedByTheEconomy() {
        BankResult r = env.bank.transfer(boss.id(), "acme", bossAccount.id(), Money.of("USD", 99_999), "w-5");
        assertInstanceOf(TransferResult.InsufficientFunds.class, ((BankResult.Transferred) r).result());
        assertEquals(Money.of("USD", 10_000), env.accounts.balance(orgAccount.id(), "USD"));
        assertEquals(Money.of("USD", 0), env.accounts.balance(bossAccount.id(), "USD"));
    }

    @Test
    void unknownAndArchivedOrganizationsFail() {
        assertEquals(new BankResult.Failed(new OrgError.UnknownOrg("nope")),
                env.bank.transfer(boss.id(), "nope", bossAccount.id(), Money.of("USD", 1), "w-6"));
        ok(env.orgs.archive("acme"));
        assertEquals(new BankResult.Failed(new OrgError.Archived("acme")),
                env.bank.transfer(boss.id(), "acme", bossAccount.id(), Money.of("USD", 1), "w-7"));
        assertEquals(Money.of("USD", 10_000), env.accounts.balance(orgAccount.id(), "USD"));
    }

    @Test
    void depositsDoNotNeedAPermission() {
        env.fund(bossAccount, "USD", 500);
        TransferResult r = env.txs.transfer(TransferRequest.builder()
                .from(bossAccount.id()).to(orgAccount.id()).amount(Money.of("USD", 500))
                .reason("org.deposit").idempotencyKey("dep-1").build());
        assertInstanceOf(TransferResult.Success.class, r);
        assertEquals(Money.of("USD", 10_500), env.accounts.balance(orgAccount.id(), "USD"));
    }

    @Test
    void withdrawalRecordsOrganizationAndCharacterInMetadata() {
        env.bank.transfer(boss.id(), "acme", bossAccount.id(), Money.of("USD", 100), "w-8");
        String meta = env.txs.findByKey("w-8").orElseThrow().metadataJson();
        assertTrue(meta.contains("\"org\":\"acme\""), meta);
        assertTrue(meta.contains(boss.id().value().toString()), meta);
    }
}
