package dev.qrm.neoforge.server;

import dev.qrm.api.QRM;
import dev.qrm.api.moderation.ModerationService;
import dev.qrm.api.staff.StaffLogService;
import dev.qrm.neoforge.net.PageKind;
import dev.qrm.neoforge.net.PagePayload;
import dev.qrm.neoforge.net.PageRequestPayload;
import dev.qrm.neoforge.net.PlayerInfoLine;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lato server delle pagine di righe del pannello (registro azioni, mute attivi, ultimi avvertimenti): il nodo del tipo di pagina
 * è ricontrollato a ogni richiesta, con limite di frequenza.
 */
public final class PageServer {
    /** Righe di una pagina del pannello. */
    public static final int PAGE = 60;
    private static final Logger LOG = LoggerFactory.getLogger("qrm");
    private static final RateLimiter LIMITER = new RateLimiter(250, Clock.systemUTC());

    private PageServer() {}

    public static void onPlayerLeave(ServerPlayer player) {
        LIMITER.forget(player.getUUID());
    }

    public static void onRequest(PageRequestPayload payload, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        PageKind kind = payload.kind();
        if (!StaffAccess.has(player, kind.node())) return;
        if (!LIMITER.tryAcquire(player.getUUID())) return;
        try {
            PacketDistributor.sendToPlayer(player, new PagePayload(kind.titleKey(), lines(kind)));
        } catch (RuntimeException e) {
            LOG.error("Staff page {} failed for {}", kind, player.getUUID(), e);
        }
    }

    /** Le righe della pagina; pubblico per i test di integrazione e per i comandi. */
    static List<PlayerInfoLine> lines(PageKind kind) {
        ZoneId zone = ZoneId.systemDefault();
        return switch (kind) {
            case LOG -> LogLines.build(QRM.get(StaffLogService.class).recent(PAGE), zone);
            case MUTES -> ModerationLines.mutes(QRM.get(ModerationService.class).activeMutes(PAGE), zone);
            case WARNINGS -> ModerationLines.recentWarnings(QRM.get(ModerationService.class).recentWarnings(PAGE), zone);
        };
    }
}
