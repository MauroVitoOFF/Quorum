package dev.qrm.neoforge.client;

import java.util.Optional;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

/**
 * Bersaglio del pannello staff: il giocatore scelto nella schermata staff, altrimenti quello inquadrato. Solo comodità
 * della UI: il server valida sempre il bersaglio di ogni azione.
 */
public final class StaffTarget {
    public record Target(UUID id, String name) {}

    private static volatile Target chosen;

    private StaffTarget() {}

    public static void choose(UUID id, String name) {
        chosen = new Target(id, name);
    }

    public static void reset() {
        chosen = null;
    }

    public static Optional<Target> effective(Minecraft mc) {
        Target c = chosen;
        if (c != null) return Optional.of(c);
        if (mc.crosshairPickEntity instanceof Player p && p != mc.player) {
            return Optional.of(new Target(p.getUUID(), p.getGameProfile().name()));
        }
        return Optional.empty();
    }
}
