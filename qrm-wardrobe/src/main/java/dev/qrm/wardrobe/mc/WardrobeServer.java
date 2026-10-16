package dev.qrm.wardrobe.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.data.ModDataService;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.job.Assignment;
import dev.qrm.api.job.JobRegistry;
import dev.qrm.api.job.JobService;
import dev.qrm.api.point.PointUse;
import dev.qrm.wardrobe.logic.ChooseResult;
import dev.qrm.wardrobe.logic.JobLookup;
import dev.qrm.wardrobe.logic.ModDataWardrobeStorage;
import dev.qrm.wardrobe.logic.OutfitCatalog;
import dev.qrm.wardrobe.logic.OutfitDefinition;
import dev.qrm.wardrobe.logic.WardrobeRules;
import dev.qrm.wardrobe.logic.WardrobeService;
import dev.qrm.wardrobe.logic.WardrobeWire;
import dev.qrm.wardrobe.logic.Wearer;
import dev.qrm.wardrobe.net.WardrobePayloads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Stato del Module sul server: il servizio delle divise e i collegamenti con lavoro, servizio e Character. */
public final class WardrobeServer {
    /** Chi deve essere avvisato quando cambia qualcosa. Lo imposta il livello di rete. */
    public interface Sync {
        void catalogChanged();

        void wornChanged();

        void playerJoined(ServerPlayer player);
    }

    private static final Sync NO_SYNC = new Sync() {
        @Override
        public void catalogChanged() {}

        @Override
        public void wornChanged() {}

        @Override
        public void playerJoined(ServerPlayer player) {}
    };

    private static WardrobeService service;
    private static Sync sync = NO_SYNC;

    private WardrobeServer() {}

    public static void setSync(Sync s) {
        sync = s;
    }

    static JobLookup jobs() {
        JobRegistry registry = QRM.get(JobRegistry.class);
        return new JobLookup() {
            @Override
            public boolean hasJob(String job) {
                return registry.find(job).isPresent();
            }

            @Override
            public OptionalInt gradeLevel(String job, String grade) {
                return registry.find(job).flatMap(j -> j.grade(grade))
                        .map(g -> OptionalInt.of(g.level())).orElse(OptionalInt.empty());
            }
        };
    }

    public static synchronized WardrobeService service() {
        if (service == null) {
            service = new WardrobeService(new ModDataWardrobeStorage(QRM.get(ModDataService.class)), jobs());
            service.setCatalog(loadCatalog());
        }
        return service;
    }

    /** Dimentica lo stato del mondo corrente. */
    public static synchronized void reset() {
        service = null;
    }

    private static OutfitCatalog loadCatalog() {
        List<String> problems = new ArrayList<>();
        OutfitCatalog catalog = OutfitCatalog.load(WardrobeFiles.outfitsDir(), jobs(), problems::add);
        for (String p : problems) WardrobeMod.LOG.warn("Divisa ignorata: {}", p);
        WardrobeMod.LOG.info("{} divise caricate, {} problemi", catalog.all().size(), problems.size());
        return catalog;
    }

    /** Rilegge la cartella, toglie le divise non più valide e avvisa i client. Restituisce quante divise ha letto. */
    public static int reload() {
        WardrobeService s = service();
        OutfitCatalog catalog = loadCatalog();
        s.setCatalog(catalog);
        for (CharacterId c : s.allWorn().keySet()) s.revalidate(c, wearerOf(c));
        sync.catalogChanged();
        sync.wornChanged();
        return catalog.all().size();
    }

    public static Optional<Wearer> wearerOf(CharacterId character) {
        Optional<Assignment> a = QRM.get(JobService.class).assignmentOf(character);
        return a.map(x -> new Wearer(x.jobId(), x.gradeId(), x.onDuty()));
    }

    /** Ricontrolla la divisa di un Character e avvisa i client se è cambiato qualcosa. */
    public static void revalidate(CharacterId character) {
        if (service().revalidate(character, wearerOf(character))) sync.wornChanged();
    }

    /** Giocatore → divisa della sua Character attiva, per chi è online. */
    public static Map<UUID, String> wornByOnlinePlayer() {
        Map<UUID, String> out = new HashMap<>();
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return out;
        CharacterService characters = QRM.get(CharacterService.class);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            characters.activeOf(new PlayerId(p.getUUID())).map(PlayerCharacter::id)
                    .flatMap(service()::worn).ifPresent(id -> out.put(p.getUUID(), id));
        }
        return out;
    }

    public static void onPlayerJoined(PlayerId id) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer p = server.getPlayerList().getPlayer(id.value());
        if (p != null) sync.playerJoined(p);
    }

    public static void onWornMayHaveChanged() {
        sync.wornChanged();
    }

    /** Armadietto aperto da un giocatore: quale punto, di quale lavoro e per quale Character. */
    private record Session(ResourceKey<Level> dimension, BlockPos pos, int pointId, String job, CharacterId character) {}

    private static final double MAX_DISTANCE = 8.0;
    private static final Map<UUID, Session> SESSIONS = new HashMap<>();

    /** Il clic sul punto: manda al client la lista delle divise che il grado permette. */
    static void onUse(PointUse use) {
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        ServerPlayer player = server.getPlayerList().getPlayer(use.player().value());
        if (player == null) return;
        if (!player.connection.hasChannel(WardrobePayloads.Open.TYPE)) {
            player.sendSystemMessage(Component.translatable("qrm_wardrobe.need_mod"));
            return;
        }
        var pos = use.point().position();
        Session session = new Session(player.level().dimension(), new BlockPos(pos.x(), pos.y(), pos.z()),
                use.point().id(), use.point().owner().id(), use.character());
        synchronized (SESSIONS) {
            SESSIONS.put(player.getUUID(), session);
        }
        sendOpen(player, session);
    }

    private static void sendOpen(ServerPlayer player, Session session) {
        WardrobeService s = service();
        List<String> ids = wearerOf(session.character()).map(w -> WardrobeRules
                .available(s.catalog(), session.job(), w, jobs()).stream().map(OutfitDefinition::id).toList())
                .orElse(List.of());
        PacketDistributor.sendToPlayer(player, new WardrobePayloads.Open(WardrobeWire.encode(
                new WardrobeWire.Open(ids, s.worn(session.character()).orElse("")))));
    }

    /** La scelta del client: si ricontrolla tutto, il client non decide niente. */
    public static void onChoose(Player p, byte[] data) {
        if (!(p instanceof ServerPlayer player)) return;
        String outfitId;
        try {
            outfitId = WardrobeWire.decodeChoose(data);
        } catch (IllegalArgumentException e) {
            return;
        }
        Session session;
        synchronized (SESSIONS) {
            session = SESSIONS.get(player.getUUID());
        }
        if (session == null || !player.level().dimension().equals(session.dimension())
                || player.distanceToSqr(Vec3.atCenterOf(session.pos())) > MAX_DISTANCE * MAX_DISTANCE) {
            forget(player.getUUID());
            return;
        }
        Optional<CharacterId> active = QRM.get(CharacterService.class).activeOf(new PlayerId(player.getUUID()))
                .map(PlayerCharacter::id);
        if (active.isEmpty() || !active.get().equals(session.character())) {
            forget(player.getUUID());
            return;
        }
        ChooseResult result = service().choose(session.character(), outfitId, session.job(), wearerOf(session.character()));
        if (result == ChooseResult.OK) sync.wornChanged();
        sendOpen(player, session);
    }

    public static void forget(UUID player) {
        synchronized (SESSIONS) {
            SESSIONS.remove(player);
        }
    }
}
