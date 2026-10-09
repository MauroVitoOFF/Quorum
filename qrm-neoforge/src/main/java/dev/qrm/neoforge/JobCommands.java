package dev.qrm.neoforge;

import dev.qrm.neoforge.server.StaffAudit;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.*;
import dev.qrm.api.permission.PermissionService;
import dev.qrm.neoforge.server.DutyActions;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/** Comandi di lavori e permessi; i literal sono innestati da {@link QrmCommands}. */
final class JobCommands {
    private JobCommands() {}

    /** {@code /qrm job info} */
    static LiteralArgumentBuilder<CommandSourceStack> job() {
        return Commands.literal("job").then(Commands.literal("info").executes(JobCommands::info));
    }

    /** {@code /qrm duty on|off} */
    static LiteralArgumentBuilder<CommandSourceStack> duty() {
        return Commands.literal("duty")
                .executes(JobCommands::toggleDuty)
                .then(Commands.literal("on").executes(c -> duty(c, true)))
                .then(Commands.literal("off").executes(c -> duty(c, false)));
    }

    /** {@code /qrm admin job list|set|fire} (innestato sotto il ramo admin, già riservato agli operatori) */
    static LiteralArgumentBuilder<CommandSourceStack> adminJob() {
        return Commands.literal("job")
                .then(Commands.literal("list").executes(JobCommands::list))
                .then(setNode())
                .then(removeNode("fire"));
    }

    /**
     * {@code /job}: da solo mostra il proprio lavoro; gli operatori hanno in più {@code set}, {@code remove} e
     * {@code list}, nascosti agli altri giocatori.
     */
    static LiteralArgumentBuilder<CommandSourceStack> jobRoot() {
        return Commands.literal("job")
                .executes(JobCommands::info)
                .then(Commands.literal("info").executes(JobCommands::info))
                .then(setNode().requires(QrmCommands::isOp))
                .then(removeNode("remove").requires(QrmCommands::isOp))
                .then(Commands.literal("list").requires(QrmCommands::isOp).executes(JobCommands::list));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> setNode() {
        return Commands.literal("set")
                .then(Commands.argument("player", EntityArgument.player())
                        .then(Commands.argument("job", StringArgumentType.word())
                                .suggests(CommandSuggestions.JOBS)
                                .then(Commands.argument("grade", StringArgumentType.word())
                                        .suggests(CommandSuggestions.GRADES)
                                        .executes(StaffAudit.audited("cmd.job.set", JobCommands::set)))));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> removeNode(String name) {
        return Commands.literal(name)
                .then(Commands.argument("player", EntityArgument.player()).executes(StaffAudit.audited("cmd.job.fire", JobCommands::fire)));
    }

    /** {@code /qrm admin perm grant|deny|clear <giocatore> <nodo>}; il nodo può contenere il jolly ({@code police.*}). */
    static LiteralArgumentBuilder<CommandSourceStack> adminPerm() {
        return Commands.literal("perm")
                .then(permLiteral("grant", Op.GRANT))
                .then(permLiteral("deny", Op.DENY))
                .then(permLiteral("clear", Op.CLEAR));
    }

    private enum Op { GRANT, DENY, CLEAR }

    private static LiteralArgumentBuilder<CommandSourceStack> permLiteral(String name, Op op) {
        return Commands.literal(name).then(Commands.argument("player", EntityArgument.player())
                .then(Commands.argument("node", StringArgumentType.greedyString()).executes(StaffAudit.audited("cmd.perm." + op.name().toLowerCase(java.util.Locale.ROOT),
                        c -> perm(c, op)))));
    }

    // ---- giocatore -----------------------------------------------------------------------------------------

    private static int info(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        Optional<PlayerCharacter> pc = QrmCommands.active(p);
        if (pc.isEmpty()) return QrmCommands.fail(c, "Nessun personaggio attivo.");
        Optional<Assignment> a = QRM.get(JobService.class).assignmentOf(pc.get().id());
        if (a.isEmpty()) return QrmCommands.ok(c, "Disoccupato.");
        JobRegistry registry = QRM.get(JobRegistry.class);
        Optional<JobDefinition> job = registry.find(a.get().jobId());
        String jobLabel = job.map(JobDefinition::label).orElse(a.get().jobId());
        String gradeLabel = job.flatMap(j -> j.grade(a.get().gradeId())).map(GradeDefinition::label)
                .orElse(a.get().gradeId());
        QrmCommands.ok(c, "Lavoro: " + jobLabel + " - " + gradeLabel
                + (a.get().known() ? "" : "  [definizione mancante]")
                + "  (" + (a.get().onDuty() ? "in servizio" : "fuori servizio") + ")");
        String perms = QRM.get(PermissionService.class).effective(pc.get().id()).stream().collect(Collectors.joining(", "));
        return QrmCommands.ok(c, "Permessi: " + (perms.isEmpty() ? "nessuno" : perms));
    }

    private static int duty(CommandContext<CommandSourceStack> c, boolean on) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        DutyActions.Result r = DutyActions.setDuty(QRM.get(CharacterService.class), QRM.get(JobService.class),
                new PlayerId(p.getUUID()), on, QrmConfig.uiConfig().selfDuty());
        return dutyResult(c, r, on);
    }

    /** {@code /duty} senza argomenti: inverte il servizio. */
    private static int toggleDuty(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer p = c.getSource().getPlayerOrException();
        DutyActions.Toggle t = DutyActions.toggle(QRM.get(CharacterService.class), QRM.get(JobService.class),
                new PlayerId(p.getUUID()), QrmConfig.uiConfig().selfDuty());
        return dutyResult(c, t.result(), t.on());
    }

    private static int dutyResult(CommandContext<CommandSourceStack> c, DutyActions.Result r, boolean on) {
        return switch (r) {
            case OK -> QrmCommands.ok(c, on ? "Sei in servizio." : "Sei fuori servizio.");
            case DISABLED -> QrmCommands.fail(c, "Il servizio si gestisce in azienda.");
            case NO_CHARACTER -> QrmCommands.fail(c, "Nessun personaggio attivo.");
            case NO_JOB -> QrmCommands.fail(c, "Non hai un lavoro.");
            case ALREADY -> QrmCommands.fail(c, on ? "Sei già in servizio." : "Sei già fuori servizio.");
            case UNKNOWN_JOB -> QrmCommands.fail(c, "Il tuo lavoro non è più definito: contatta un amministratore.");
        };
    }

    // ---- amministratori ------------------------------------------------------------------------------------

    private static int list(CommandContext<CommandSourceStack> c) {
        List<JobDefinition> jobs = QRM.get(JobRegistry.class).all().stream()
                .sorted(java.util.Comparator.comparing(JobDefinition::id)).toList();
        if (jobs.isEmpty()) return QrmCommands.ok(c, "Nessun lavoro definito.");
        for (JobDefinition j : jobs) {
            String grades = j.grades().stream().map(g -> g.id() + "[" + g.level() + "]").collect(Collectors.joining(", "));
            QrmCommands.ok(c, j.id() + " (" + j.label() + "): " + grades);
        }
        return jobs.size();
    }

    private static Optional<CharacterId> targetCharacter(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        ServerPlayer target = EntityArgument.getPlayer(c, "player");
        return QrmCommands.active(target).map(PlayerCharacter::id);
    }

    private static int set(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Optional<CharacterId> id = targetCharacter(c);
        if (id.isEmpty()) return QrmCommands.fail(c, "Il giocatore non ha un personaggio attivo.");
        AssignResult r = QRM.get(JobService.class).assign(id.get(), StringArgumentType.getString(c, "job"),
                StringArgumentType.getString(c, "grade"));
        return switch (r) {
            case AssignResult.Assigned assigned -> QrmCommands.ok(c,
                    "Assegnato: " + assigned.assignment().jobId() + "/" + assigned.assignment().gradeId());
            case AssignResult.UnknownCharacter unknown -> QrmCommands.fail(c, "Personaggio sconosciuto.");
            case AssignResult.NotUsable notUsable -> QrmCommands.fail(c, "Personaggio non utilizzabile: " + notUsable.status());
            case AssignResult.UnknownJob unknownJob -> QrmCommands.fail(c, "Lavoro sconosciuto: " + unknownJob.jobId());
            case AssignResult.UnknownGrade unknownGrade -> QrmCommands.fail(c, "Grado sconosciuto: " + unknownGrade.gradeId());
            case AssignResult.NoAssignment none -> QrmCommands.fail(c, "Il personaggio non ha un lavoro.");
            case AssignResult.Cancelled cancelled -> QrmCommands.fail(c,
                    "Operazione annullata da " + cancelled.by() + ": " + cancelled.reason());
        };
    }

    private static int fire(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Optional<CharacterId> id = targetCharacter(c);
        if (id.isEmpty()) return QrmCommands.fail(c, "Il giocatore non ha un personaggio attivo.");
        return QRM.get(JobService.class).dismiss(id.get())
                ? QrmCommands.ok(c, "Licenziato.")
                : QrmCommands.fail(c, "Nessun lavoro da rimuovere, oppure operazione annullata.");
    }

    private static int perm(CommandContext<CommandSourceStack> c, Op op) throws CommandSyntaxException {
        Optional<CharacterId> id = targetCharacter(c);
        if (id.isEmpty()) return QrmCommands.fail(c, "Il giocatore non ha un personaggio attivo.");
        String node = StringArgumentType.getString(c, "node").strip();
        PermissionService perms = QRM.get(PermissionService.class);
        try {
            switch (op) {
                case GRANT -> perms.grant(id.get(), node);
                case DENY -> perms.deny(id.get(), node);
                case CLEAR -> perms.clear(id.get(), node);
            }
        } catch (IllegalArgumentException e) {
            return QrmCommands.fail(c, "Nodo non valido: " + node);
        }
        return QrmCommands.ok(c, "Fatto: " + op.name().toLowerCase() + " " + node);
    }
}
