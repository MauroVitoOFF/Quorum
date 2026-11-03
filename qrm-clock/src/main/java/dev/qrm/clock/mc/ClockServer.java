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

/** Tiene il sole e la luna del mondo allineati al tempo di gioco, e dice che ora e'. */
public final class ClockServer {
    private static final int SYNC_EVERY_TICKS = 20;
    /** Sotto questo scarto (in tick di gioco) non si corregge: la deriva tra due controlli e' molto minore. */
    private static final int DRIFT_TICKS = 10;

    private ClockServer() {}

    /** Data, ora e stagione di gioco in questo momento. */
    static GameTime now() {
        ZonedDateTime real = ZonedDateTime.now(ClockConfig.zone());
        int tick = ClockRules.tickOfDay(millisOfDay(real), ClockConfig.DAY_MINUTES.get());
        return new GameTime(real.toLocalDate(), ClockRules.hourOf(tick), ClockRules.minuteOf(tick),
                ClockRules.seasonOf(real.toLocalDate(), ClockConfig.SOUTHERN_HEMISPHERE.get()));
    }

    private static long millisOfDay(ZonedDateTime t) {
        return t.toLocalTime().toNanoOfDay() / 1_000_000L;
    }

    static void tick(MinecraftServer server) {
        if (server.getTickCount() % SYNC_EVERY_TICKS != 0 || !ClockConfig.CONTROL_WORLD_TIME.get()) return;
        ServerClockManager clocks = server.overworld().clockManager();
        Holder<WorldClock> clock = server.registryAccess().lookupOrThrow(Registries.WORLD_CLOCK)
                .getOrThrow(WorldClocks.OVERWORLD);

        int dayMinutes = ClockConfig.DAY_MINUTES.get();
        float rate = ClockRules.rateFor(dayMinutes);
        if (clocks.getRate(clock) != rate) clocks.setRate(clock, rate);

        long total = clocks.getTotalTicks(clock);
        int current = (int) (total % ClockRules.DAY_TICKS);
        int target = ClockRules.tickOfDay(millisOfDay(ZonedDateTime.now(ClockConfig.zone())), dayMinutes);
        int delta = ClockRules.shortestDelta(current, target);
        if (Math.abs(delta) > DRIFT_TICKS) clocks.setTotalTicks(clock, total + delta);
    }
}
