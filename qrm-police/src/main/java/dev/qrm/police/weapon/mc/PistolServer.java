package dev.qrm.police.weapon.mc;

import dev.qrm.api.QRM;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.emergency.IncapacitationRegistry;
import dev.qrm.police.mc.PoliceItems;
import dev.qrm.police.weapon.logic.ActionSession;
import dev.qrm.police.weapon.logic.Ballistics;
import dev.qrm.police.weapon.logic.FireResult;
import dev.qrm.police.weapon.logic.MagazineState;
import dev.qrm.police.weapon.logic.PistolAction;
import dev.qrm.police.weapon.logic.PistolIntents;
import dev.qrm.police.weapon.logic.PistolIntents.Admission;
import dev.qrm.police.weapon.logic.PistolRules;
import dev.qrm.police.weapon.logic.PistolTimings;
import dev.qrm.police.weapon.logic.SessionOutcome;
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
     * @param itemId    id della pistola (o del caricatore, per {@code INSPECT_MAG}) su cui è partita
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
            }
            case INTERACT_TAP -> {
                if (pistol) startOnPistol(player, kind, held, PistolIntents.tapFor(stateOf(held)));
                else if (magazine) startInspectMagazine(player, kind, held);
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

    private static void setAim(ServerPlayer player, boolean aiming) {
        boolean changed = aiming ? AIMING.add(player.getUUID()) : AIMING.remove(player.getUUID());
        if (changed) broadcast(player, new PistolPayloads.Aim(player.getId(), aiming), false);
    }

    // ---- sessioni ----

    private static void startOnPistol(ServerPlayer player, Kind kind, ItemStack held, PistolAction action) {
        Running running = SESSIONS.get(player.getUUID());
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

    private static void startInspectMagazine(ServerPlayer player, Kind kind, ItemStack held) {
        Running running = SESSIONS.get(player.getUUID());
        Admission admission = PistolIntents.admit(running == null ? null : running.action());
        if (incapacitated(player) || admission == Admission.REFUSE) {
            reject(player, kind);
            return;
        }
        if (admission == Admission.PREEMPT_AND_START) end(player, true);
        MagazineState mag = held.get(WeaponComponents.MAGAZINE.get());
        begin(player, ActionSession.inspectMagazine(mag, characterId(player)), PistolAction.INSPECT_MAG, mag.id());
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
        }
        // Sessioni rimaste senza giocatore (per esempio uscita non ancora notificata): via senza commit.
        SESSIONS.keySet().removeIf(id -> server.getPlayerList().getPlayer(id) == null);
    }

    private static void tickSession(ServerPlayer player, long now) {
        Running r = SESSIONS.get(player.getUUID());
        if (r == null || r.startTick() == now) return;
        boolean same = sameWeapon(player, r);
        boolean inspectMag = r.action() == PistolAction.INSPECT_MAG;
        // Si scrive nell'oggetto solo nei tick in cui un commit ha cambiato qualcosa: un'altra modifica allo stack
        // (per esempio l'armeria durante un'ispezione) non viene sovrascritta da una copia vecchia.
        boolean inspectedBefore = inspectMag && r.session().inspectedMagazine().isPresent();
        WeaponState stateBefore = inspectMag ? null : r.session().state();
        SessionOutcome outcome = r.session().tick(same, incapacitated(player));
        if (same) {
            ItemStack held = player.getInventory().getSelectedItem();
            if (inspectMag) {
                if (!inspectedBefore && r.session().inspectedMagazine().isPresent()) {
                    // Solo il campo del commit: il caricatore com'è ora, noto anche a questo personaggio.
                    MagazineState current = held.get(WeaponComponents.MAGAZINE.get());
                    if (current != null) held.set(WeaponComponents.MAGAZINE.get(), current.knownTo(characterId(player)));
                }
            } else if (!Objects.equals(stateBefore, r.session().state())) {
                held.set(WeaponComponents.PISTOL_STATE.get(), r.session().state());
            }
        }
        if (outcome.status() != SessionOutcome.Status.RUNNING) {
            end(player, outcome.status() == SessionOutcome.Status.CANCELLED);
        }
    }

    /**
     * Vero se il giocatore impugna ancora l'oggetto su cui è partita la sessione: vivo e connesso, stesso slot
     * selezionato, e nello slot una pistola (o un caricatore, per {@code INSPECT_MAG}) con lo stesso id.
     */
    private static boolean sameWeapon(ServerPlayer player, Running r) {
        if (!player.isAlive() || player.hasDisconnected()) return false;
        if (player.getInventory().getSelectedSlot() != r.slot()) return false;
        ItemStack held = player.getInventory().getSelectedItem();
        if (r.action() == PistolAction.INSPECT_MAG) {
            MagazineState mag = held.is(PoliceItems.MAGAZINE.get()) ? held.get(WeaponComponents.MAGAZINE.get()) : null;
            return mag != null && mag.id().equals(r.itemId());
        }
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
