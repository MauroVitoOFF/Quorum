package dev.qrm.neoforge.server;

import dev.qrm.api.character.CharacterService;
import dev.qrm.api.character.PlayerCharacter;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.point.InteractionPoint;
import dev.qrm.api.point.PointPosition;
import dev.qrm.api.point.PointService;
import dev.qrm.api.point.PointType;
import dev.qrm.api.point.PointTypeRegistry;
import dev.qrm.api.point.PointUse;
import java.util.Optional;
import java.util.function.Predicate;

/** Il click destro su un blocco, lato server. Pura: nessuna dipendenza da Minecraft. */
public final class PointInteraction {
    public enum Outcome {
        /** Nessun punto attivo qui: il blocco si comporta normalmente. */
        NOT_A_POINT,
        NO_CHARACTER,
        DENIED,
        USED
    }

    private PointInteraction() {}

    /**
     * Se alla posizione c'è un punto attivo ne verifica l'accesso e, se concesso, chiama l'handler del tipo. Tutto
     * tranne {@link Outcome#NOT_A_POINT} richiede che l'uso normale del blocco sia annullato.
     */
    public static Outcome use(PointService points, PointTypeRegistry types, CharacterService characters,
            PlayerId player, PointPosition position, Predicate<InteractionPoint> worldActive) {
        Optional<InteractionPoint> found = points.findAt(position);
        if (found.isEmpty()) return Outcome.NOT_A_POINT;
        InteractionPoint point = found.get();
        if (points.inactiveReason(point).isPresent() || !worldActive.test(point)) return Outcome.NOT_A_POINT;
        Optional<PlayerCharacter> active = characters.activeOf(player);
        if (active.isEmpty()) return Outcome.NO_CHARACTER;
        if (!points.canUse(active.get().id(), point)) return Outcome.DENIED;
        PointType type = types.find(point.typeId()).orElseThrow();
        type.handler().onUse(new PointUse(player, active.get().id(), point));
        return Outcome.USED;
    }
}
