package dev.qrm.police.weapon.logic.anim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class RemoteSoundSchedulerTest {
    private static AnimClip clip(AnimEvent... events) {
        return new AnimClip(3.0, false, false, Map.of(), List.of(events));
    }

    private static AnimEvent sound(double t, String id) {
        return new AnimEvent(t, "sound", List.of(id));
    }

    @Test
    void firesSoundsAtTheirTicks() {
        RemoteSoundScheduler s = new RemoteSoundScheduler();
        s.start(7, clip(sound(0.5, "pistol.mag_out"), sound(1.0, "pistol.mag_in")));
        for (int i = 1; i < 10; i++) assertTrue(s.tick().isEmpty(), "tick " + i);
        assertEquals(List.of(new RemoteSoundScheduler.Due(7, "pistol.mag_out")), s.tick());
        for (int i = 11; i < 20; i++) assertTrue(s.tick().isEmpty(), "tick " + i);
        assertEquals(List.of(new RemoteSoundScheduler.Due(7, "pistol.mag_in")), s.tick());
        assertFalse(s.isActive(7));
        assertTrue(s.tick().isEmpty());
    }

    @Test
    void skipsFireAndNonSoundEvents() {
        RemoteSoundScheduler s = new RemoteSoundScheduler();
        s.start(1, clip(sound(0.0, "pistol.fire"), new AnimEvent(0.0, "muzzle_flash", List.of()),
                new AnimEvent(0.0, "sound", List.of())));
        assertFalse(s.isActive(1));
        assertTrue(s.tick().isEmpty());
    }

    @Test
    void cancelStopsPendingSounds() {
        RemoteSoundScheduler s = new RemoteSoundScheduler();
        s.start(1, clip(sound(0.5, "pistol.mag_out")));
        s.start(2, clip(sound(0.5, "pistol.mag_in")));
        s.cancel(1);
        for (int i = 1; i < 10; i++) s.tick();
        assertEquals(List.of(new RemoteSoundScheduler.Due(2, "pistol.mag_in")), s.tick());
    }

    @Test
    void restartReplacesAndClearDropsAll() {
        RemoteSoundScheduler s = new RemoteSoundScheduler();
        s.start(1, clip(sound(0.5, "pistol.mag_out")));
        s.start(1, clip(sound(0.1, "pistol.draw")));
        s.tick();
        assertEquals(List.of(new RemoteSoundScheduler.Due(1, "pistol.draw")), s.tick());
        s.start(3, clip(sound(1.0, "pistol.mag_in")));
        s.clear();
        assertFalse(s.isActive(3));
    }

    @Test
    void nullClipAndSameTickOrdering() {
        RemoteSoundScheduler s = new RemoteSoundScheduler();
        s.start(1, null);
        assertFalse(s.isActive(1));
        s.start(1, clip(sound(0.1, "b"), sound(0.1, "a")));
        s.tick();
        assertEquals(List.of("b", "a"), s.tick().stream().map(RemoteSoundScheduler.Due::soundId).toList());
    }
}
