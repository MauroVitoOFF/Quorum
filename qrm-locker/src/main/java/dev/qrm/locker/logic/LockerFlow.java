package dev.qrm.locker.logic;

import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.point.InteractionPoint;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Regole di apertura, validità e salvataggio degli armadietti aperti. Un giocatore ne ha uno solo alla volta; il
 * contenuto si scrive a fine tick se cambiato, alla chiusura e allo spegnimento. Va usato dal solo thread del server.
 */
public final class LockerFlow {
    private static final Logger LOG = LoggerFactory.getLogger("qrm_locker");

    public enum Failure { ALREADY_OPEN, CORRUPT }

    /** Esito di {@link #open}: o una sessione o un motivo. */
    public record OpenResult(Session session, Failure failure) {
        public boolean ok() {
            return session != null;
        }
    }

    private final LockerStorage storage;
    private final LockerWorld world;
    private final Map<PlayerId, Session> open = new HashMap<>();

    public LockerFlow(LockerStorage storage, LockerWorld world) {
        this.storage = storage;
        this.world = world;
    }

    public boolean isOpen(PlayerId player) {
        return open.containsKey(player);
    }

    public Optional<Session> sessionOf(PlayerId player) {
        return Optional.ofNullable(open.get(player));
    }

    /**
     * Apre l'armadietto del personaggio per il punto. Un documento non valido non si apre e non si sovrascrive.
     * Il chiamante deve poi collegare il contenuto vivo con {@link Session#bind}.
     */
    public OpenResult open(PlayerId player, CharacterId character, InteractionPoint point) {
        if (open.containsKey(player)) return new OpenResult(null, Failure.ALREADY_OPEN);
        String key = LockerKeys.of(point.owner());
        LockerSlots loaded;
        Optional<String> raw = storage.read(character, key);
        try {
            loaded = raw.isPresent() ? LockerSlots.fromJson(raw.get()) : LockerSlots.empty();
        } catch (LockerDocumentException e) {
            LOG.error("Armadietto illeggibile, non lo apro: chiave {} personaggio {} ({})", key, character.value(),
                    e.getMessage());
            return new OpenResult(null, Failure.CORRUPT);
        }
        Session session = new Session(player, character, point, key, loaded);
        open.put(player, session);
        return new OpenResult(session, null);
    }

    /** Chiude la sessione: se il contenuto è cambiato lo scrive subito. Vale anche per una sessione già chiusa. */
    public void close(Session session) {
        if (open.get(session.player) != session) return;
        open.remove(session.player);
        if (session.dirty) save(session);
    }

    /** Scrive gli armadietti cambiati (fine tick). */
    public void flushDirty() {
        if (open.isEmpty()) return;
        for (Session s : new ArrayList<>(open.values())) {
            if (s.dirty) save(s);
        }
    }

    /** Scrive tutto e chiude tutto (spegnimento del server). */
    public void closeAll() {
        List<Session> all = new ArrayList<>(open.values());
        open.clear();
        for (Session s : all) {
            if (s.dirty) save(s);
        }
    }

    private void save(Session s) {
        s.dirty = false;
        if (s.source == null) return;
        try {
            storage.write(s.character, s.key, s.source.get().toJson());
        } catch (RuntimeException e) {
            s.dirty = true;
            LOG.error("Salvataggio dell'armadietto fallito: chiave {} personaggio {}", s.key, s.character.value(), e);
        }
    }

    /** Un armadietto aperto da un giocatore. */
    public final class Session {
        private final PlayerId player;
        private final CharacterId character;
        private final InteractionPoint point;
        private final String key;
        private final LockerSlots loaded;
        private Supplier<LockerSlots> source;
        private boolean dirty;

        private Session(PlayerId player, CharacterId character, InteractionPoint point, String key, LockerSlots loaded) {
            this.player = player;
            this.character = character;
            this.point = point;
            this.key = key;
            this.loaded = loaded;
        }

        public PlayerId player() {
            return player;
        }

        public CharacterId character() {
            return character;
        }

        public InteractionPoint point() {
            return point;
        }

        public String key() {
            return key;
        }

        /** Contenuto letto dal database, da trasferire nel container. */
        public LockerSlots loaded() {
            return loaded;
        }

        /** Collega il contenuto vivo: {@code source} produce lo stato attuale da scrivere. */
        public void bind(Supplier<LockerSlots> source) {
            this.source = source;
            this.dirty = false;
        }

        public void markDirty() {
            this.dirty = true;
        }

        public boolean dirty() {
            return dirty;
        }

        /**
         * @param near il giocatore è nello stesso mondo e vicino al blocco (lo sa il livello Minecraft)
         */
        public boolean stillValid(boolean near) {
            if (!near) return false;
            Optional<InteractionPoint> current = world.point(point.id());
            if (current.isEmpty() || !sameSpot(current.get())) return false;
            return world.canUse(character, current.get()) && world.isActive(player, character);
        }

        private boolean sameSpot(InteractionPoint p) {
            return p.id() == point.id() && p.typeId().equals(point.typeId()) && p.owner().equals(point.owner())
                    && p.position().equals(point.position());
        }
    }
}
