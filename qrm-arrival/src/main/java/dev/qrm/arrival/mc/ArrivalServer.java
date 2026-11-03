package dev.qrm.arrival.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.data.ModDataService;
import dev.qrm.arrival.logic.AlleyScenario;
import dev.qrm.arrival.logic.ArrivalFiles;
import dev.qrm.arrival.logic.ArrivalFlow;
import dev.qrm.arrival.logic.ArrivalPoint;
import dev.qrm.arrival.logic.ArrivalPolicy;
import dev.qrm.arrival.logic.ArrivalSession.Phase;
import dev.qrm.arrival.logic.ModDataArrivalStorage;
import dev.qrm.arrival.logic.ScenarioPicker;
import java.time.Clock;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Stato del Module sul server: file, flusso dell'arrivo e collegamento con i giocatori online. */
public final class ArrivalServer {
    /** Chi deve essere avvisato quando cambia la fase del risveglio di un giocatore. Lo imposta il livello di rete. */
    public interface Sync {
        void phase(ServerPlayer player, Phase phase, int elapsed);
    }

    private static final Sync NO_SYNC = (player, phase, elapsed) -> {};
    /** Quanto può allontanarsi dal punto un giocatore bloccato prima di essere riportato indietro. */
    private static final double MAX_DRIFT = 0.5;

    private static ArrivalFiles files;
    private static ArrivalFlow flow;
    private static Sync sync = NO_SYNC;
    private static final Map<UUID, Vec3> anchors = new HashMap<>();

    private ArrivalServer() {}

    static void setSync(Sync s) {
        sync = s;
    }

    static synchronized ArrivalFiles files() {
        if (files == null) {
            files = new ArrivalFiles(FMLPaths.CONFIGDIR.get().resolve(ArrivalMod.MOD_ID),
                    msg -> ArrivalMod.LOG.warn("Arrivo: {}", msg));
        }
        return files;
    }

    private static synchronized ArrivalFlow flow() {
        if (flow == null) {
            ModDataArrivalStorage storage = new ModDataArrivalStorage(QRM.get(ModDataService.class), Clock.systemUTC());
            ScenarioPicker picker = new ScenarioPicker(List.of(new AlleyScenario(() -> files().spawn())), new Random());
            flow = new ArrivalFlow(new ArrivalPolicy(storage), storage, picker, files().timing(), new McHooks());
        }
        return flow;
    }

    /** Rilegge i file e riparte da zero: gli arrivi in corso si interrompono. */
    static synchronized int reload() {
        files().reload();
        if (flow != null) flow.reset();
        flow = null;
        anchors.clear();
        return files().kit().entries().size();
    }

    static synchronized void reset() {
        files = null;
        flow = null;
        anchors.clear();
    }

    public static void onSelected(PlayerCharacter character) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        UUID player = character.playerId().value();
        if (server.getPlayerList().getPlayer(player) == null) return;
        if (!flow().onSelected(player, character) && files().spawn().isEmpty()) {
            ArrivalMod.LOG.info("Arrivo saltato per {}: nessun punto di arrivo impostato (/qrm arrival setspawn)",
                    character.firstName());
        }
    }

    static void onTick(ServerPlayer player) {
        UUID id = player.getUUID();
        if (!flow().locked(id)) return;
        pin(player);
        flow().tick(id);
    }

    static boolean locked(net.minecraft.world.entity.player.Player player) {
        return !player.level().isClientSide() && flow().locked(player.getUUID());
    }

    /** Il giocatore esce, muore o cambia Character. */
    static void forget(UUID player) {
        if (flow != null) flow.forget(player);
        anchors.remove(player);
    }

    private static void pin(ServerPlayer player) {
        Vec3 anchor = anchors.get(player.getUUID());
        if (anchor == null) return;
        player.setDeltaMovement(Vec3.ZERO);
        if (player.position().distanceToSqr(anchor) > MAX_DRIFT * MAX_DRIFT) {
            player.connection.teleport(anchor.x, anchor.y, anchor.z, player.getYRot(), player.getXRot());
        }
    }

    private static final class McHooks implements ArrivalFlow.Hooks {
        @Override
        public boolean teleport(UUID id, ArrivalPoint point) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            if (server == null) return false;
            ServerPlayer player = server.getPlayerList().getPlayer(id);
            Identifier dimension = Identifier.tryParse(point.dimension());
            if (player == null || dimension == null) return false;
            ServerLevel level = server.getLevel(ResourceKey.create(Registries.DIMENSION, dimension));
            if (level == null) {
                ArrivalMod.LOG.warn("Arrivo: la dimensione {} non esiste, il giocatore entra normalmente", point.dimension());
                return false;
            }
            if (!player.teleportTo(level, point.x(), point.y(), point.z(), Set.of(), point.yaw(), 0.0F, true)) {
                ArrivalMod.LOG.warn("Arrivo: teletrasporto fallito verso {}", point);
                return false;
            }
            anchors.put(id, new Vec3(point.x(), point.y(), point.z()));
            return true;
        }

        @Override
        public void giveKit(UUID id) {
            ServerPlayer player = onlinePlayer(id);
            if (player != null) KitGiver.give(player, files().kit());
        }

        @Override
        public void phase(UUID id, Phase phase, int elapsed) {
            if (phase == Phase.DONE || phase == Phase.ABORTED) anchors.remove(id);
            ServerPlayer player = onlinePlayer(id);
            if (player != null) sync.phase(player, phase, elapsed);
        }

        private static ServerPlayer onlinePlayer(UUID id) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            return server == null ? null : server.getPlayerList().getPlayer(id);
        }
    }
}
