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
import dev.qrm.wardrobe.logic.JobLookup;
import dev.qrm.wardrobe.logic.ModDataWardrobeStorage;
import dev.qrm.wardrobe.logic.OutfitCatalog;
import dev.qrm.wardrobe.logic.WardrobeService;
import dev.qrm.wardrobe.logic.Wearer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
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

    /** Il clic sul punto: aprirà la schermata (Task 6). */
    static void onUse(PointUse use) {
        WardrobeMod.LOG.info("Armadietto aperto da {}", use.player().value());
    }
}
