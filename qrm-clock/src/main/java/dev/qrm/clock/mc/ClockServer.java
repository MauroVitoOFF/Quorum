package dev.qrm.clock.mc;

import dev.qrm.api.clock.GameTime;
import dev.qrm.clock.logic.ClockRules;
import java.time.ZonedDateTime;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.clock.ServerClockManager;
import net.minecraft.world.clock.WorldClock;
import net.minecraft.world.clock.WorldClocks;

/** Dice che ora, che giorno e che stagione e' secondo il calendario reale. Non tocca il sole del mondo. */
public final class ClockServer {
    private ClockServer() {}

    /** Data, ora e stagione in questo momento. */
    static GameTime now() {
        ZonedDateTime real = ZonedDateTime.now(ClockConfig.zone());
        return new GameTime(real.toLocalDate(), real.getHour(), real.getMinute(),
                ClockRules.seasonOf(real.toLocalDate(), ClockConfig.SOUTHERN_HEMISPHERE.get()));
    }

    /**
     * Le versioni 0.37 di questo modulo allungavano il giorno cambiando il ritmo dell'orologio del mondo, e il ritmo
     * resta salvato nel mondo: all'avvio lo si riporta a quello normale di Minecraft.
     */
    static void restoreVanillaRate(MinecraftServer server) {
        ServerClockManager clocks = server.overworld().clockManager();
        Holder<WorldClock> clock = server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK)
                .getOrThrow(WorldClocks.OVERWORLD);
        if (clocks.getRate(clock) != 1.0F) clocks.setRate(clock, 1.0F);
    }
}
