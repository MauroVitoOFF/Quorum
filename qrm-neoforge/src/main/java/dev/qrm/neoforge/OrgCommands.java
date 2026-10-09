package dev.qrm.neoforge;

import dev.qrm.neoforge.server.StaffAudit;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.qrm.api.Money;
import dev.qrm.api.QRM;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.economy.AccountService;
import dev.qrm.api.economy.CurrencyRegistry;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.org.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/** Comandi delle organizzazioni; i literal sono innestati da {@link QrmCommands}. */
final class OrgCommands {
    private OrgCommands() {}

    /** {@code /qrm org list|info <org>} */
    static LiteralArgumentBuilder<CommandSourceStack> org() {
        return Commands.literal("org")
                .then(Commands.literal("list").executes(OrgCommands::list))
                .then(infoNode());
    }

    /**
     * {@code /org}: da solo elenca le proprie organizzazioni; {@code list} e {@code info} sono per tutti, il resto
     * ({@code create}, {@code archive}, {@code rank}, {@code member}) è riservato agli operatori e nascosto agli altri.
     */
    static LiteralArgumentBuilder<CommandSourceStack> orgRoot() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("org")
                .executes(OrgCommands::list)
                .then(Commands.literal("list").executes(OrgCommands::list))
                .then(infoNode());
        adminNodes().forEach(node -> root.then(node.requires(QrmCommands::isOp)));
        return root;
    }

    /** {@code /qrm admin org ...} (innestato sotto il ramo admin, già riservato agli operatori) */
    static LiteralArgumentBuilder<CommandSourceStack> adminOrg() {
        LiteralArgumentBuilder<CommandSourceStack> root = Commands.literal("org");
        adminNodes().forEach(root::then);
        return root;
    }

    private static LiteralArgumentBuilder<CommandSourceStack> infoNode() {
        return Commands.literal("info")
                .then(Commands.argument("org", StringArgumentType.word())
                        .suggests(CommandSuggestions.ORGS)
                        .executes(OrgCommands::info));
    }

    private static List<LiteralArgumentBuilder<CommandSourceStack>> adminNodes() {
        return List.of(
                Commands.literal("create")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .then(Commands.argument("label", StringArgumentType.greedyString())
                                        .executes(StaffAudit.audited("cmd.org.create", OrgCommands::create)))),
                Commands.literal("archive")
                        .then(Commands.argument("id", StringArgumentType.word())
                                .suggests(CommandSuggestions.ORGS)
                                .executes(StaffAudit.audited("cmd.org.archive", OrgCommands::archive))),
                Commands.literal("rank")
                        .then(Commands.literal("set")
                                .then(Commands.argument("org", StringArgumentType.word())
                                        .suggests(CommandSuggestions.ORGS)
                                        .then(Commands.argument("rank", StringArgumentType.word())
                                                .suggests(CommandSuggestions.RANKS)
                                                .then(Commands.argument("level", IntegerArgumentType.integer(0))
                                                        .executes(StaffAudit.audited("cmd.org.rank_set", c -> rankSet(c, "")))
                                                        .then(Commands.argument("nodes", StringArgumentType.greedyString())
                                                                .executes(StaffAudit.audited("cmd.org.rank_set",
                                                                c -> rankSet(c, StringArgumentType.getString(c, "nodes")))))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("org", StringArgumentType.word())
                                        .suggests(CommandSuggestions.ORGS)
                                        .then(Commands.argument("rank", StringArgumentType.word())
                                                .suggests(CommandSuggestions.RANKS)
                                                .executes(StaffAudit.audited("cmd.org.rank_remove", OrgCommands::rankRemove))))),
                Commands.literal("member")
                        .then(Commands.literal("add")
                                .then(Commands.argument("org", StringArgumentType.word())
                                        .suggests(CommandSuggestions.ORGS)
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("rank", StringArgumentType.word())
                                                        .suggests(CommandSuggestions.RANKS)
                                                        .executes(StaffAudit.audited("cmd.org.member_add", OrgCommands::memberAdd))))))
                        .then(Commands.literal("setrank")
                                .then(Commands.argument("org", StringArgumentType.word())
                                        .suggests(CommandSuggestions.ORGS)
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .then(Commands.argument("rank", StringArgumentType.word())
                                                        .suggests(CommandSuggestions.RANKS)
                                                        .executes(StaffAudit.audited("cmd.org.member_set_rank", OrgCommands::memberSetRank))))))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("org", StringArgumentType.word())
                                        .suggests(CommandSuggestions.ORGS)
                                        .then(Commands.argument("player", EntityArgument.player())
                                                .executes(StaffAudit.audited("cmd.org.member_remove", OrgCommands::memberRemove))))));
    }

    // ---- giocatore -----------------------------------------------------------------------------------------

    private static int list(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Optional<PlayerCharacter> pc = QrmCommands.active(p);
        if (pc.isEmpty()) return QrmCommands.fail(c, "Nessun personaggio attivo.");
        List<Organization> orgs = QRM.get(OrganizationService.class).organizationsOf(pc.get().id());
        if (orgs.isEmpty()) return QrmCommands.ok(c, "Non appartieni a nessuna organizzazione.");
        for (Organization o : orgs) QrmCommands.ok(c, o.id() + " (" + o.label() + ")");
        return orgs.size();
    }

    private static int info(CommandContext<CommandSourceStack> c) {
        OrganizationService service = QRM.get(OrganizationService.class);
        String id = StringArgumentType.getString(c, "org");
        boolean op = QrmCommands.isOp(c.getSource());
        Optional<Organization> org = service.find(id);
        if (!op) {
            ServerPlayer player = c.getSource().getPlayer();
            Optional<PlayerCharacter> pc = player == null ? Optional.empty() : QrmCommands.active(player);
            boolean member = pc.isPresent() && service.organizationsOf(pc.get().id()).stream().anyMatch(o -> o.id().equals(id));
            if (!member) return QrmCommands.fail(c, "Non appartieni a questa organizzazione.");
        }
        if (org.isEmpty()) return QrmCommands.fail(c, describe(new OrgError.UnknownOrg(id)));
        QrmCommands.ok(c, org.get().label() + " [" + org.get().status() + "]");
        String ranks = service.ranksOf(id).stream().map(r -> r.id() + "[" + r.level() + "]").collect(Collectors.joining(", "));
        QrmCommands.ok(c, "Rank: " + (ranks.isEmpty() ? "nessuno" : ranks));
        QrmCommands.ok(c, "Membri: " + service.membersOf(id).size());
        CurrencyRegistry currencies = QRM.get(CurrencyRegistry.class);
        List<Money> balances = QRM.get(AccountService.class).balances(org.get().account());
        if (balances.isEmpty()) return QrmCommands.ok(c, "Saldo: vuoto.");
        for (Money m : balances) {
            QrmCommands.ok(c, currencies.find(m.currency()).map(cur -> cur.format(m.amount()))
                    .orElse(m.amount() + " " + m.currency()));
        }
        return 1;
    }

    // ---- amministratori ------------------------------------------------------------------------------------

    private static int create(CommandContext<CommandSourceStack> c) {
        try {
            return report(c, QRM.get(OrganizationService.class).create(StringArgumentType.getString(c, "id"),
                    StringArgumentType.getString(c, "label").strip()), o -> "Creata: " + o.id());
        } catch (IllegalArgumentException e) {
            return QrmCommands.fail(c, "Id o nome non validi: " + e.getMessage());
        }
    }

    private static int archive(CommandContext<CommandSourceStack> c) {
        return report(c, QRM.get(OrganizationService.class).archive(StringArgumentType.getString(c, "id")),
                o -> "Archiviata: " + o.id());
    }

    private static int rankSet(CommandContext<CommandSourceStack> c, String nodes) {
        Set<String> perms = nodes.isBlank() ? Set.of() : new HashSet<>(Arrays.asList(nodes.strip().split("\\s+")));
        try {
            Rank rank = new Rank(StringArgumentType.getString(c, "rank"), StringArgumentType.getString(c, "rank"),
                    IntegerArgumentType.getInteger(c, "level"), perms);
            return report(c, QRM.get(OrganizationService.class).defineRank(StringArgumentType.getString(c, "org"), rank),
                    r -> "Rank " + r.id() + " [" + r.level() + "] salvato.");
        } catch (IllegalArgumentException e) {
            return QrmCommands.fail(c, "Rank o nodi non validi: " + e.getMessage());
        }
    }

    private static int rankRemove(CommandContext<CommandSourceStack> c) {
        return report(c, QRM.get(OrganizationService.class).removeRank(StringArgumentType.getString(c, "org"),
                StringArgumentType.getString(c, "rank")), r -> "Rank rimosso: " + r.id());
    }

    private static Optional<CharacterId> target(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        return QrmCommands.active(EntityArgument.getPlayer(c, "player")).map(PlayerCharacter::id);
    }

    private static int memberAdd(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Optional<CharacterId> id = target(c);
        if (id.isEmpty()) return QrmCommands.fail(c, "Il giocatore non ha un personaggio attivo.");
        return report(c, QRM.get(OrganizationService.class).addMember(StringArgumentType.getString(c, "org"), id.get(),
                StringArgumentType.getString(c, "rank")), m -> "Aggiunto come " + m.rankId() + ".");
    }

    private static int memberSetRank(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Optional<CharacterId> id = target(c);
        if (id.isEmpty()) return QrmCommands.fail(c, "Il giocatore non ha un personaggio attivo.");
        return report(c, QRM.get(OrganizationService.class).setRank(StringArgumentType.getString(c, "org"), id.get(),
                StringArgumentType.getString(c, "rank")), m -> "Rank cambiato in " + m.rankId() + ".");
    }

    private static int memberRemove(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Optional<CharacterId> id = target(c);
        if (id.isEmpty()) return QrmCommands.fail(c, "Il giocatore non ha un personaggio attivo.");
        return report(c, QRM.get(OrganizationService.class).removeMember(StringArgumentType.getString(c, "org"), id.get()),
                m -> "Rimosso dall'organizzazione.");
    }

    // ---- esiti ---------------------------------------------------------------------------------------------

    private static <T> int report(CommandContext<CommandSourceStack> c, OrgResult<T> result, Function<T, String> okText) {
        return switch (result) {
            case OrgResult.Ok<T> ok -> QrmCommands.ok(c, okText.apply(ok.value()));
            case OrgResult.Failed<T> failed -> QrmCommands.fail(c, describe(failed.error()));
        };
    }

    private static String describe(OrgError error) {
        return switch (error) {
            case OrgError.UnknownOrg e -> "Organizzazione sconosciuta: " + e.orgId();
            case OrgError.Archived e -> "Organizzazione archiviata: " + e.orgId();
            case OrgError.Exists e -> "Organizzazione già esistente: " + e.orgId();
            case OrgError.UnknownRank e -> "Rank sconosciuto: " + e.rankId();
            case OrgError.RankInUse e -> "Rank in uso da dei membri: " + e.rankId();
            case OrgError.LevelTaken e -> "Livello già usato da un altro rank: " + e.level();
            case OrgError.UnknownCharacter e -> "Personaggio sconosciuto.";
            case OrgError.NotUsable e -> "Personaggio non utilizzabile: " + e.status();
            case OrgError.AlreadyMember e -> "È già membro.";
            case OrgError.NotMember e -> "Non è membro.";
            case OrgError.Cancelled e -> "Operazione annullata da " + e.by() + ": " + e.reason();
        };
    }
}
