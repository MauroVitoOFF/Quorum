package dev.qrm.immersion.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.data.ModDataService;
import dev.qrm.api.economy.OwnerRef;
import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.immersion.logic.BodyPose;
import dev.qrm.immersion.logic.ImmersionRules;
import dev.qrm.immersion.logic.KnownCitizens;
import dev.qrm.immersion.net.ImmersionPayloads;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/** Stato server del modulo: pose, conoscenze, richieste di presentazione e bussate. */
public final class ImmersionServer {
    static final String NAMESPACE = "qrm_immersion";
    static final String KNOWN_KEY = "known";
    private static final int POSE_CHECK_EVERY = 10;

    /** Le conoscenze caricate per un giocatore e per quale Character (nullo se non ne ha una attiva). */
    private static final class Held {
        final CharacterId character;
        final KnownCitizens known;

        Held(CharacterId character, KnownCitizens known) {
            this.character = character;
            this.known = known;
        }
    }

    private record Pending(UUID from, int expiresAt) {}

    private record Delayed(int dueTick, ServerLevel level, BlockPos pos, SoundEvent sound, float volume, float pitch) {}

    private static final Map<UUID, BodyPose> POSES = new HashMap<>();
    private static final Map<UUID, Held> KNOWN = new HashMap<>();
    /** Destinatario -> chi gli ha teso la mano. */
    private static final Map<UUID, Pending> PENDING = new HashMap<>();
    private static final Map<UUID, Integer> LAST_KNOCK = new HashMap<>();
    private static final List<Delayed> DELAYED = new ArrayList<>();

    private ImmersionServer() {}

    static synchronized void reset() {
        POSES.clear();
        KNOWN.clear();
        PENDING.clear();
        LAST_KNOCK.clear();
        DELAYED.clear();
    }

    // ---- pose ----

    public static BodyPose poseOf(Entity e) {
        return POSES.getOrDefault(e.getUUID(), BodyPose.NONE);
    }

    /** Vero se il giocatore ha le mani alzate: non puo' attaccare, usare oggetti ne' costruire. */
    public static boolean handsUp(Player p) {
        return poseOf(p) == BodyPose.HANDS_UP;
    }

    /** Imposta la posa di un giocatore o di un manichino e la manda a chi lo vede (e a lui stesso). */
    public static void setPose(Entity e, BodyPose pose) {
        if (pose == BodyPose.NONE) {
            if (POSES.remove(e.getUUID()) == null) return;
        } else if (POSES.put(e.getUUID(), pose) == pose) {
            return;
        }
        PacketDistributor.sendToPlayersTrackingEntityAndSelf(e, new ImmersionPayloads.PoseUpdate(e.getId(), pose.ordinal()));
    }

    public static void onPoseRequest(ServerPlayer p, int kind) {
        if (p.isSpectator() || !p.isAlive() || MedicalLink.unfit(p)) {
            setPose(p, BodyPose.NONE);
            return;
        }
        BodyPose current = poseOf(p);
        switch (kind) {
            case ImmersionPayloads.REQUEST_TOGGLE_HANDS_UP ->
                    setPose(p, current == BodyPose.HANDS_UP ? BodyPose.NONE : BodyPose.HANDS_UP);
            case ImmersionPayloads.REQUEST_POINT_START -> {
                if (current != BodyPose.HANDS_UP) setPose(p, BodyPose.POINTING);
            }
            case ImmersionPayloads.REQUEST_POINT_STOP -> {
                if (current == BodyPose.POINTING) setPose(p, BodyPose.NONE);
            }
            default -> {}
        }
    }

    /** A chi comincia a vedere un'entita' si dice in che posa e'. */
    static void onStartTracking(ServerPlayer viewer, Entity target) {
        BodyPose pose = poseOf(target);
        if (pose != BodyPose.NONE) {
            PacketDistributor.sendToPlayer(viewer, new ImmersionPayloads.PoseUpdate(target.getId(), pose.ordinal()));
        }
    }

    // ---- conoscenze ----

    private static Optional<PlayerCharacter> characterOf(UUID player) {
        return QRM.get(CharacterService.class).activeOf(new PlayerId(player));
    }

    private static Held heldOf(ServerPlayer p) {
        return KNOWN.computeIfAbsent(p.getUUID(), id -> {
            Optional<CharacterId> character = characterOf(id).map(PlayerCharacter::id);
            KnownCitizens known = character
                    .flatMap(c -> QRM.get(ModDataService.class).get(NAMESPACE, OwnerRef.character(c), KNOWN_KEY))
                    .map(KnownCitizens::fromJson)
                    .orElseGet(KnownCitizens::new);
            return new Held(character.orElse(null), known);
        });
    }

    private static void save(Held h) {
        if (h.character == null) return;
        ModDataService data = QRM.get(ModDataService.class);
        OwnerRef owner = OwnerRef.character(h.character);
        if (h.known.isEmpty()) data.delete(NAMESPACE, owner, KNOWN_KEY);
        else data.put(NAMESPACE, owner, KNOWN_KEY, h.known.toJson());
    }

    /** Il nome da mostrare per un'entita': Nome Cognome RP se ha una Character, altrimenti il nome utente. */
    static String nameOf(Entity e) {
        if (e instanceof ServerPlayer sp) {
            return characterOf(sp.getUUID()).map(c -> c.firstName() + " " + c.lastName())
                    .orElseGet(() -> sp.getGameProfile().name());
        }
        if (e instanceof Mannequin m && m.hasCustomName()) return m.getCustomName().getString();
        return "Manichino";
    }

    private static Component label(ServerPlayer viewer, Entity subject) {
        Held h = heldOf(viewer);
        return h.known.knows(subject.getUUID())
                ? Component.literal(nameOf(subject))
                : Component.translatable("qrm_immersion.stranger", ImmersionRules.strangerCode(subject.getUUID()));
    }

    private static void learn(ServerPlayer viewer, Entity subject) {
        Held h = heldOf(viewer);
        if (h.known.learn(subject.getUUID(), nameOf(subject))) save(h);
        sendKnown(viewer);
    }

    /** Manda al giocatore la lista aggiornata di chi conosce, rinfrescando i nomi di chi e' online. */
    static void sendKnown(ServerPlayer p) {
        Held h = heldOf(p);
        MinecraftServer server = p.level().getServer();
        boolean dirty = false;
        List<ImmersionPayloads.KnownEntry> out = new ArrayList<>();
        for (Map.Entry<UUID, String> e : new ArrayList<>(h.known.view().entrySet())) {
            String name = e.getValue();
            ServerPlayer online = server == null ? null : server.getPlayerList().getPlayer(e.getKey());
            if (online != null) {
                String fresh = nameOf(online);
                if (!fresh.equals(name)) {
                    h.known.learn(e.getKey(), fresh);
                    name = fresh;
                    dirty = true;
                }
            }
            out.add(new ImmersionPayloads.KnownEntry(e.getKey(), name));
        }
        if (dirty) save(h);
        PacketDistributor.sendToPlayer(p, new ImmersionPayloads.Known(out));
    }

    /** Un Character e' stato scelto: chi lo conosce deve rivederne il nome. */
    static void resyncAll() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        for (ServerPlayer p : server.getPlayerList().getPlayers()) sendKnown(p);
    }

    /** Cambio di Character o uscita: si salva e si ricarica al prossimo bisogno. */
    static void forget(UUID player) {
        Held h = KNOWN.remove(player);
        if (h != null) save(h);
        PENDING.remove(player);
        PENDING.values().removeIf(p -> p.from.equals(player));
        LAST_KNOCK.remove(player);
        POSES.remove(player);
    }

    static void forgetEntity(Entity e) {
        POSES.remove(e.getUUID());
    }

    // ---- stringere la mano ----

    private static boolean fit(ServerPlayer p) {
        return !p.isSpectator() && p.isAlive() && !handsUp(p) && !MedicalLink.unfit(p);
    }

    /**
     * Shift + clic destro a mani vuote su una persona. @return vero se l'interazione e' stata gestita (e va annullata).
     * Un ferito a terra non si presenta: li' c'e' la rianimazione del modulo medico.
     */
    static boolean handshake(ServerPlayer a, Entity target) {
        if (!fit(a) || a.distanceTo(target) > ImmersionRules.HANDSHAKE_REACH) return false;
        int now = a.level().getServer().getTickCount();
        if (target instanceof Mannequin dummy) {
            // Il manichino di prova accetta subito, cosi' si prova da soli.
            learn(a, dummy);
            introduced(a, dummy);
            return true;
        }
        if (!(target instanceof ServerPlayer b) || b == a || !fit(b)) return false;

        Pending toMe = PENDING.get(a.getUUID());
        if (toMe != null && toMe.from.equals(b.getUUID()) && toMe.expiresAt > now) {
            PENDING.remove(a.getUUID());
            learn(a, b);
            learn(b, a);
            introduced(a, b);
            introduced(b, a);
            return true;
        }
        Pending old = PENDING.get(b.getUUID());
        boolean fresh = old == null || !old.from.equals(a.getUUID()) || old.expiresAt <= now;
        PENDING.put(b.getUUID(), new Pending(a.getUUID(), now + ImmersionRules.HANDSHAKE_TICKS));
        a.swing(InteractionHand.MAIN_HAND, true);
        a.sendSystemMessage(Component.translatable("qrm_immersion.handshake.offered", label(a, b)), true);
        if (fresh) b.sendSystemMessage(Component.translatable("qrm_immersion.handshake.request", label(b, a)), false);
        return true;
    }

    /** Gesto, suono e messaggio per chi si e' presentato a {@code other}. */
    private static void introduced(ServerPlayer who, Entity other) {
        who.swing(InteractionHand.MAIN_HAND, true);
        who.level().playSound(null, who.getX(), who.getY(), who.getZ(), SoundEvents.PLAYER_ATTACK_NODAMAGE,
                SoundSource.PLAYERS, 0.8F, 1.2F);
        who.sendSystemMessage(Component.translatable("qrm_immersion.handshake.done", nameOf(other)), true);
    }

    // ---- bussare ----

    /** Una bussata alla porta in {@code pos}. @return vero se la bussata c'e' stata (non in pausa tra due colpi). */
    static boolean knock(ServerPlayer p, ServerLevel level, BlockPos pos, boolean loud, boolean iron) {
        int now = level.getServer().getTickCount();
        Integer last = LAST_KNOCK.get(p.getUUID());
        if (last != null && now - last < ImmersionRules.KNOCK_COOLDOWN_TICKS) return false;
        LAST_KNOCK.put(p.getUUID(), now);

        if (loud) {
            level.playSound(null, pos, iron ? SoundEvents.ZOMBIE_ATTACK_IRON_DOOR : SoundEvents.ZOMBIE_ATTACK_WOODEN_DOOR,
                    SoundSource.BLOCKS, 1.2F, 0.8F);
        } else {
            SoundEvent knock = iron ? SoundEvents.ZOMBIE_ATTACK_IRON_DOOR : SoundEvents.WOOD_HIT;
            float volume = iron ? 0.6F : 1.0F;
            float pitch = iron ? 1.6F : 0.6F;
            level.playSound(null, pos, knock, SoundSource.BLOCKS, volume, pitch);
            DELAYED.add(new Delayed(now + 5, level, pos, knock, volume, pitch));
        }
        double radius = loud ? ImmersionRules.KNOCK_HEARD_LOUD : ImmersionRules.KNOCK_HEARD_NORMAL;
        Component msg = Component.translatable(loud ? "qrm_immersion.knock.loud" : "qrm_immersion.knock.normal");
        Vec3 center = Vec3.atCenterOf(pos);
        for (ServerPlayer other : level.players()) {
            if (other.distanceToSqr(center) <= radius * radius) other.sendSystemMessage(msg, true);
        }
        return true;
    }

    // ---- tick ----

    static void tick(MinecraftServer server) {
        int now = server.getTickCount();
        if (!DELAYED.isEmpty()) {
            Iterator<Delayed> it = DELAYED.iterator();
            while (it.hasNext()) {
                Delayed d = it.next();
                if (d.dueTick <= now) {
                    d.level.playSound(null, d.pos, d.sound, SoundSource.BLOCKS, d.volume, d.pitch);
                    it.remove();
                }
            }
        }
        if (now % POSE_CHECK_EVERY != 0) return;
        PENDING.values().removeIf(p -> p.expiresAt <= now);
        for (ServerPlayer p : server.getPlayerList().getPlayers()) {
            if (poseOf(p) != BodyPose.NONE && (p.isSpectator() || !p.isAlive() || MedicalLink.unfit(p))) {
                setPose(p, BodyPose.NONE);
            }
        }
    }
}
