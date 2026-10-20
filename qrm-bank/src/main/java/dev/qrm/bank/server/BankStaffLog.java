package dev.qrm.bank.server;

import dev.qrm.api.QRM;
import dev.qrm.api.staff.StaffLogEntry;
import dev.qrm.api.staff.StaffLogService;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Scrittura nel registro delle azioni staff per la parte della banca. Una scrittura che fallisce non blocca l'azione. */
final class BankStaffLog {
    private static final Logger LOG = LoggerFactory.getLogger("qrm_bank");

    private BankStaffLog() {}

    /** Azione dalla schermata staff della banca (un giocatore). */
    static void screen(ServerPlayer staff, String action, Optional<UUID> targetId, Optional<String> targetName,
            String detail, StaffLogEntry.Outcome outcome) {
        write(Optional.of(staff.getUUID()), staff.getGameProfile().name(), "bank." + action, targetId, targetName, detail,
                outcome);
    }

    /** Azione da comando {@code /qrm bank ...} (un giocatore o la console). */
    static void command(CommandSourceStack source, String action, Optional<UUID> targetId, Optional<String> targetName,
            String detail) {
        ServerPlayer staff = source.getPlayer();
        write(Optional.ofNullable(staff).map(ServerPlayer::getUUID),
                staff != null ? staff.getGameProfile().name() : source.getTextName(), "cmd.bank." + action, targetId,
                targetName, detail, StaffLogEntry.Outcome.OK);
    }

    private static void write(Optional<UUID> staffId, String staffName, String action, Optional<UUID> targetId,
            Optional<String> targetName, String detail, StaffLogEntry.Outcome outcome) {
        try {
            QRM.get(StaffLogService.class).record(staffId, staffName, action, targetId, targetName, detail, outcome);
        } catch (RuntimeException e) {
            LOG.error("Could not write the staff log entry '{}' for {}", action, staffName, e);
        }
    }
}
