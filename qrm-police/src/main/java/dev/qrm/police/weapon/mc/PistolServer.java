package dev.qrm.police.weapon.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.emergency.IncapacitationRegistry;
import dev.qrm.police.mc.PoliceItems;
import dev.qrm.police.weapon.logic.ActionSession;
import dev.qrm.police.weapon.logic.Ballistics;
import dev.qrm.police.weapon.logic.FireResult;
import dev.qrm.police.weapon.logic.MagazineLoader;
import dev.qrm.police.weapon.logic.MagazineState;
import dev.qrm.police.weapon.logic.PistolAction;
import dev.qrm.police.weapon.logic.PistolIntents;
import dev.qrm.police.weapon.logic.PistolIntents.Admission;
import dev.qrm.police.weapon.logic.PistolRules;
import dev.qrm.police.weapon.logic.PistolTimings;
import dev.qrm.police.weapon.logic.SessionOutcome;
import dev.qrm.police.weapon.logic.WeaponDef;
import dev.qrm.police.weapon.logic.WeaponState;
import dev.qrm.police.weapon.logic.input.InputRateLimit;
import dev.qrm.police.weapon.net.PistolPayloads;
import dev.qrm.police.weapon.net.PistolPayloads.Input.Kind;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.random.RandomGenerator;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * Lato server della pistola, autoritativo: munizioni, colpi a segno, danno e sessioni d'azione (estrazione,
 * ricarica, carrello, controlli) si decidono solo qui. Il client manda intenzioni ({@link Kind}).
 *
 * <p>Lo stato vive in mappe per giocatore (UUID), svuotate all'uscita ({@link #forget}) e all'arresto del server
 * ({@link #reset}); fa eccezione l'id del personaggio, che resta fino all'arresto per poterlo rimandare al rientro.
 * Nessun controllo di servizio o ruolo (D0).
 *
 * <p>Stato a terra (R38): a ogni tick si confronta con l'ultimo valore mandato al giocatore e, se cambia, gli si manda
 * {@link PistolPayloads.Incapacitated} (anche all'ingresso); andando a terra la mira si spegne. A terra la mira non si
 * accende e nessuna azione parte.
 *
 * <p>Le intenzioni diverse da {@code FIRE} e {@code AIM_OFF} hanno un limite di frequenza per giocatore
 * ({@link InputRateLimit}); le eccedenti si ignorano in silenzio. {@code AIM_OFF} ne è escluso: spegne soltanto e,
 * se perso, lascerebbe una mira accesa.
 */
public final class PistolServer {
    private PistolServer() {}

    /** Volume del colpo: con un evento a raggio variabile vale circa 16 blocchi per unità. */
    private static final float FIRE_VOLUME = 4.0f;
    /** Tick dell'ultimo colpo per chi non ha mai sparato (vedi {@link PistolTimings#canFire}). */
    private static final long NEVER_SHOT = -1000L;

    /**
     * Sessione in corso di un giocatore.
     *
     * @param itemId    id della pistola su cui è partita
     * @param startTick tick del server in cui è partita: in quel tick non avanza
     */
    private record Running(ActionSession session, PistolAction action, int slot, UUID itemId, long startTick) {}

    private static final Map<UUID, Running> SESSIONS = new HashMap<>();
    private static final Map<UUID, Long> LAST_SHOT = new HashMap<>();
    private static final Set<UUID> AIMING = new HashSet<>();
    private static final Map<UUID, Integer> LAST_SLOT = new HashMap<>();
    /** Id della pistola nello slot selezionato all'ultimo controllo ({@code null} se non c'era una pistola). */
    private static final Map<UUID, UUID> LAST_PISTOL = new HashMap<>();
    /** Ultimo stato a terra mandato al giocatore. */
    private static final Map<UUID, Boolean> LAST_INCAPACITATED = new HashMap<>();
    private static final Map<UUID, InputRateLimit> RATE = new HashMap<>();
    private static final Map<UUID, String> CHARACTERS = new HashMap<>();

    // ---- intenzioni ----

    /** Riceve un'intenzione del giocatore (thread del server). */
    public static void onInput(ServerPlayer player, Kind kind) {
        if (!player.isAlive() || player.hasDisconnected()) return;
        if (kind != Kind.FIRE && kind != Kind.AIM_OFF
                && !RATE.computeIfAbsent(player.getUUID(), u -> new InputRateLimit())
                        .allow(player.level().getServer().getTickCount())) {
            return;
        }
        // Un cambio dell'oggetto impugnato non ancora visto dal tick va applicato prima dell'intenzione (annulla,
        // estrazione).
        syncHeld(player);
        ItemStack held = player.getInventory().getSelectedItem();
        boolean pistol = held.is(PoliceItems.PISTOL.get());
        boolean magazine = !pistol && held.is(PoliceItems.MAGAZINE.get());
        if (pistol) WeaponComponents.ensurePistolId(held, UUID::randomUUID);
        if (magazine) WeaponComponents.ensureMagazineId(held, UUID::randomUUID);
        switch (kind) {
            case FIRE -> {
                if (pistol) fire(player, held);
            }
            case AIM_ON -> setAim(player, PistolIntents.allowsAim(pistol, incapacitated(player)));
            case AIM_OFF -> setAim(player, false);
            case RELOAD -> {
                if (pistol) startOnPistol(player, kind, held, PistolIntents.reloadFor(stateOf(held)));
                else if (magazine) startMagazineRefill(player, held);
            }
            case INTERACT_TAP -> {
                if (pistol) startOnPistol(player, kind, held, PistolIntents.tapFor(stateOf(held)));
            }
            case INTERACT_HOLD -> {
                if (pistol) startOnPistol(player, kind, held, PistolAction.INSPECT);
            }
        }
    }

    /**
     * Un giocatore inizia a vedere {@code target}: gli si manda lo stato di mira attuale di {@code target} (R30).
     * Si manda anche "non mira", così un client che ricordava una mira vecchia per lo stesso id (rinascita, che
     * conserva l'id dell'entità) si corregge.
     */
    public static void onStartTracking(ServerPlayer tracker, Entity target) {
        if (target instanceof ServerPlayer subject && subject != tracker) {
            sendTo(tracker, new PistolPayloads.Aim(subject.getId(), AIMING.contains(subject.getUUID())));
        }
    }

    /**
     * Rinascita o cambio di dimensione: la mira si spegne, come sul client, e chi osserva lo sa (R30); la sessione in
     * corso è annullata (il client dimentica la sua azione in quel momento e tornerebbe a prevedere spari).
     */
    public static void onRespawnOrDimensionChange(ServerPlayer player) {
        setAim(player, false);
        end(player, true);
    }

    /** Vero se il giocatore sta mirando con la pistola. */
    public static boolean isAiming(UUID player) {
        return AIMING.contains(player);
    }

    private static void fire(ServerPlayer player, ItemStack held) {
        UUID id = player.getUUID();
        long now = player.level().getServer().getTickCount();
        Running running = SESSIONS.get(id);
        Admission admission = PistolIntents.admit(running == null ? null : running.action());
        if (incapacitated(player) || admission == Admission.REFUSE
                || !PistolTimings.serverCanFire(LAST_SHOT.getOrDefault(id, NEVER_SHOT), now)) {
            reject(player, Kind.FIRE);
            return;
        }
        if (admission == Admission.PREEMPT_AND_START) end(player, true);

        FireResult result = PistolRules.fire(stateOf(held));
        // A vuoto: clic solo sul client, il server non cambia né manda nulla.
        if (!result.fired()) return;
        held.set(WeaponComponents.PISTOL_STATE.get(), result.state());
        LAST_SHOT.put(id, now);

        ServerLevel level = player.level();
        RandomGenerator random = player.getRandom()::nextLong;
        PistolRaycast.Hit hit = PistolRaycast.trace(player, isAiming(id), random);
        hit.target().ifPresent(t ->
                t.hurtServer(level, PistolDamage.source(level, player), Ballistics.damage(hit.headshot())));

        level.playSound(player, player.getX(), player.getEyeY(), player.getZ(), PistolSounds.PISTOL_FIRE,
                SoundSource.PLAYERS, FIRE_VOLUME, 1.0f);
        level.playSound(null, hit.to().x, hit.to().y, hit.to().z, PistolRaycast.impactSound(level, hit.block(), player),
                SoundSource.PLAYERS, 1.0f, 1.0f);
        // Anche al tiratore: il suo client disegna la traccia verso il punto d'impatto deciso qui.
        broadcast(player, new PistolPayloads.Shot(player.getId(), hit.from(), hit.to(), hit.block(),
                hit.target().isPresent()), true);
    }

    // ---- riempimento a mano ----

    /** Riempimento in corso di un caricatore: quanti colpi restano da inserire e quando va il prossimo. */
    private static final class RefillJob {
        final UUID magazineId;
        int remaining;
        long nextAt;

        RefillJob(UUID magazineId, int remaining, long nextAt) {
            this.magazineId = magazineId;
            this.remaining = remaining;
            this.nextAt = nextAt;
        }
    }

    private static final Map<UUID, RefillJob> REFILLS = new HashMap<>();
    private static final org.slf4j.Logger REFILL_LOG = org.slf4j.LoggerFactory.getLogger("qrm_police");

    /**
     * R con il caricatore in mano: prende dall'inventario i proiettili che servono (al massimo fino alla capienza della
     * definizione dell'arma) e li inserisce uno alla volta. Il server decide la quantità; senza proiettili o con il
     * caricatore già pieno avvisa soltanto.
     */
    private static void startMagazineRefill(ServerPlayer player, ItemStack held) {
        if (incapacitated(player) || !player.isAlive()) return;
        MagazineState mag = WeaponComponents.ensureMagazineId(held, UUID::randomUUID);
        int capacity = Math.min(mag.capacity(), WeaponDef.pistol().magazineCapacity());
        int pending = mag.rounds() + reservedRounds(player, mag.id());
        if (pending >= capacity) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("qrm_police.refill.full"));
            return;
        }
        int moved = MagazineLoader.loadable(pending, mag.capacity(), WeaponDef.pistol().magazineCapacity(),
                countBullets(player), false);
        if (moved <= 0) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("qrm_police.refill.no_bullets"));
            return;
        }
        int taken = takeBullets(player, moved);
        if (taken > 0) startRefill(player, mag.id(), taken);
    }

    private static int countBullets(ServerPlayer player) {
        int total = 0;
        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        for (int i = 0; i < net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE; i++) {
            ItemStack s = inv.getItem(i);
            if (s.is(PoliceItems.BULLET.get())) total += s.getCount();
        }
        return total;
    }

    private static int takeBullets(ServerPlayer player, int wanted) {
        int taken = 0;
        net.minecraft.world.entity.player.Inventory inv = player.getInventory();
        for (int i = 0; i < net.minecraft.world.entity.player.Inventory.INVENTORY_SIZE && taken < wanted; i++) {
            ItemStack s = inv.getItem(i);
            if (!s.is(PoliceItems.BULLET.get())) continue;
            int n = Math.min(s.getCount(), wanted - taken);
            s.shrink(n);
            taken += n;
        }
        return taken;
    }

    /** Colpi già presi dalla mano del giocatore e non ancora nel caricatore, per quel caricatore. */
    public static int reservedRounds(ServerPlayer player, UUID magazineId) {
        RefillJob job = REFILLS.get(player.getUUID());
        return job != null && job.magazineId.equals(magazineId) ? job.remaining : 0;
    }

    /**
     * Il giocatore riempie il caricatore che ha in mano con {@code bullets} proiettili: li ha già tolti dall'inventario; il server li inserisce uno alla volta, al ritmo delle clip, e avvisa il client perché animi lo stesso
     * numero di giri.
     */
    public static void startRefill(ServerPlayer player, UUID magazineId, int bullets) {
        long now = player.level().getServer().getTickCount();
        RefillJob job = REFILLS.get(player.getUUID());
        if (job != null && job.magazineId.equals(magazineId)) {
            job.remaining += bullets;
        } else {
            if (job != null) cancelRefill(player, false);
            REFILLS.put(player.getUUID(),
                    new RefillJob(magazineId, bullets, now + MagazineLoader.firstInsertTicks()));
        }
        REFILL_LOG.debug("Riempimento di {}: {} colpi, primo tra {} tick", player.getGameProfile().name(), bullets,
                MagazineLoader.firstInsertTicks());
        sendTo(player, new PistolPayloads.Refill(bullets));
    }

    private static void tickRefill(ServerPlayer player, long now) {
        RefillJob job = REFILLS.get(player.getUUID());
        if (job == null) return;
        ItemStack held = player.getMainHandItem();
        MagazineState mag = held.is(PoliceItems.MAGAZINE.get()) ? held.get(WeaponComponents.MAGAZINE.get()) : null;
        // Il caricatore non è più in mano, o il giocatore è stato colpito, è a terra o è morto: si smette.
        String why = mag == null || !mag.id().equals(job.magazineId) ? "il caricatore non e' piu' in mano"
                : player.hurtTime > 0 ? "colpito" : incapacitated(player) ? "a terra"
                : player.isDeadOrDying() ? "morto" : null;
        if (why != null) {
            REFILL_LOG.info("Riempimento di {} interrotto: {} ({} colpi restituiti)", player.getGameProfile().name(), why,
                    job.remaining);
            cancelRefill(player, true);
            return;
        }
        if (now < job.nextAt) return;
        int capacity = Math.min(mag.capacity(), WeaponDef.pistol().magazineCapacity());
        if (mag.rounds() >= capacity) {
            REFILL_LOG.info("Riempimento di {} chiuso: caricatore pieno ({} colpi restituiti)",
                    player.getGameProfile().name(), job.remaining);
            cancelRefill(player, true); // pieno: i colpi che avanzano tornano al giocatore
            return;
        }
        held.set(WeaponComponents.MAGAZINE.get(), mag.withRounds(mag.rounds() + 1).knownTo(characterId(player)));
        job.remaining--;
        job.nextAt += MagazineLoader.insertIntervalTicks();
        ServerLevel level = player.level();
        level.playSound(player, player.getX(), player.getY(), player.getZ(), PistolSounds.PISTOL_BULLET_LOAD,
                SoundSource.PLAYERS, 0.6f, 1.0f);
        if (job.remaining <= 0) {
            REFILLS.remove(player.getUUID());
            sendTo(player, new PistolPayloads.RefillStop());
        }
    }

    /**
     * Interrompe il riempimento (se c'è) e restituisce al giocatore i colpi non ancora inseriti: nell'inventario, o a
     * terra se non c'è posto. Con {@code notify} il client chiude l'animazione.
     */
    public static void cancelRefill(ServerPlayer player, boolean notify) {
        RefillJob job = REFILLS.remove(player.getUUID());
        if (job == null) return;
        if (job.remaining > 0) {
            ItemStack back = new ItemStack(PoliceItems.BULLET.get(), job.remaining);
            if (!player.getInventory().add(back)) player.drop(back, false);
        }
        if (notify) sendTo(player, new PistolPayloads.RefillStop());
    }

    private static void setAim(ServerPlayer player, boolean aiming) {
        boolean changed = aiming ? AIMING.add(player.getUUID()) : AIMING.remove(player.getUUID());
        if (changed) broadcast(player, new PistolPayloads.Aim(player.getId(), aiming), false);
    }

    // ---- sessioni ----

    private static void startOnPistol(ServerPlayer player, Kind kind, ItemStack held, PistolAction action) {
        Running running = SESSIONS.get(player.getUUID());
        // Lo stesso tasto premuto di nuovo mentre l'azione è in corso non la fa ripartire.
        if (running != null && running.action() == action) return;
        Admission admission = PistolIntents.admit(running == null ? null : running.action());
        if (incapacitated(player) || admission == Admission.REFUSE) {
            reject(player, kind);
            return;
        }
        WeaponState state = stateOf(held);
        Optional<ActionSession> session =
                ActionSession.start(action, state, new PlayerMagazines(player), characterId(player));
        if (session.isEmpty()) {
            reject(player, kind);
            return;
        }
        if (admission == Admission.PREEMPT_AND_START) end(player, true);
        begin(player, session.get(), action, state.id());
    }

    private static void begin(ServerPlayer player, ActionSession session, PistolAction action, UUID itemId) {
        int slot = player.getInventory().getSelectedSlot();
        long now = player.level().getServer().getTickCount();
        SESSIONS.put(player.getUUID(), new Running(session, action, slot, itemId, now));
        broadcast(player, new PistolPayloads.ActionStarted(player.getId(), action), true);
    }

    /** Chiude la sessione del giocatore, se c'è, e lo comunica a lui e a chi lo osserva. */
    private static void end(ServerPlayer player, boolean cancelled) {
        if (SESSIONS.remove(player.getUUID()) != null) {
            broadcast(player, new PistolPayloads.ActionEnded(player.getId(), cancelled), true);
        }
    }

    /** Avanza le sessioni e controlla i cambi di slot. Da chiamare a ogni tick del server. */
    public static void tick(MinecraftServer server) {
        long now = server.getTickCount();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            syncHeld(player);
            syncIncapacitated(player);
            // Pistola buttata o spostata senza cambiare slot: la mira non ha più senso.
            if (AIMING.contains(player.getUUID()) && !player.getInventory().getSelectedItem().is(PoliceItems.PISTOL.get())) {
                setAim(player, false);
            }
            tickSession(player, now);
            tickRefill(player, now);
        }
        // Sessioni rimaste senza giocatore (per esempio uscita non ancora notificata): via senza commit.
        SESSIONS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    private static void tickSession(ServerPlayer player, long now) {
        Running r = SESSIONS.get(player.getUUID());
        if (r == null || r.startTick() == now) return;
        boolean same = sameWeapon(player, r);
        // Si scrive nell'oggetto solo nei tick in cui un commit ha cambiato qualcosa: un'altra modifica allo stack
        // (per esempio l'armeria durante un'ispezione) non viene sovrascritta da una copia vecchia.
        WeaponState stateBefore = r.session().state();
        SessionOutcome outcome = r.session().tick(same, incapacitated(player));
        if (same && !Objects.equals(stateBefore, r.session().state())) {
            player.getInventory().getSelectedItem().set(WeaponComponents.PISTOL_STATE.get(), r.session().state());
        }
        if (outcome.status() != SessionOutcome.Status.RUNNING) {
            end(player, outcome.status() == SessionOutcome.Status.CANCELLED);
        }
    }

    /**
     * Vero se il giocatore impugna ancora l'oggetto su cui è partita la sessione: vivo e connesso, stesso slot
     * selezionato, e nello slot una pistola con lo stesso id.
     */
    private static boolean sameWeapon(ServerPlayer player, Running r) {
        if (!player.isAlive() || player.hasDisconnected()) return false;
        if (player.getInventory().getSelectedSlot() != r.slot()) return false;
        ItemStack held = player.getInventory().getSelectedItem();
        WeaponState state = held.is(PoliceItems.PISTOL.get()) ? held.get(WeaponComponents.PISTOL_STATE.get()) : null;
        return state != null && state.id().equals(r.itemId());
    }

    /**
     * Applica un cambio dell'oggetto impugnato: un altro slot selezionato, oppure una pistola diversa (o prima
     * assente) comparsa nello slot già selezionato. La mira si azzera, la sessione in corso è annullata e, se ora in
     * mano c'è una pistola, parte l'estrazione ({@code DRAW}, blocca lo sparo).
     */
    private static void syncHeld(ServerPlayer player) {
        UUID id = player.getUUID();
        int slot = player.getInventory().getSelectedSlot();
        ItemStack held = player.getInventory().getSelectedItem();
        UUID pistolId = held.is(PoliceItems.PISTOL.get())
                ? WeaponComponents.ensurePistolId(held, UUID::randomUUID).id()
                : null;
        boolean known = LAST_SLOT.containsKey(id);
        Integer previousSlot = LAST_SLOT.put(id, slot);
        UUID previousPistol = LAST_PISTOL.put(id, pistolId);
        if (!known) return;
        boolean slotChanged = previousSlot != slot;
        boolean pistolAppeared = pistolId != null && !pistolId.equals(previousPistol);
        if (!slotChanged && !pistolAppeared) return;
        setAim(player, false);
        end(player, true);
        if (pistolId == null || !player.isAlive() || incapacitated(player)) return;
        WeaponState state = stateOf(held);
        ActionSession.start(PistolAction.DRAW, state, new PlayerMagazines(player), characterId(player))
                .ifPresent(s -> begin(player, s, PistolAction.DRAW, state.id()));
    }

    /** Manda al giocatore il suo stato a terra se è cambiato; andando a terra la mira si spegne. */
    private static void syncIncapacitated(ServerPlayer player) {
        boolean now = incapacitated(player);
        Boolean previous = LAST_INCAPACITATED.put(player.getUUID(), now);
        if (previous != null && previous == now) return;
        sendTo(player, new PistolPayloads.Incapacitated(now));
        if (now) setAim(player, false);
    }

    // ---- personaggio ----

    /** Id del personaggio usato dal server (conoscenza dei caricatori); in mancanza, l'UUID del giocatore. */
    public static String characterId(ServerPlayer player) {
        return CHARACTERS.getOrDefault(player.getUUID(), player.getUUID().toString());
    }

    /** Un personaggio è stato scelto: lo ricorda e manda al giocatore lo stesso id per i tooltip. */
    public static void onCharacterSelected(PlayerCharacter character) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        UUID playerId = character.playerId().value();
        String characterId = character.id().value().toString();
        server.execute(() -> {
            CHARACTERS.put(playerId, characterId);
            ServerPlayer player = server.getPlayerList().getPlayer(playerId);
            if (player != null) sendTo(player, new PistolPayloads.Identity(characterId));
        });
    }

    /** All'ingresso manda l'id già noto (o l'UUID di ripiego), così client e server concordano da subito. */
    public static void onLogin(ServerPlayer player) {
        sendTo(player, new PistolPayloads.Identity(characterId(player)));
        boolean incapacitated = incapacitated(player);
        LAST_INCAPACITATED.put(player.getUUID(), incapacitated);
        sendTo(player, new PistolPayloads.Incapacitated(incapacitated));
    }

    // ---- pulizia ----

    /** Dimentica sessioni, mira, oggetto impugnato, stato a terra, limite e ultimo colpo di un giocatore che esce. */
    public static void forget(UUID player) {
        REFILLS.remove(player);
        SESSIONS.remove(player);
        LAST_SHOT.remove(player);
        AIMING.remove(player);
        LAST_SLOT.remove(player);
        LAST_PISTOL.remove(player);
        LAST_INCAPACITATED.remove(player);
        RATE.remove(player);
    }

    /** Svuota tutto all'arresto del server. */
    public static void reset() {
        SESSIONS.clear();
        LAST_SHOT.clear();
        AIMING.clear();
        LAST_SLOT.clear();
        LAST_PISTOL.clear();
        LAST_INCAPACITATED.clear();
        RATE.clear();
        CHARACTERS.clear();
    }

    // ---- utilità ----

    private static WeaponState stateOf(ItemStack pistol) {
        return pistol.getOrDefault(WeaponComponents.PISTOL_STATE.get(), WeaponComponents.DEFAULT_PISTOL);
    }

    /** Se il registro dello stato a terra non c'è, nessuno è a terra. */
    private static boolean incapacitated(ServerPlayer player) {
        try {
            return QRM.get(IncapacitationRegistry.class).isIncapacitated(player.getUUID());
        } catch (IllegalStateException notAvailable) {
            return false;
        }
    }

    private static void reject(ServerPlayer player, Kind kind) {
        sendTo(player, new PistolPayloads.Rejected(kind));
    }

    /** Il canale è opzionale: a un client senza il Module non si manda nulla (NeoForge lancerebbe un'eccezione). */
    private static void sendTo(ServerPlayer player, CustomPacketPayload payload) {
        if (player.connection != null && player.connection.hasChannel(payload)) {
            PacketDistributor.sendToPlayer(player, payload);
        }
    }

    /** A chi osserva {@code subject} e, se {@code self}, anche a lui. */
    private static void broadcast(ServerPlayer subject, CustomPacketPayload payload, boolean self) {
        for (ServerPlayer watcher : subject.level().getChunkSource().chunkMap.getPlayersWatching(subject)) {
            if (watcher != subject) sendTo(watcher, payload);
        }
        if (self) sendTo(subject, payload);
    }
}
