package dev.qrm.neoforge.server;

import dev.qrm.api.QRM;
import dev.qrm.api.staff.StaffAccessService;
import dev.qrm.neoforge.net.StaffAccessPayload;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.Permissions;
import net.neoforged.neoforge.network.PacketDistributor;

/** Permessi del pannello staff: l'operatore (GAMEMASTER) ha ogni nodo, gli altri solo i grant per account. */
public final class StaffAccess {
    private StaffAccess() {}

    public static boolean isOp(ServerPlayer player) {
        return player.createCommandSourceStack().permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER);
    }

    public static boolean has(ServerPlayer player, String node) {
        return isOp(player) || QRM.get(StaffAccessService.class).has(player.getUUID(), node);
    }

    /**
     * Chi non è operatore non può agire su un operatore: un grant come {@code staff.player.ban} non deve bastare a
     * bannare, espellere o silenziare l'amministratore.
     */
    public static boolean mayTarget(boolean staffIsOp, boolean targetIsOp) {
        return staffIsOp || !targetIsOp;
    }

    /** Per i comandi: la console (senza giocatore) conta solo se è operatore. */
    public static boolean allows(CommandSourceStack source, String node) {
        if (source.permissions().hasPermission(Permissions.COMMANDS_GAMEMASTER)) return true;
        ServerPlayer player = source.getPlayer();
        return player != null && QRM.get(StaffAccessService.class).has(player.getUUID(), node);
    }

    /** Pura: cosa dire al client. I pattern in eccesso rispetto al limite del codec vengono troncati. */
    public static StaffAccessPayload payloadFor(boolean op, Set<String> grants) {
        if (op) return new StaffAccessPayload(true, List.of());
        List<String> sorted = new TreeSet<>(grants).stream().limit(StaffAccessPayload.MAX_PATTERNS).toList();
        return new StaffAccessPayload(false, sorted);
    }

    /** Invia al giocatore i suoi permessi staff (se il client ha il canale QRM). */
    public static void sync(ServerPlayer player) {
        if (!player.connection.hasChannel(StaffAccessPayload.TYPE)) return;
        Set<String> grants = QRM.get(StaffAccessService.class).grantsOf(player.getUUID());
        PacketDistributor.sendToPlayer(player, payloadFor(isOp(player), grants));
    }
}
