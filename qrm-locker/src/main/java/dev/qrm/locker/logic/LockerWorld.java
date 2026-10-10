package dev.qrm.locker.logic;

import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;
import dev.qrm.api.point.InteractionPoint;
import java.util.Optional;

/** Ciò che serve a sapere se un armadietto aperto è ancora utilizzabile. */
public interface LockerWorld {
    Optional<InteractionPoint> point(int id);

    boolean canUse(CharacterId character, InteractionPoint point);

    /** Vero se il personaggio è ancora quello attivo del giocatore. */
    boolean isActive(PlayerId player, CharacterId character);
}
