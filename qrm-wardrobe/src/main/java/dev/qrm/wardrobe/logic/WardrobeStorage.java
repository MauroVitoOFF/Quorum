package dev.qrm.wardrobe.logic;

import dev.qrm.api.id.CharacterId;
import java.util.Optional;

/** Dove si ricorda la divisa indossata da ogni Character. */
public interface WardrobeStorage {
    Optional<String> read(CharacterId character);

    /** {@code null} toglie la divisa. */
    void write(CharacterId character, String outfitIdOrNull);
}
