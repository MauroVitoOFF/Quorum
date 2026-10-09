package dev.qrm.neoforge.server;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.qrm.api.QRM;
import dev.qrm.api.staff.StaffLogEntry;
import dev.qrm.api.staff.StaffLogService;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scrittura nel registro delle azioni staff e avvolgimento dei comandi admin. Una scrittura che fallisce non blocca mai
 * l'azione dello staff: l'errore va nel log del server.
 */
public final class StaffAudit {
    private static final Logger LOG = LoggerFactory.getLogger("qrm");

    private StaffAudit() {}

    /** Il corpo di un comando: restituisce il risultato Brigadier ({@code > 0} = riuscito). */
    @FunctionalInterface
    public interface Body {
        int run() throws CommandSyntaxException;
    }

    public static void record(Optional<UUID> staffId, String staffName, String action, Optional<UUID> targetId,
            Optional<String> targetName, String detail, StaffLogEntry.Outcome outcome) {
        try {
            QRM.get(StaffLogService.class).record(staffId, staffName, action, targetId, targetName, detail, outcome);
        } catch (RuntimeException e) {
            LOG.error("Could not write the staff log entry '{}' for {}", action, staffName, e);
        }
    }

    /** Un'azione di un giocatore staff, con un bersaglio facoltativo (anche non più online). */
    public static void recordPlayer(ServerPlayer staff, String action, Optional<ServerPlayer> target, UUID targetId,
            String detail, StaffLogEntry.Outcome outcome) {
        record(Optional.of(staff.getUUID()), staff.getGameProfile().name(), action,
                Optional.ofNullable(target.map(ServerPlayer::getUUID).orElse(targetId)),
                target.map(p -> p.getGameProfile().name()), detail, outcome);
    }

    /** L'azione di un comando eseguito da un giocatore o dalla console: il bersaglio è l'argomento {@code player}, se c'è. */
    public static void recordCommand(CommandContext<CommandSourceStack> c, String action) {
        CommandSourceStack source = c.getSource();
        ServerPlayer staff = source.getPlayer();
        ServerPlayer target = null;
        try {
            target = EntityArgument.getPlayer(c, "player");
        } catch (IllegalArgumentException | CommandSyntaxException ignored) {
            // il comando non ha un giocatore come argomento
        }
        record(Optional.ofNullable(staff).map(ServerPlayer::getUUID),
                staff != null ? staff.getGameProfile().name() : source.getTextName(), action,
                Optional.ofNullable(target).map(ServerPlayer::getUUID),
                Optional.ofNullable(target).map(p -> p.getGameProfile().name()),
                StaffAuditText.detailOf(c.getInput()), StaffLogEntry.Outcome.OK);
    }

    /** Esegue il corpo e, se riesce, registra l'azione del comando. */
    public static int run(CommandContext<CommandSourceStack> c, String action, Body body) throws CommandSyntaxException {
        int result = body.run();
        if (result > 0) recordCommand(c, action);
        return result;
    }

    /** Avvolge un esecutore: se riesce, registra l'azione. */
    public static Command<CommandSourceStack> audited(String action, Command<CommandSourceStack> delegate) {
        return c -> run(c, action, () -> delegate.run(c));
    }
}
