package dev.qrm.api.point;

import dev.qrm.api.id.CharacterId;
import dev.qrm.api.id.PlayerId;

/** Un click autorizzato su un punto. */
public record PointUse(PlayerId player, CharacterId character, InteractionPoint point) {}
