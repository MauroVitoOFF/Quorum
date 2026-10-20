package dev.qrm.police.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.staff.StaffLogEntry;
import dev.qrm.api.staff.StaffLogService;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Scrittura nel registro delle azioni staff per i comandi MDT. Una scrittura che fallisce non blocca l'azione. */
final class PoliceStaffLog {
    private static final Logger LOG = LoggerFactory.getLogger("qrm_police");

    private PoliceStaffLog() {}

    /** Azione da comando {@code /qrm mdt ...} (un giocatore, la console o il giocatore di una partita singola). */
    static void command(CommandSourceStack source, String action) {
        ServerPlayer staff = source.getPlayer();
        String staffName = staff != null ? staff.getGameProfile().name() : source.getTextName();
        try {
            QRM.get(StaffLogService.class).record(Optional.ofNullable(staff).map(ServerPlayer::getUUID), staffName,
                    "cmd.mdt." + action, Optional.<UUID>empty(), Optional.empty(), "", StaffLogEntry.Outcome.OK);
        } catch (RuntimeException e) {
            LOG.error("Could not write the staff log entry '{}' for {}", action, staffName, e);
        }
    }
}
