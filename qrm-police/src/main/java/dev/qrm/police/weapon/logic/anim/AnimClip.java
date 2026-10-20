package dev.qrm.police.weapon.logic.anim;

import java.util.List;
import java.util.Map;

/** Clip di animazione {@code qrm.anim/2}: durata, cicli, canali per osso ed eventi. */
public record AnimClip(double length, boolean loop, boolean holdLast, Map<String, Bone> bones, List<AnimEvent> events) {

    /** Canali di un osso; un canale assente è {@code null} (vale il riposo). */
    public record Bone(AnimChannel position, AnimChannel rotation, AnimChannel scale) {}

    public AnimClip {
        bones = Map.copyOf(bones);
        events = List.copyOf(events);
    }
}
