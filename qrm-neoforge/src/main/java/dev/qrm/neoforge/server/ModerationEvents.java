package dev.qrm.neoforge.server;

import dev.qrm.api.QRM;
import dev.qrm.api.moderation.ModerationService;
import dev.qrm.api.moderation.Mute;
import dev.qrm.api.moderation.MuteChangedEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.event.CommandEvent;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Applica i mute: scarta la chat e i comandi di messaggistica di un giocatore silenziato e lo avvisa. */
public final class ModerationEvents {
    private static final Logger LOG = LoggerFactory.getLogger("qrm");

    /** I mute dei giocatori online. */
    public static final MuteCache CACHE = new MuteCache();

    private ModerationEvents() {}

    public static void onLogin(ServerPlayer player) {
        try {
            QRM.get(ModerationService.class).activeMute(player.getUUID()).ifPresent(CACHE::put);
        } catch (RuntimeException e) {
            LOG.error("Could not load the mute of {}", player.getUUID(), e);
        }
    }

    /** Il mute di un giocatore è cambiato (da qualsiasi parte): riallinea la cache se è online. */
    public static void onMuteChanged(MuteChangedEvent event) {
        try {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            boolean online = server != null && server.getPlayerList().getPlayer(event.player()) != null;
            CACHE.refresh(event.player(), QRM.get(ModerationService.class).activeMute(event.player()), online);
        } catch (RuntimeException e) {
            LOG.error("Could not refresh the mute of {}", event.player(), e);
        }
    }

    public static void onLogout(ServerPlayer player) {
        CACHE.remove(player.getUUID());
    }

    public static void onChat(ServerChatEvent event) {
        ServerPlayer player = event.getPlayer();
        Optional<Mute> mute = CACHE.active(player.getUUID(), Instant.now());
        if (mute.isEmpty()) return;
        event.setCanceled(true);
        player.sendSystemMessage(Component.literal(MuteNotice.blocked(mute.get(), ZoneId.systemDefault())));
    }

    public static void onCommand(CommandEvent event) {
        ServerPlayer player = event.getParseResults().getContext().getSource().getPlayer();
        if (player == null || CACHE.isEmpty()) return;
        if (!MutedCommands.isBlocked(event.getParseResults().getReader().getString())) return;
        Optional<Mute> mute = CACHE.active(player.getUUID(), Instant.now());
        if (mute.isEmpty()) return;
        event.setCanceled(true);
        player.sendSystemMessage(Component.literal(MuteNotice.blocked(mute.get(), ZoneId.systemDefault())));
    }
}
