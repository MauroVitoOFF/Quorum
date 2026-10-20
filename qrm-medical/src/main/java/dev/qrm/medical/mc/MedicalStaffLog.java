package dev.qrm.medical.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.staff.StaffLogEntry;
import dev.qrm.api.staff.StaffLogService;
import java.util.Optional;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Scrittura nel registro delle azioni staff per i comandi medici. Una scrittura che fallisce non blocca l'azione. */
final class MedicalStaffLog {
    private static final Logger LOG = LoggerFactory.getLogger("qrm_medical");

    private MedicalStaffLog() {}

    /** Azione da comando {@code /qrm medical ...} (un giocatore o la console) sul giocatore {@code target}. */
    static void command(CommandSourceStack source, String action, ServerPlayer target) {
        ServerPlayer staff = source.getPlayer();
        String staffName = staff != null ? staff.getGameProfile().name() : source.getTextName();
        try {
            QRM.get(StaffLogService.class).record(Optional.ofNullable(staff).map(ServerPlayer::getUUID), staffName,
                    "cmd.medical." + action, Optional.of(target.getUUID()), Optional.of(target.getGameProfile().name()),
                    "", StaffLogEntry.Outcome.OK);
        } catch (RuntimeException e) {
            LOG.error("Could not write the staff log entry '{}' for {}", action, staffName, e);
        }
    }
}
