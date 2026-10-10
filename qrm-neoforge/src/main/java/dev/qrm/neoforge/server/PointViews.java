package dev.qrm.neoforge.server;

import dev.qrm.api.id.CharacterId;
import dev.qrm.api.point.InteractionPoint;
import dev.qrm.api.point.PointService;
import dev.qrm.api.point.PointType;
import dev.qrm.api.point.PointTypeRegistry;
import dev.qrm.neoforge.net.PointEntry;
import dev.qrm.neoforge.net.PointsPayload;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

/** Quali punti comunicare a un giocatore. Pura: nessuna dipendenza da Minecraft; il filtro è sempre sul server. */
public final class PointViews {
    private PointViews() {}

    /**
     * @param character il personaggio attivo, se c'è: senza personaggio si vedono solo i punti pubblici
     * @param active se il punto è attivo anche nel mondo (dimensione e blocco esistono)
     */
    public static List<PointEntry> visibleTo(PointService points, PointTypeRegistry types,
            Optional<CharacterId> character, Predicate<InteractionPoint> active) {
        List<PointEntry> out = new ArrayList<>();
        for (InteractionPoint p : points.all()) {
            if (out.size() >= PointsPayload.MAX_POINTS) break;
            if (!active.test(p)) continue;
            boolean visible = p.publicVisible() || (character.isPresent() && points.canUse(character.get(), p));
            if (!visible) continue;
            PointType type = types.find(p.typeId()).orElse(null);
            if (type == null) continue;
            out.add(new PointEntry(p.id(), p.typeId(), p.position().dimension(), p.position().x(), p.position().y(),
                    p.position().z(), type.labelKey(), Optional.ofNullable(p.name()),
                    Optional.ofNullable(p.face()).map(f -> f.name().toLowerCase(java.util.Locale.ROOT))));
        }
        return out;
    }
}
