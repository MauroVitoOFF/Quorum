package dev.qrm.example;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.qrm.api.Money;
import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.economy.*;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.GradeDefinition;
import dev.qrm.api.job.JobDefinition;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.org.OrganizationService;
import dev.qrm.api.permission.PermissionService;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Mod("qrm_example")
public final class ExampleMod {
    private static final Logger LOG = LoggerFactory.getLogger("qrm_example");
    private static final String OWNER = "qrm_example";

    public ExampleMod(IEventBus modBus) {
        // 1. Registrare una valuta: possibile già nel costruttore, il registro esiste prima del server.
        QRM.get(CurrencyRegistry.class).register(new Currency("EXC", "E", 2));

        // 1b. Registrare un lavoro: anche questo possibile nel costruttore (il registro esiste prima del server).
        QRM.get(JobRegistry.class).register(new JobDefinition("example_worker", "Operaio di esempio", List.of(
                new GradeDefinition("trainee", "Apprendista", 1, Set.of("example.work"), Money.of("EXC", 5_000)),
                new GradeDefinition("foreman", "Caposquadra", 2, Set.of("example.*", "org.account.withdraw"), null)),
                "example_corp"));

        // 2. Intervenire PRIMA di una transazione: blocca le ragioni che iniziano con "illegal.".
        QRM.events().register(TransactionPreEvent.class, OWNER, e -> {
            if (e.request().reason().startsWith("illegal.")) e.cancel(OWNER, "illegal transaction");
        });

        // 3. Reagire DOPO una transazione.
        QRM.events().register(TransactionPostEvent.class, OWNER, e ->
                LOG.info("Transaction {} moved {} {}", e.transaction().id(),
                        e.transaction().amount().amount(), e.transaction().amount().currency()));

        NeoForge.EVENT_BUS.addListener((RegisterCommandsEvent e) -> e.getDispatcher().register(
                Commands.literal("example")
                        .then(Commands.literal("paycheck").executes(ExampleMod::paycheck))
                        .then(Commands.literal("work").executes(ExampleMod::work))
                        .then(Commands.literal("shop").executes(ExampleMod::shop))));
    }

    /** Paga 100.00 EXC dal conto di sistema al personaggio attivo del giocatore. */
    private static int paycheck(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        AccountService accounts = QRM.get(AccountService.class);
        Optional<Account> target = QRM.get(CharacterService.class).activeOf(new PlayerId(p.getUUID()))
                .flatMap(pc -> accounts.accountsOf(OwnerRef.character(pc.id())).stream().findFirst());
        if (target.isEmpty()) {
            c.getSource().sendFailure(Component.literal("Nessun personaggio attivo."));
            return 0;
        }
        TransferResult r = QRM.get(TransactionService.class).transfer(TransferRequest.builder()
                .from(accounts.systemAccount().id()).to(target.get().id())
                .amount(Money.of("EXC", 10_000)).reason("example.paycheck")
                .idempotencyKey(UUID.randomUUID().toString()).build());
        boolean ok = r instanceof TransferResult.Success;
        c.getSource().sendSuccess(() -> Component.literal(ok ? "Stipendio accreditato." : "Pagamento non riuscito: " + r), false);
        return ok ? 1 : 0;
    }

    /** Esegue "lavoro" solo se il personaggio attivo ha il permesso example.work (serve il lavoro example_worker in servizio). */
    private static int work(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Optional<dev.qrm.api.character.PlayerCharacter> pc =
                QRM.get(CharacterService.class).activeOf(new PlayerId(p.getUUID()));
        if (pc.isEmpty()) {
            c.getSource().sendFailure(Component.literal("Nessun personaggio attivo."));
            return 0;
        }
        boolean allowed = QRM.get(PermissionService.class).has(pc.get().id(), "example.work");
        if (allowed) {
            c.getSource().sendSuccess(() -> Component.literal("Hai lavorato."), false);
        } else {
            c.getSource().sendFailure(Component.literal(
                    "Permesso example.work mancante (serve il lavoro example_worker, in servizio)."));
        }
        return allowed ? 1 : 0;
    }

    /**
     * Mostra se il personaggio attivo può prelevare dalla cassa di "example_corp" (serve esserne membro con il nodo
     * org.account.withdraw, oppure avere il lavoro example_worker/foreman in servizio). L'organizzazione si crea con
     * /qrm admin org create example_corp Example Corp.
     */
    private static int shop(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Optional<dev.qrm.api.character.PlayerCharacter> pc =
                QRM.get(CharacterService.class).activeOf(new PlayerId(p.getUUID()));
        if (pc.isEmpty()) {
            c.getSource().sendFailure(Component.literal("Nessun personaggio attivo."));
            return 0;
        }
        boolean allowed = QRM.get(OrganizationService.class).can(pc.get().id(), "example_corp", "org.account.withdraw");
        c.getSource().sendSuccess(() -> Component.literal(allowed
                ? "Puoi prelevare dalla cassa di example_corp."
                : "Non puoi prelevare dalla cassa di example_corp."), false);
        return allowed ? 1 : 0;
    }
}
