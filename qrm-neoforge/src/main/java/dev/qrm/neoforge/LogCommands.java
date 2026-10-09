package dev.qrm.neoforge;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.qrm.api.QRM;
import dev.qrm.api.staff.StaffLogEntry;
import dev.qrm.api.staff.StaffLogService;
import dev.qrm.neoforge.net.PlayerInfoLine;
import dev.qrm.neoforge.server.LogLines;
import dev.qrm.neoforge.server.StaffAudit;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /qrm admin log [giocatore] [n]} mostra le ultime azioni del registro staff (di tutti o riguardanti un giocatore) e
 * {@code /qrm admin log prune <giorni>} cancella le voci più vecchie. Solo operatori (il ramo {@code admin}).
 */
final class LogCommands {
    private static final int DEFAULT_COUNT = 20;

    private LogCommands() {}

    static LiteralArgumentBuilder<CommandSourceStack> adminLog() {
        return Commands.literal("log")
                .executes(c -> show(c, Optional.empty(), DEFAULT_COUNT))
                .then(Commands.literal("prune")
                        .then(Commands.argument("days", IntegerArgumentType.integer(1, 36500)).executes(LogCommands::prune)))
                .then(Commands.argument("player", EntityArgument.player())
                        .executes(c -> show(c, Optional.of(EntityArgument.getPlayer(c, "player")), DEFAULT_COUNT))
                        .then(Commands.argument("n", IntegerArgumentType.integer(1, 200)).executes(
                                c -> show(c, Optional.of(EntityArgument.getPlayer(c, "player")),
                                        IntegerArgumentType.getInteger(c, "n")))));
    }

    private static int show(CommandContext<CommandSourceStack> c, Optional<ServerPlayer> player, int n)
            throws CommandSyntaxException {
        StaffLogService log = QRM.get(StaffLogService.class);
        List<StaffLogEntry> entries = player.map(p -> log.about(p.getUUID(), n)).orElseGet(() -> log.recent(n));
        List<PlayerInfoLine> lines = LogLines.build(entries, ZoneId.systemDefault());
        if (entries.isEmpty()) return QrmCommands.ok(c, "Nessuna azione registrata.");
        for (PlayerInfoLine l : lines) {
            QrmCommands.ok(c, l.labelKey().substring(1) + "  " + l.value());
        }
        return lines.size();
    }

    private static int prune(CommandContext<CommandSourceStack> c) {
        int days = IntegerArgumentType.getInteger(c, "days");
        int removed = QRM.get(StaffLogService.class).pruneOlderThan(Instant.now().minus(Duration.ofDays(days)));
        ServerPlayer staff = c.getSource().getPlayer();
        StaffAudit.record(Optional.ofNullable(staff).map(ServerPlayer::getUUID),
                staff != null ? staff.getGameProfile().name() : c.getSource().getTextName(), "cmd.log.prune",
                Optional.<UUID>empty(), Optional.empty(), "più vecchie di " + days + " giorni: " + removed + " voci",
                StaffLogEntry.Outcome.OK);
        return QrmCommands.ok(c, "Cancellate " + removed + " voci più vecchie di " + days + " giorni.");
    }
}
