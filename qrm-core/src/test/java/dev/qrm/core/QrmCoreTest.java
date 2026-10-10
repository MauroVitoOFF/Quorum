package dev.qrm.core;

import static org.junit.jupiter.api.Assertions.*;

import dev.qrm.api.Money;
import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.CreateCharacterResult;
import dev.qrm.api.economy.*;
import dev.qrm.api.event.QrmReadyEvent;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.player.PlayerService;
import dev.qrm.core.economy.InMemoryCurrencyRegistry;
import dev.qrm.core.event.SimpleEventBus;
import dev.qrm.core.job.InMemoryJobRegistry;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class QrmCoreTest {
    private QrmCore core;

    @AfterEach
    void tearDown() {
        if (core != null) core.close();
        QRM.clear();
    }

    @Test
    void fullFlowThroughPublicApi() {
        SimpleEventBus bus = new SimpleEventBus();
        InMemoryCurrencyRegistry currencies = new InMemoryCurrencyRegistry();
        currencies.register(new Currency("USD", "$", 2));
        QRM.register(dev.qrm.api.event.EventBus.class, bus);
        QRM.register(CurrencyRegistry.class, currencies);
        AtomicInteger ready = new AtomicInteger();
        QRM.events().register(QrmReadyEvent.class, "t", e -> ready.incrementAndGet());

        core = QrmCore.start(bus, currencies, new InMemoryJobRegistry(), new dev.qrm.core.point.InMemoryPointTypeRegistry(), "jdbc:sqlite::memory:", null, null, CoreConfig.defaults(), Clock.systemUTC());

        assertEquals(1, ready.get());
        PlayerId pid = new PlayerId(UUID.randomUUID());
        QRM.get(PlayerService.class).onJoin(pid);
        var created = (CreateCharacterResult.Created) QRM.get(CharacterService.class)
                .create(pid, "Mario", "Rossi", LocalDate.of(1990, 1, 1));

        AccountService accounts = QRM.get(AccountService.class);
        List<Account> owned = accounts.accountsOf(OwnerRef.character(created.character().id()));
        assertEquals(1, owned.size());

        TransactionService txs = QRM.get(TransactionService.class);
        var r = txs.transfer(TransferRequest.builder().from(accounts.systemAccount().id()).to(owned.get(0).id())
                .amount(Money.of("USD", 1000)).reason("payroll").idempotencyKey("e2e-1").build());
        assertInstanceOf(TransferResult.Success.class, r);
        assertEquals(Money.of("USD", 1000), accounts.balance(owned.get(0).id(), "USD"));
        assertEquals(List.of(), QRM.get(AuditService.class).verify());
    }

    @Test
    void closeUnregistersServicesButKeepsAdapterRegistrations() {
        SimpleEventBus bus = new SimpleEventBus();
        InMemoryCurrencyRegistry currencies = new InMemoryCurrencyRegistry();
        QRM.register(dev.qrm.api.event.EventBus.class, bus);
        core = QrmCore.start(bus, currencies, new InMemoryJobRegistry(), new dev.qrm.core.point.InMemoryPointTypeRegistry(), "jdbc:sqlite::memory:", null, null, CoreConfig.defaults(), Clock.systemUTC());
        core.close();
        core = null;
        assertThrows(IllegalStateException.class, () -> QRM.get(PlayerService.class));
        assertSame(bus, QRM.events());
    }

    @Test
    void closedCoreLeavesNoStaleCharacterHandlerOnSameBus() {
        SimpleEventBus real = new SimpleEventBus();
        AtomicInteger live = new AtomicInteger();
        dev.qrm.api.event.EventBus bus = new dev.qrm.api.event.EventBus() {
            @Override
            public <E extends dev.qrm.api.event.Event> dev.qrm.api.event.Registration register(Class<E> type,
                    dev.qrm.api.event.Priority priority, boolean ignoreCancelled, String owner,
                    java.util.function.Consumer<? super E> handler) {
                var reg = real.register(type, priority, ignoreCancelled, owner, handler);
                live.incrementAndGet();
                return () -> {
                    live.decrementAndGet();
                    reg.unregister();
                };
            }

            @Override
            public <E extends dev.qrm.api.event.Event> E post(E event) {
                return real.post(event);
            }
        };
        InMemoryCurrencyRegistry currencies = new InMemoryCurrencyRegistry();
        QRM.register(dev.qrm.api.event.EventBus.class, bus);
        QrmCore first = QrmCore.start(bus, currencies, new InMemoryJobRegistry(), new dev.qrm.core.point.InMemoryPointTypeRegistry(), "jdbc:sqlite::memory:", null, null, CoreConfig.defaults(), Clock.systemUTC());
        assertEquals(2, live.get());
        first.close();
        assertEquals(0, live.get());
        core = QrmCore.start(bus, currencies, new InMemoryJobRegistry(), new dev.qrm.core.point.InMemoryPointTypeRegistry(), "jdbc:sqlite::memory:", null, null, CoreConfig.defaults(), Clock.systemUTC());
        assertEquals(2, live.get());
        PlayerId pid = new PlayerId(UUID.randomUUID());
        QRM.get(PlayerService.class).onJoin(pid);
        var created = (CreateCharacterResult.Created) QRM.get(CharacterService.class)
                .create(pid, "Mario", "Rossi", LocalDate.of(1990, 1, 1));
        assertEquals(1, QRM.get(AccountService.class)
                .accountsOf(OwnerRef.character(created.character().id())).size());
    }
}
