package dev.qrm.police.weapon.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class PistolTimingsTest {
    private static final Path ANIMATIONS =
            Path.of("src/main/resources/assets/qrm_police/animations/police_pistol.json");

    @Test
    void ilServerTolleraUnTickInMenoDelClient() {
        assertEquals(4, PistolTimings.SERVER_FIRE_COOLDOWN_TICKS);
        assertFalse(PistolTimings.serverCanFire(100, 103));
        assertTrue(PistolTimings.serverCanFire(100, 104));
        // Il client resta a 5: due pressioni a 5 tick arrivate a 4 tick di distanza passano.
        assertFalse(PistolTimings.canFire(100, 104));
    }

    @Test
    void cooldownBoundary() {
        assertFalse(PistolTimings.canFire(100, 104));
        assertTrue(PistolTimings.canFire(100, 105));
        assertTrue(PistolTimings.canFire(100, 106));
        assertEquals(5, PistolTimings.FIRE_COOLDOWN_TICKS);
        assertEquals(6, PistolTimings.HOLD_THRESHOLD_TICKS);
        assertEquals(4, PistolTimings.AIM_TICKS);
    }

    @Test
    void blocksFireOnlyForDrawReloadsAndSlide() {
        Set<PistolAction> blocking = Set.of(PistolAction.DRAW, PistolAction.RELOAD_TACTICAL,
                PistolAction.RELOAD_EMPTY, PistolAction.SLIDE_RELEASE, PistolAction.SLIDE_RACK);
        for (PistolAction a : PistolAction.values()) {
            assertEquals(blocking.contains(a), a.blocksFire(), a.name());
        }
    }

    @Test
    void timingsMatchExportedAnimation() throws IOException {
        JsonObject clips;
        try (Reader r = Files.newBufferedReader(ANIMATIONS)) {
            clips = JsonParser.parseReader(r).getAsJsonObject().getAsJsonObject("animations");
        }
        for (PistolAction a : PistolAction.values()) {
            if (!clips.has(a.clip())) {
                continue; // clip non ancora esportata (Parte F): saltata
            }
            JsonObject clip = clips.getAsJsonObject(a.clip());
            // piccola tolleranza: 0.8 * 20 non deve diventare 17 per un errore di virgola mobile
            int expected = (int) Math.ceil(clip.get("length").getAsDouble() * 20 - 1e-6);
            assertEquals(expected, a.durationTicks(), a.clip() + " durata");

            Set<Map.Entry<Integer, CommitPoint>> exported = new HashSet<>();
            JsonArray events = clip.has("events") ? clip.getAsJsonArray("events") : new JsonArray();
            for (JsonElement e : events) {
                JsonObject ev = e.getAsJsonObject();
                if (!"commit".equals(ev.get("type").getAsString())) {
                    continue;
                }
                String arg = ev.getAsJsonArray("args").get(0).getAsString();
                CommitPoint point = null;
                for (CommitPoint p : CommitPoint.values()) {
                    if (p.arg().equals(arg)) {
                        point = p;
                    }
                }
                assertTrue(point != null, a.clip() + " commit sconosciuto: " + arg);
                exported.add(Map.entry((int) Math.round(ev.get("t").getAsDouble() * 20), point));
            }
            assertEquals(exported, new HashSet<>(a.commits()), a.clip() + " commit");
            assertEquals(a.commits().size(), exported.size(), a.clip() + " commit duplicati");
        }
        // INSPECT_MAG non avrà mai una clip propria (regola R33): non è richiesta.
        assumeTrue(clips.has(PistolAction.SLIDE_RELEASE.clip()) && clips.has(PistolAction.SLIDE_RACK.clip())
                        && clips.has(PistolAction.AMMO_CHECK.clip()),
                "alcune clip non ancora esportate: verificate solo quelle presenti");
    }
}
