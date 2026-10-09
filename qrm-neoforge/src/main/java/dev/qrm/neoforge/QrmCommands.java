package dev.qrm.neoforge;

import dev.qrm.neoforge.server.StaffAudit;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.qrm.api.Money;
import dev.qrm.api.QRM;
import dev.qrm.api.character.*;
import dev.qrm.api.economy.*;
import dev.qrm.api.id.PlayerId;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.RegisterCommandsEvent;

final class QrmCommands {
    private QrmCommands() {}

    static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
        ShortCommands.register(event.getDispatcher(), QrmConfig.SHORT_ALIASES.get());
    }

    static void register(CommandDispatcher<CommandSourceStack> d) {
        d.register(Commands.literal("qrm")
                .then(characterNode("character"))
                .then(Commands.literal("balance").executes(QrmCommands::balance))
                .then(JobCommands.job())
                .then(JobCommands.duty())
                .then(OrgCommands.org())
                .then(Commands.literal("pay").then(playerAmountCurrency(QrmCommands::pay)))
                .then(Commands.literal("admin")
                        .requires(QrmCommands::isOp)
                        .then(Commands.literal("grant").then(playerAmountCurrency(QrmCommands::grant)))
                        .then(Commands.literal("take").then(playerAmountCurrency(QrmCommands::take)))
                        .then(Commands.literal("audit").executes(QrmCommands::audit))
                        .then(JobCommands.adminJob())
                        .then(JobCommands.adminPerm())
                        .then(OrgCommands.adminOrg())
                        .then(StaffCommands.adminStaff())
                        .then(LogCommands.adminLog())));
    }

    /** Un comando che opera su un importo e su una valuta facoltativa ({@code null} = valuta predefinita). */
    @FunctionalInterface
    interface CurrencyHandler {
        int run(CommandContext<CommandSourceStack> c, String currencyCode) throws CommandSyntaxException;
    }

    /** {@code <giocatore> <importo> [valuta]}, condiviso da pay, grant e take (anche nelle scorciatoie). */
    static RequiredArgumentBuilder<CommandSourceStack, ?> playerAmountCurrency(CurrencyHandler handler) {
        return Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("amount", StringArgumentType.word())
                        .executes(c -> handler.run(c, null))
                        .then(Commands.argument("currency", StringArgumentType.word())
                                .suggests(CommandSuggestions.CURRENCIES)
                                .executes(c -> handler.run(c, StringArgumentType.getString(c, "currency")))));
    }

    /** {@code create|list|select|archive}, sotto {@code /qrm character} e sotto {@code /char}. */
    static LiteralArgumentBuilder<CommandSourceStack> characterNode(String name) {
        return Commands.literal(name)
                .then(Commands.literal("create")
                        .then(Commands.argument("first", StringArgumentType.string())
                                .then(Commands.argument("last", StringArgumentType.string())
                                        .then(Commands.argument("birth", StringArgumentType.word())
                                                .executes(QrmCommands::characterCreate)))))
                .then(Commands.literal("list").executes(QrmCommands::characterList))
                .then(Commands.literal("select")
                        .then(Commands.argument("n", IntegerArgumentType.integer(1))
                                .executes(QrmCommands::characterSelect)))
                .then(Commands.literal("archive")
                        .then(Commands.argument("n", IntegerArgumentType.integer(1))
                                .executes(QrmCommands::characterArchive)));
    }

    /** Livello operatore (gamemaster), lo stesso richiesto da tutto il ramo admin. */
    static boolean isOp(CommandSourceStack source) {
        return source.permissions().hasPermission(net.minecraft.server.permissions.Permissions.COMMANDS_GAMEMASTER);
    }

    // ---- helpers -------------------------------------------------------------------------------------------

    static int ok(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendSuccess(() -> Component.literal(text), false);
        return 1;
    }

    static int fail(CommandContext<CommandSourceStack> c, String text) {
        c.getSource().sendFailure(Component.literal(text));
        return 0;
    }

    static Optional<PlayerCharacter> active(ServerPlayer p) {
        return QRM.get(CharacterService.class).activeOf(PlayerEvents.idOf(p));
    }

    private static Optional<Account> accountOf(PlayerCharacter pc) {
        return QRM.get(AccountService.class).accountsOf(OwnerRef.character(pc.id())).stream().findFirst();
    }

    private static List<PlayerCharacter> usable(PlayerId id) {
        return QRM.get(CharacterService.class).listOf(id).stream()
                .filter(pc -> pc.status() != CharacterStatus.ARCHIVED).toList();
    }

    private static Optional<Currency> currency(String code) {
        String resolved = code != null ? code.toUpperCase() : QrmConfig.DEFAULT_CURRENCY.get();
        return QRM.get(CurrencyRegistry.class).find(resolved);
    }

    private static String describe(TransferResult r) {
        return switch (r) {
            case TransferResult.Success s -> "Operazione completata.";
            case TransferResult.Duplicate d -> "Operazione già eseguita.";
            case TransferResult.InsufficientFunds f -> "Fondi insufficienti.";
            case TransferResult.AccountNotFound n -> "Conto non trovato.";
            case TransferResult.AccountFrozen z -> "Conto congelato.";
            case TransferResult.UnknownCurrency u -> "Valuta sconosciuta: " + u.code();
            case TransferResult.InvalidRequest i -> "Richiesta non valida: " + i.reason();
            case TransferResult.Cancelled x -> "Operazione annullata da " + x.by() + ": " + x.reason();
        };
    }

    // ---- character -----------------------------------------------------------------------------------------

    private static int characterCreate(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        LocalDate birth;
        try {
            birth = LocalDate.parse(StringArgumentType.getString(c, "birth"));
        } catch (DateTimeParseException e) {
            return fail(c, "Data non valida, usa il formato AAAA-MM-GG.");
        }
        var r = QRM.get(CharacterService.class).create(PlayerEvents.idOf(p),
                StringArgumentType.getString(c, "first"), StringArgumentType.getString(c, "last"), birth);
        return switch (r) {
            case CreateCharacterResult.Created cr -> ok(c, "Personaggio creato: " + cr.character().firstName() + " " + cr.character().lastName());
            case CreateCharacterResult.UnknownPlayer u -> fail(c, "Giocatore non registrato.");
            case CreateCharacterResult.SlotsFull s -> fail(c, "Hai raggiunto il numero massimo di personaggi.");
            case CreateCharacterResult.InvalidName n -> fail(c, "Nome non valido: " + n.reason());
            case CreateCharacterResult.NameTaken t -> fail(c, "Nome già in uso.");
        };
    }

    static int characterList(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        List<PlayerCharacter> list = usable(PlayerEvents.idOf(p));
        if (list.isEmpty()) return fail(c, "Non hai personaggi.");
        Optional<PlayerCharacter> active = active(p);
        for (int i = 0; i < list.size(); i++) {
            PlayerCharacter pc = list.get(i);
            boolean isActive = active.isPresent() && active.get().id().equals(pc.id());
            ok(c, (i + 1) + ". " + pc.firstName() + " " + pc.lastName() + (isActive ? "  [attivo]" : ""));
        }
        return list.size();
    }

    private static int characterSelect(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        List<PlayerCharacter> list = usable(PlayerEvents.idOf(p));
        int n = IntegerArgumentType.getInteger(c, "n");
        if (n > list.size()) return fail(c, "Numero non valido.");
        var r = QRM.get(CharacterService.class).select(PlayerEvents.idOf(p), list.get(n - 1).id());
        return r instanceof SelectCharacterResult.Selected ? ok(c, "Personaggio selezionato.") : fail(c, "Impossibile selezionare.");
    }

    private static int characterArchive(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        List<PlayerCharacter> list = usable(PlayerEvents.idOf(p));
        int n = IntegerArgumentType.getInteger(c, "n");
        if (n > list.size()) return fail(c, "Numero non valido.");
        return QRM.get(CharacterService.class).archive(PlayerEvents.idOf(p), list.get(n - 1).id())
                ? ok(c, "Personaggio archiviato.") : fail(c, "Impossibile archiviare.");
    }

    // ---- economy -------------------------------------------------------------------------------------------

    static int balance(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Optional<Account> acc = active(p).flatMap(QrmCommands::accountOf);
        if (acc.isEmpty()) return fail(c, "Nessun personaggio attivo.");
        List<Money> balances = QRM.get(AccountService.class).balances(acc.get().id());
        if (balances.isEmpty()) return ok(c, "Saldo: vuoto.");
        CurrencyRegistry reg = QRM.get(CurrencyRegistry.class);
        for (Money m : balances) {
            ok(c, reg.find(m.currency()).map(cur -> cur.format(m.amount())).orElse(m.amount() + " " + m.currency()));
        }
        return balances.size();
    }

    static int pay(CommandContext<CommandSourceStack> c, String currencyCode) throws CommandSyntaxException {
        ServerPlayer from = c.getSource().getPlayerOrException();
        ServerPlayer to = EntityArgument.getPlayer(c, "player");
        Optional<Account> a = active(from).flatMap(QrmCommands::accountOf);
        Optional<Account> b = active(to).flatMap(QrmCommands::accountOf);
        if (a.isEmpty()) return fail(c, "Nessun personaggio attivo.");
        if (b.isEmpty()) return fail(c, "Il destinatario non ha un personaggio attivo.");
        return transferCommand(c, a.get(), b.get(), currencyCode, "player.pay");
    }

    static int grant(CommandContext<CommandSourceStack> c, String currencyCode) throws CommandSyntaxException {
        return StaffAudit.run(c, "cmd.money.grant", () -> grantNow(c, currencyCode));
    }

    private static int grantNow(CommandContext<CommandSourceStack> c, String currencyCode) throws CommandSyntaxException {
        ServerPlayer to = EntityArgument.getPlayer(c, "player");
        Optional<Account> b = active(to).flatMap(QrmCommands::accountOf);
        if (b.isEmpty()) return fail(c, "Il destinatario non ha un personaggio attivo.");
        return transferCommand(c, QRM.get(AccountService.class).systemAccount(), b.get(), currencyCode, "admin.grant");
    }

    /** Toglie denaro al personaggio attivo del giocatore (verso il conto di sistema); come un normale trasferimento non va in negativo. */
    static int take(CommandContext<CommandSourceStack> c, String currencyCode) throws CommandSyntaxException {
        return StaffAudit.run(c, "cmd.money.take", () -> takeNow(c, currencyCode));
    }

    private static int takeNow(CommandContext<CommandSourceStack> c, String currencyCode) throws CommandSyntaxException {
        ServerPlayer from = EntityArgument.getPlayer(c, "player");
        Optional<Account> a = active(from).flatMap(QrmCommands::accountOf);
        if (a.isEmpty()) return fail(c, "Il giocatore non ha un personaggio attivo.");
        return transferCommand(c, a.get(), QRM.get(AccountService.class).systemAccount(), currencyCode, "admin.take");
    }

    private static int transferCommand(CommandContext<CommandSourceStack> c, Account from, Account to,
            String currencyCode, String reason) {
        Optional<Currency> cur = currency(currencyCode);
        if (cur.isEmpty()) return fail(c, "Valuta sconosciuta.");
        long amount;
        try {
            amount = cur.get().parseAmount(StringArgumentType.getString(c, "amount"));
        } catch (IllegalArgumentException e) {
            return fail(c, e.getMessage());
        }
        TransferResult r = QRM.get(TransactionService.class).transfer(TransferRequest.builder()
                .from(from.id()).to(to.id()).amount(Money.of(cur.get().code(), amount))
                .reason(reason).idempotencyKey(UUID.randomUUID().toString()).build());
        return r instanceof TransferResult.Success ? ok(c, describe(r)) : fail(c, describe(r));
    }

    private static int audit(CommandContext<CommandSourceStack> c) {
        List<AuditIssue> issues = QRM.get(AuditService.class).verify();
        if (issues.isEmpty()) return ok(c, "Audit: tutti i saldi sono coerenti.");
        issues.forEach(i -> fail(c, "Incoerenza " + i.account().value() + " " + i.currency()
                + ": atteso " + i.expected() + ", trovato " + i.actual()));
        return 0;
    }
}
